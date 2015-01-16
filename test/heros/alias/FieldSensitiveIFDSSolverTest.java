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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;

import static heros.alias.TestHelper.*;

public class FieldSensitiveIFDSSolverTest {

	private TestHelper helper;

	@Before
	public void before() {
		System.err.println("-----");
		helper = new TestHelper();
	}
	
	@Rule
	public TestWatcher watcher = new TestWatcher() {
		protected void failed(Throwable e, org.junit.runner.Description description) {
			System.err.println("---failed: "+description.getMethodName()+" ----");
		};
	};
	
	@Test
	@Ignore("assumes k-limiting not used")
	public void mergeWithExistingPrefixFacts() {
		helper.method("foo", 
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1")),
				normalStmt("b").succ("b", flow("1", "1.f")).succ("c", flow("1", "2")),
				normalStmt("c").succ("d", kill("2")));
				
		helper.runSolver(false, "a");
	}
	
	@Test
	public void dontMergeWithExistingNonPrefixFacts() {
		helper.method("foo", 
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1.f")),
				normalStmt("b").succ("b", flow("1.f", "1"), kill("1")).succ("c", flow("1.f", "2"), kill("1")),
				normalStmt("c").succ("d", kill("2")));
				
		helper.runSolver(false, "a");
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
				callSite("c").calls("xyz", flow("2", "3")));
		
