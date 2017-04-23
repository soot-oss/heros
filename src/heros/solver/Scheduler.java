package heros.solver;

import java.util.LinkedList;

public class Scheduler {
	protected LinkedList<Runnable> worklist = new LinkedList<Runnable>();

	public void add(Runnable runnable) {
		worklist.add(runnable);
	}

	public Runnable poll() {
		return worklist.poll();
	}

	public void awaitExecution() {
		while (worklist != null && !worklist.isEmpty()) {
			Runnable task = worklist.poll();
			task.run();
		}
	}
}
