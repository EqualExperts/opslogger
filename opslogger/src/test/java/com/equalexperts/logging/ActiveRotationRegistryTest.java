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
    public void add_shouldAddAnEntryToTheSetOfInstancesAffectedByThePostRotateEvent() throws Exception {
        ActiveRotationSupport ars = mock(ActiveRotationSupport.class);

        registry.add(ars);

        registry.getPostRotateEvent().run();
        verify(ars).postRotate();
    }

    @Test
    public void remove_shouldRemoveAnEntryFromTheSetOfInstancesAffectedByThePostRotateEvent() throws Exception {
        ActiveRotationSupport ars = registry.add(mock(ActiveRotationSupport.class));

        registry.remove(ars);

        registry.getPostRotateEvent().run();
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
    public void postRotateEvent_shouldCallPostRotateOnEveryRegisteredActiveRotationSupportInstance() throws Exception {
        ActiveRotationSupport ars = registry.add(mock(ActiveRotationSupport.class));
        ActiveRotationSupport anotherArs = registry.add(mock(ActiveRotationSupport.class));

        Runnable event = registry.getPostRotateEvent();

        event.run();

        verify(ars).postRotate();
        verify(anotherArs).postRotate();
    }

    @Test
    public void postRotateEvent_shouldThrowARuntimeException_whenAnInstanceThrowsAnInterruptedException() throws Exception {
        InterruptedException expectedException = new InterruptedException();
        ActiveRotationSupport ars = registry.add(mock(ActiveRotationSupport.class));
        doThrow(expectedException).when(ars).postRotate();

        try {
            registry.getPostRotateEvent().run();
            fail("expected a RuntimeException");
        } catch (RuntimeException e) {
            assertSame(expectedException, e.getCause());
        }
    }
}
