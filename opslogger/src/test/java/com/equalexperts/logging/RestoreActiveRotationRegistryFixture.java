package com.equalexperts.logging;

import com.equalexperts.logging.impl.ActiveRotationRegistry;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class RestoreActiveRotationRegistryFixture implements TestRule {
    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                ActiveRotationRegistry original = ActiveRotationRegistry.getSingletonInstance();
                ActiveRotationRegistry.setSingletonInstance(new ActiveRotationRegistry());
                try {
                    base.evaluate();
                } finally {
                    ActiveRotationRegistry.setSingletonInstance(original);
                }
            }
        };
    }
}
