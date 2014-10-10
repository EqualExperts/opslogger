package com.equalexperts.logging;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * Like an {@link java.util.concurrent.Executor Executor}, except that the execute method
 * returns a {@link java.util.concurrent.Future Future}, and execution is guaranteed to be
 * executed asynchronously.
 *
 * Spawns a new thread when @{link execute execute} is called, and the various methods of the
 * Future can be used to control the thread or wait for it to finish.
 *
 * This is more testable than directly manipulating threads ({@link java.lang.Thread Thread}
 * has final methods that can't be intercepted by normal mocking libraries), and is easier than
 * dealing with a full {@link java.util.concurrent.ExecutorService ExecutorService}, which has to be
 * stopped and is perhaps more useful with a stream of small tasks than a single long-running one.
 *
 * @see java.util.concurrent.Executor
 * @see java.util.concurrent.Future
 */
class AsyncExecutor {
    private final ThreadFactory factory;

    AsyncExecutor(ThreadFactory factory) {
        this.factory = factory;
    }

    public Future<?> execute(Runnable runnable) {
        CompletableFuture<?> result = new CompletableFuture<Object>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                throw new UnsupportedOperationException("Calling thread.stop is deprecated. Arrange termination directly with the runnable");
            }
        };
        factory.newThread(() -> {
            try {
                runnable.run();
            } finally {
                result.complete(null);
            }
        }).start();
        return result;
    }
}
