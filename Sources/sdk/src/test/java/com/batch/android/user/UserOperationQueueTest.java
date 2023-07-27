package com.batch.android.user;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class UserOperationQueueTest {

    @Test
    public void testAddOperation() {
        UserOperationQueue queue = new UserOperationQueue();
        UserOperation operation = new DummyOperation(1);
        queue.addOperation(operation);
        UserOperation expected = queue.popOperations().get(0);
        Assert.assertEquals(expected, operation);
    }

    @Test
    public void testAddFirstOperation() {
        UserOperationQueue queue = new UserOperationQueue();
        UserOperation operation = new DummyOperation(1);
        UserOperation operation2 = new DummyOperation(2);
        queue.addOperation(operation);
        queue.addFirstOperation(operation2);
        UserOperation expected = queue.popOperations().get(0);
        Assert.assertEquals(expected, operation2);
    }

    @Test
    public void testPopOperations() {
        UserOperationQueue queue = new UserOperationQueue();
        UserOperation operation = new DummyOperation(1);
        UserOperation operation2 = new DummyOperation(2);
        queue.addOperation(operation);
        queue.addOperation(operation2);
        List<UserOperation> operations = queue.popOperations();
        Assert.assertEquals(operation, operations.get(0));
        Assert.assertEquals(operation2, operations.get(1));
        Assert.assertEquals(0, queue.size());
    }

    @Test
    public void testSize() {
        UserOperationQueue queue = new UserOperationQueue();
        queue.addOperation(new DummyOperation(1));
        queue.addOperation(new DummyOperation(2));
        Assert.assertEquals(2, queue.size());
    }

    private static class DummyOperation implements UserOperation {

        public int id;

        @Override
        public void execute(SQLUserDatasource datasource) throws Exception {
            //do nothing
        }

        public DummyOperation(int id) {
            this.id = id;
        }
    }
}
