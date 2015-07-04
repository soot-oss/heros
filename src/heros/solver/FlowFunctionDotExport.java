/*******************************************************************************
 * Copyright (c) 2015 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package heros.solver;

import heros.InterproceduralCFG;
import heros.ItemPrinter;
import heros.SolverDebugConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
	private final IDESolver<N, D, M, ?, I> solver;
	private final ItemPrinter<N, D, M> printer;
	/**
	 * Constructor.
	 * @param solver The solver instance to dump.
	 * @param printer The printer object to use to create the string representations of
	 * the nodes, facts, and methods in the exploded super-graph.
	 */
	public FlowFunctionDotExport(IDESolver<N, D, M, ?, I> solver, ItemPrinter<N, D, M> printer) {
		this.solver = solver;
		this.printer = printer;
	}
	
	private <T> long doNumber(Map<T, Long> map, T value, long val) {
		if(map.containsKey(value)) {
			return val;
		}
		map.put(value, val);
		return val + 1;
	}
	
	private <K,U> Set<U> getOrMakeSet(Map<K,Set<U>> map, K key) {
		if(map.containsKey(key)) {
			return map.get(key);
		}
		HashSet<U> toRet = new HashSet<U>();
		map.put(key, toRet);
		return toRet;
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
			long factCounter = 0;
			Map<Pair<N, D>, Long> factNumbers = new HashMap<Pair<N, D>, Long>();
			long unitCounter = 0;
			Map<N, Long> unitNumbers = new HashMap<N, Long>();
			Map<N, Set<D>> factsForUnit = new HashMap<N, Set<D>>();
			Map<M, Set<N>> methodToUnit = new HashMap<M, Set<N>>();
			for(Cell<N,N,Map<D,Set<D>>> c : solver.computedEdges.cellSet()) {
				N sourceUnit = c.getRowKey();
				N destUnit = c.getColumnKey();
				getOrMakeSet(methodToUnit, solver.icfg.getMethodOf(destUnit)).add(destUnit);
				getOrMakeSet(methodToUnit, solver.icfg.getMethodOf(sourceUnit)).add(sourceUnit);
				unitCounter = doNumber(unitNumbers, sourceUnit, unitCounter);
				unitCounter = doNumber(unitNumbers, destUnit, unitCounter);
				Set<D> sourceFacts = getOrMakeSet(factsForUnit, sourceUnit);
				Set<D> destFacts = getOrMakeSet(factsForUnit, destUnit);
				for(Map.Entry<D, Set<D>> entry : c.getValue().entrySet()) {
					factCounter = doNumber(factNumbers, new Pair<N, D>(sourceUnit, entry.getKey()), factCounter);
					sourceFacts.add(entry.getKey());
					for(D destFact : entry.getValue()) {
						factCounter = doNumber(factNumbers, new Pair<N, D>(destUnit, destFact), factCounter);
						destFacts.add(destFact);
					}
				}
			}
			pf.println("digraph ifds {" +
					"node[shape=record];"
			);
			ArrayList<String> interProc = new ArrayList<String>();
			int methodCounter = 0;
			for(Map.Entry<M, Set<N>> kv : methodToUnit.entrySet()) {
				Set<N> intraProc = kv.getValue();
				pf.println("subgraph cluster" + methodCounter + " {");
				methodCounter++;
				for(N methodUnit : intraProc) {
					Set<D> loc = factsForUnit.get(methodUnit);
					long unitNumber = unitNumbers.get(methodUnit);
					String unitText = printer.printNode(methodUnit, kv.getKey()).replace("\\", "\\\\").replace("\"", "\\\"").replace("<", "\\<").replace(">", "\\>");
					pf.print("u"+ unitNumber + " [shape=record,label=\""+ unitText + " ");
					for(D hl : loc) {
						pf.print("| <f" + factNumbers.get(new Pair<N, D>(methodUnit, hl)) + "> " + printer.printFact(hl));
					}
					pf.println("\"];");
				}
				for(N methodUnit : intraProc) {
					long sourceNumber = unitNumbers.get(methodUnit);
					Map<N, Map<D, Set<D>>> flows = solver.computedEdges.row(methodUnit);
					for(Map.Entry<N, Map<D, Set<D>>> kv2 : flows.entrySet()) {
						N destUnit = kv2.getKey();
						long destNumber = unitNumbers.get(destUnit);
						for(Map.Entry<D, Set<D>> pointFlow : kv2.getValue().entrySet()) {
							long sourceFactNumber = factNumbers.get(new Pair<N, D>(methodUnit, pointFlow.getKey()));
							
							for(D destFact : pointFlow.getValue()) {
								long destFactNumber = factNumbers.get(new Pair<N, D>(destUnit, destFact));
								String edge = "u" + sourceNumber + ":f" + sourceFactNumber + " -> u" + destNumber + ":f" + destFactNumber;
								if(intraProc.contains(destUnit)) {
									pf.println(edge+ ";");
								} else {
									interProc.add(edge + " [style=dotted];");
								}
							}
						}
					}
				}
				pf.println("label=\"" + printer.printMethod(kv.getKey()) + "\";");
				pf.println("}");
			}
			for(String interProcE : interProc) {
				pf.println(interProcE);
			}
			pf.println("}");
		} catch (FileNotFoundException e) {	
			throw new RuntimeException("Writing dot output failed", e); 
		} finally {
			pf.close();
		}
	}
}
