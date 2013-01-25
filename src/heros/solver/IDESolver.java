/*******************************************************************************
 * Copyright (c) 2012 Eric Bodden.
 * Copyright (c) 2013 Tata Consultancy Services & École Polytechnique de Montréal
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 *     Marc-André Laverdière-Papineau - Fixed race condition
 ******************************************************************************/
package heros.solver;


import heros.DontSynchronize;
import heros.EdgeFunction;
import heros.EdgeFunctionCache;
import heros.EdgeFunctions;
import heros.FlowFunction;
import heros.FlowFunctionCache;
import heros.FlowFunctions;
import heros.IDETabulationProblem;
import heros.InterproceduralCFG;
import heros.JoinLattice;
import heros.SynchronizedBy;
import heros.ZeroedFlowFunctions;
import heros.edgefunc.EdgeIdentity;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;


/**
 * Solves the given {@link IDETabulationProblem} as described in the 1996 paper by Sagiv,
 * Horwitz and Reps. To solve the problem, call {@link #solve()}. Results can then be
 * queried by using {@link #resultAt(Object, Object)} and {@link #resultsAt(Object)}.
 * 
 * Note that this solver and its data structures internally use mostly {@link LinkedHashSet}s
 * instead of normal {@link HashSet}s to fix the iteration order as much as possible. This
 * is to produce, as much as possible, reproducible benchmarking results. We have found
 * that the iteration order can matter a lot in terms of speed.
 *
 * @param <N> The type of nodes in the interprocedural control-flow graph. 
 * @param <D> The type of data-flow facts to be computed by the tabulation problem.
 * @param <M> The type of objects used to represent methods.
 * @param <V> The type of values to be computed along flow edges.
 * @param <I> The type of inter-procedural control-flow graph being used.
 */
public class IDESolver<N,D,M,V,I extends InterproceduralCFG<N, M>> {
	
	public static CacheBuilder<Object, Object> DEFAULT_CACHE_BUILDER = CacheBuilder.newBuilder().concurrencyLevel(Runtime.getRuntime().availableProcessors()).initialCapacity(10000).softValues();
	
	public static final boolean DEBUG = !System.getProperty("HEROS_DEBUG", "false").equals("false");
	
	//executor for dispatching individual compute jobs (may be multi-threaded)
	@DontSynchronize("only used by single thread")
	protected ThreadPoolExecutor executor;
	
	@DontSynchronize("only used by single thread")
	protected int numThreads;
	
	//the number of currently running tasks
	protected final AtomicLong numTasks = new AtomicLong();
	
	@SynchronizedBy("thread safe data structure, consistent locking when used")
	protected final JumpFunctions<N,D,V> jumpFn;
	
	@SynchronizedBy("thread safe data structure, only modified internally")
	protected final I icfg;
	
	//stores summaries that were queried before they were computed
	//see CC 2010 paper by Naeem, Lhotak and Rodriguez
	@SynchronizedBy("consistent lock on 'incoming'")
	protected final Table<N,D,Table<N,D,EdgeFunction<V>>> endSummary = HashBasedTable.create();

	//edges going along calls
	//see CC 2010 paper by Naeem, Lhotak and Rodriguez
	@SynchronizedBy("consistent lock on field")
	protected final Table<N,D,Map<N,Set<D>>> incoming = HashBasedTable.create();
	
	@DontSynchronize("stateless")
	protected final FlowFunctions<N, D, M> flowFunctions;

	@DontSynchronize("stateless")
	protected final EdgeFunctions<N,D,M,V> edgeFunctions;

	@DontSynchronize("only used by single thread")
	protected final Set<N> initialSeeds;

	@DontSynchronize("stateless")
	protected final JoinLattice<V> valueLattice;
	
	@DontSynchronize("stateless")
	protected final EdgeFunction<V> allTop;

	@DontSynchronize("only used by single thread - phase II not parallelized (yet)")
	protected final Table<N,D,V> val = HashBasedTable.create();	
	
	@DontSynchronize("benign races")
	public long flowFunctionApplicationCount;

