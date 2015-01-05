/*******************************************************************************
 * Copyright (c) 2014 Johannes Lerch, Johannes Späth.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch, Johannes Späth - initial API and implementation
 ******************************************************************************/
package heros.alias;

import static heros.alias.AccessPathUtil.cloneWithConcatenatedAccessPath;
import heros.DontSynchronize;
import heros.FlowFunctionCache;
import heros.InterproceduralCFG;
import heros.SynchronizedBy;
import heros.alias.FieldReference.Any;
import heros.alias.FieldReference.SpecificFieldReference;
import heros.alias.FlowFunction.AnnotatedFact;
import heros.solver.CountingThreadPoolExecutor;
import heros.solver.IFDSSolver;
import heros.solver.PathEdge;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;

public class FieldSensitiveIFDSSolver<N, BaseValue, D extends FieldSensitiveFact<BaseValue, D>, M, I extends InterproceduralCFG<N, M>> {


	public static CacheBuilder<Object, Object> DEFAULT_CACHE_BUILDER = CacheBuilder.newBuilder().concurrencyLevel
			(Runtime.getRuntime().availableProcessors()).initialCapacity(10000).softValues();
	
    protected static final Logger logger = LoggerFactory.getLogger(FieldSensitiveIFDSSolver.class);

    //enable with -Dorg.slf4j.simpleLogger.defaultLogLevel=trace
    public static final boolean DEBUG = logger.isDebugEnabled();

	protected CountingThreadPoolExecutor executor;
	
	@DontSynchronize("only used by single thread")
	protected int numThreads;
	
	@SynchronizedBy("thread safe data structure, consistent locking when used")
	protected final JumpFunctions<N,D> jumpFn;
	
	@SynchronizedBy("thread safe data structure, only modified internally")
	protected final I icfg;
	
	//stores summaries that were queried before they were computed
	//see CC 2010 paper by Naeem, Lhotak and Rodriguez
	@SynchronizedBy("consistent lock on 'incoming'")
	protected final MyConcurrentHashMap<M,Set<SummaryEdge<D, N>>> endSummary =
			new MyConcurrentHashMap<M, Set<SummaryEdge<D, N>>>();
	
	//edges going along calls
	//see CC 2010 paper by Naeem, Lhotak and Rodriguez
	@SynchronizedBy("consistent lock on field")
	protected final MyConcurrentHashMap<M, Set<IncomingEdge<D, N>>> incoming =
			new MyConcurrentHashMap<M, Set<IncomingEdge<D, N>>>();
	
	protected final MyConcurrentHashMap<M, Set<PathEdge<N,D>>> pausedEdges = new MyConcurrentHashMap<M, Set<PathEdge<N,D>>>();
	
	@DontSynchronize("stateless")
	protected final FlowFunctions<N, D, M> flowFunctions;
	
	@DontSynchronize("only used by single thread")
	protected final Map<N,Set<D>> initialSeeds;
	
	@DontSynchronize("benign races")
	public long propagationCount;
	
	@DontSynchronize("stateless")
	protected final D zeroValue;
	
	@DontSynchronize("readOnly")
	protected final FlowFunctionCache<N,D,M> ffCache = null; 
	
	@DontSynchronize("readOnly")
	protected final boolean followReturnsPastSeeds;
	
	
	/**
	 * Creates a solver for the given problem, which caches flow functions and edge functions.
	 * The solver must then be started by calling {@link #solve()}.
	 */
	public FieldSensitiveIFDSSolver(IFDSTabulationProblem<N,D,M,I> tabulationProblem) {
		this(tabulationProblem, DEFAULT_CACHE_BUILDER);
	}

