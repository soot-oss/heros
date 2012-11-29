package de.bodden.ide.template;

import de.bodden.ide.EdgeFunction;
import de.bodden.ide.EdgeFunctions;
import de.bodden.ide.IDETabulationProblem;
import de.bodden.ide.InterproceduralCFG;
import de.bodden.ide.JoinLattice;

/**
 * This is a template for {@link IDETabulationProblem}s that automatically caches values
 * that ought to be cached. This class uses the Factory Method design pattern.
 * The {@link InterproceduralCFG} is passed into the constructor so that it can be conveniently
 * reused for solving multiple different {@link IDETabulationProblem}s.
 * This class is specific to Soot. 
 * 
 * @param <D> The type of data-flow facts to be computed by the tabulation problem.
 * @param <V> The type of values to be computed along flow edges.
 * @param <I> The type of inter-procedural control-flow graph being used.
 */
public abstract class DefaultIDETabulationProblem<N,D,M,V,I extends InterproceduralCFG<N,M>>
    extends DefaultIFDSTabulationProblem<N,D,M,I> implements IDETabulationProblem<N,D,M,V,I>{

	private final EdgeFunction<V> allTopFunction;
	private final JoinLattice<V> joinLattice;
	private final EdgeFunctions<N,D,M,V> edgeFunctions;
	
	public DefaultIDETabulationProblem(I icfg) {
		super(icfg);
		this.allTopFunction = createAllTopFunction();
		this.joinLattice = createJoinLattice();
		this.edgeFunctions = createEdgeFunctionsFactory();
	}

	protected abstract EdgeFunction<V> createAllTopFunction();

	protected abstract JoinLattice<V> createJoinLattice();

	protected abstract EdgeFunctions<N,D,M,V> createEdgeFunctionsFactory();
	
	@Override
	public final EdgeFunction<V> allTopFunction() {
		return allTopFunction;
	}
	
	@Override
	public final JoinLattice<V> joinLattice() {
		return joinLattice;
	}
	
	@Override
	public final EdgeFunctions<N,D,M,V> edgeFunctions() {
		return edgeFunctions;
	}
	
}
