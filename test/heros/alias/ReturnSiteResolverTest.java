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

public class ReturnSiteResolverTest {

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
	private TestStatement returnSite;
	private ReturnSiteResolver<TestFieldRef, TestFact, TestStatement, TestMethod> sut;
	private TestFact fact;
	private InterestCallback<TestFieldRef, TestFact, TestStatement, TestMethod> callback;
	private Resolver<TestFieldRef, TestFact, TestStatement, TestMethod> callEdgeResolver;

	@Before
	public void before() {
		analyzer = mock(PerAccessPathMethodAnalyzer.class);
		returnSite = new TestStatement("returnSite");
		sut = new ReturnSiteResolver<>(analyzer, returnSite);
		fact = new TestFact("value");
		callback = mock(InterestCallback.class);
		callEdgeResolver = mock(CallEdgeResolver.class);
	}

	@Test
	public void emptyIncomingFact() {
		sut.addIncoming(new WrappedFact<>(fact, createAccessPath(), callEdgeResolver), callEdgeResolver, getDelta());
		verify(analyzer).scheduleEdgeTo(eq(new WrappedFactAtStatement<>(returnSite, new WrappedFact<>(fact, createAccessPath(), sut))));
		assertTrue(sut.isInterestGiven());
	}

	@Test
	public void resolveViaIncomingFact() {
		sut.resolve(getDeltaConstraint("a"), callback);
		sut.addIncoming(new WrappedFact<>(fact, createAccessPath("a"), callEdgeResolver), callEdgeResolver, getDelta());
		verify(callback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(createAccessPath("a"))));
	}

	@Test
	public void registerCallbackAtIncomingResolver() {
		Resolver<TestFieldRef, TestFact, TestStatement, TestMethod> resolver = mock(Resolver.class);
		sut.addIncoming(new WrappedFact<>(fact, createAccessPath(), resolver), callEdgeResolver, getDelta());
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
		
		sut.addIncoming(new WrappedFact<>(fact, createAccessPath(), resolver), callEdgeResolver, getDelta());
		sut.resolve(getDeltaConstraint("a"), callback);
		
		verify(callback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(createAccessPath("a"))));
	}
	
	@Test
	public void resolveViaDelta() {
		sut.addIncoming(new WrappedFact<>(fact, createAccessPath(), callEdgeResolver), callEdgeResolver, getDelta("a"));
		sut.resolve(getDeltaConstraint("a"), callback);
		verify(callback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(createAccessPath("a"))));
	}
	
	@Test
	public void resolveViaDeltaTwice() {
		final InterestCallback<TestFieldRef, TestFact, TestStatement, TestMethod> innerCallback = mock(InterestCallback.class);
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				ReturnSiteResolver<TestFieldRef, TestFact, TestStatement, TestMethod> resolver = (ReturnSiteResolver<TestFieldRef, TestFact, TestStatement, TestMethod>) invocation.getArguments()[1];
				resolver.resolve(getDeltaConstraint("b"), innerCallback);
				return null;
			}
		}).when(callback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(createAccessPath("a"))));
		
		sut.addIncoming(new WrappedFact<>(fact, createAccessPath(), callEdgeResolver), callEdgeResolver, getDelta("a", "b"));
		sut.resolve(getDeltaConstraint("a"), callback);
		
		verify(innerCallback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(createAccessPath("a", "b"))));
	}
	
	@Test
	public void resolveViaDeltaAndThenViaCallSite() {
		final InterestCallback<TestFieldRef, TestFact, TestStatement, TestMethod> innerCallback = mock(InterestCallback.class);
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				ReturnSiteResolver<TestFieldRef, TestFact, TestStatement, TestMethod> resolver = (ReturnSiteResolver<TestFieldRef, TestFact, TestStatement, TestMethod>) invocation.getArguments()[1];
				resolver.resolve(getDeltaConstraint("b"), innerCallback);
				return null;
			}
		}).when(callback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(createAccessPath("a"))));
		
		sut.addIncoming(new WrappedFact<>(fact, createAccessPath(), callEdgeResolver), callEdgeResolver, getDelta("a"));
		sut.resolve(getDeltaConstraint("a"), callback);
		verify(innerCallback).canBeResolvedEmpty();
	}

	@Test
	public void resolveViaCallEdgeResolverAtCallSite() {
		sut.addIncoming(new WrappedFact<>(fact, createAccessPath(), callEdgeResolver), callEdgeResolver, getDelta());
		sut.resolve(getDeltaConstraint("a"), callback);
		verify(callback).canBeResolvedEmpty();
	}
	
	@Test
	public void resolveViaResolverAtCallSite() {
		Resolver<TestFieldRef, TestFact, TestStatement, TestMethod> resolver = mock(Resolver.class);
		sut.addIncoming(new WrappedFact<>(fact, createAccessPath(), callEdgeResolver), resolver, getDelta());
		sut.resolve(getDeltaConstraint("a"), callback);
		verify(resolver).resolve(eq(getDeltaConstraint("a")), any(InterestCallback.class));
	}
	
	@Test
	public void resolveViaResolverAtCallSiteTwice() {
		Resolver<TestFieldRef, TestFact, TestStatement, TestMethod> resolver = mock(Resolver.class);
		final Resolver<TestFieldRef, TestFact, TestStatement, TestMethod> nestedResolver = mock(Resolver.class);
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				InterestCallback innerCallback = (InterestCallback) invocation.getArguments()[1];
				innerCallback.interest(analyzer, nestedResolver);
				return null;
			}
		}).when(resolver).resolve(eq(getDeltaConstraint("a")), any(InterestCallback.class));
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				InterestCallback innerCallback = (InterestCallback) invocation.getArguments()[1];
				innerCallback.interest(analyzer, nestedResolver);
				return null;
			}
		}).when(nestedResolver).resolve(eq(getDeltaConstraint("b")), any(InterestCallback.class));
		
		final InterestCallback<TestFieldRef, TestFact, TestStatement, TestMethod> secondCallback = mock(InterestCallback.class);
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				ReturnSiteResolver<TestFieldRef, TestFact, TestStatement, TestMethod> resolver = (ReturnSiteResolver) invocation.getArguments()[1];
				resolver.resolve(getDeltaConstraint("b"), secondCallback);
				return null;
			}
			
		}).when(callback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(createAccessPath("a"))));
		
		sut.addIncoming(new WrappedFact<>(fact, createAccessPath(), callEdgeResolver), resolver, getDelta());
		sut.resolve(getDeltaConstraint("a"), callback);
		
		verify(secondCallback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(createAccessPath("a", "b"))));
	}
	
	@Test
	public void resolveAsEmptyViaIncomingResolver() {
		Resolver<TestFieldRef, TestFact, TestStatement, TestMethod> resolver = mock(Resolver.class);
		Delta<TestFieldRef> delta = new AccessPath<TestFieldRef>().getDeltaTo(new AccessPath<TestFieldRef>().appendExcludedFieldReference(new TestFieldRef("a")));
		
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				InterestCallback innerCallback = (InterestCallback) invocation.getArguments()[1];
				innerCallback.canBeResolvedEmpty();
				return null;
			}
		}).when(resolver).resolve(eq(getDeltaConstraint("a")), any(InterestCallback.class));

		sut.addIncoming(new WrappedFact<>(fact, createAccessPath(), resolver), callEdgeResolver, delta);
		sut.resolve(getDeltaConstraint("a"), callback);
		
		verify(callback, never()).canBeResolvedEmpty();
		verify(callback, never()).interest(any(PerAccessPathMethodAnalyzer.class), any(Resolver.class));
	}
	
	@Test
	public void resolveViaCallSiteResolver() {
		Resolver<TestFieldRef, TestFact, TestStatement, TestMethod> resolver = mock(Resolver.class);
		
		sut.addIncoming(new WrappedFact<>(fact, createAccessPath(), callEdgeResolver), resolver, getDelta());
		sut.resolve(getDeltaConstraint("a"), callback);
		
		verify(resolver).resolve(eq(getDeltaConstraint("a")), any(InterestCallback.class));
	}
	
	private class ReturnSiteResolverArgumentMatcher extends
			ArgumentMatcher<ReturnSiteResolver<TestFieldRef, TestFact, TestStatement, TestMethod>> {

		private AccessPath<TestFieldRef> accPath;

		public ReturnSiteResolverArgumentMatcher(AccessPath<TestFieldRef> accPath) {
			this.accPath = accPath;
		}

		@Override
		public boolean matches(Object argument) {
			ReturnSiteResolver resolver = (ReturnSiteResolver) argument;
			return resolver.isInterestGiven() && resolver.getResolvedAccessPath().equals(accPath) && resolver.getReturnSite().equals(returnSite);
		}
	}
}