	@DontSynchronize("benign races")
	public long flowFunctionConstructionCount;
	
	@DontSynchronize("benign races")
	public long propagationCount;
	
	@DontSynchronize("benign races")
	public long durationFlowFunctionConstruction;
	
	@DontSynchronize("benign races")
	public long durationFlowFunctionApplication;

	@DontSynchronize("stateless")
	protected final D zeroValue;
	
	@DontSynchronize("readOnly")
	protected final FlowFunctionCache<N,D,M> ffCache; 

	@DontSynchronize("readOnly")
	protected final EdgeFunctionCache<N,D,M,V> efCache;

	@DontSynchronize("readOnly")
	protected final boolean followReturnsPastSeeds;

	/**
	 * Creates a solver for the given problem, which caches flow functions and edge functions.
	 * The solver must then be started by calling {@link #solve()}.
	 */
	public IDESolver(IDETabulationProblem<N,D,M,V,I> tabulationProblem) {
		this(tabulationProblem, true);
	}

	/**
	 * Creates a solver for the given problem, which caches flow functions and edge functions.
	 * The solver must then be started by calling {@link #solve()}.
	 * @param autoAddZero Automatically add ZERO nodes within flow functions.
	 */
	public IDESolver(IDETabulationProblem<N,D,M,V,I> tabulationProblem, boolean autoAddZero) {
		this(tabulationProblem, autoAddZero, DEFAULT_CACHE_BUILDER, DEFAULT_CACHE_BUILDER);
	}

	/**
	 * Creates a solver for the given problem, constructing caches with the given {@link CacheBuilder}. The solver must then be started by calling
	 * {@link #solve()}.
	 * @param autoAddZero Automatically add ZERO nodes within flow functions.
	 * @param flowFunctionCacheBuilder A valid {@link CacheBuilder} or <code>null</code> if no caching is to be used for flow functions.
	 * @param edgeFunctionCacheBuilder A valid {@link CacheBuilder} or <code>null</code> if no caching is to be used for edge functions.
	 */
	public IDESolver(IDETabulationProblem<N,D,M,V,I> tabulationProblem, boolean autoAddZero, @SuppressWarnings("rawtypes") CacheBuilder flowFunctionCacheBuilder, @SuppressWarnings("rawtypes") CacheBuilder edgeFunctionCacheBuilder) {
		if(DEBUG) {
			flowFunctionCacheBuilder = flowFunctionCacheBuilder.recordStats();
			edgeFunctionCacheBuilder = edgeFunctionCacheBuilder.recordStats();
		}
		this.zeroValue = tabulationProblem.zeroValue();
		this.icfg = tabulationProblem.interproceduralCFG();		
		FlowFunctions<N, D, M> flowFunctions = autoAddZero ? new ZeroedFlowFunctions<N,D,M>(tabulationProblem.flowFunctions(), tabulationProblem.zeroValue()) : tabulationProblem.flowFunctions(); 
		EdgeFunctions<N, D, M, V> edgeFunctions = tabulationProblem.edgeFunctions();
		if(flowFunctionCacheBuilder!=null) {
			ffCache = new FlowFunctionCache<N,D,M>(flowFunctions, flowFunctionCacheBuilder);
			flowFunctions = ffCache;
		} else {
			ffCache = null;
		}
		if(edgeFunctionCacheBuilder!=null) {
			efCache = new EdgeFunctionCache<N,D,M,V>(edgeFunctions, edgeFunctionCacheBuilder);
			edgeFunctions = efCache;
		} else {
			efCache = null;
		}
		this.flowFunctions = flowFunctions;
		this.edgeFunctions = edgeFunctions;
		this.initialSeeds = tabulationProblem.initialSeeds();
		this.valueLattice = tabulationProblem.joinLattice();
		this.allTop = tabulationProblem.allTopFunction();
		this.jumpFn = new JumpFunctions<N,D,V>(allTop);
		this.followReturnsPastSeeds = tabulationProblem.followReturnsPastSeeds();
	}

