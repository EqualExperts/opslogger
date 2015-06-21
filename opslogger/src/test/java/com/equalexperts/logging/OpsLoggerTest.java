package com.equalexperts.logging;

import com.equalexperts.logging.impl.ActiveRotationRegistry;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class OpsLoggerTest {

    @Rule
    public RestoreActiveRotationRegistryFixture registryFixture = new RestoreActiveRotationRegistryFixture();

    @Test
    public void refreshFileHandles_shouldRefreshFileHandlesOnAllRegisteredDestinationsThatSupportActiveRotation() throws Exception {
        ActiveRotationRegistry registry = spy(new ActiveRotationRegistry());
        ActiveRotationRegistry.setSingletonInstance(registry);

        OpsLogger.refreshFileHandles();

        verify(registry).refreshFileHandles();
    }
}
