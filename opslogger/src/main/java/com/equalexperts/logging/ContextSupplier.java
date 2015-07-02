package com.equalexperts.logging;

import java.util.Map;

@FunctionalInterface
public interface ContextSupplier {
    Map<String,String> getMessageContext();
}