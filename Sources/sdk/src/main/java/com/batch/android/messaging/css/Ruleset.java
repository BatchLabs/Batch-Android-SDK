package com.batch.android.messaging.css;

import java.util.ArrayList;
import java.util.List;

public class Ruleset {

    public String selector;

    public List<Declaration> declarations;

    public Ruleset() {
        declarations = new ArrayList<>();
    }
}
