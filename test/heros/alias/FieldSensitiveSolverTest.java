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
package heros.alias;



import org.junit.Before;
import org.junit.Test;

import static heros.alias.TestHelper.*;

public class FieldSensitiveSolverTest {

	private TestHelper helper;

	@Before
	public void before() {
		helper = new TestHelper();
	}
	
	@Test
	public void fieldReadAndWrite() {
		helper.method("bar", 
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1")),
				normalStmt("b").succ("c", flow("1", writeField("f"), "2.f")),
				normalStmt("c").succ("d", flow("2.f", "2.f")),
				normalStmt("d").succ("e", flow("2.f", readField("f"), "4")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void createSummaryForBaseValue() {
		helper.method("bar", 
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1")),
				normalStmt("b").succ("c", flow("1", writeField("field"), "2.field")),
				callSite("c").calls("foo", flow("2.field", "3.field")));
		
		helper.method("foo",startPoints("d"),
				normalStmt("d").succ("e", flow("3", "4")),
				normalStmt("e").succ("f", flow("4","4")));
		helper.runSolver(false, "a");
	}
	
	@Test
	public void reuseSummaryForBaseValue() {
		helper.method("bar", 
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1")),
				normalStmt("b").succ("c", flow("1", writeField("field"), "2.field")),
				callSite("c").calls("foo", flow("2.field", "3.field")).retSite("retC", flow("2.field", "2.field")));
		
		helper.method("foo",startPoints("d"),
				normalStmt("d").succ("e", flow("3", "4")),
				normalStmt("e").succ("f", flow("4","4")),
				exitStmt("f").returns(over("c"), to("retC"), flow("4.field", "5.field")).returns(over("g"), to("retG"), flow("4.anotherField", "6.anotherField")));

		helper.method("xyz", 
				startPoints("g"),
				callSite("g").calls("foo", flow("0", "3.anotherField")).retSite("retG", kill("0")));
		
		helper.runSolver(false, "a", "g");
	}
	
	@Test
	public void hold() {
		helper.method("bar", 
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1")),
				normalStmt("b").succ("c", flow("1", readField("field"), "2.field")),
				callSite("c").calls("foo", flow("2.field", "3.field")));
		
		helper.method("foo",
				startPoints("d"),
				normalStmt("d").succ("e", flow("3", readField("notfield"), "5"), flow("3", "3")),
				normalStmt("e").succ("f", flow("3","4")));
		helper.runSolver(false, "a");
	}
	
	@Test
	public void holdAndResume() {
		helper.method("bar", 
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1")),
				normalStmt("b").succ("c", flow("1", writeField("field"), "2.field")),
				callSite("c").calls("foo", flow("2.field", "3.field")).retSite("rs", kill("2.field")),
				callSite("rs").calls("foo", flow("5", "3.notfield")));
		
		helper.method("foo",startPoints("d"),
				normalStmt("d").succ("e", flow("3", "3"), flow("3", readField("notfield"), "6")),
				normalStmt("e").succ("f", flow("3","4"), kill("6")),
				exitStmt("f").returns(over("c"), to("rs"), flow("4.field", "5")));
		
		helper.runSolver(false, "a", "g");
	}

	@Test
	public void doNotHoldIfInterestedTransitiveCallerExists() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", readField("f"), "1.f")),
				callSite("b").calls("bar", flow("1.f", "2.f")));
		
		helper.method("bar",
				startPoints("c"),
				callSite("c").calls("xyz", flow("2", "3"), flow("2.f", "3.f")));
		
		helper.method("xyz", 
				startPoints("d"),
				normalStmt("d").succ("e", flow("3", readField("f"), "4")),
				normalStmt("e").succ("f"	, kill("4")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void prefixFactOfOnHoldFactIncoming() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1")),
				callSite("b").calls("bar", flow("1", "2.f")).retSite("e", kill("1")),
				callSite("e").calls("bar", flow("2", "2")).retSite("g", kill("2")));
		
		helper.method("bar", 
				startPoints("c"),
				normalStmt("c").succ("d", flow("2", readField("g"), "3"), flow("2", "2")),
				exitStmt("d").returns(over("b"), to("e"), flow("2.f", "2")).returns(over("e"), to("g"),  kill("2"), kill("3")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void doNotPauseZeroSources() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", readField("f"), "1.f")),
				normalStmt("b").succ("c", kill("1.f")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void happyPath() {
		helper.method("bar", 
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "x")),
				normalStmt("b").succ("c", flow("x", "x")),
				callSite("c").calls("foo", flow("x", "y")).retSite("f", flow("x", "x")));
		
		helper.method("foo",
				startPoints("d"),
				normalStmt("d").succ("e", flow("y", "y", "z")),
				exitStmt("e").returns(over("c"), to("f"), flow("z", "u"), flow("y")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void reuseSummary() {
		helper.method("foo", 
				startPoints("a"),
				callSite("a").calls("bar", flow("0", "x")).retSite("b", flow("0", "y")),
				callSite("b").calls("bar", flow("y", "x")).retSite("c", flow("y")),
				normalStmt("c").succ("c0", flow("w", "0")));
		
		helper.method("bar",
				startPoints("d"),
				normalStmt("d").succ("e", flow("x", "z")),
				exitStmt("e").returns(over("a"), to("b"), flow("z", "y"))
							  .returns(over("b"), to("c"), flow("z", "w")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void reuseSummaryForRecursiveCall() {
		helper.method("foo",
				startPoints("a"),
				callSite("a").calls("bar", flow("0", "1")).retSite("b", flow("0")),
				normalStmt("b").succ("c", flow("2", "3")));
		
		helper.method("bar",
				startPoints("g"),
				normalStmt("g").succ("h", flow("1", "1")).succ("i", flow("1", "1")),
				callSite("i").calls("bar", flow("1", "1")).retSite("h", flow("1")),
				exitStmt("h").returns(over("a"), to("b"), flow("1"), flow("2" ,"2"))
							.returns(over("i"), to("h"), flow("1","2"), flow("2", "2")));
				
		helper.runSolver(false, "a");
	}
	
	@Test
	public void branch() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b1", flow("0", "x")).succ("b2", flow("0", "x")),
				normalStmt("b1").succ("c", flow("x", "x", "y")),
				normalStmt("b2").succ("c", flow("x", "x")),
				normalStmt("c").succ("d", flow("x", "z"), flow("y", "w")),
				normalStmt("d").succ("e", flow("z"), flow("w")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void unbalancedReturn() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1")),
				exitStmt("b").returns(over("x"),  to("y"), flow("1", "1")));
		
		helper.method("bar", 
				startPoints("unused"),
				normalStmt("y").succ("z", flow("1", "2")));
		
		helper.runSolver(true, "a");
	}
	
	@Test
	public void artificalReturnEdgeForNoCallersCase() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1")),
				exitStmt("b").returns(null, null, flow("1", "1")));
		
		helper.runSolver(true, "a");
	}
	
}