	/**
	 * Creates a solver for the given problem, constructing caches with the given {@link CacheBuilder}. The solver must then be started by calling
	 * {@link #solve()}.
	 * @param flowFunctionCacheBuilder A valid {@link CacheBuilder} or <code>null</code> if no caching is to be used for flow functions.
	 * @param edgeFunctionCacheBuilder A valid {@link CacheBuilder} or <code>null</code> if no caching is to be used for edge functions.
	 */
	public FieldSensitiveIFDSSolver(IFDSTabulationProblem<N,D,M,I> tabulationProblem, @SuppressWarnings("rawtypes") CacheBuilder flowFunctionCacheBuilder) {
		if(logger.isDebugEnabled())
			flowFunctionCacheBuilder = flowFunctionCacheBuilder.recordStats();
		this.zeroValue = tabulationProblem.zeroValue();
		this.icfg = tabulationProblem.interproceduralCFG();		
	/*	FlowFunctions<N, D, M> flowFunctions = tabulationProblem.autoAddZero() ?
				new ZeroedFlowFunctions<N,D,M>(tabulationProblem.flowFunctions(), zeroValue) : tabulationProblem.flowFunctions();*/ 
		FlowFunctions<N, D, M> flowFunctions = tabulationProblem.flowFunctions(); 
		/*if(flowFunctionCacheBuilder!=null) {
			ffCache = new FlowFunctionCache<N,D,M>(flowFunctions, flowFunctionCacheBuilder);
			flowFunctions = ffCache;
		} else {
			ffCache = null;
		}*/
		this.flowFunctions = flowFunctions;
		this.initialSeeds = tabulationProblem.initialSeeds();
		this.jumpFn = new JumpFunctions<N,D>();
		this.followReturnsPastSeeds = tabulationProblem.followReturnsPastSeeds();
		this.numThreads = Math.max(1,tabulationProblem.numThreads());
		this.executor = getExecutor();
	}

	/**
	 * Runs the solver on the configured problem. This can take some time.
	 */
	public void solve() {		
		submitInitialSeeds();
		awaitCompletionComputeValuesAndShutdown();
	}

	/**
	 * Schedules the processing of initial seeds, initiating the analysis.
	 * Clients should only call this methods if performing synchronization on
	 * their own. Normally, {@link #solve()} should be called instead.
	 */
	protected void submitInitialSeeds() {
		for(Entry<N, Set<D>> seed: initialSeeds.entrySet()) {
			N startPoint = seed.getKey();
			for(D val: seed.getValue())
				propagate(zeroValue, startPoint, val, null, false);
			jumpFn.addFunction(new PathEdge<N, D>(zeroValue, startPoint, zeroValue));
		}
	}

	/**
	 * Awaits the completion of the exploded super graph. When complete, computes result values,
	 * shuts down the executor and returns.
	 */
	protected void awaitCompletionComputeValuesAndShutdown() {
		{
			//run executor and await termination of tasks
			runExecutorAndAwaitCompletion();
		}
		if(logger.isDebugEnabled())
			printStats();

		//ask executor to shut down;
		//this will cause new submissions to the executor to be rejected,
		//but at this point all tasks should have completed anyway
		executor.shutdown();
		//similarly here: we await termination, but this should happen instantaneously,
		//as all tasks should have completed
		runExecutorAndAwaitCompletion();
	}

