/*******************************************************************************
 * Copyright (c) 2014 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros;

import org.junit.Before;
import org.junit.Test;
import static heros.utilities.TestHelper.*;

import heros.utilities.TestHelper;

public class BiDiIFDSSolverTest {

	private TestHelper forwardHelper;
	private TestHelper backwardHelper;
	
	@Before
	public void before() {
		forwardHelper = new TestHelper();
		backwardHelper = new TestHelper();
	}
	
	@Test
	public void happyPath() {
		forwardHelper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b"),
				normalStmt("b").succ("c", flow("0", "1")),
				exitStmt("c").expectArtificalFlow(flow("1")));
		
		backwardHelper.method("foo",
				startPoints("c"),
				normalStmt("c").succ("b"),
				normalStmt("b").succ("a", flow("0", "2")),
				exitStmt("a").expectArtificalFlow(flow("2")));
		
		forwardHelper.runBiDiSolver(backwardHelper, "b");
	}
}
