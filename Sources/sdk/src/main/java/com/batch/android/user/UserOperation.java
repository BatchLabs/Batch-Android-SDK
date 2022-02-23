package com.batch.android.user;

/**
 * An operation to execute on the user datasource
 */
public interface UserOperation {
    void execute(SQLUserDatasource datasource) throws Exception;
}
