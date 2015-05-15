package com.equalexperts.logging.impl;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveRotationRegistry {

    private static ActiveRotationRegistry singletonInstance = new ActiveRotationRegistry();

    private final Set<ActiveRotationSupport> registeredInstances = ConcurrentHashMap.newKeySet();

    public static ActiveRotationRegistry getSingletonInstance() {
        return singletonInstance;
    }

    public static void setSingletonInstance(ActiveRotationRegistry newRegistry) {
        singletonInstance = newRegistry;
    }

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
