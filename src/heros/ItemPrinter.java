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
package heros;

/**
 * Interface for creating string representations of nodes, facts,
 * and methods in the IDE/IFDS problem.
 *
 * @param <N> The type of nodes in the interprocedural control-flow graph.
 * @param <D> The type of data-flow facts to computed by the tabulation problem.
 * @param <M> The type of objects used to represent methods.
 */
public interface ItemPrinter<N, D, M> {
	public String printNode(N node, M parentMethod);
	public String printFact(D fact);
	public String printMethod(M method);
	public static final ItemPrinter<Object, Object, Object> DEFAULT_PRINTER = new ItemPrinter<Object, Object, Object>() {
		@Override
		public String printNode(Object node, Object parentMethod) {
			return node.toString();
		}

		@Override
		public String printFact(Object fact) {
			return fact.toString();
		}

		@Override
		public String printMethod(Object method) {
			return method.toString();
		}
	};
}
