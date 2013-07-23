/*******************************************************************************
 * Copyright (c) 2013 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package heros;

/**
 * An abstraction that can be summarized.
 */
public interface SummarizableAbstraction<A> {
	
	/**
	 * Returns a summarized copy of this abstraction which projects away constant data
	 * (such as a source in taint analysis), for instance by nulling the appropriate fields.  
	 */
	public SummarizableAbstraction<A> summarize();

	/**
	 * Undoes the effect of the summarize method by expanding this abstraction,
	 * returning a copy where previously nulled fields are replaced with values of fullAbstraction.
	 */
	public SummarizableAbstraction<A> expand(A fullAbstraction);

}
