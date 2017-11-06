/*******************************************************************************
 * Copyright (c) 2015 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     John Toman - initial API and implementation 
 ******************************************************************************/
package heros.solver;

import heros.InterproceduralCFG;
import heros.ItemPrinter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

/**
 * A class to dump the results of flow functions to a dot file for visualization.
 * 
 * This class can be used for both IDE and IFDS problems that have implemented the
 * {@link SolverDebugConfiguration} and overridden {@link SolverDebugConfiguration#recordEdges()}
 * to return true.
 * 
 * @param <N> The type of nodes in the interprocedural control-flow graph. Typically {@link Unit}.
 * @param <D> The type of data-flow facts to be computed by the tabulation problem.
 * @param <M> The type of objects used to represent methods. Typically {@link SootMethod}.
 * @param <I> The type of inter-procedural control-flow graph being used.
 */
public class FlowFunctionDotExport<N,D,M,I extends InterproceduralCFG<N, M>> {
	private static class Numberer<D> {
		long counter = 1;
		Map<D, Long> map = new HashMap<D, Long>();
		
		public void add(D o) {
			if(map.containsKey(o)) {
				return;
			}
			map.put(o, counter++);
		}
		public long get(D o) {
			if(o == null) {
				throw new IllegalArgumentException("Null key");
			}
			if(!map.containsKey(o)) {
				throw new IllegalArgumentException("Failed to find number for: " + o);
			}
			return map.get(o);
			
		}
	}
	private final IDESolver<N, D, M, ?, I> solver;
	private final ItemPrinter<? super N, ? super D, ? super M> printer;
	private final Set<M> methodWhitelist;
	
	/**
	 * Constructor.
	 * @param solver The solver instance to dump.
	 * @param printer The printer object to use to create the string representations of
	 * the nodes, facts, and methods in the exploded super-graph.
	 */
	public FlowFunctionDotExport(IDESolver<N, D, M, ?, I> solver, ItemPrinter<? super N, ? super D, ? super M> printer) {
		this(solver, printer, null);
	}
	
	/**
	 * Constructor.
	 * @param solver The solver instance to dump.
	 * @param printer The printer object to use to create the string representations of
	 * the nodes, facts, and methods in the exploded super-graph.
	 * @param methodWhitelist A set of methods of type M for which the full graphs should be printed.
	 * Flow functions for which both unit endpoints are not contained in a method in methodWhitelist are not printed.
	 * Callee/caller edges into/out of the methods in the set are still printed.  
	 */
	public FlowFunctionDotExport(IDESolver<N, D, M, ?, I> solver, ItemPrinter<? super N, ? super D, ? super M> printer, Set<M> methodWhitelist) {
		this.solver = solver;
		this.printer = printer;
		this.methodWhitelist = methodWhitelist;
	}
	
	private static <K,U> Set<U> getOrMakeSet(Map<K,Set<U>> map, K key) {
		if(map.containsKey(key)) {
			return map.get(key);
		}
		HashSet<U> toRet = new HashSet<U>();
		map.put(key, toRet);
		return toRet;
	}
	
	private String escapeLabelString(String toEscape) { 
		return toEscape.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("<", "\\<")
				.replace(">", "\\>");
	}
	
	private class UnitFactTracker {
		private Numberer<Pair<N, D>> factNumbers = new Numberer<Pair<N, D>>();
		private Numberer<N> unitNumbers = new Numberer<N>();
		private Map<N, Set<D>> factsForUnit = new HashMap<N, Set<D>>();
		private Map<M, Set<N>> methodToUnit = new HashMap<M, Set<N>>();
		
		private Map<M, Set<N>> stubMethods = new HashMap<M, Set<N>>();
		
		public void registerFactAtUnit(N unit, D fact) {
			getOrMakeSet(factsForUnit, unit).add(fact);
			factNumbers.add(new Pair<N, D>(unit, fact));
		}

		public void registerUnit(M method, N unit) {
			unitNumbers.add(unit);
			getOrMakeSet(methodToUnit, method).add(unit);
		}
		
		public void registerStubUnit(M method, N unit) {
			assert !methodToUnit.containsKey(method);
			unitNumbers.add(unit);
			getOrMakeSet(stubMethods, method).add(unit);
		}
		
		public String getUnitLabel(N unit) {
			return "u" + unitNumbers.get(unit);
		}
		
		public String getFactLabel(N unit, D fact) {
			return "f" + factNumbers.get(new Pair<N, D>(unit, fact));
		}
		
		public String getEdgePoint(N unit, D fact) {
			return this.getUnitLabel(unit) + ":" + this.getFactLabel(unit, fact);
		}
	}
	
