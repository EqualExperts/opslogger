package com.equalexperts.logging;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class ActiveRotationRegistry {

    private final Set<ActiveRotationSupport> registeredInstances = ConcurrentHashMap.newKeySet();

    public <T extends ActiveRotationSupport> T add(T instance) {
        registeredInstances.add(instance);
        return instance;
    }

    public void remove(ActiveRotationSupport instance) {
        registeredInstances.remove(instance);
    }

    public boolean contains(ActiveRotationSupport instance) {
        return registeredInstances.contains(instance);
    }

    public void refreshFileHandles() {
        registeredInstances.forEach(ActiveRotationRegistry::safelyCallPostRotate);
    }

    private static void safelyCallPostRotate(ActiveRotationSupport instance) {
        try {
            instance.refreshFileHandles();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
