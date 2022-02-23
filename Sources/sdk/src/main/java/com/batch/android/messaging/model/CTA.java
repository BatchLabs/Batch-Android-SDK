package com.batch.android.messaging.model;

import com.batch.android.json.JSONObject;
import java.io.Serializable;

public class CTA extends Action implements Serializable {

    private static final long serialVersionUID = 0L;

    public String label;

    public CTA(String label, String action, JSONObject args) {
        super(action, args);
        this.label = label;
    }
}