		helper.method("xyz", 
				startPoints("d"),
				normalStmt("d").succ("e", flow("3", readField("f"), "4")),
				normalStmt("e").succ("f"	, kill("4")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void prefixFactOfSummaryIgnored() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0","1")),
				callSite("b").calls("bar", flow("1", "2.f")).retSite("e", kill("1")),
				callSite("e").calls("bar", flow("4", "2")).retSite("f", kill("4")),
				normalStmt("f").succ("g"));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c").succ("d", flow("2", readField("f"), "3")),
				exitStmt("d").returns(over("b"), to("e"), flow("3", "4")).returns(over("e"), to("f")));
		
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
	@Ignore("assumes k-limiting not used")
	public void loopAndMerge() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1")),
				normalStmt("b").succ("c", flow("1", readField("f"), "2"), flow("1", "1"), flow("2", "2")),
				normalStmt("c").succ("d", flow("1", "1", "1.f"), flow("2", "2")),
				normalStmt("d").succ("e", flow("1", "1"), flow("2", "2")).succ("b", flow("1", "1"), flow("2", "2")),
				normalStmt("e").succ("f", kill("1"), kill("2")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void doNotMergePrefixFacts() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b1", flow("0", "1")).succ("b2", flow("0", "1")),
				normalStmt("b1").succ("c", flow("1", "1")),
				normalStmt("b2").succ("c", flow("1", "1.f")),
				normalStmt("c").succ("d", kill("1"), kill("1.f")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void pauseOnOverwrittenFieldOfInterest() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1.f")),
				callSite("b").calls("bar", flow("1.f", "2.f")));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c").succ("d", flow("2", writeField("f"), "2^f")),
				normalStmt("d").succ("e")); //only interested in 2.f, but f excluded so this should not be reached
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void pauseOnOverwrittenFieldOfInterest2() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1.f")),
				callSite("b").calls("bar", flow("1.f", "2.f.g")));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c").succ("d", flow("2", writeField("f"), "2^f")),
				normalStmt("d").succ("e")); //only interested in 2.f.g, but f excluded so this should not be reached
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void doNotPauseOnOverwrittenFieldOfInterestedPrefix() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1.f")),
				callSite("b").calls("bar", flow("1.f", "2")));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c").succ("d", flow("2", writeField("f"), "2^f")),
				normalStmt("d").succ("e", kill("2^f"))); 
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void pauseOnTransitiveExclusion() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1.f")),
				callSite("b").calls("bar", flow("1.f", "2.f")));
		
		helper.method("bar",
				startPoints("c"),
				callSite("c").calls("xyz", flow("2", "3")));
		
		helper.method("xyz",
				startPoints("d"),
				normalStmt("d").succ("e", flow("3", writeField("f"), "3^f")),
				normalStmt("e").succ("f")); 
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void resumePausedOnTransitiveExclusion() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1.f")),
				callSite("b").calls("bar", flow("1.f", "2.f")));
		
		helper.method("bar",
				startPoints("c"),
				callSite("c").calls("xyz", flow("2", "3")));
		
		helper.method("xyz",
				startPoints("d"),
				normalStmt("d").succ("e", flow("3", writeField("f"), "3^f"), flow("3", "4")),
				callSite("e").calls("bar", flow("4", "2.g"), kill("3^f"))); 
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void resumeEdgePausedOnOverwrittenField() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1.f")),
				callSite("b").calls("bar", flow("1.f", "2.f")).retSite("e", kill("1.f")),
				callSite("e").calls("bar", flow("4", "2.g")).retSite("f", kill("4")));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c").succ("d", flow("2", writeField("f"), "2^f"), flow("2", "3")),
				exitStmt("d").returns(over("b"), to("e"), flow("3.f", "4")).returns(over("e"), to("f"), kill("3.g"), kill("2.g" /* 2^f is back substituted to 2.g*/))); 
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void resumeEdgePausedOnOverwrittenFieldForPrefixes() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1.f")),
				callSite("b").calls("bar", flow("1.f", "2.f")).retSite("e", kill("1.f")),
				callSite("e").calls("bar", flow("4", "2")).retSite("f", kill("4")));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c").succ("d", flow("2", writeField("f"), "2^f"), flow("2", "3")),
				exitStmt("d").returns(over("b"), to("e"), flow("3.f", "4")).returns(over("e"), to("f"), kill("3"), kill("2^f"))); 
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void exclusionOnPotentiallyInterestedCaller() {
		helper.method("foo",
				startPoints("sp"),
				normalStmt("sp").succ("a", flow("0", "1")),
				callSite("a").calls("bar", flow("1", "1^f")).retSite("d", kill("1")));
		
		helper.method("bar",
				startPoints("b"),
				normalStmt("b").succ("c", flow("1", readField("f"), "2.f")),
				exitStmt("c").returns(over("a"), to("d")));
		
		helper.runSolver(false, "sp");
	}
	
	@Test
	public void registerPausedEdgeInLateCallers() {
		helper.method("foo", 
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1.g")),
				callSite("b").calls("bar", flow("1.g", "1.g")).retSite("e", kill("1.g")),
				normalStmt("e").succ("f", flow("1.g", "3")),
				callSite("f").calls("bar", flow("3", "1")).retSite("g", kill("3"))); 
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c").succ("d", flow("1", readField("f"), "2"), flow("1", "1")),
				exitStmt("d").returns(over("b"), to("e"), flow("1.g", "1.g") /* ignore fact 2, not possible with this caller ctx*/).returns(over("f"), to("g"), kill("1"), kill("2")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	@Ignore("assumes alternative to k-limitting is used")
	public void mergeExcludedField() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1")),
				normalStmt("b").succ("c", flow("1", "2", "2^f")),
				normalStmt("c").succ("d", kill("2")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void resumeOnTransitiveInterestedCaller() {
		helper.method("foo",
				startPoints("sp"),
				normalStmt("sp").succ("a", flow("0", "1.f")),
				callSite("a").calls("bar", flow("1.f", "1.f")).retSite("f", kill("1.f")),
				callSite("f").calls("bar", flow("2", "1.g")));
				
		helper.method("bar",
				startPoints("b"),
				callSite("b").calls("xyz", flow("1", "1")).retSite("e", kill("1")),
				exitStmt("e").returns(over("a"), to("f"), flow("2", "2")));
		
		helper.method("xyz",
				startPoints("c"),
				normalStmt("c").succ("d", flow("1", readField("g"), "3"), flow("1", readField("f"), "2")),
				exitStmt("d").returns(over("b"), to("e"), flow("2", "2"), kill("3")));
		
				
		helper.runSolver(false, "sp");
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
	
	@Test
	public void pauseEdgeMutuallyRecursiveCallers() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1.x")),
				callSite("b").calls("bar",flow("1.x", "2.x")));
		
		helper.method("bar",
				startPoints("c"),
				callSite("c").calls("xyz", flow("2", "3")));
		
		helper.method("xyz",
				startPoints("d"),
				callSite("d").calls("bar", flow("3", "2")).retSite("e", flow("3", "3")),
				normalStmt("e").succ("f", flow("3", readField("f"), "4")));
				
		helper.runSolver(false, "a");
	}
	
	@Test
	public void pauseDiamondShapedCallerChain() {
		helper.method("bar",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1.x")),
				callSite("b").calls("foo1", flow("1.x", "2.x")).calls("foo2", flow("1.x", "2.x")));
		
		helper.method("foo1",
				startPoints("c1"),
				callSite("c1").calls("xyz", flow("2", "3")));
		
		helper.method("foo2",
				startPoints("c2"),
				callSite("c2").calls("xyz", flow("2", "3")));
		
		helper.method("xyz",
				startPoints("d"),
				normalStmt("d").succ("e", flow("3", readField("f"), "4")),
				normalStmt("e").succ("f"));
		
		helper.runSolver(false, "a");
	}

	@Test
	public void dontPauseDiamondShapedCallerChain() {
		helper.method("bar",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1.x")),
				callSite("b").calls("foo1", flow("1.x", "2.f")).calls("foo2", flow("1.x", "2.f")));
		
		helper.method("foo1",
				startPoints("c1"),
				callSite("c1").calls("xyz", flow("2", "3")));
		
		helper.method("foo2",
				startPoints("c2"),
				callSite("c2").calls("xyz", flow("2", "3")));
		
		helper.method("xyz",
				startPoints("d"),
				normalStmt("d").succ("e", flow("3", readField("f"), "4")),
				normalStmt("e").succ("f", kill("4")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void correctDeltaConstraintApplication() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1")),
				callSite("b").calls("bar", flow("1", "1")));
		
		helper.method("bar",
				startPoints("c"),
				normalStmt("c").succ("d", flow("1", writeField("a"), "1^a")),
				callSite("d").calls("xyz", flow("1^a", "1^a")));
		
		helper.method("xyz",
				startPoints("e"),
				normalStmt("e").succ("f", flow("1", readField("f"), "2")),
				callSite("f").calls("baz", flow("2", "3")));
		
		helper.method("baz",
				startPoints("g"),
				normalStmt("g").succ("h", flow("3", readField("a"), "4")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void pauseForSameSourceMultipleTimes() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1.f")),
				callSite("b").calls("bar", flow("1.f", "2.f")));
				
		helper.method("bar",
				startPoints("c"),
				normalStmt("c").succ("d", flow("2", readField("x"), "3"), flow("2", "2")),
				normalStmt("d").succ("e", flow("2", readField("x"), "4")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void pauseForSameSourceMultipleTimesTransitively() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b", flow("0", "1.f")),
				callSite("b").calls("xyz", flow("1.f", "2.f")).retSite("f", flow("1.f", "1.f")),
				callSite("f").calls("xyz", flow("1.f", "2.f")));
		
		helper.method("xyz",
				startPoints("g"),
				callSite("g").calls("bar", flow("2", "2")));
				
		helper.method("bar",
				startPoints("c"),
				normalStmt("c").succ("d", flow("2", readField("x"), "3"), flow("2", "2")),
				normalStmt("d").succ("e", flow("2", readField("x"), "4")));
		
		helper.runSolver(false, "a");
	}
}
