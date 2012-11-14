package soot.jimple.interproc.ifds.template;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.interproc.ifds.FlowFunctions;
import soot.jimple.interproc.ifds.IFDSTabulationProblem;
import soot.jimple.interproc.ifds.InterproceduralCFG;

/**
 * This is a template for {@link IFDSTabulationProblem}s that automatically caches values
 * that ought to be cached. This class uses the Factory Method design pattern.
 * The {@link InterproceduralCFG} is passed into the constructor so that it can be conveniently
 * reused for solving multiple different {@link IFDSTabulationProblem}s.
 * This class is specific to Soot. 
 * 
 * @param <D> The type of data-flow facts to be computed by the tabulation problem.
 */
public abstract class DefaultIFDSTabulationProblem<D,I extends InterproceduralCFG<Unit,SootMethod>> implements IFDSTabulationProblem<Unit, D, SootMethod,I> {

	private final I icfg;
	private FlowFunctions<Unit, D, SootMethod> flowFunctions;
	private D zeroValue;
	
	public DefaultIFDSTabulationProblem(I icfg) {
		this.icfg = icfg;
	}
	
	protected abstract FlowFunctions<Unit, D, SootMethod> createFlowFunctionsFactory();

	protected abstract D createZeroValue();

	@Override
	public final FlowFunctions<Unit, D, SootMethod> flowFunctions() {
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
