package com.equalexperts.logging;

interface ActiveRotationSupport {
    void postRotate() throws InterruptedException;
}
