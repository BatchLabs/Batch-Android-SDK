package com.batch.android.user;

import java.util.LinkedList;
import java.util.List;

public class UserOperationQueue {

    /**
     * User operation queue
     */
    private final List<UserOperation> operationQueue;

    public UserOperationQueue() {
        this.operationQueue = new LinkedList<>();
    }

    public UserOperationQueue(List<UserOperation> operations) {
        this.operationQueue = new LinkedList<>(operations);
    }

    /**
     * Add an operation to queue
     * @param operation user op
     */
    public void addOperation(UserOperation operation) {
        synchronized (operationQueue) {
            operationQueue.add(operation);
        }
    }

    /**
     * Add an operation to the beginning of the queue
     * @param operation user op
     */
    public void addFirstOperation(UserOperation operation) {
        synchronized (operationQueue) {
            operationQueue.add(0, operation);
        }
    }

    /**
     * Pop all operations of the queue
     * @return all operations in the queue
     */
    public List<UserOperation> popOperations() {
        synchronized (operationQueue) {
            final List<UserOperation> operations = new LinkedList<>(operationQueue);
            operationQueue.clear();
            return operations;
        }
    }

    /**
     * Get the size of the queue
     * @return the size of the queue
     */
    public int size() {
        synchronized (operationQueue) {
            return this.operationQueue.size();
        }
    }
}
