package com.equalexperts.logging.impl;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import java.util.concurrent.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AsyncExecutorTest {
    private final ThreadFactory factory = mock(ThreadFactory.class);
    private final AsyncExecutor asyncExecutor = new AsyncExecutor(factory);
    private Thread createdThread;

    @Before
    public void setup() {
        when(factory.newThread(any())).then((InvocationOnMock invocation) -> {
            Runnable r = (Runnable) invocation.getArguments()[0];
            createdThread = new Thread(r);
            return createdThread;
        });
    }

    @Test
    public void execute_shouldCreateAndStartAThreadWithTheProvidedRunnableAndReturnAFuture() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Future<?> result = asyncExecutor.execute(latch::countDown);
        latch.await(5, TimeUnit.SECONDS); //give the new thread time to start to running

        verify(factory).newThread(any()); //The provided runnable will be wrapped, not passed directly
        assertEquals(0, latch.getCount()); //but the runnable will be executed
        assertNotNull(result);
        assertNotNull(createdThread);
        assertNotEquals(Thread.State.NEW, createdThread.getState());
    }

    /*
        The following tests are for method calls on the Future instance return by the execute method
     */

    @Test
    public void get_shouldJoinTheCreatedThread_givenAFutureReturnedByExecute() throws Exception {
        long startTime = System.nanoTime();
        Future<?> result = asyncExecutor.execute(suppressCheckedExceptions(() -> Thread.sleep(250L)));
        result.get();
        long endTime = System.nanoTime();

        assertThat(endTime - startTime, isGreaterThan(100 * 1000000L));
    }

    @Test
    public void get_shouldSucceed_whenTheThreadThrowsAnException_givenAFutureReturnedByExecute() throws Exception {
        Future<?> result = asyncExecutor.execute(() -> {throw new RuntimeException("blah"); });
        result.get(100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void getWithATimeout_shouldSucceed_whenTheThreadEndsQuickly_givenAFutureReturnedByExecute() throws Exception {
        Future<?> result = asyncExecutor.execute(suppressCheckedExceptions(() -> Thread.sleep(50L)));
        result.get(100, TimeUnit.MILLISECONDS);
    }
    
    @Test
    public void getWithATimeout_shouldThrowATimeoutException_whenTheThreadDoesNotEndQuickly_givenAFutureReturnedByExecute() throws Exception {
        Future<?> result = asyncExecutor.execute(suppressCheckedExceptions(() -> Thread.sleep(250L)));

        try {
            result.get(50, TimeUnit.MILLISECONDS);
            fail("Expected an exception");
        } catch (TimeoutException ignore) {}
    }

    @Test
    public void cancel_shouldThrowAnUnsupportedOperationException_givenAFutureReturnedByExecute() throws Exception {
        Future<?> result = asyncExecutor.execute(() -> {});

        try {
            result.cancel(true);
            fail("Expected an exception");
        } catch (UnsupportedOperationException ignore) {}
    }

    @Test
    public void isCancelled_shouldReturnFalse_givenAFutureReturnedByExecute() throws Exception {
        Future<?> result = asyncExecutor.execute(() -> {});

        assertFalse(result.isCancelled());
    }

    @Test
    public void isDone_shouldReturnTrue_whenTheThreadIsAlive_givenAFutureReturnedByExecute() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Future<?> result = asyncExecutor.execute(suppressCheckedExceptions(latch::await));

        try {
            assertFalse(result.isDone());
        } finally {
            latch.countDown(); //don't want a thread waiting forever...
        }
    }

    @Test
    public void isDone_shouldReturnFalse_whenTheThreadIsFinished_givenAFutureReturnedByExecute() throws Exception {
        Future<?> result = asyncExecutor.execute(() -> {});

        Thread.sleep(50L); //give other thread a chance to finish...

        assertTrue(result.isDone());
    }

    private static Runnable suppressCheckedExceptions(RunnableThatThrows r) {
        return () -> {
            try {
                r.run();
            } catch (RuntimeException e) {
                throw e;
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static  Matcher<Long> isGreaterThan(final long other) {
        return new BaseMatcher<Long>() {
            @Override
            public boolean matches(Object item) {
                if (!(item instanceof Long)) {
                    return false;
                }
                long itemValue = (Long) item;

                return itemValue > other;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("greater than").appendValue(other);
            }
        };
    }

    @FunctionalInterface
    private static interface RunnableThatThrows {
        void run() throws Exception;
    }
}