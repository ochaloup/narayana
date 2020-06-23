package com.arjuna.ats.arjuna.coordinator;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.arjuna.ats.arjuna.common.arjPropertyManager;

public class TwoPhaseCommitThreadPool {
    private static final int poolSize = arjPropertyManager.getCoordinatorEnvironmentBean()
            .getMaxTwoPhaseCommitThreads();
    private static final ExecutorService executor = Executors.newFixedThreadPool(poolSize);

    public static Future<Integer> submitJob(Callable<Integer> job) {
        return executor.submit(job);
    }

    public static void submitJob(Runnable job) {
        executor.submit(job);
    }

    public static CompletionService<Boolean> getNewCompletionService() {
        return new ExecutorCompletionService<Boolean>(executor);
    }
}
