package com.github.seregamorph.maven.test.builder;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.apache.maven.project.MavenProject;

class SignalingExecutorCompletionService {

    private static final ThreadLocal<Consumer<MavenProject>> currentSignaler = new ThreadLocal<>();

    private final ExecutorService executor;
    private final BlockingQueue<Try<MavenProject>> signaledQueue;

    SignalingExecutorCompletionService(ExecutorService executor) {
        this.executor = Objects.requireNonNull(executor);
        this.signaledQueue = new LinkedBlockingQueue<>();
    }

    static void signal(MavenProject value) {
        Consumer<MavenProject> signaler = currentSignaler.get();
        if (signaler != null) {
            signaler.accept(value);
        }
    }

    Future<?> submit(Callable<MavenProject> call) {
        Objects.requireNonNull(call);
        return executor.submit(new FutureTask<>(() -> {
            AtomicBoolean signaled = new AtomicBoolean(false);
            currentSignaler.set(value -> {
                // no race condition here with "if (!signaled.get())" block, because it's the same thread
                signaled.set(true);
                signaledQueue.add(Try.success(value));
            });
            try {
                MavenProject result = call.call();
                if (!signaled.get()) {
                    signaledQueue.add(Try.success(result));
                }
                return result;
            } catch (Throwable e) {
                signaledQueue.add(Try.failure(e));
                if (e instanceof Exception) {
                    throw e;
                } else {
                    throw new RuntimeException(e);
                }
            } finally {
                currentSignaler.remove();
            }
        }));
    }

    public MavenProject takeSignaled() throws InterruptedException, ExecutionException {
        Try<MavenProject> t = signaledQueue.take();
        return t.get();
    }

    private abstract static class Try<T> {
        abstract T get() throws ExecutionException;

        static <T> Try<T> success(T value) {
            return new Try<T>() {
                @Override
                T get() {
                    return value;
                }
            };
        }

        static <T> Try<T> failure(Throwable e) {
            return new Try<>() {
                @Override
                T get() throws ExecutionException {
                    throw new ExecutionException(e);
                }
            };
        }
    }
}
