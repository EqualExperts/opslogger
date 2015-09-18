package com.equalexperts.logging.impl;

import com.equalexperts.logging.DiagnosticContextSupplier;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static org.junit.Assert.*;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class DiagnosticContextTest {
    @Test
    public void constructor_shouldCreateAnEmptyContext_givenANullProvider() throws Exception {
        assertEquals(emptyMap(), new DiagnosticContext(null).getMergedContext());
    }

    @Test
    public void constructor_shouldCreateAnEmptyContext_givenAProviderThatReturnsNull() throws Exception {
        assertEquals(emptyMap(), new DiagnosticContext(() -> null).getMergedContext());
    }

    @Test
    public void constructor_shouldSafelyCopyTheProvidedContextInformationIntoANewMap() throws Exception {
        Map<String, String> expectedContext = new HashMap<>();
        expectedContext.put("baker", "a");
        expectedContext.put("able", "a");
        expectedContext.put("charlie", "a");

        Map<String, String> actualContext = new DiagnosticContext(() -> expectedContext).getMergedContext();

        assertNotSame(expectedContext, actualContext);
        assertEquals(expectedContext, actualContext);

        //ensure it's a copy (as opposed to an adapatation) by clearing the original
        expectedContext.clear();
        assertNotEquals(expectedContext, actualContext);
    }

    @Test
    public void constructor_shouldPreserveInsertionOrder() throws Exception {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("baker", "a");
        context.put("able", "b");
        context.put("charlie", "c");

        StringBuilder sb = new StringBuilder();

        new DiagnosticContext(() -> context).printContextInformation(sb);

        assertEquals("baker=a;able=b;charlie=c,", sb.toString());
    }

    @Test
    public void constructor_shouldCreateAnUnmodifiableMap() throws Exception {
        Map<String, String> expectedContext = new LinkedHashMap<>();
        expectedContext.put("foo", "bar");

        Map<String, String> actualContext = new DiagnosticContext(() -> expectedContext).getMergedContext();
        ensureUnmodifiableMap(actualContext);
    }

    @Test
    public void printContextInformation_shouldPrintNameValuePairsSeparatedBySemiColonsInTheCorrectOrderFollowedByAComma() throws Exception {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("baker", "a");
        context.put("able", "b");
        context.put("charlie", "c");

        StringBuilder sb = new StringBuilder();

        new DiagnosticContext(() -> context).printContextInformation(sb);

        assertEquals(sb.toString(), "baker=a;able=b;charlie=c,");
    }

    @Test
    public void printContextInformation_shouldExcludeMappingsWithEmptyValues() throws Exception {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("baker", "a");
        context.put("able", "");
        context.put("charlie", "c");

        StringBuilder sb = new StringBuilder();

        new DiagnosticContext(() -> context).printContextInformation(sb);

        assertEquals(sb.toString(), "baker=a;charlie=c,");
    }

    @Test
    public void printContextInformation_shouldExcludeMappingsWithNullValues() throws Exception {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("baker", "a");
        context.put("able", null);
        context.put("charlie", "c");

        StringBuilder sb = new StringBuilder();

        new DiagnosticContext(() -> context).printContextInformation(sb);

        assertEquals(sb.toString(), "baker=a;charlie=c,");
    }

    @Test
    public void printContextInformation_shouldNotPrintATrailingComma_givenAnEffectivelyEmptyMergedContext() throws Exception {
        Map<String, String> emptyValue = new LinkedHashMap<>();
        emptyValue.put("able", "");

        Map<String, String> nullValue = new LinkedHashMap<>();
        nullValue.put("able", null);

        StringBuilder sb = new StringBuilder();

        new DiagnosticContext(() -> emptyValue).printContextInformation(sb);
        new DiagnosticContext(() -> nullValue).printContextInformation(sb);
        new DiagnosticContext(() -> null).printContextInformation(sb);
        new DiagnosticContext(null).printContextInformation(sb);

        assertEquals(sb.toString(), "");
    }

    private void ensureUnmodifiableMap(Map<String, String> mergedContext) {
        assertSame("map should be unmodifiable", mergedContext.getClass(), unmodifiableMap(new HashMap<>()).getClass());
    }
}
