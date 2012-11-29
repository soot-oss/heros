package de.bodden.ide.template;

import de.bodden.ide.FlowFunctions;
import de.bodden.ide.IFDSTabulationProblem;
import de.bodden.ide.InterproceduralCFG;

/**
 * This is a template for {@link IFDSTabulationProblem}s that automatically caches values
 * that ought to be cached. This class uses the Factory Method design pattern.
 * The {@link InterproceduralCFG} is passed into the constructor so that it can be conveniently
 * reused for solving multiple different {@link IFDSTabulationProblem}s.
 * This class is specific to Soot. 
 * 
 * @param <D> The type of data-flow facts to be computed by the tabulation problem.
 */
public abstract class DefaultIFDSTabulationProblem<N,D,M, I extends InterproceduralCFG<N,M>> implements IFDSTabulationProblem<N,D,M,I> {

	private final I icfg;
	private FlowFunctions<N,D,M> flowFunctions;
	private D zeroValue;
	
	public DefaultIFDSTabulationProblem(I icfg) {
		this.icfg = icfg;
	}
	
	protected abstract FlowFunctions<N, D, M> createFlowFunctionsFactory();

	protected abstract D createZeroValue();

	@Override
	public final FlowFunctions<N,D,M> flowFunctions() {
		if(flowFunctions==null) {
			flowFunctions = createFlowFunctionsFactory();
		}
		return flowFunctions;
	}

	@Override
	public final I interproceduralCFG() {
		return icfg;
	}

	@Override
	public final D zeroValue() {
		if(zeroValue==null) {
			zeroValue = createZeroValue();
		}
		return zeroValue;
	}

}
