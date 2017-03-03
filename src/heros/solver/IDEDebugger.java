package heros.solver;

import heros.InterproceduralCFG;

public interface IDEDebugger<N, D, M, V, I extends InterproceduralCFG<N, M>> {
	public void addSummary(M methodToSummary, PathEdge<N, D> summary);
	public void normalFlow(N start, D startFact, N target, D targetFact);
	public void callFlow(N start, D startFact, N target, D targetFact);
	public void callToReturn(N start, D startFact, N target, D targetFact);
	public void returnFlow(N start, D startFact, N target, D targetFact);
	public void setValue(N start, D startFact, V value);
}
