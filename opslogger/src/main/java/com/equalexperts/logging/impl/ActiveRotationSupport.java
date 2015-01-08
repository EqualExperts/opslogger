package com.equalexperts.logging.impl;

public interface ActiveRotationSupport {
    void refreshFileHandles() throws InterruptedException;
}
