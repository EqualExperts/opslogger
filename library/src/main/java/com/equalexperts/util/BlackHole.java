package com.equalexperts.util;

public class BlackHole<T> implements Consumer<T> {
    @Override
    public void accept(T t) {
        // consume silently
    }
}
