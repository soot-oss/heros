/*******************************************************************************
 * Copyright (c) 2012 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package heros.solver;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A {@link ThreadPoolExecutor} which keeps track of the number of spawned
 * tasks to allow clients to await their completion. 
 */
public class CountingThreadPoolExecutor extends ThreadPoolExecutor {
	
	protected final CountLatch numRunningTasks = new CountLatch(0);
	
	protected final Set<Throwable> exceptions = new HashSet<Throwable>();

	public CountingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	@Override
	public void execute(Runnable command) {
		numRunningTasks.increment();
		super.execute(command);
	}
	
	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		numRunningTasks.decrement();
		if(t!=null) exceptions.add(t);
		super.afterExecute(r, t);
	}

	/**
	 * Awaits the completion of all spawned tasks.
	 */
	public void awaitCompletion() throws InterruptedException {
		numRunningTasks.awaitZero();
	}
	
	/**
	 * Awaits the completion of all spawned tasks.
	 */
	public void awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
		numRunningTasks.awaitZero(timeout, unit);
	}
	
	/**
	 * Returns the set of exceptions thrown during task execution (if any).
	 */
	public Set<Throwable> getExceptions() {
		return exceptions;
	}

}