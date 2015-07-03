package com.equalexperts.logging.impl;

import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class DiagnosticContextTest {
    @Test
    public void constructor_shouldSafelyCopyAllProvidedContextInformationIntoASingleMergedContext() throws Exception {
        Map<String, String> globalContext = new HashMap<>();
        globalContext.put("foo", "a");
        globalContext.put("bar", "a");
        Map<String, String> localContext = new HashMap<>();
        localContext.put("bar", "b");

        Map<String, String> globalContextSpy = spy(globalContext);
        Map<String, String> localContextSpy = spy(localContext);

        DiagnosticContext dc = new DiagnosticContext(globalContext, localContext);

        Map<String, String> mergedContext = dc.getMergedContext();

        assertEquals(2, mergedContext.size());
        ensureUnmodifiableMap(mergedContext);
        ensureNotModified(globalContextSpy);
        ensureNotModified(localContextSpy);
        assertEquals("b", mergedContext.get("bar"));
    }

    @Test
    public void constructor_shouldRespectParameterOrder_whenMergingContextInformation() throws Exception {
        //later contexts should override earlier ones

        Map<String, String> globalContext = new LinkedHashMap<>();
        globalContext.put("foo", "a");
        globalContext.put("bar", "a");

        Map<String, String> localContext = new LinkedHashMap<>();
        localContext.put("foo", "b");
        localContext.put("baz", "b");

        assertEquals("b", new DiagnosticContext(globalContext, localContext).getMergedContext().get("foo"));
        assertEquals("a", new DiagnosticContext(localContext, globalContext).getMergedContext().get("foo"));
    }

    @Test
    public void constructor_shouldPreserveInsertionOrder_whenMergingContexts() throws Exception {
        Map<String, String> globalContext = new LinkedHashMap<>();
        globalContext.put("baker", "a");
        globalContext.put("able", "a");
        globalContext.put("charlie", "a");

        Map<String, String> localContext = new LinkedHashMap<>();
        localContext.put("able", "b");
        localContext.put("dog", "b");

        Map<String, String> mergedContext = new DiagnosticContext(globalContext, localContext).getMergedContext();

        List<String> keysInOrder = mergedContext.keySet().stream().collect(toList());
        assertThat(keysInOrder, contains("baker", "able", "charlie", "dog"));
    }

    @Test
    public void constructor_shouldIgnoreNullContexts() throws Exception {
        Map<String, String> context = new HashMap<>();
        context.put("foo", "a");

        assertEquals(context, new DiagnosticContext(context, null).getMergedContext());
        assertEquals(context, new DiagnosticContext(null, context).getMergedContext());
    }

    @Test
    public void constructor_shouldCreateAnEmptyMergedContext_whenAllProvidedContextsAreNull() throws Exception {
        assertEquals(emptyMap(), new DiagnosticContext(null, null, null).getMergedContext());
    }

    @Test
    public void constructor_shouldCreateAnEmptyMergedContext_whenNoContextsAreProvided() throws Exception {
        try {
            new DiagnosticContext();
            fail("expected an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Must provide at least one context");
        }
    }
    
    @Test
    public void constructor_shouldThrowAnIllegalArgumentException_givenANullVarargs() throws Exception {
        try {
            new DiagnosticContext((Map<String,String>[]) null);
            fail("expected an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Must provide at least one context");
        }
    }

    @Test
    public void printContextInformation_shouldPrintNameValuePairsSeparatedBySemiColonsInTheCorrectOrderFollowedByAComma() throws Exception {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("baker", "a");
        context.put("able", "b");
        context.put("charlie", "c");

        StringBuilder sb = new StringBuilder();

        new DiagnosticContext(context).printContextInformation(sb);

        assertEquals(sb.toString(), "baker=a;able=b;charlie=c,");
    }

    @Test
    public void printContextInformation_shouldExcludeMappingsWithEmptyValues() throws Exception {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("baker", "a");
        context.put("able", "");
        context.put("charlie", "c");

        StringBuilder sb = new StringBuilder();

        new DiagnosticContext(context).printContextInformation(sb);

        assertEquals(sb.toString(), "baker=a;charlie=c,");
    }

    @Test
    public void printContextInformation_shouldExcludeMappingsWithNullValues() throws Exception {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("baker", "a");
        context.put("able", null);
        context.put("charlie", "c");

        StringBuilder sb = new StringBuilder();

        new DiagnosticContext(context).printContextInformation(sb);

        assertEquals(sb.toString(), "baker=a;charlie=c,");
    }

    @Test
    public void printContextInformation_shouldNotPrintATrailingComma_givenAnEffectivelyEmptyMergedContext() throws Exception {
        Map<String, String> emptyValue = new LinkedHashMap<>();
        emptyValue.put("able", "");

        Map<String, String> nullValue = new LinkedHashMap<>();
        nullValue.put("able", null);

        StringBuilder sb = new StringBuilder();

        new DiagnosticContext(emptyValue).printContextInformation(sb);
        new DiagnosticContext(nullValue).printContextInformation(sb);
        new DiagnosticContext((Map<String, String>) null).printContextInformation(sb);

        assertEquals(sb.toString(), "");
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    private void ensureNotModified(Map<String, String> spy) {
        verify(spy, never()).put(anyString(), anyString());
        verify(spy, never()).remove(any());
        verify(spy, never()).remove(any(), any());
        verify(spy, never()).clear();
        verify(spy, never()).putAll(any());
        verify(spy, never()).putIfAbsent(anyString(), anyString());
        verify(spy, never()).compute(anyString(), any());
        verify(spy, never()).computeIfAbsent(anyString(), any());
        verify(spy, never()).computeIfPresent(anyString(), any());
        verify(spy, never()).replace(anyString(), anyString(), anyString());
        verify(spy, never()).replace(anyString(), anyString());
        verify(spy, never()).replaceAll(any());
        verify(spy, never()).merge(anyString(), anyString(), any());
    }

    private void ensureUnmodifiableMap(Map<String, String> mergedContext) {
        assertSame("map should be unmodifiable", mergedContext.getClass(), unmodifiableMap(new HashMap<>()).getClass());
    }
}
