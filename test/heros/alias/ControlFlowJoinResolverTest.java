/*******************************************************************************
 * Copyright (c) 2015 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.alias;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import heros.alias.AccessPath.Delta;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;

public class ControlFlowJoinResolverTest {

	private static DeltaConstraint<TestFieldRef> getDeltaConstraint(String... fieldRefs) {
		return new DeltaConstraint<>(getDelta(fieldRefs));
	}

	private static Delta<TestFieldRef> getDelta(String... fieldRefs) {
		AccessPath<TestFieldRef> accPath = createAccessPath(fieldRefs);
		return new AccessPath<TestFieldRef>().getDeltaTo(accPath);
	}

	protected static AccessPath<TestFieldRef> createAccessPath(String... fieldRefs) {
		AccessPath<TestFieldRef> accPath = new AccessPath<>();
		for (String fieldRef : fieldRefs) {
			accPath = accPath.addFieldReference(new TestFieldRef(fieldRef));
		}
		return accPath;
	}

	private PerAccessPathMethodAnalyzer<TestFieldRef, TestFact, TestStatement, TestMethod> analyzer;
	private TestStatement joinStmt;
	private ControlFlowJoinResolver<TestFieldRef, TestFact, TestStatement, TestMethod> sut;
	private TestFact fact;
	private InterestCallback<TestFieldRef, TestFact, TestStatement, TestMethod> callback;
	private Resolver<TestFieldRef, TestFact, TestStatement, TestMethod> callEdgeResolver;

	@Before
	public void before() {
		analyzer = mock(PerAccessPathMethodAnalyzer.class);
		joinStmt = new TestStatement("joinStmt");
		sut = new ControlFlowJoinResolver<>(analyzer, joinStmt);
		fact = new TestFact("value");
		callback = mock(InterestCallback.class);
		callEdgeResolver = mock(CallEdgeResolver.class);
	}

	@Test
	public void emptyIncomingFact() {
		sut.addIncoming(new WrappedFact<>(fact, createAccessPath(), callEdgeResolver));
		verify(analyzer).processFlowFromJoinStmt(eq(new WrappedFactAtStatement<>(joinStmt, new WrappedFact<>(fact, createAccessPath(), sut))));
		assertTrue(sut.isInterestGiven());
	}

	@Test
	public void resolveViaIncomingFact() {
		sut.resolve(getDeltaConstraint("a"), callback);
		sut.addIncoming(new WrappedFact<>(fact, createAccessPath("a"), callEdgeResolver));
		verify(callback).interest(eq(analyzer), argThat(new ResolverArgumentMatcher(createAccessPath("a"))));
	}

	@Test
	public void registerCallbackAtIncomingResolver() {
		Resolver<TestFieldRef, TestFact, TestStatement, TestMethod> resolver = mock(Resolver.class);
		sut.addIncoming(new WrappedFact<>(fact, createAccessPath(), resolver));
		sut.resolve(getDeltaConstraint("a"), callback);
		verify(resolver).resolve(eq(getDeltaConstraint("a")), any(InterestCallback.class));
	}
	
	@Test
	public void resolveViaIncomingResolver() {
		Resolver<TestFieldRef, TestFact, TestStatement, TestMethod> resolver = mock(Resolver.class);
		final Resolver<TestFieldRef, TestFact, TestStatement, TestMethod> nestedResolver = mock(Resolver.class);
		Mockito.doAnswer(new Answer(){
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				InterestCallback<TestFieldRef, TestFact, TestStatement, TestMethod> argCallback = 
						(InterestCallback<TestFieldRef, TestFact, TestStatement, TestMethod>) invocation.getArguments()[1];
				argCallback.interest(null, nestedResolver);
				return null;
			}
		}).when(resolver).resolve(eq(getDeltaConstraint("a")), any(InterestCallback.class));
		
		sut.addIncoming(new WrappedFact<>(fact, createAccessPath(), resolver));
		sut.resolve(getDeltaConstraint("a"), callback);
		
		verify(callback).interest(eq(analyzer), argThat(new ResolverArgumentMatcher(createAccessPath("a"))));
	}
	
	
	private class ResolverArgumentMatcher extends
			ArgumentMatcher<ReturnSiteResolver<TestFieldRef, TestFact, TestStatement, TestMethod>> {

		private AccessPath<TestFieldRef> accPath;

		public ResolverArgumentMatcher(AccessPath<TestFieldRef> accPath) {
			this.accPath = accPath;
		}

		@Override
		public boolean matches(Object argument) {
			ControlFlowJoinResolver resolver = (ControlFlowJoinResolver) argument;
			return resolver.isInterestGiven() && resolver.getResolvedAccessPath().equals(accPath) && resolver.getJoinStmt().equals(joinStmt);
		}
	}
}
