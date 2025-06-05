package com.batch.android.messaging.model;

import com.batch.android.json.JSONObject;
import java.io.Serializable;
import java.util.Objects;

public class Action implements Serializable {

    private static final long serialVersionUID = 0L;

    public String action;
    public JSONObject args;

    public Action(String action, JSONObject args) {
        this.action = action;
        this.args = args;
    }

    public boolean isDismissAction() {
        return action == null || "batch.dismiss".equals(action);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Action action1 = (Action) o;
        return Objects.equals(action, action1.action) && Objects.equals(args, action1.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, args);
    }
}