	private void numberEdges(Table<N, N, Map<D, Set<D>>> edgeSet, UnitFactTracker utf) {
		for(Cell<N,N,Map<D,Set<D>>> c : edgeSet.cellSet()) {
			N sourceUnit = c.getRowKey();
			N destUnit = c.getColumnKey();
			M destMethod = solver.icfg.getMethodOf(destUnit);
			M sourceMethod = solver.icfg.getMethodOf(sourceUnit);
			if(isMethodFiltered(sourceMethod) && isMethodFiltered(destMethod)) {
				continue;
			}
			if(isMethodFiltered(destMethod)) {
				utf.registerStubUnit(destMethod, destUnit);
			} else {
				utf.registerUnit(destMethod, destUnit);
			}
			if(isMethodFiltered(sourceMethod)) {
				utf.registerStubUnit(sourceMethod, sourceUnit);
			} else {
				utf.registerUnit(sourceMethod, sourceUnit);
			}
			for(Map.Entry<D, Set<D>> entry : c.getValue().entrySet()) {
				utf.registerFactAtUnit(sourceUnit, entry.getKey());
				for(D destFact : entry.getValue()) {
					utf.registerFactAtUnit(destUnit, destFact);
				}
			}
		}
	}

	private boolean isMethodFiltered(M method) {
		return methodWhitelist != null && !methodWhitelist.contains(method);
	}
	
	private boolean isNodeFiltered(N node) {
		return isMethodFiltered(solver.icfg.getMethodOf(node));
	}
	
	private void printMethodUnits(Set<N> units, M method, PrintStream pf, UnitFactTracker utf) {
		for(N methodUnit : units) {
			Set<D> loc = utf.factsForUnit.get(methodUnit);
			String unitText = escapeLabelString(printer.printNode(methodUnit, method));
			pf.print(utf.getUnitLabel(methodUnit) + " [shape=record,label=\""+ unitText + " ");
			for(D hl : loc) {
				pf.print("| <" + utf.getFactLabel(methodUnit, hl) + "> " + escapeLabelString(printer.printFact(hl)));
			}
			pf.println("\"];");
		}
	}
	
	/**
	 * Write a graph representation of the flow functions computed by the solver
	 * to the file indicated by fileName.
	 * 
	 * <b>Note:</b> This method should only be called after 
	 * the solver passed to this object's constructor has had its {@link IDESolver#solve()}
	 * method called.  
	 * @param fileName The output file to which to write the dot representation.
	 */
	public void dumpDotFile(String fileName) {
		File f = new File(fileName);
		PrintStream pf = null;
		try {
			pf = new PrintStream(f);
			UnitFactTracker utf = new UnitFactTracker();
			
			numberEdges(solver.computedIntraPEdges, utf);
			numberEdges(solver.computedInterPEdges, utf);
			
			pf.println("digraph ifds {" +
					"node[shape=record];"
			);
			int methodCounter = 0;
			for(Map.Entry<M, Set<N>> kv : utf.methodToUnit.entrySet()) {
				Set<N> intraProc = kv.getValue();
				pf.println("subgraph cluster" + methodCounter + " {");
				methodCounter++;
				printMethodUnits(intraProc, kv.getKey(), pf, utf);
				for(N methodUnit : intraProc) {
					Map<N, Map<D, Set<D>>> flows = solver.computedIntraPEdges.row(methodUnit);
					for(Map.Entry<N, Map<D, Set<D>>> kv2 : flows.entrySet()) {
						N destUnit = kv2.getKey();
						for(Map.Entry<D, Set<D>> pointFlow : kv2.getValue().entrySet()) {
							for(D destFact : pointFlow.getValue()) {
								String edge = utf.getEdgePoint(methodUnit, pointFlow.getKey()) + " -> " + utf.getEdgePoint(destUnit, destFact);
								pf.print(edge);
								pf.println(";");
							}
						}
					}
				}
				pf.println("label=\"" + escapeLabelString(printer.printMethod(kv.getKey())) + "\";");
				pf.println("}");
			}
			for(Map.Entry<M, Set<N>> kv : utf.stubMethods.entrySet()) {
				pf.println("subgraph cluster" + methodCounter++ + " {");
				printMethodUnits(kv.getValue(), kv.getKey(), pf, utf);
				pf.println("label=\"" + escapeLabelString("[STUB] " + printer.printMethod(kv.getKey())) + "\";");
				pf.println("graph[style=dotted];");
				pf.println("}");
			}
			for(Cell<N, N, Map<D, Set<D>>> c : solver.computedInterPEdges.cellSet()) {
				if(isNodeFiltered(c.getRowKey()) && isNodeFiltered(c.getColumnKey())) {
					continue;
				}
				for(Map.Entry<D, Set<D>> kv : c.getValue().entrySet()) {
					for(D dFact : kv.getValue()) {
						pf.print(utf.getEdgePoint(c.getRowKey(), kv.getKey()));
						pf.print(" -> ");
						pf.print(utf.getEdgePoint(c.getColumnKey(), dFact));
						pf.println(" [style=dotted];");
					}
				}
			}
			pf.println("}");
		} catch (FileNotFoundException e) {	
			throw new RuntimeException("Writing dot output failed", e); 
		} finally {
			if(pf != null) {
				pf.close();
			}
		}
	}
}
