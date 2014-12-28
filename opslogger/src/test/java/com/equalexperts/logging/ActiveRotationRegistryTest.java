package com.equalexperts.logging;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ActiveRotationRegistryTest {

    private final ActiveRotationRegistry registry = new ActiveRotationRegistry();

    @Test
    public void add_shouldReturnTheProvidedArgument() throws Exception {
        ActiveRotationSupport ars = mock(ActiveRotationSupport.class);

        ActiveRotationSupport result = registry.add(ars);

        assertSame(ars, result);
    }

    @Test
    public void add_shouldAddAnEntryToTheSetOfInstancesAffectedByRefreshFileHandles() throws Exception {
        ActiveRotationSupport ars = mock(ActiveRotationSupport.class);

        registry.add(ars);

        registry.refreshFileHandles();
        verify(ars).postRotate();
    }

    @Test
    public void add_shouldNotRequireACast_givenAnImplementationOfActiveRotationSupport() throws Exception {
        @SuppressWarnings("UnusedDeclaration") //assignment necessary for the test — we're really testing that this compiles
        Foo foo = registry.add(new Foo());
    }

    @Test
    public void remove_shouldRemoveAnEntryFromTheSetOfInstancesAffectedByRefreshFileHandles() throws Exception {
        ActiveRotationSupport ars = registry.add(mock(ActiveRotationSupport.class));

        registry.remove(ars);

        registry.refreshFileHandles();
        verifyZeroInteractions(ars);
    }

    @Test
    public void remove_shouldNotComplain_givenAnUnregisteredInstance() throws Exception {
        ActiveRotationSupport ars = mock(ActiveRotationSupport.class);

        registry.remove(ars);
    }

    @Test
    public void contains_shouldReturnTrue_givenAnAddedInstance() throws Exception {
        ActiveRotationSupport ars = mock(ActiveRotationSupport.class);

        registry.add(ars);

        assertTrue(registry.contains(ars));
    }

    @Test
    public void contains_shouldReturnFalse_givenAnInstanceThatHasNotBeenAdded() throws Exception {
        ActiveRotationSupport ars = mock(ActiveRotationSupport.class);

        assertFalse(registry.contains(ars));
    }

    @Test
    public void contains_shouldReturnFalse_givenAnInstanceThatHasBeenAddedAndRemoved() throws Exception {
        ActiveRotationSupport ars = mock(ActiveRotationSupport.class);

        registry.add(ars);
        registry.remove(ars);

        assertFalse(registry.contains(ars));
    }

    @Test
    public void refreshFileHandles_shouldCallPostRotateOnEveryRegisteredActiveRotationSupportInstance() throws Exception {
        ActiveRotationSupport ars = registry.add(mock(ActiveRotationSupport.class));
        ActiveRotationSupport anotherArs = registry.add(mock(ActiveRotationSupport.class));

        registry.refreshFileHandles();

        verify(ars).postRotate();
        verify(anotherArs).postRotate();
    }

    @Test
    public void refreshFileHandles_shouldThrowARuntimeException_whenAnInstanceThrowsAnInterruptedException() throws Exception {
        InterruptedException expectedException = new InterruptedException();
        ActiveRotationSupport ars = registry.add(mock(ActiveRotationSupport.class));
        doThrow(expectedException).when(ars).postRotate();

        try {
            registry.refreshFileHandles();
            fail("expected a RuntimeException");
        } catch (RuntimeException e) {
            assertSame(expectedException, e.getCause());
        }
    }

    @Test
    public void refreshFileHandles_shouldNotCallPostRotateOnAnInstanceThatHasNotBeenAdded() throws Exception {
        ActiveRotationSupport ars = mock(ActiveRotationSupport.class);

        registry.refreshFileHandles();

        verifyZeroInteractions(ars);
    }

    @Test
    public void refreshFileHandles_shouldNotCallPostRotateOnAnInstanceThatHasBeenRemoved() throws Exception {
        ActiveRotationSupport ars = mock(ActiveRotationSupport.class);
        registry.add(ars);
        registry.remove(ars);

        registry.refreshFileHandles();

        verifyZeroInteractions(ars);
    }

    private static class Foo implements ActiveRotationSupport {
        @Override
        public void postRotate() throws InterruptedException {
            //not relevant for this test
        }
    }
}
