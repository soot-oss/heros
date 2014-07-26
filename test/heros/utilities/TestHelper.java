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
package heros.utilities;

import static org.junit.Assert.assertTrue;
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.IFDSTabulationProblem;
import heros.InterproceduralCFG;
import heros.solver.BiDiIFDSSolver;
import heros.solver.IFDSSolver;
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

	private Multimap<Method, Statement> method2startPoint = HashMultimap.create();
	private List<NormalEdge> normalEdges = Lists.newLinkedList();
	private List<CallEdge> callEdges = Lists.newLinkedList();
	private List<Call2ReturnEdge> call2retEdges = Lists.newLinkedList();
	private List<ReturnEdge> returnEdges = Lists.newLinkedList();
	private Map<Statement, Method> stmt2method = Maps.newHashMap();
	private Multiset<ExpectedFlowFunction> remainingFlowFunctions = HashMultiset.create();

	public MethodHelper method(String methodName, Statement[] startingPoints, EdgeBuilder... edgeBuilders) {
		MethodHelper methodHelper = new MethodHelper(new Method(methodName));
		methodHelper.startPoints(startingPoints);
		for(EdgeBuilder edgeBuilder : edgeBuilders){
			methodHelper.edges(edgeBuilder.edges());
		}
		return methodHelper;
	}

	public static Statement[] startPoints(String... startingPoints) {
		Statement[] result = new Statement[startingPoints.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = new Statement(startingPoints[i]);
		}
		return result;
	}

	public static EdgeBuilder.NormalStmtBuilder normalStmt(String stmt) {
		return new EdgeBuilder.NormalStmtBuilder(new Statement(stmt));
	}
	
	public static EdgeBuilder.CallSiteBuilder callSite(String callSite) {
		return new EdgeBuilder.CallSiteBuilder(new Statement(callSite));
	}
	
	public static EdgeBuilder.ExitStmtBuilder exitStmt(String exitStmt) {
		return new EdgeBuilder.ExitStmtBuilder(new Statement(exitStmt));
	}
	
	public static Statement over(String callSite) {
		return new Statement(callSite);
	}
	
	public static Statement to(String returnSite) {
		return new Statement(returnSite);
	}
	
	public static ExpectedFlowFunction kill(String source) {
		return kill(1, source);
	}
	
	public static ExpectedFlowFunction kill(int times, String source) {
		return new ExpectedFlowFunction(times, new Fact(source));
	}

	public static ExpectedFlowFunction flow(String source, String... targets) {
		return flow(1, source, targets);
	}
	
	public static ExpectedFlowFunction flow(int times, String source, String... targets) {
		Fact[] targetFacts = new Fact[targets.length];
		for(int i=0; i<targets.length; i++) {
			targetFacts[i] = new Fact(targets[i]);
		}
		return new ExpectedFlowFunction(times, new Fact(source), targetFacts);
	}
	
	public static int times(int times) {
		return times;
	}

	public InterproceduralCFG<Statement, Method> buildIcfg() {
		return new InterproceduralCFG<Statement, Method>() {

			@Override
			public boolean isStartPoint(Statement stmt) {
				return method2startPoint.values().contains(stmt);
			}

			@Override
			public boolean isFallThroughSuccessor(Statement stmt, Statement succ) {
				throw new IllegalStateException();
			}

			@Override
			public boolean isExitStmt(Statement stmt) {
				for(ReturnEdge edge : returnEdges) {
					if(edge.exitStmt.equals(stmt))
						return true;
				}
				return false;
			}

			@Override
			public boolean isCallStmt(final Statement stmt) {
				return Iterables.any(callEdges, new Predicate<CallEdge>() {
					@Override
					public boolean apply(CallEdge edge) {
						return edge.callSite.equals(stmt);
					}
				});
			}

			@Override
			public boolean isBranchTarget(Statement stmt, Statement succ) {
				throw new IllegalStateException();
			}

			@Override
			public List<Statement> getSuccsOf(Statement n) {
				LinkedList<Statement> result = Lists.newLinkedList();
				for (NormalEdge edge : normalEdges) {
					if (edge.includeInCfg && edge.unit.equals(n))
						result.add(edge.succUnit);
				}
				return result;
			}

			@Override
			public Collection<Statement> getStartPointsOf(Method m) {
				return method2startPoint.get(m);
			}

			@Override
			public Collection<Statement> getReturnSitesOfCallAt(Statement n) {
				Set<Statement> result = Sets.newHashSet();
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
			public Method getMethodOf(Statement n) {
				return stmt2method.get(n);
			}

			@Override
			public Set<Statement> getCallsFromWithin(Method m) {
				throw new IllegalStateException();
			}

			@Override
			public Collection<Statement> getCallersOf(Method m) {
				Set<Statement> result = Sets.newHashSet();
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
			public Collection<Method> getCalleesOfCallAt(Statement n) {
				List<Method> result = Lists.newLinkedList();
				for (CallEdge edge : callEdges) {
					if (edge.includeInCfg && edge.callSite.equals(n)) {
						result.add(edge.destinationMethod);
					}
				}
				return result;
			}

			@Override
			public Set<Statement> allNonCallStartNodes() {
				throw new IllegalStateException();
			}
		};
	}

	public void assertAllFlowFunctionsUsed() {
		assertTrue("These Flow Functions were expected, but never used: \n" + Joiner.on(",\n").join(remainingFlowFunctions),
				remainingFlowFunctions.isEmpty());
	}

	private void addOrVerifyStmt2Method(Statement stmt, Method m) {
		if (stmt2method.containsKey(stmt) && !stmt2method.get(stmt).equals(m)) {
			throw new IllegalArgumentException("Statement " + stmt + " is used in multiple methods: " + m + " and " + stmt2method.get(stmt));
		}
		stmt2method.put(stmt, m);
	}

	public MethodHelper method(Method method) {
		MethodHelper h = new MethodHelper(method);
		return h;
	}

	public class MethodHelper {

		private Method method;

		public MethodHelper(Method method) {
			this.method = method;
		}

		public void edges(Collection<Edge> edges) {
			for(Edge edge : edges) {
				for(ExpectedFlowFunction ff : edge.flowFunctions) {
					remainingFlowFunctions.add(ff, ff.times);
				}
				
				edge.accept(new EdgeVisitor() {
					@Override
					public void visit(heros.utilities.TestHelper.ReturnEdge edge) {
						addOrVerifyStmt2Method(edge.exitStmt, method);
						edge.calleeMethod = method;
						returnEdges.add(edge);
					}
					
					@Override
					public void visit(heros.utilities.TestHelper.Call2ReturnEdge edge) {
						addOrVerifyStmt2Method(edge.callSite, method);
						addOrVerifyStmt2Method(edge.returnSite, method);
						call2retEdges.add(edge);
					}
					
					@Override
					public void visit(heros.utilities.TestHelper.CallEdge edge) {
						addOrVerifyStmt2Method(edge.callSite, method);
						callEdges.add(edge);
					}
					
					@Override
					public void visit(heros.utilities.TestHelper.NormalEdge edge) {
						addOrVerifyStmt2Method(edge.unit, method);
						addOrVerifyStmt2Method(edge.succUnit, method);
						normalEdges.add(edge);
					}
				});
			}
		}

		public void startPoints(Statement[] startingPoints) {
			method2startPoint.putAll(method, Lists.newArrayList(startingPoints));
		}
	}
	
	public static class ExpectedFlowFunction {

		public final Fact source;
		public final Fact[] targets;
		public Edge edge;
		private int times;

		public ExpectedFlowFunction(int times, Fact source, Fact... targets) {
			this.times = times;
			this.source = source;
			this.targets = targets;
		}

		@Override
		public String toString() {
			return String.format("%s: %s -> {%s}", edge, source, Joiner.on(",").join(targets));
		}
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

		private Statement unit;
		private Statement succUnit;

		public NormalEdge(Statement unit, Statement succUnit, ExpectedFlowFunction...flowFunctions) {
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

		private Statement callSite;
		private Method destinationMethod;

		public CallEdge(Statement callSite, Method destinationMethod, ExpectedFlowFunction...flowFunctions) {
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
		private Statement callSite;
		private Statement returnSite;

		public Call2ReturnEdge(Statement callSite, Statement returnSite, ExpectedFlowFunction...flowFunctions) {
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

		private Statement exitStmt;
		private Statement returnSite;
		private Statement callSite;
		private Method calleeMethod;

		public ReturnEdge(Statement callSite, Statement exitStmt, Statement returnSite, ExpectedFlowFunction...flowFunctions) {
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

	public FlowFunctions<Statement, Fact, Method> flowFunctions() {
		return new FlowFunctions<Statement, Fact, Method>() {

			@Override
			public FlowFunction<Fact> getReturnFlowFunction(Statement callSite, Method calleeMethod, Statement exitStmt, Statement returnSite) {
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
			public FlowFunction<Fact> getNormalFlowFunction(final Statement curr, final Statement succ) {
				for (final NormalEdge edge : normalEdges) {
					if (edge.unit.equals(curr) && edge.succUnit.equals(succ)) {
						return createFlowFunction(edge);
					}
				}
				throw new AssertionError(String.format("No Flow Function expected for %s -> %s", curr, succ));
			}

			@Override
			public FlowFunction<Fact> getCallToReturnFlowFunction(Statement callSite, Statement returnSite) {
				for (final Call2ReturnEdge edge : call2retEdges) {
					if (edge.callSite.equals(callSite) && edge.returnSite.equals(returnSite)) {
						return createFlowFunction(edge);
					}
				}
				throw new AssertionError(String.format("No Flow Function expected for call to return edge %s -> %s", callSite, returnSite));
			}

			@Override
			public FlowFunction<Fact> getCallFlowFunction(Statement callStmt, Method destinationMethod) {
				for (final CallEdge edge : callEdges) {
					if (edge.callSite.equals(callStmt) && edge.destinationMethod.equals(destinationMethod)) {
						return createFlowFunction(edge);
					}
				}
				throw new AssertionError(String.format("No Flow Function expected for call %s -> %s", callStmt, destinationMethod));
			}

			private FlowFunction<Fact> createFlowFunction(final Edge edge) {
				return new FlowFunction<Fact>() {
					@Override
					public Set<Fact> computeTargets(Fact source) {
						for (ExpectedFlowFunction ff : edge.flowFunctions) {
							if (ff.source.equals(source)) {
								if (remainingFlowFunctions.remove(ff)) {
									return Sets.newHashSet(ff.targets);
								} else {
									throw new AssertionError(String.format("Flow Function '%s' was used multiple times on edge '%s'", ff, edge));
								}
							}
						}
						throw new AssertionError(String.format("Fact '%s' was not expected at edge '%s'", source, edge));
					}
				};
			}
		};
	}

	public void runSolver(final boolean followReturnsPastSeeds, final String...initialSeeds) {
		IFDSSolver<Statement, Fact, Method, InterproceduralCFG<Statement, Method>> solver =
				new IFDSSolver<Statement, Fact, Method, InterproceduralCFG<Statement, Method>>(
				createTabulationProblem(followReturnsPastSeeds, initialSeeds));

		solver.solve();
		assertAllFlowFunctionsUsed();
	}
	
	
	public static enum TabulationProblemExchange {AsSpecified, ExchangeForwardAndBackward};
	public void runBiDiSolver(TestHelper backwardHelper, TabulationProblemExchange direction, final String...initialSeeds) {
		BiDiIFDSSolver<Statement, Fact, Method, InterproceduralCFG<Statement, Method>> solver =
				direction == TabulationProblemExchange.AsSpecified ? 
				new BiDiIFDSSolver<Statement, Fact, Method, InterproceduralCFG<Statement, Method>>(createTabulationProblem(true, initialSeeds), 
									backwardHelper.createTabulationProblem(true, initialSeeds)) :
				new BiDiIFDSSolver<Statement, Fact, Method, InterproceduralCFG<Statement, Method>>(backwardHelper.createTabulationProblem(true, initialSeeds), 
									createTabulationProblem(true, initialSeeds));
		
		solver.solve();
		assertAllFlowFunctionsUsed();
		backwardHelper.assertAllFlowFunctionsUsed();
	}
	
	private IFDSTabulationProblem<Statement, Fact, Method, InterproceduralCFG<Statement, Method>> createTabulationProblem(final boolean followReturnsPastSeeds, final String[] initialSeeds) {
		final InterproceduralCFG<Statement, Method> icfg = buildIcfg();
		final FlowFunctions<Statement, Fact, Method> flowFunctions = flowFunctions();
		
		return new IFDSTabulationProblem<Statement, Fact, Method, InterproceduralCFG<Statement, Method>>() {

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
			public FlowFunctions<Statement, Fact, Method> flowFunctions() {
				return flowFunctions;
			}

			@Override
			public InterproceduralCFG<Statement, Method> interproceduralCFG() {
				return icfg;
			}

			@Override
			public Map<Statement, Set<Fact>> initialSeeds() {
				Map<Statement, Set<Fact>> result = Maps.newHashMap();
				for (String stmt : initialSeeds) {
					result.put(new Statement(stmt), Sets.newHashSet(new Fact("0")));
				}
				return result;
			}

			@Override
			public Fact zeroValue() {
				return new Fact("0");
			}
		};
	}
}
