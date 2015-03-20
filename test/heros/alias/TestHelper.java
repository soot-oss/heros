/*******************************************************************************
 * Copyright (c) 2014 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.alias;

import static org.junit.Assert.assertTrue;
import heros.alias.FlowFunction.ConstrainedFact;
import heros.alias.FlowFunction.Constraint;
import heros.alias.IFDSTabulationProblem;
import heros.InterproceduralCFG;
import heros.solver.IFDSSolver;
import heros.solver.Pair;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

public class TestHelper {

	private Multimap<TestMethod, TestStatement> method2startPoint = HashMultimap.create();
	private List<NormalEdge> normalEdges = Lists.newLinkedList();
	private List<CallEdge> callEdges = Lists.newLinkedList();
	private List<Call2ReturnEdge> call2retEdges = Lists.newLinkedList();
	private List<ReturnEdge> returnEdges = Lists.newLinkedList();
	private Map<TestStatement, TestMethod> stmt2method = Maps.newHashMap();
	private Multiset<ExpectedFlowFunction> remainingFlowFunctions = HashMultiset.create();
	private TestDebugger<TestFieldRef, TestFact, TestStatement, TestMethod, InterproceduralCFG<TestStatement, TestMethod>> debugger;

	public TestHelper(TestDebugger<TestFieldRef, TestFact, TestStatement, TestMethod, InterproceduralCFG<TestStatement, TestMethod>> debugger) {
		this.debugger = debugger;
	}

	public MethodHelper method(String methodName, TestStatement[] startingPoints, EdgeBuilder... edgeBuilders) {
		MethodHelper methodHelper = new MethodHelper(new TestMethod(methodName));
		methodHelper.startPoints(startingPoints);
		for(EdgeBuilder edgeBuilder : edgeBuilders){
			methodHelper.edges(edgeBuilder.edges());
		}
		return methodHelper;
	}

	public static TestStatement[] startPoints(String... startingPoints) {
		TestStatement[] result = new TestStatement[startingPoints.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = new TestStatement(startingPoints[i]);
		}
		return result;
	}

	public static EdgeBuilder.NormalStmtBuilder normalStmt(String stmt, ExpectedFlowFunction...flowFunctions) {
		return new EdgeBuilder.NormalStmtBuilder(new TestStatement(stmt), flowFunctions);
	}
	
	public static EdgeBuilder.CallSiteBuilder callSite(String callSite) {
		return new EdgeBuilder.CallSiteBuilder(new TestStatement(callSite));
	}
	
	public static EdgeBuilder.ExitStmtBuilder exitStmt(String exitStmt) {
		return new EdgeBuilder.ExitStmtBuilder(new TestStatement(exitStmt));
	}
	
	public static TestStatement over(String callSite) {
		return new TestStatement(callSite);
	}
	
	public static TestStatement to(String returnSite) {
		return new TestStatement(returnSite);
	}
	
	public static ExpectedFlowFunction kill(String source) {
		return kill(1, source);
	}
	
	public static ExpectedFlowFunction kill(int times, String source) {
		return new ExpectedFlowFunction(times, new TestFact(source)) {
			@Override
			public ConstrainedFact<TestFieldRef, TestFact, TestStatement, TestMethod> apply(TestFact target, AccessPathHandler<TestFieldRef, TestFact, TestStatement, TestMethod> accPathHandler) {
				throw new IllegalStateException();
			}
			
			@Override
			public String transformerString() {
				return "";
			}
		};
	}

	public static AccessPathTransformer readField(final String fieldName) {
		return new AccessPathTransformer() {
			@Override
			public ConstrainedFact<TestFieldRef, TestFact, TestStatement, TestMethod> apply(TestFact target, AccessPathHandler<TestFieldRef, TestFact, TestStatement, TestMethod> accPathHandler) {
				return accPathHandler.read(new TestFieldRef(fieldName)).generate(target);
			}

			@Override
			public String toString() {
				return "read("+fieldName+")";
			}
		};
	}
	
	public static AccessPathTransformer prependField(final String fieldName) {
		return new AccessPathTransformer() {
			@Override
			public ConstrainedFact<TestFieldRef, TestFact, TestStatement, TestMethod> apply(TestFact target, AccessPathHandler<TestFieldRef, TestFact, TestStatement, TestMethod> accPathHandler) {
				return accPathHandler.prepend(new TestFieldRef(fieldName)).generate(target);
			}
			
			@Override
			public String toString() {
				return "prepend("+fieldName+")";
			}
		};
	}
	
	public static AccessPathTransformer overwriteField(final String fieldName) {
		return new AccessPathTransformer() {
			@Override
			public ConstrainedFact<TestFieldRef, TestFact, TestStatement, TestMethod> apply(TestFact target, AccessPathHandler<TestFieldRef, TestFact, TestStatement, TestMethod> accPathHandler) {
				return accPathHandler.overwrite(new TestFieldRef(fieldName)).generate(target);
			}
			
			@Override
			public String toString() {
				return "write("+fieldName+")";
			}
		};
	}
	
	public static ExpectedFlowFunction flow(String source, final AccessPathTransformer transformer, String... targets) {
		return flow(1, source, transformer, targets);
	}
	
	public static ExpectedFlowFunction flow(int times, String source, final AccessPathTransformer transformer, String... targets) {
		TestFact[] targetFacts = new TestFact[targets.length];
		for(int i=0; i<targets.length; i++) {
			targetFacts[i] = new TestFact(targets[i]);
		}
		return new ExpectedFlowFunction(times, new TestFact(source), targetFacts) {
			@Override
			public ConstrainedFact<TestFieldRef, TestFact, TestStatement, TestMethod> apply(TestFact target, AccessPathHandler<TestFieldRef, TestFact, TestStatement, TestMethod> accPathHandler) {
				return transformer.apply(target, accPathHandler);
			}
			
			@Override
			public String transformerString() {
				return transformer.toString();
			}
		};
	}
	
	private static interface AccessPathTransformer {

		ConstrainedFact<TestFieldRef, TestFact, TestStatement, TestMethod> apply(TestFact target, AccessPathHandler<TestFieldRef, TestFact, TestStatement, TestMethod> accPathHandler); 
		
	}
	
	public static ExpectedFlowFunction flow(String source, String... targets) {
		return flow(1, source, targets);
	}
	
	public static ExpectedFlowFunction flow(int times, String source, String... targets) {
		return flow(times, source, new AccessPathTransformer() {
			@Override
			public ConstrainedFact<TestFieldRef, TestFact, TestStatement, TestMethod> apply(TestFact target, AccessPathHandler<TestFieldRef, TestFact, TestStatement, TestMethod> accPathHandler) {
				return accPathHandler.generate(target);
			}
			
			@Override
			public String toString() {
				return "";
			}
			
		}, targets);
	}
	
	public static int times(int times) {
		return times;
	}

	public InterproceduralCFG<TestStatement, TestMethod> buildIcfg() {
		return new InterproceduralCFG<TestStatement, TestMethod>() {

			@Override
			public boolean isStartPoint(TestStatement stmt) {
				return method2startPoint.values().contains(stmt);
			}

			@Override
			public boolean isFallThroughSuccessor(TestStatement stmt, TestStatement succ) {
				throw new IllegalStateException();
			}

			@Override
			public boolean isExitStmt(TestStatement stmt) {
				for(ReturnEdge edge : returnEdges) {
					if(edge.exitStmt.equals(stmt))
						return true;
				}
				return false;
			}

			@Override
			public boolean isCallStmt(final TestStatement stmt) {
				return Iterables.any(callEdges, new Predicate<CallEdge>() {
					@Override
					public boolean apply(CallEdge edge) {
						return edge.callSite.equals(stmt);
					}
				});
			}

			@Override
			public boolean isBranchTarget(TestStatement stmt, TestStatement succ) {
				throw new IllegalStateException();
			}

			@Override
			public List<TestStatement> getSuccsOf(TestStatement n) {
				LinkedList<TestStatement> result = Lists.newLinkedList();
				for (NormalEdge edge : normalEdges) {
					if (edge.includeInCfg && edge.unit.equals(n))
						result.add(edge.succUnit);
				}
				return result;
			}

			@Override
			public List<TestStatement> getPredsOf(TestStatement stmt) {
				LinkedList<TestStatement> result = Lists.newLinkedList();
				for (NormalEdge edge : normalEdges) {
					if (edge.includeInCfg && edge.succUnit.equals(stmt))
						result.add(edge.unit);
				}
				return result;
			}

			@Override
			public Collection<TestStatement> getStartPointsOf(TestMethod m) {
				return method2startPoint.get(m);
			}

			@Override
			public Collection<TestStatement> getReturnSitesOfCallAt(TestStatement n) {
				Set<TestStatement> result = Sets.newHashSet();
				for (Call2ReturnEdge edge : call2retEdges) {
					if (edge.includeInCfg && edge.callSite.equals(n))
						result.add(edge.returnSite);
				}
				for(ReturnEdge edge : returnEdges) {
					if(edge.includeInCfg && edge.callSite.equals(n))
						result.add(edge.returnSite);
				}
				return result;
			}

			@Override
			public TestMethod getMethodOf(TestStatement n) {
				if(stmt2method.containsKey(n))
					return stmt2method.get(n);
				else
					throw new IllegalArgumentException("Statement "+n+" is not defined in any method.");
			}

			@Override
			public Set<TestStatement> getCallsFromWithin(TestMethod m) {
				throw new IllegalStateException();
			}

			@Override
			public Collection<TestStatement> getCallersOf(TestMethod m) {
				Set<TestStatement> result = Sets.newHashSet();
				for (CallEdge edge : callEdges) {
					if (edge.includeInCfg && edge.destinationMethod.equals(m)) {
						result.add(edge.callSite);
					}
				}
				for (ReturnEdge edge : returnEdges) {
					if (edge.includeInCfg && edge.calleeMethod.equals(m)) {
						result.add(edge.callSite);
					}
				}
				return result;
			}

			@Override
			public Collection<TestMethod> getCalleesOfCallAt(TestStatement n) {
				List<TestMethod> result = Lists.newLinkedList();
				for (CallEdge edge : callEdges) {
					if (edge.includeInCfg && edge.callSite.equals(n)) {
						result.add(edge.destinationMethod);
					}
				}
				return result;
			}

			@Override
			public Set<TestStatement> allNonCallStartNodes() {
				throw new IllegalStateException();
			}
		};
	}

	public void assertAllFlowFunctionsUsed() {
		assertTrue("These Flow Functions were expected, but never used: \n" + Joiner.on(",\n").join(remainingFlowFunctions),
				remainingFlowFunctions.isEmpty());
	}

	private void addOrVerifyStmt2Method(TestStatement stmt, TestMethod m) {
		if (stmt2method.containsKey(stmt) && !stmt2method.get(stmt).equals(m)) {
			throw new IllegalArgumentException("Statement " + stmt + " is used in multiple methods: " + m + " and " + stmt2method.get(stmt));
		}
		stmt2method.put(stmt, m);
	}

	public MethodHelper method(TestMethod method) {
		MethodHelper h = new MethodHelper(method);
		return h;
	}

	public class MethodHelper {

		private TestMethod method;

		public MethodHelper(TestMethod method) {
			this.method = method;
		}

		public void edges(Collection<Edge> edges) {
			for(Edge edge : edges) {
				for(ExpectedFlowFunction ff : edge.flowFunctions) {
					if(!remainingFlowFunctions.contains(ff))
						remainingFlowFunctions.add(ff, ff.times);
				}
				
				edge.accept(new EdgeVisitor() {
					@Override
					public void visit(ReturnEdge edge) {
						addOrVerifyStmt2Method(edge.exitStmt, method);
						edge.calleeMethod = method;
						returnEdges.add(edge);
					}
					
					@Override
					public void visit(Call2ReturnEdge edge) {
						addOrVerifyStmt2Method(edge.callSite, method);
						addOrVerifyStmt2Method(edge.returnSite, method);
						call2retEdges.add(edge);
					}
					
					@Override
					public void visit(CallEdge edge) {
						addOrVerifyStmt2Method(edge.callSite, method);
						callEdges.add(edge);
					}
					
					@Override
					public void visit(NormalEdge edge) {
						addOrVerifyStmt2Method(edge.unit, method);
						addOrVerifyStmt2Method(edge.succUnit, method);
						normalEdges.add(edge);
					}
				});
			}
		}

		public void startPoints(TestStatement[] startingPoints) {
			method2startPoint.putAll(method, Lists.newArrayList(startingPoints));
		}
	}
	
	private static String expectedFlowFunctionsToString(ExpectedFlowFunction[] flowFunctions) {
		String result = "";
		for(ExpectedFlowFunction ff : flowFunctions)
			result += ff.source+"->"+Joiner.on(",").join(ff.targets)+ff.transformerString()+", ";
		return result;
	}
	
	public static abstract class ExpectedFlowFunction {

		public final TestFact source;
		public final TestFact[] targets;
		public Edge edge;
		private int times;

		public ExpectedFlowFunction(int times, TestFact source, TestFact... targets) {
			this.times = times;
			this.source = source;
			this.targets = targets;
		}

		@Override
		public String toString() {
			return String.format("%s: %s -> {%s}", edge, source, Joiner.on(",").join(targets));
		}
		
		public abstract String transformerString();

		public abstract FlowFunction.ConstrainedFact<TestFieldRef, TestFact, TestStatement, TestMethod> apply(TestFact target, AccessPathHandler<TestFieldRef, TestFact, TestStatement, TestMethod> accPathHandler);
	}
	
	private static interface EdgeVisitor {
		void visit(NormalEdge edge);
		void visit(CallEdge edge);
		void visit(Call2ReturnEdge edge);
		void visit(ReturnEdge edge);
	}

	public static abstract class Edge {
		public final ExpectedFlowFunction[] flowFunctions;
		public boolean includeInCfg = true;

		public Edge(ExpectedFlowFunction...flowFunctions) {
			this.flowFunctions = flowFunctions;
			for(ExpectedFlowFunction ff : flowFunctions) {
				ff.edge = this;
			}
		}
		
		public abstract void accept(EdgeVisitor visitor);
	}

	public static class NormalEdge extends Edge {

		private TestStatement unit;
		private TestStatement succUnit;

		public NormalEdge(TestStatement unit, TestStatement succUnit, ExpectedFlowFunction...flowFunctions) {
			super(flowFunctions);
			this.unit = unit;
			this.succUnit = succUnit;
		}

		@Override
		public String toString() {
			return String.format("%s -normal-> %s", unit, succUnit);
		}

		@Override
		public void accept(EdgeVisitor visitor) {
			visitor.visit(this);
		}
	}

	public static class CallEdge extends Edge {

		private TestStatement callSite;
		private TestMethod destinationMethod;

		public CallEdge(TestStatement callSite, TestMethod destinationMethod, ExpectedFlowFunction...flowFunctions) {
			super(flowFunctions);
			this.callSite = callSite;
			this.destinationMethod = destinationMethod;
		}

		@Override
		public String toString() {
			return String.format("%s -call-> %s", callSite, destinationMethod);
		}
		
		@Override
		public void accept(EdgeVisitor visitor) {
			visitor.visit(this);
		}
	}

	public static class Call2ReturnEdge extends Edge {
		private TestStatement callSite;
		private TestStatement returnSite;

		public Call2ReturnEdge(TestStatement callSite, TestStatement returnSite, ExpectedFlowFunction...flowFunctions) {
			super(flowFunctions);
			this.callSite = callSite;
			this.returnSite = returnSite;
		}

		@Override
		public String toString() {
			return String.format("%s -call2ret-> %s", callSite, returnSite);
		}
		
		@Override
		public void accept(EdgeVisitor visitor) {
			visitor.visit(this);
		}
	}

	public static class ReturnEdge extends Edge {

		private TestStatement exitStmt;
		private TestStatement returnSite;
		private TestStatement callSite;
		private TestMethod calleeMethod;

		public ReturnEdge(TestStatement callSite, TestStatement exitStmt, TestStatement returnSite, ExpectedFlowFunction...flowFunctions) {
			super(flowFunctions);
			this.callSite = callSite;
			this.exitStmt = exitStmt;
			this.returnSite = returnSite;
			if(callSite == null || returnSite == null)
				includeInCfg = false;
		}

		@Override
		public String toString() {
			return String.format("%s -return-> %s", exitStmt, returnSite);
		}
		
		@Override
		public void accept(EdgeVisitor visitor) {
			visitor.visit(this);
		}
	}
	
	private static boolean nullAwareEquals(Object a, Object b) {
		if(a == null)
			return b==null;
		else
			return a.equals(b);
	}

	public FlowFunctions<TestStatement, TestFieldRef, TestFact, TestMethod> flowFunctions() {
		return new FlowFunctions<TestStatement, TestFieldRef, TestFact, TestMethod>() {

			@Override
			public FlowFunction<TestFieldRef, TestFact, TestStatement, TestMethod> getReturnFlowFunction(TestStatement callSite, TestMethod calleeMethod, TestStatement exitStmt, TestStatement returnSite) {
				for (final ReturnEdge edge : returnEdges) {
					if (nullAwareEquals(callSite, edge.callSite) && edge.calleeMethod.equals(calleeMethod)
							&& edge.exitStmt.equals(exitStmt) && nullAwareEquals(edge.returnSite, returnSite)) {
						return createFlowFunction(edge);
					}
				}
				throw new AssertionError(String.format("No Flow Function expected for return edge %s -> %s (call edge: %s -> %s)", exitStmt,
						returnSite, callSite, calleeMethod));
			}

			@Override
			public FlowFunction<TestFieldRef, TestFact, TestStatement, TestMethod> getNormalFlowFunction(final TestStatement curr) {
				for (final NormalEdge edge : normalEdges) {
					if (edge.unit.equals(curr)) {
						return createFlowFunction(edge);
					}
				}
				throw new AssertionError(String.format("No Flow Function expected for %s", curr));
			}

			@Override
			public FlowFunction<TestFieldRef, TestFact, TestStatement, TestMethod> getCallToReturnFlowFunction(TestStatement callSite, TestStatement returnSite) {
				for (final Call2ReturnEdge edge : call2retEdges) {
					if (edge.callSite.equals(callSite) && edge.returnSite.equals(returnSite)) {
						return createFlowFunction(edge);
					}
				}
				throw new AssertionError(String.format("No Flow Function expected for call to return edge %s -> %s", callSite, returnSite));
			}

			@Override
			public FlowFunction<TestFieldRef, TestFact, TestStatement, TestMethod> getCallFlowFunction(TestStatement callStmt, TestMethod destinationMethod) {
				for (final CallEdge edge : callEdges) {
					if (edge.callSite.equals(callStmt) && edge.destinationMethod.equals(destinationMethod)) {
						return createFlowFunction(edge);
					}
				}
				throw new AssertionError(String.format("No Flow Function expected for call %s -> %s", callStmt, destinationMethod));
			}

			private FlowFunction<TestFieldRef, TestFact, TestStatement, TestMethod> createFlowFunction(final Edge edge) {
				return new FlowFunction<TestFieldRef, TestFact, TestStatement, TestMethod>() {
					@Override
					public Set<FlowFunction.ConstrainedFact<TestFieldRef, TestFact, TestStatement, TestMethod>> computeTargets(TestFact source,
							AccessPathHandler<TestFieldRef, TestFact, TestStatement, TestMethod> accPathHandler) {
						Set<ConstrainedFact<TestFieldRef, TestFact, TestStatement, TestMethod>> result = Sets.newHashSet();
						boolean found = false;
						for (ExpectedFlowFunction ff : edge.flowFunctions) {
							if (ff.source.equals(source)) {
								if (remainingFlowFunctions.remove(ff)) {
									for(TestFact target : ff.targets) {
										result.add(ff.apply(target, accPathHandler));
									}
									found = true;
								} else {
									throw new AssertionError(String.format("Flow Function '%s' was used multiple times on edge '%s'", ff, edge));
								}
							}
						}
						if(found)
							return result;
						else
							throw new AssertionError(String.format("Fact '%s' was not expected at edge '%s'", source, edge));
					}
				};
			}
		};
	}

	public void runSolver(final boolean followReturnsPastSeeds, final String...initialSeeds) {
		Scheduler scheduler = new Scheduler();
		FieldSensitiveIFDSSolver<TestFieldRef, TestFact, TestStatement, TestMethod, InterproceduralCFG<TestStatement,TestMethod>> solver = new FieldSensitiveIFDSSolver<TestFieldRef ,TestFact, TestStatement, TestMethod, InterproceduralCFG<TestStatement,TestMethod>>(
				createTabulationProblem(followReturnsPastSeeds, initialSeeds), new FactMergeHandler<TestFact>() {
					@Override
					public void merge(TestFact previousFact, TestFact currentFact) {
					}

					@Override
					public void restoreCallingContext(TestFact factAtReturnSite, TestFact factAtCallSite) {
					}
					
				}, debugger, scheduler);
		addExpectationsToDebugger();
		scheduler.runAndAwaitCompletion();
		
		assertAllFlowFunctionsUsed();
	}
	
	
	private void addExpectationsToDebugger() {
		for(NormalEdge edge : normalEdges) {
			debugger.expectNormalFlow(edge.unit, expectedFlowFunctionsToString(edge.flowFunctions));
		}
		for(CallEdge edge : callEdges) {
			debugger.expectCallFlow(edge.callSite, edge.destinationMethod, expectedFlowFunctionsToString(edge.flowFunctions));
		}
		for(Call2ReturnEdge edge : call2retEdges) {
			debugger.expectNormalFlow(edge.callSite, expectedFlowFunctionsToString(edge.flowFunctions));
		}
		for(ReturnEdge edge : returnEdges) {
			debugger.expectReturnFlow(edge.exitStmt, edge.returnSite, expectedFlowFunctionsToString(edge.flowFunctions));
		}
	}

	private IFDSTabulationProblem<TestStatement, TestFieldRef, TestFact, TestMethod, InterproceduralCFG<TestStatement, TestMethod>> createTabulationProblem(final boolean followReturnsPastSeeds, final String[] initialSeeds) {
		final InterproceduralCFG<TestStatement, TestMethod> icfg = buildIcfg();
		final FlowFunctions<TestStatement, TestFieldRef, TestFact, TestMethod> flowFunctions = flowFunctions();
		
		return new IFDSTabulationProblem<TestStatement,TestFieldRef,  TestFact, TestMethod, InterproceduralCFG<TestStatement, TestMethod>>() {

			@Override
			public boolean followReturnsPastSeeds() {
				return followReturnsPastSeeds;
			}

			@Override
			public boolean autoAddZero() {
				return false;
			}

			@Override
			public int numThreads() {
				return 1;
			}

			@Override
			public boolean computeValues() {
				return false;
			}

			@Override
			public FlowFunctions<TestStatement,TestFieldRef,  TestFact, TestMethod> flowFunctions() {
				return flowFunctions;
			}

			@Override
			public InterproceduralCFG<TestStatement, TestMethod> interproceduralCFG() {
				return icfg;
			}

			@Override
			public Map<TestStatement, Set<TestFact>> initialSeeds() {
				Map<TestStatement, Set<TestFact>> result = Maps.newHashMap();
				for (String stmt : initialSeeds) {
					result.put(new TestStatement(stmt), Sets.newHashSet(new TestFact("0")));
				}
				return result;
			}

			@Override
			public TestFact zeroValue() {
				return new TestFact("0");
			}
			
			@Override
			public ZeroHandler<TestFieldRef> zeroHandler() {
				return new ZeroHandler<TestFieldRef>() {
					@Override
					public boolean shouldGenerateAccessPath(AccessPath<TestFieldRef> accPath) {
						return true;
					}
				};
			}
		};
	}
}
