package com.equalexperts.logging;

interface ActiveRotationSupport {
    void refreshFileHandles() throws InterruptedException;
}
