package com.quadmeup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.quadmeup.enums.SinkType;
import com.quadmeup.exceptions.DoneException;

public class Processor {
    
    private static Logger LOGGER = LogManager.getLogger(Processor.class);

    private static final int PARALLEL_EXECUTORS = 10;
    private static final int EXECUTOR_QUEUE_LENGTH = 15;

    //As we will be reporting roughly half the number of sinks, we can have a smaller number of sink executors
    private static final int SINK_EXECUTORS = PARALLEL_EXECUTORS / 2;

    private boolean finished = false;
    private TaskProvider taskProvider;
    private ThreadPoolExecutor executorService;
    private ThreadPoolExecutor sinkExecutorService;
    private SampleStorage sampleStorage;
    private List<FutureTask<Task>> futures;
    private List<FutureTask<Task>> sinkFutures;

    public Processor() {
        sampleStorage = new SampleStorage();
        taskProvider = new TaskProvider(sampleStorage);
        executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(PARALLEL_EXECUTORS);
        sinkExecutorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(SINK_EXECUTORS); // For simplicuty, we won't be limiting the queue size as data will be sent gradually
        futures = new ArrayList<FutureTask<Task>>();
        sinkFutures = new ArrayList<FutureTask<Task>>();
    }

    private void executorCleanup(List<FutureTask<Task>> list) {
        for (Iterator<FutureTask<Task>> it = list.iterator(); it.hasNext(); ) {
            final FutureTask<Task> future = it.next();
            if (future.isDone()) {
                it.remove();
            }
        }
    }

    public void run() throws InterruptedException {

        try {

            //The main loop that gets data from Sources
            while (!finished) {

                executorCleanup(futures);
                executorCleanup(sinkFutures);

                //Add new threads to fill the queue
                // Bear in mind, queue is longer than the number of executors, so we can have some threads waiting for execution
                for (int i = 0; i < EXECUTOR_QUEUE_LENGTH - futures.size(); i++) {
                    FutureTask<Task> future = (FutureTask<Task>) executorService.submit(taskProvider.getSourceTask());                    
                    futures.add(future);
                }

                //If we have anything in the joined list, let's start reporting it
                String toSinkJoin = sampleStorage.getDuplicate();
                if (toSinkJoin != null) {
                    FutureTask<Task> sinkFuture = (FutureTask<Task>) sinkExecutorService.submit(taskProvider.getSinkTask(SinkType.JOINED, toSinkJoin)); 
                    sinkFutures.add(sinkFuture);
                }

                //We start new threads only every n-milliseconds to avoid overloading the system. This value can be adjusted
                //to maximize the performance of the system
                Thread.sleep(5);

            }

        } catch (DoneException doneException) {
            //We are done, it's time to end polling sources and shut down executors. We dont' even care if there are any running
            executorService.shutdown();
            LOGGER.info("Done fetching, reporting orphans");
        }

        //We are done, at this moment we still have to find out if we have any left joins left.
        //We should not but additional check is not a big problem and it adds extra safety
        boolean goJoins = true;
        while (goJoins) {

            String joinSample = sampleStorage.getDuplicate();

            if (joinSample == null) {
                goJoins = false;
                break;
            }

            FutureTask<Task> sinkFuture = (FutureTask<Task>) sinkExecutorService.submit(taskProvider.getSinkTask(SinkType.JOINED, joinSample)); 
            sinkFutures.add(sinkFuture);
            Thread.sleep(5);
        }

        //Post processing for finding orphans

        boolean goOrphans = true;
        while (goOrphans) {
                
            String orphanSample = sampleStorage.get();

            if (orphanSample == null) {
                goOrphans = false;
                break;
            }

            FutureTask<Task> sinkFuture = (FutureTask<Task>) sinkExecutorService.submit(taskProvider.getSinkTask(SinkType.ORPHANED, orphanSample)); 
            sinkFutures.add(sinkFuture);
            Thread.sleep(5);
        }

        //At this point we still have a running executor with unknown number of threads. We have to wait for it to finish
        while (sinkExecutorService.getActiveCount() > 0) {
            Thread.sleep(5);
        }

        //We are done, we can shut down the executor
        sinkExecutorService.shutdown();

        LOGGER.info("DONE");

    }

}