	/**
	 * Runs the solver on the configured problem. This can take some time.
	 * Uses a number of threads equal to the return value of
	 * <code>Runtime.getRuntime().availableProcessors()</code>.
	 */
	public void solve() {
		solve(Runtime.getRuntime().availableProcessors());
	}
	
	/**
	 * Runs the solver on the configured problem. This can take some time.
	 * @param numThreads The number of threads to use.
	 */
	public void solve(int numThreads) {
		this.numThreads = numThreads < 2? 1: numThreads;
		this.executor = new ThreadPoolExecutor(1, numThreads, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		
		for(N startPoint: initialSeeds) {
			propagate(zeroValue, startPoint, zeroValue, allTop);
			scheduleEdgeProcessing(new PathEdge<N,D,M>(zeroValue, startPoint, zeroValue));
			jumpFn.addFunction(zeroValue, startPoint, zeroValue, EdgeIdentity.<V>v());
		}
		{
		  /**
		   * Forward-tabulates the same-level realizable paths and associated functions.
		   * Note that this is a little different from the original IFDS formulations because
		   * we can have statements that are, for instance, both "normal" and "exit" statements.
		   * This is for instance the case on a "throw" statement that may on the one hand
		   * lead to a catch block but on the other hand exit the method depending
		   * on the exception being thrown.
		   */
			final long before = System.currentTimeMillis();
			do { //Wait until we are done the processing
        try {
          Thread.sleep(200);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
			}	while (!executor.getQueue().isEmpty() || numTasks.longValue() > executor.getCompletedTaskCount());
			durationFlowFunctionConstruction = System.currentTimeMillis() - before;
		}
		{
			final long before = System.currentTimeMillis();
			computeValues();
			durationFlowFunctionApplication = System.currentTimeMillis() - before;
		}
		if(DEBUG) 
			printStats();
		
		executor.shutdown();
	}

  /**
   * Dispatch the processing of a given edge. It may be executed in a different thread.
   * @param edge the edge to process
   */
  private void scheduleEdgeProcessing(PathEdge<N,D,M> edge){
    numTasks.getAndIncrement();
    executor.execute(new PathEdgeProcessingTask(edge));
    propagationCount++;
  }
	
  /**
   * Dispatch the processing of a given value. It may be executed in a different thread.
   * @param vpt
   */
  private void scheduleValueProcessing(ValuePropagationTask vpt){
    numTasks.getAndIncrement();
    executor.execute(vpt);
  }
	
	/**
	 * Lines 13-20 of the algorithm; processing a call site in the caller's context.
	 * 
	 * For each possible callee, registers incoming call edges.
	 * Also propagates call-to-return flows and summarized callee flows within the caller. 
	 * 
	 * @param edge an edge whose target node resembles a method call
	 */
	private void processCall(PathEdge<N,D,M> edge) {
		final D d1 = edge.factAtSource();
		final N n = edge.getTarget(); // a call node; line 14...
		final D d2 = edge.factAtTarget();
		EdgeFunction<V> f = jumpFunction(edge);
		List<N> returnSiteNs = icfg.getReturnSitesOfCallAt(n);
		
		//for each possible callee
		Set<M> callees = icfg.getCalleesOfCallAt(n);
		for(M sCalledProcN: callees) { //still line 14
			
			//compute the call-flow function
			FlowFunction<D> function = flowFunctions.getCallFlowFunction(n, sCalledProcN);
			flowFunctionConstructionCount++;
			Set<D> res = function.computeTargets(d2);
			
			//for each callee's start point(s)
			for(N sP: icfg.getStartPointsOf(sCalledProcN)) {					
				//for each result node of the call-flow function
				for(D d3: res) {
					//create initial self-loop
					propagate(d3, sP, d3, EdgeIdentity.<V>v()); //line 15
	
					//register the fact that <sp,d3> has an incoming edge from <n,d2>
					Set<Cell<N, D, EdgeFunction<V>>> endSumm;
					synchronized (incoming) {
						//line 15.1 of Naeem/Lhotak/Rodriguez
						addIncoming(sP,d3,n,d2);
						//line 15.2, copy to avoid concurrent modification exceptions by other threads
						endSumm = new HashSet<Table.Cell<N,D,EdgeFunction<V>>>(endSummary(sP, d3));						
					}
					
					//still line 15.2 of Naeem/Lhotak/Rodriguez
					//for each already-queried exit value <eP,d4> reachable from <sP,d3>,
					//create new caller-side jump functions to the return sites
					//because we have observed a potentially new incoming edge into <sP,d3>
					for(Cell<N, D, EdgeFunction<V>> entry: endSumm) {
						N eP = entry.getRowKey();
						D d4 = entry.getColumnKey();
						EdgeFunction<V> fCalleeSummary = entry.getValue();
						//for each return site
						for(N retSiteN: icfg.getReturnSitesOfCallAt(n)) {
							//compute return-flow function
							FlowFunction<D> retFunction = flowFunctions.getReturnFlowFunction(n, sCalledProcN, eP, retSiteN);
							flowFunctionConstructionCount++;
							//for each target value of the function
							for(D d5: retFunction.computeTargets(d4)) {
								//update the caller-side summary function
								EdgeFunction<V> f4 = edgeFunctions.getCallEdgeFunction(n, d2, sCalledProcN, d3);
								EdgeFunction<V> f5 = edgeFunctions.getReturnEdgeFunction(n, sCalledProcN, eP, d4, retSiteN, d5);
								EdgeFunction<V> fPrime = f4.composeWith(fCalleeSummary).composeWith(f5);							
								for (N returnSiteN : returnSiteNs) {
									propagate(d1, returnSiteN, d3, f.composeWith(fPrime));
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
			flowFunctionConstructionCount++;
			for(D d3: callToReturnFlowFunction.computeTargets(d2)) {
				EdgeFunction<V> edgeFnE = edgeFunctions.getCallToReturnEdgeFunction(n, d2, returnSiteN, d3);
				propagate(d1, returnSiteN, d3, f.composeWith(edgeFnE));
			}
		}
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
	private void processExit(PathEdge<N,D,M> edge) {
		final N n = edge.getTarget(); // an exit node; line 21...
		EdgeFunction<V> f = jumpFunction(edge);
		M methodThatNeedsSummary = icfg.getMethodOf(n);
		
		final D d1 = edge.factAtSource();
		final D d2 = edge.factAtTarget();
		
		//for each of the method's start points
		for(N sP: icfg.getStartPointsOf(methodThatNeedsSummary)) {
			//line 21.1 of Naeem/Lhotak/Rodriguez
			
			//register end-summary
			Set<Entry<N, Set<D>>> inc;
			synchronized (incoming) {
				addEndSummary(sP, d1, n, d2, f);
				//copy to avoid concurrent modification exceptions by other threads
				inc = new HashSet<Map.Entry<N,Set<D>>>(incoming(d1, sP));
			}
			
			//for each incoming call edge already processed
			//(see processCall(..))
			for (Entry<N,Set<D>> entry: inc) {
				//line 22
				N c = entry.getKey();
				//for each return site
				for(N retSiteC: icfg.getReturnSitesOfCallAt(c)) {
					//compute return-flow function
					FlowFunction<D> retFunction = flowFunctions.getReturnFlowFunction(c, methodThatNeedsSummary,n,retSiteC);
					flowFunctionConstructionCount++;
					Set<D> targets = retFunction.computeTargets(d2);
					//for each incoming-call value
					for(D d4: entry.getValue()) {
						//for each target value at the return site
						//line 23
						for(D d5: targets) {
							//compute composed function
							EdgeFunction<V> f4 = edgeFunctions.getCallEdgeFunction(c, d4, icfg.getMethodOf(n), d1);
							EdgeFunction<V> f5 = edgeFunctions.getReturnEdgeFunction(c, icfg.getMethodOf(n), n, d2, retSiteC, d5);
							EdgeFunction<V> fPrime = f4.composeWith(f).composeWith(f5);
							//for each jump function coming into the call, propagate to return site using the composed function
							for(Map.Entry<D,EdgeFunction<V>> valAndFunc: jumpFn.reverseLookup(c,d4).entrySet()) {
								EdgeFunction<V> f3 = valAndFunc.getValue();
								if(!f3.equalTo(allTop)); {
									D d3 = valAndFunc.getKey();
									propagate(d3, retSiteC, d5, f3.composeWith(fPrime));
								}
							}
						}
					}
				}
			}
			
			//handling for unbalanced problems where we return out of a method whose call was never processed
			if(inc.isEmpty() && followReturnsPastSeeds) {
				Set<N> callers = icfg.getCallersOf(methodThatNeedsSummary);
				for(N c: callers) {
					for(N retSiteC: icfg.getReturnSitesOfCallAt(c)) {
						FlowFunction<D> retFunction = flowFunctions.getReturnFlowFunction(c, methodThatNeedsSummary,n,retSiteC);
						flowFunctionConstructionCount++;
						Set<D> targets = retFunction.computeTargets(d2);
						for(D d5: targets) {
							EdgeFunction<V> f5 = edgeFunctions.getReturnEdgeFunction(c, icfg.getMethodOf(n), n, d2, retSiteC, d5);
							propagate(d2, retSiteC, d5, f.composeWith(f5));
						}
					}
				}
				if(callers.isEmpty()) {
					FlowFunction<D> normalFlowFunction = flowFunctions.getNormalFlowFunction(n,n);
					flowFunctionConstructionCount++;
					normalFlowFunction.computeTargets(d2);
				}
			}
		}
	}
	
	/**
	 * Lines 33-37 of the algorithm.
	 * Simply propagate normal, intra-procedural flows.
	 * @param edge
	 */
	private void processNormalFlow(PathEdge<N,D,M> edge) {
		final D d1 = edge.factAtSource();
		final N n = edge.getTarget(); 
		final D d2 = edge.factAtTarget();
		EdgeFunction<V> f = jumpFunction(edge);
		for (N m : icfg.getSuccsOf(n)) {
			FlowFunction<D> flowFunction = flowFunctions.getNormalFlowFunction(n,m);
			flowFunctionConstructionCount++;
			Set<D> res = flowFunction.computeTargets(d2);
			for (D d3 : res) {
				EdgeFunction<V> fprime = f.composeWith(edgeFunctions.getNormalEdgeFunction(n, d2, m, d3));
				propagate(d1, m, d3, fprime); 
			}
		}
	}
	
	private void propagate(D sourceVal, N target, D targetVal, EdgeFunction<V> f) {
		EdgeFunction<V> jumpFnE;
		EdgeFunction<V> fPrime;
		boolean newFunction;
		synchronized (jumpFn) {
			jumpFnE = jumpFn.reverseLookup(target, targetVal).get(sourceVal);
			if(jumpFnE==null) jumpFnE = allTop; //JumpFn is initialized to all-top (see line [2] in SRH96 paper)
			fPrime = jumpFnE.joinWith(f);
			newFunction = !fPrime.equalTo(jumpFnE);
			if(newFunction) {
				jumpFn.addFunction(sourceVal, target, targetVal, fPrime);
			}
		}

		if(newFunction) {
			PathEdge<N,D,M> edge = new PathEdge<N,D,M>(sourceVal, target, targetVal);
	    scheduleEdgeProcessing(edge);

			if(DEBUG) {
				if(targetVal!=zeroValue) {			
					StringBuilder result = new StringBuilder();
					result.append("EDGE:  <");
					result.append(icfg.getMethodOf(target));
					result.append(",");
					result.append(sourceVal);
					result.append("> -> <");
					result.append(target);
					result.append(",");
					result.append(targetVal);
					result.append("> - ");
					result.append(fPrime);
					System.err.println(result.toString());
				}
			}
		}
	}
	
	/**
	 * Computes the final values for edge functions.
	 */
	private void computeValues() {	
		//Phase II(i)
		for(N startPoint: initialSeeds) {
			setVal(startPoint, zeroValue, valueLattice.bottomElement());
			Pair<N, D> superGraphNode = new Pair<N,D>(startPoint, zeroValue); 
			scheduleValueProcessing(new ValuePropagationTask(superGraphNode));
		}
		
    do { //Wait until we are done the processing
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    } while (!executor.getQueue().isEmpty() || numTasks.longValue() > executor.getCompletedTaskCount());
		
		//Phase II(ii)
		//we create an array of all nodes and then dispatch fractions of this array to multiple threads
		Set<N> allNonCallStartNodes = icfg.allNonCallStartNodes();
		@SuppressWarnings("unchecked")
		N[] nonCallStartNodesArray = (N[]) new Object[allNonCallStartNodes.size()];
		int i=0;
		for (N n : allNonCallStartNodes) {
			nonCallStartNodesArray[i] = n;
			i++;
		}
		//No need to keep track of the number of tasks scheduled here, since we call shutdown
		for(int t=0;t<numThreads; t++) {
			executor.execute(new ValueComputationTask(nonCallStartNodesArray, t));
		}
		//wait until done
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private void propagateValueAtStart(Pair<N, D> nAndD, N n) {
		D d = nAndD.getO2();		
		M p = icfg.getMethodOf(n);
		for(N c: icfg.getCallsFromWithin(p)) {					
			Set<Entry<D, EdgeFunction<V>>> entries; 
			synchronized (jumpFn) {
				entries = jumpFn.forwardLookup(d,c).entrySet();
				for(Map.Entry<D,EdgeFunction<V>> dPAndFP: entries) {
					D dPrime = dPAndFP.getKey();
					EdgeFunction<V> fPrime = dPAndFP.getValue();
					N sP = n;
					propagateValue(c,dPrime,fPrime.computeTarget(val(sP,d)));
					flowFunctionApplicationCount++;
				}
			}
		}
	}
	
	private void propagateValueAtCall(Pair<N, D> nAndD, N n) {
		D d = nAndD.getO2();
		for(M q: icfg.getCalleesOfCallAt(n)) {
			FlowFunction<D> callFlowFunction = flowFunctions.getCallFlowFunction(n, q);
			flowFunctionConstructionCount++;
			for(D dPrime: callFlowFunction.computeTargets(d)) {
				EdgeFunction<V> edgeFn = edgeFunctions.getCallEdgeFunction(n, d, q, dPrime);
				for(N startPoint: icfg.getStartPointsOf(q)) {
					propagateValue(startPoint,dPrime, edgeFn.computeTarget(val(n,d)));
					flowFunctionApplicationCount++;
				}
			}
		}
	}
	
	private void propagateValue(N nHashN, D nHashD, V v) {
		synchronized (val) {
			V valNHash = val(nHashN, nHashD);
			V vPrime = valueLattice.join(valNHash,v);
			if(!vPrime.equals(valNHash)) {
				setVal(nHashN, nHashD, vPrime);
	      scheduleValueProcessing(new ValuePropagationTask(new Pair<N,D>(nHashN,nHashD)));
			}
		}
	}

	private V val(N nHashN, D nHashD){ 
		V l = val.get(nHashN, nHashD);
		if(l==null) return valueLattice.topElement(); //implicitly initialized to top; see line [1] of Fig. 7 in SRH96 paper
		else return l;
	}
	
	private void setVal(N nHashN, D nHashD,V l){ 
		val.put(nHashN, nHashD,l);
		if(DEBUG)
			System.err.println("VALUE: "+icfg.getMethodOf(nHashN)+" "+nHashN+" "+nHashD+ " " + l);
	}

	private EdgeFunction<V> jumpFunction(PathEdge<N, D, M> edge) {
		synchronized (jumpFn) {
			EdgeFunction<V> function = jumpFn.forwardLookup(edge.factAtSource(), edge.getTarget()).get(edge.factAtTarget());
			if(function==null) return allTop; //JumpFn initialized to all-top, see line [2] in SRH96 paper
			return function;
		}
	}

	private Set<Cell<N, D, EdgeFunction<V>>> endSummary(N sP, D d3) {
		Table<N, D, EdgeFunction<V>> map = endSummary.get(sP, d3);
		if(map==null) return Collections.emptySet();
		return map.cellSet();
	}

	private void addEndSummary(N sP, D d1, N eP, D d2, EdgeFunction<V> f) {
		Table<N, D, EdgeFunction<V>> summaries = endSummary.get(sP, d1);
		if(summaries==null) {
			summaries = HashBasedTable.create();
			endSummary.put(sP, d1, summaries);
		}
		//note: at this point we don't need to join with a potential previous f
		//because f is a jump function, which is already properly joined
		//within propagate(..)
		summaries.put(eP,d2,f);
	}	
	
	private Set<Entry<N, Set<D>>> incoming(D d1, N sP) {
		Map<N, Set<D>> map = incoming.get(sP, d1);
		if(map==null) return Collections.emptySet();
		return map.entrySet();		
	}
	
	private void addIncoming(N sP, D d3, N n, D d2) {
		Map<N, Set<D>> summaries = incoming.get(sP, d3);
		if(summaries==null) {
			summaries = new HashMap<N, Set<D>>();
			incoming.put(sP, d3, summaries);
		}
		Set<D> set = summaries.get(n);
		if(set==null) {
			set = new HashSet<D>();
			summaries.put(n,set);
		}
		set.add(d2);
	}	
	
	/**
	 * Returns the V-type result for the given value at the given statement. 
	 */
	public V resultAt(N stmt, D value) {
		return val.get(stmt, value);
	}
	
	/**
	 * Returns the resulting environment for the given statement.
	 * The artificial zero value is automatically stripped.
	 */
	public Map<D,V> resultsAt(N stmt) {
		//filter out the artificial zero-value
		return Maps.filterKeys(val.row(stmt), new Predicate<D>() {

			public boolean apply(D val) {
				return val!=zeroValue;
			}
		});
	}

	public void printStats() {
		if(DEBUG) {
			if(ffCache!=null)
				ffCache.printStats();
			if(efCache!=null)
				efCache.printStats();
		} else {
			System.err.println("No statistics were collected, as DEBUG is disabled.");
		}
	}
	
	private class PathEdgeProcessingTask implements Runnable {
		private final PathEdge<N, D, M> edge;

		public PathEdgeProcessingTask(PathEdge<N, D, M> edge) {
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
	
	private class ValuePropagationTask implements Runnable {
		private final Pair<N, D> nAndD;

		public ValuePropagationTask(Pair<N,D> nAndD) {
			this.nAndD = nAndD;
		}

		public void run() {
			N n = nAndD.getO1();
			if(icfg.isStartPoint(n)) {
				propagateValueAtStart(nAndD, n);
			}
			if(icfg.isCallStmt(n)) {
				propagateValueAtCall(nAndD, n);
			}
		}
	}
	
	private class ValueComputationTask implements Runnable {
		private final N[] values;
		final int num;

		public ValueComputationTask(N[] values, int num) {
			this.values = values;
			this.num = num;
		}

		public void run() {
			int sectionSize = (int) Math.floor(values.length / numThreads) + numThreads;
			for(int i = sectionSize * num; i < Math.min(sectionSize * (num+1),values.length); i++) {
				N n = values[i];
				for(N sP: icfg.getStartPointsOf(icfg.getMethodOf(n))) {					
					Set<Cell<D, D, EdgeFunction<V>>> lookupByTarget;
					lookupByTarget = jumpFn.lookupByTarget(n);
					for(Cell<D, D, EdgeFunction<V>> sourceValTargetValAndFunction : lookupByTarget) {
						D dPrime = sourceValTargetValAndFunction.getRowKey();
						D d = sourceValTargetValAndFunction.getColumnKey();
						EdgeFunction<V> fPrime = sourceValTargetValAndFunction.getValue();
						synchronized (val) {
							setVal(n,d,valueLattice.join(val(n,d),fPrime.computeTarget(val(sP,dPrime))));
						}
						flowFunctionApplicationCount++;
					}
				}
			}
		}
	}

}
