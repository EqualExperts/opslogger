package com.equalexperts.logging;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class ActiveRotationRegistry {

    private final Set<ActiveRotationSupport> registeredInstances = ConcurrentHashMap.newKeySet();

    public ActiveRotationSupport add(ActiveRotationSupport instance) {
        registeredInstances.add(instance);
        return instance;
    }

    public void remove(ActiveRotationSupport instance) {
        registeredInstances.remove(instance);
    }

    public Runnable getPostRotateEvent() {
        return () -> registeredInstances.forEach(ActiveRotationRegistry::safelyCallPostRotate);
    }

    private static void safelyCallPostRotate(ActiveRotationSupport instance) {
        try {
            instance.postRotate();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