	/**
	 * Runs execution, re-throwing exceptions that might be thrown during its execution.
	 */
	private void runExecutorAndAwaitCompletion() {
		try {
			executor.awaitCompletion();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Throwable exception = executor.getException();
		if(exception!=null) {
			throw new RuntimeException("There were exceptions during IFDS analysis. Exiting.",exception);
		}
	}

    /**
     * Dispatch the processing of a given edge. It may be executed in a different thread.
     * @param edge the edge to process
     */
    protected void scheduleEdgeProcessing(PathEdge<N,D> edge){
    	// If the executor has been killed, there is little point
    	// in submitting new tasks
    	if (executor.isTerminating())
    		return;
    	executor.execute(new PathEdgeProcessingTask(edge));
    	propagationCount++;
    }
	
	/**
	 * Lines 13-20 of the algorithm; processing a call site in the caller's context.
	 * 
	 * For each possible callee, registers incoming call edges.
	 * Also propagates call-to-return flows and summarized callee flows within the caller. 
	 * 
	 * @param edge an edge whose target node resembles a method call
	 */
	private void processCall(PathEdge<N,D> edge) {
		final D d1 = edge.factAtSource();
		final N n = edge.getTarget(); // a call node; line 14...

        logger.trace("Processing call to {}", n);

		final D d2 = edge.factAtTarget();
		assert d2 != null;
		Collection<N> returnSiteNs = icfg.getReturnSitesOfCallAt(n);
		
		//for each possible callee
		Collection<M> callees = icfg.getCalleesOfCallAt(n);
		for(M sCalledProcN: callees) { //still line 14
			//compute the call-flow function
			FlowFunction<D> function = flowFunctions.getCallFlowFunction(n, sCalledProcN);
			Set<AnnotatedFact<D>> res = computeCallFlowFunction(function, d1, d2);
			
			Collection<N> startPointsOf = icfg.getStartPointsOf(sCalledProcN);
			//for each result node of the call-flow function
			for(AnnotatedFact<D> d3: res) {
				//for each callee's start point(s)
				for(N sP: startPointsOf) {
					//create initial self-loop
					D abstractStartPointFact = d3.getFact().cloneWithAccessPath();
					propagate(abstractStartPointFact, sP, abstractStartPointFact, n, false); //line 15
				}
				
				//register the fact that <sp,d3> has an incoming edge from <n,d2>
				//line 15.1 of Naeem/Lhotak/Rodriguez
				IncomingEdge<D, N> incomingEdge = new IncomingEdge<D, N>(d3.getFact(),n,d1,d2);
				if (!addIncoming(sCalledProcN, incomingEdge))
					continue;
				
				resumeEdges(sCalledProcN, d3.getFact());
				registerInterestedCaller(sCalledProcN, incomingEdge);
				
				
				//line 15.2
				Set<SummaryEdge<D, N>> endSumm = endSummary(sCalledProcN, d3.getFact());
					
				//still line 15.2 of Naeem/Lhotak/Rodriguez
				//for each already-queried exit value <eP,d4> reachable from <sP,d3>,
				//create new caller-side jump functions to the return sites
				//because we have observed a potentially new incoming edge into <sP,d3>
				if (endSumm != null)
					for(SummaryEdge<D, N> summary: endSumm) {
						if(AccessPathUtil.isPrefixOf(summary.getSourceFact(), d3.getFact())) {
							Optional<D> d4 = AccessPathUtil.applyAbstractedSummary(d3.getFact(), summary);
							if(d4.isPresent()) {
								//for each return site
								for(N retSiteN: returnSiteNs) {
									//compute return-flow function
									FlowFunction<D> retFunction = flowFunctions.getReturnFlowFunction(n, sCalledProcN, summary.getTargetStmt(), retSiteN);
									//for each target value of the function
									for(AnnotatedFact<D> d5: computeReturnFlowFunction(retFunction, d4.get(), n)) {
										D d5p_restoredCtx = restoreContextOnReturnedFact(d2, d5.getFact());
										propagate(d1, retSiteN, d5p_restoredCtx, n, false);
									}
								}
							}
						} 
					}
			}
		}
		//line 17-19 of Naeem/Lhotak/Rodriguez		
		//process intra-procedural flows along call-to-return flow functions
		for (N returnSiteN : returnSiteNs) {
			FlowFunction<D> callToReturnFlowFunction = flowFunctions.getCallToReturnFlowFunction(n, returnSiteN);
			for(AnnotatedFact<D> d3: computeCallToReturnFlowFunction(callToReturnFlowFunction, d1, d2))
				propagate(d1, returnSiteN, d3.getFact(), n, false);
		}
	}

	private void resumeEdges(M method, D factAtMethodStartPoint) {
		//TODO: Check for concurrency issues
		Set<PathEdge<N, D>> edges = pausedEdges.get(method);
		if(edges != null) {
			for(PathEdge<N, D> edge : edges) {
				if(AccessPathUtil.isPrefixOf(edge.factAtSource(), factAtMethodStartPoint)) {
					if(edges.remove(edge))  {
						logger.trace("RESUME-EDGE: {}", edge);
						propagate(edge.factAtSource(), edge.getTarget(), edge.factAtTarget(), null, false);
					}
				}
			}
		}
	}
	
	private void registerInterestedCaller(M method, IncomingEdge<D, N> incomingEdge) {
		Set<PathEdge<N, D>> edges = pausedEdges.get(method);
		if(edges != null) {
			for(PathEdge<N, D> edge : edges) {
				if(AccessPathUtil.isPrefixOf(incomingEdge.getCalleeSourceFact(), edge.factAtSource())) {
					logger.trace("RECHECKING-PAUSED-EDGE: {} for new incoming edge {}", edge, incomingEdge);
					
					FieldReference[] accessPathDelta = AccessPathUtil.getAccessPathDelta(
							incomingEdge.getCalleeSourceFact().getAccessPath(), edge.factAtSource().getAccessPath());
				
					if(checkForInterestedCallers(incomingEdge.getCallerSourceFact(), incomingEdge.getCallSite(), accessPathDelta)) {
						propagate(zeroValue.equals(incomingEdge.getCallerSourceFact()) ? zeroValue : AccessPathUtil.cloneWithConcatenatedAccessPath(incomingEdge.getCallerSourceFact(), accessPathDelta), 
								incomingEdge.getCallSite(), AccessPathUtil.cloneWithConcatenatedAccessPath(incomingEdge.getCallerCallSiteFact(), accessPathDelta), null, false);
					} else {
						pauseEdge(AccessPathUtil.cloneWithConcatenatedAccessPath(incomingEdge.getCallerSourceFact(), accessPathDelta), 
								incomingEdge.getCallSite(), AccessPathUtil.cloneWithConcatenatedAccessPath(incomingEdge.getCallerCallSiteFact(), accessPathDelta));
					}
				}
			}
		}
	}

	/**
	 * Computes the call flow function for the given call-site abstraction
	 * @param callFlowFunction The call flow function to compute
	 * @param d1 The abstraction at the current method's start node.
	 * @param d2 The abstraction at the call site
	 * @return The set of caller-side abstractions at the callee's start node
	 */
	protected Set<AnnotatedFact<D>> computeCallFlowFunction
			(FlowFunction<D> callFlowFunction, D d1, D d2) {
		return callFlowFunction.computeTargets(d2);
	}

	/**
	 * Computes the call-to-return flow function for the given call-site
	 * abstraction
	 * @param callToReturnFlowFunction The call-to-return flow function to
	 * compute
	 * @param d1 The abstraction at the current method's start node.
	 * @param d2 The abstraction at the call site
	 * @return The set of caller-side abstractions at the return site
	 */
	protected Set<AnnotatedFact<D>> computeCallToReturnFlowFunction
			(FlowFunction<D> callToReturnFlowFunction, D d1, D d2) {
		return callToReturnFlowFunction.computeTargets(d2);
	}
	
	/**
	 * Lines 21-32 of the algorithm.
	 * 
	 * Stores callee-side summaries.
	 * Also, at the side of the caller, propagates intra-procedural flows to return sites
	 * using those newly computed summaries.
	 * 
	 * @param edge an edge whose target node resembles a method exits
	 */
	protected void processExit(PathEdge<N,D> edge) {
		final N n = edge.getTarget(); // an exit node; line 21...
		M methodThatNeedsSummary = icfg.getMethodOf(n);
		
		final D d1 = edge.factAtSource();
		final D d2 = edge.factAtTarget();
		
		//for each of the method's start points, determine incoming calls
		
		//line 21.1 of Naeem/Lhotak/Rodriguez
		//register end-summary
		SummaryEdge<D, N> summaryEdge = new SummaryEdge<D, N>(d1, n, d2);
		if (!addEndSummary(methodThatNeedsSummary, summaryEdge))
			return;
		
		//for each incoming call edge already processed
		//(see processCall(..))
		for (IncomingEdge<D, N> incomingEdge : incoming(methodThatNeedsSummary)) {
			// line 22
			N callSite = incomingEdge.getCallSite();
			// for each return site
			for (N retSiteC : icfg.getReturnSitesOfCallAt(callSite)) {
				// compute return-flow function
				FlowFunction<D> retFunction = flowFunctions.getReturnFlowFunction(callSite, methodThatNeedsSummary, n, retSiteC);
				
				if(AccessPathUtil.isPrefixOf(d1, incomingEdge.getCalleeSourceFact())) {
					Optional<D> concreteCalleeExitFact = AccessPathUtil.applyAbstractedSummary(incomingEdge.getCalleeSourceFact(), summaryEdge);
					if(concreteCalleeExitFact.isPresent()) {
						Set<AnnotatedFact<D>> callerTargetFacts = computeReturnFlowFunction(retFunction, concreteCalleeExitFact.get(), callSite);
	
						// for each incoming-call value
						for (AnnotatedFact<D> callerTargetAnnotatedFact : callerTargetFacts) {
							D callerTargetFact = restoreContextOnReturnedFact(incomingEdge.getCallerCallSiteFact(), callerTargetAnnotatedFact.getFact());
							propagate(incomingEdge.getCallerSourceFact(), retSiteC, callerTargetFact, callSite, false);
						}
					}
				}
			}
		}
		
		
		//handling for unbalanced problems where we return out of a method with a fact for which we have no incoming flow
		//note: we propagate that way only values that originate from ZERO, as conditionally generated values should only
		//be propagated into callers that have an incoming edge for this condition
		if(followReturnsPastSeeds && d1 == zeroValue && incoming(methodThatNeedsSummary).isEmpty()) {
			Collection<N> callers = icfg.getCallersOf(methodThatNeedsSummary);
			for(N c: callers) {
				for(N retSiteC: icfg.getReturnSitesOfCallAt(c)) {
					FlowFunction<D> retFunction = flowFunctions.getReturnFlowFunction(c, methodThatNeedsSummary,n,retSiteC);
					Set<AnnotatedFact<D>> targets = computeReturnFlowFunction(retFunction, d2, c);
					for(AnnotatedFact<D> d5: targets)
						propagate(zeroValue, retSiteC, d5.getFact(), c, true);
				}
			}
			//in cases where there are no callers, the return statement would normally not be processed at all;
			//this might be undesirable if the flow function has a side effect such as registering a taint;
			//instead we thus call the return flow function will a null caller
			if(callers.isEmpty()) {
				FlowFunction<D> retFunction = flowFunctions.getReturnFlowFunction(null, methodThatNeedsSummary,n,null);
				retFunction.computeTargets(d2);
			}
		}
	}
	
	/**
	 * Computes the return flow function for the given set of caller-side
	 * abstractions.
	 * @param retFunction The return flow function to compute
	 * @param d2 The abstraction at the exit node in the callee
	 * @param callSite The call site
	 * @return The set of caller-side abstractions at the return site
	 */
	protected Set<AnnotatedFact<D>> computeReturnFlowFunction
			(FlowFunction<D> retFunction, D d2, N callSite) {
		return retFunction.computeTargets(d2);
	}

	/**
	 * Lines 33-37 of the algorithm.
	 * Simply propagate normal, intra-procedural flows.
	 * @param edge
	 */
	private void processNormalFlow(PathEdge<N,D> edge) {
		final D d1 = edge.factAtSource();
		final N n = edge.getTarget(); 
		final D d2 = edge.factAtTarget();
		
		for (N m : icfg.getSuccsOf(n)) {
			FlowFunction<D> flowFunction = flowFunctions.getNormalFlowFunction(n,m);
			Set<AnnotatedFact<D>> res = computeNormalFlowFunction(flowFunction, d1, d2);
			for (AnnotatedFact<D> d3 : res) {
				//TODO: double check if concurrency issues may arise
				
				//if reading field f
				// if d1.f element of incoming edges:
				//    create and propagate (d1.f, d2.f)
				// else 
				//	  create and set (d1.f, d2.f) on hold
				//	  create for each incoming edge inc: (inc.call-d1.f, inc.call-d2.f) and put on hold
				
				if(d3.getReadField() != null) {
					SpecificFieldReference fieldRef = new SpecificFieldReference(d3.getReadField());
					D concretizedSourceValue = AccessPathUtil.cloneWithConcatenatedAccessPath(d1, fieldRef);
					if(checkForInterestedCallers(d1, n, fieldRef)) {
						propagate(concretizedSourceValue, m, d3.getFact(), null, false);
					} else {
						pauseEdge(concretizedSourceValue, m, d3.getFact());
					}
				}
				else if(d3.getWrittenField() != null) {
					//TODO: double check if concurrency issues may arise
				
				// if writing field f
				// create edge e = (d1, d2.*\{f})
				// if d2.*\{f} element of incoming edges
				// 		continue with e
				// else 
				//		put e on hold
				// always kill (d1, d2)
					
					Any fieldRef = new Any(d3.getWrittenField());
					if(checkForInterestedCallers(d1, n, fieldRef)) {
						propagate(d1, m, d3.getFact(), null, false);
					} else {
						pauseEdge(d1, m, d3.getFact());
					}
				}
				else
					propagate(d1, m, d3.getFact(), null, false);
			}
		}
	}
	
	private void pauseEdge(D sourceValue, N targetStmt, D targetValue) {
		M method = icfg.getMethodOf(targetStmt);
		Set<PathEdge<N, D>> edges = pausedEdges.putIfAbsentElseGet(method, new ConcurrentHashSet<PathEdge<N,D>>());
		if(edges.add(new PathEdge<N,D>(sourceValue, targetStmt, targetValue))) {
			logger.trace("PAUSED: <{},{}> -> <{},{}>", method, sourceValue, targetStmt, targetValue);
		}
	}

	private boolean checkForInterestedCallers(D calleeSourceFact, N targetStmt, FieldReference... fieldRef) {
		M calleeMethod = icfg.getMethodOf(targetStmt);
		logger.trace("Checking interest at method {} in fact {} with field access {}", calleeMethod, calleeSourceFact, fieldRef);
		
		if(calleeSourceFact.equals(zeroValue))
			return true;
		
		D concretizedSourceValue = AccessPathUtil.cloneWithConcatenatedAccessPath(calleeSourceFact, fieldRef);
		if(!incomingEdgesPrefixedWith(calleeMethod, concretizedSourceValue).isEmpty()) {
			return true;
		}
		
		Set<IncomingEdge<D, N>> inc = incomingEdgesPrefixesOf(calleeMethod, concretizedSourceValue); 
		for (IncomingEdge<D, N> incomingEdge : inc) {
			if(checkForInterestedCallers(incomingEdge.getCallerSourceFact(), incomingEdge.getCallSite(), fieldRef)) {
				propagate(incomingEdge.getCallerSourceFact().equals(zeroValue) ? 
							incomingEdge.getCallerSourceFact() : 
							cloneWithConcatenatedAccessPath(incomingEdge.getCallerSourceFact(), fieldRef), 
						incomingEdge.getCallSite(), 
						cloneWithConcatenatedAccessPath(incomingEdge.getCallerCallSiteFact(), fieldRef), null, false); 
				return true;
			}
			else {
				pauseEdge(cloneWithConcatenatedAccessPath(incomingEdge.getCallerSourceFact(), fieldRef), 
						incomingEdge.getCallSite(), 
						cloneWithConcatenatedAccessPath(incomingEdge.getCallerCallSiteFact(), fieldRef));
			}
		}
		
		return false;
	}
	
	/**
	 * Computes the normal flow function for the given set of start and end
	 * abstractions.
	 * @param flowFunction The normal flow function to compute
	 * @param d1 The abstraction at the method's start node
	 * @param d1 The abstraction at the current node
	 * @return The set of abstractions at the successor node
	 */
	protected Set<AnnotatedFact<D>> computeNormalFlowFunction
			(FlowFunction<D> flowFunction, D d1, D d2) {
		return flowFunction.computeTargets(d2);
	}
	
	/**
	 * This method will be called for each incoming edge and can be used to
	 * transfer knowledge from the calling edge to the returning edge, without
	 * affecting the summary edges at the callee.
	 * 
	 * @param d4
	 *            Fact stored with the incoming edge, i.e., present at the
	 *            caller side
	 * @param d5
	 *            Fact that originally should be propagated to the caller.
	 * @return Fact that will be propagated to the caller.
	 */
	protected D restoreContextOnReturnedFact(D d4, D d5) {
		d5.setCallingContext(d4);
		return d5;
	}
	
	
	/**
	 * Propagates the flow further down the exploded super graph. 
	 * @param sourceVal the source value of the propagated summary edge
	 * @param target the target statement
	 * @param targetVal the target value at the target statement
	 * @param relatedCallSite for call and return flows the related call statement, <code>null</code> otherwise
	 *        (this value is not used within this implementation but may be useful for subclasses of {@link IFDSSolver}) 
	 * @param isUnbalancedReturn <code>true</code> if this edge is propagating an unbalanced return
	 *        (this value is not used within this implementation but may be useful for subclasses of {@link IFDSSolver})
	 */
	protected void propagate(D sourceVal, N target, D targetVal,
			/* deliberately exposed to clients */ N relatedCallSite,
			/* deliberately exposed to clients */ boolean isUnbalancedReturn) {
		final PathEdge<N,D> edge = new PathEdge<N,D>(sourceVal, target, targetVal);
		final D existingVal = jumpFn.addFunction(edge);
		//TODO: Merge d.* and d.*\{x} as d.*
		if (existingVal != null) {
			if (existingVal != targetVal)
				existingVal.addNeighbor(targetVal);
		}
		else {
			scheduleEdgeProcessing(edge);
			if(targetVal!=zeroValue)
				logger.trace("EDGE: <{},{}> -> <{},{}>", icfg.getMethodOf(target), sourceVal, target, targetVal);
		}
	}

	private Set<SummaryEdge<D, N>> endSummary(M m, final D d3) {
		Set<SummaryEdge<D, N>> map = endSummary.get(m);
		if(map == null)
			return null;
		
		return Sets.filter(map, new Predicate<SummaryEdge<D,N>>() {
			@Override
			public boolean apply(SummaryEdge<D, N> edge) {
				return AccessPathUtil.isPrefixOf(edge.getSourceFact(), d3) || AccessPathUtil.isPrefixOf(d3, edge.getSourceFact());
			}
		});
	}

	private boolean addEndSummary(M m, SummaryEdge<D,N> summaryEdge) {
		Set<SummaryEdge<D, N>> summaries = endSummary.putIfAbsentElseGet
				(m, new ConcurrentHashSet<SummaryEdge<D, N>>());
		return summaries.add(summaryEdge);
	}	

	protected Set<IncomingEdge<D, N>> incoming(M m) {
		Set<IncomingEdge<D, N>> result = incoming.get(m);
		if(result == null)
			return Collections.emptySet();
		else
			return result;
	}
	
	protected Set<IncomingEdge<D, N>> incomingEdgesPrefixedWith(M m, final D fact) {
		Set<IncomingEdge<D, N>> result = incoming(m);
		return Sets.filter(result, new Predicate<IncomingEdge<D,N>>() {
			@Override
			public boolean apply(IncomingEdge<D, N> edge) {
				return AccessPathUtil.isPrefixOf(fact, edge.getCalleeSourceFact());
			}
		});
	}
	
	protected Set<IncomingEdge<D, N>> incomingEdgesPrefixesOf(M m, final D fact) {
		Set<IncomingEdge<D, N>> result = incoming(m);
		return Sets.filter(result, new Predicate<IncomingEdge<D,N>>() {
			@Override
			public boolean apply(IncomingEdge<D, N> edge) {
				return AccessPathUtil.isPrefixOf(edge.getCalleeSourceFact(), fact);
			}
		});
	}
	
	protected boolean addIncoming(M m, IncomingEdge<D, N> incomingEdge) {
		logger.trace("Incoming Edge for method {}: {}", m, incomingEdge);
		Set<IncomingEdge<D,N>> set = incoming.putIfAbsentElseGet(m, new ConcurrentHashSet<IncomingEdge<D,N>>());
		return set.add(incomingEdge);
	}
	
	/**
	 * Factory method for this solver's thread-pool executor.
	 */
	protected CountingThreadPoolExecutor getExecutor() {
		return new CountingThreadPoolExecutor(1, this.numThreads, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}
	
	/**
	 * Returns a String used to identify the output of this solver in debug mode.
	 * Subclasses can overwrite this string to distinguish the output from different solvers.
	 */
	protected String getDebugName() {
		return "FAST IFDS SOLVER";
	}

	public void printStats() {
		if(logger.isDebugEnabled()) {
			if(ffCache!=null)
				ffCache.printStats();
		} else {
			logger.info("No statistics were collected, as DEBUG is disabled.");
		}
	}
	
	private class PathEdgeProcessingTask implements Runnable {
		private final PathEdge<N,D> edge;

		public PathEdgeProcessingTask(PathEdge<N,D> edge) {
			this.edge = edge;
		}

		public void run() {
			if(icfg.isCallStmt(edge.getTarget())) {
				processCall(edge);
			} else {
				//note that some statements, such as "throw" may be
				//both an exit statement and a "normal" statement
				if(icfg.isExitStmt(edge.getTarget())) {
					processExit(edge);
				}
				if(!icfg.getSuccsOf(edge.getTarget()).isEmpty()) {
					processNormalFlow(edge);
				}
			}
		}
	}
	

}
