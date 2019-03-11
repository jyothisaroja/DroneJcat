package com.jcat.cloud.fw.common.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Helper class for simultaneous execution of multiple tasks using Executors.
 *
 * ExecutorHelper is designed for running multiple tasks simultaneously.
 * This class accepts the tasks that are to be executed simultaneously
 * and the expected result of those task executions(success/failure).
 * It then submits them to the executor which then executes the tasks.
 * It finally returns the tasks along with their execution status
 * (success/failure). On success, it returns null and on failure,
 *  it returns the exception that caused the task to fail.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2017
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zdagjyo - 2017-04-18 - Initial version
 */
public class ParallelExecutionService {

    // Describes the expected result of a task execution.
    public enum Result {
        SUCCESS, FAILURE
    }

    // Describes various states that a task can be in.
    public enum TaskStatus {
        READY, STARTED, FINISHED, STOPPED;
    }

    // executorService object to execute the tasks
    private ExecutorService mExecutor = Executors.newWorkStealingPool();

    // collection that stores all the tasks to be executed in parallel along with their expected
    // results(success/failure)
    private Map<Runnable, Result> mTasksToExecute = new HashMap<Runnable, Result>();

    // collection that stores all the tasks along with their running status
    private Map<Runnable, TaskStatus> mTasksAndRunningStatus = new HashMap<Runnable, TaskStatus>();

    // collection that stores futures and their expected results
    private Map<Future<?>, Result> mFutureAndResults = new HashMap<Future<?>, Result>();

    // collection that stores futures and their associated tasks
    private Map<Future<?>, Runnable> mFutureAndTasks = new HashMap<Future<?>, Runnable>();

    /**
     * Method that checks if all tasks are finished.
     *
     * @return boolean
     */
    public boolean areAllTasksFinished() {
        for (Map.Entry<Runnable, TaskStatus> entry : getAllTasksRunningStatus().entrySet()) {
            if (!entry.getValue().equals(TaskStatus.FINISHED)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Main method which executes the threads/tasks.
     *
     * @param tasksToExecute - A collection that stores the tasks to be executed in parallel
     *                         along with their expected results(Success/Failure)
     */
    public void executeTasks(Map<Runnable, Result> tasksToExecute) {
        mTasksToExecute.putAll(tasksToExecute);
        // loop through all tasks and submit tasks that are not started yet
        for (Map.Entry<Runnable, Result> entry : mTasksToExecute.entrySet()) {
            // check if the task is already started, if not, set its running status to ready
            if (!mTasksAndRunningStatus.containsKey(entry.getKey())) {
                mTasksAndRunningStatus.put(entry.getKey(), TaskStatus.READY);
            }
            // check if the task is not started yet(i.e.,task in ready state), if yes, start it and set its running
            // status accordingly
            if (mTasksAndRunningStatus.get(entry.getKey()).equals(TaskStatus.READY)) {
                Future<?> future = mExecutor.submit(entry.getKey());
                mFutureAndResults.put(future, entry.getValue());
                mFutureAndTasks.put(future, entry.getKey());
                mTasksAndRunningStatus = getAllTasksRunningStatus();
            }
        }
    }

    /**
     * Method that gets the running status for all tasks.
     *
     * @return Map<Runnable, TaskStatus> - A collection that stores tasks
     *                                     and their running status
     */
    public Map<Runnable, TaskStatus> getAllTasksRunningStatus() {
        TaskStatus status = TaskStatus.STARTED;
        for (Map.Entry<Future<?>, Runnable> entry : mFutureAndTasks.entrySet()) {
            if (entry.getKey().isDone()) {
                status = TaskStatus.FINISHED;
            } else if (entry.getKey().isCancelled()) {
                status = TaskStatus.STOPPED;
            }
            mTasksAndRunningStatus.put(entry.getValue(), status);
        }
        return mTasksAndRunningStatus;
    }

    /**
     * Getter method for member variable 'mTasksToExecute'
     *
     * @return Map<Runnable, Result> - A collection that stores tasks to be executed and their expected results
     */
    public Map<Runnable, Result> getTasks() {
        return this.mTasksToExecute;
    }

    /**
     * Method that waits for the specified task to complete and returns its result.
     *
     * @return String - the execution result of task(null on success,
     *                                 exception message on failure)
     * @throws InterruptedException
     * @throws ExecutionException
     *
     */
    public String waitAndGetTaskResult(Runnable task) throws InterruptedException, ExecutionException {
        String result = null;
        // loop through each future and get the future associated with the specified task and wait for it to complete
        for (Map.Entry<Future<?>, Runnable> entry : mFutureAndTasks.entrySet()) {
            if (entry.getValue().equals(task)) {
                // check if expected result of task execution is success.
                // If yes, task execution result must be null
                // else, task execution result holds the exception
                // that caused the task to fail
                if (mFutureAndResults.get(entry.getKey()) == Result.SUCCESS) {
                    // method that waits for a task to complete
                    entry.getKey().get();
                    return null;
                } else {
                    try {
                        entry.getKey().get();
                        return null;
                    } catch (ExecutionException e) {
                        return e.getMessage();
                    }
                }
            }
        }
        return result;
    }
}
