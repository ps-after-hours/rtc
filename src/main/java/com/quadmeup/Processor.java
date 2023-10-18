package com.quadmeup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.quadmeup.exceptions.DoneException;

public class Processor {
    
    private static Logger LOGGER = LogManager.getLogger(Processor.class);

    private static final int PARALLEL_EXECUTORS = 4;
    private static final int EXECUTOR_QUEUE_LENGTH = 16;

    private boolean finished = false;
    private TaskProvider taskProvider;
    private ThreadPoolExecutor executorService;
    private SampleStorage sampleStorage;
    private List<FutureTask<Task>> futures;

    public Processor() {
        sampleStorage = new SampleStorage();
        taskProvider = new TaskProvider(sampleStorage);
        executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(PARALLEL_EXECUTORS);
        futures = new ArrayList<FutureTask<Task>>();
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

                //Add new threads to fill the queue
                // Bear in mind, queue is longer than the number of executors, so we can have some threads waiting for execution
                for (int i = 0; i < EXECUTOR_QUEUE_LENGTH - futures.size(); i++) {
                    FutureTask<Task> future = (FutureTask<Task>) executorService.submit(taskProvider.getTask());                    
                    futures.add(future);
                }

                //We start new threads only every n-milliseconds to avoid overloading the system. This value can be adjusted
                //to maximize the performance of the system
                Thread.sleep(2);
            }

        } catch (DoneException doneException) {
            //We are done, it's time to end polling sources and shut down executors. We dont' even care if there are any running
        }

        //At this point we still have a running executor with unknown number of threads. We have to wait for it to finish
        while (executorService.getActiveCount() > 0) {
            Thread.sleep(5);
        }

        //We are done, we can shut down the executor
        executorService.shutdown();

        LOGGER.error("DONE");

    }

}
