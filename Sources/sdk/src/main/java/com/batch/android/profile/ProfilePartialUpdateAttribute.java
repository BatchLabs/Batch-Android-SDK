package com.batch.android.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ProfilePartialUpdateAttribute {

    @Nullable
    private List<String> added;

    @Nullable
    private List<String> removed;

    public ProfilePartialUpdateAttribute(@Nullable List<String> add) {
        this.added = add;
    }

    public ProfilePartialUpdateAttribute(@Nullable List<String> add, @Nullable List<String> remove) {
        this.added = add;
        this.removed = remove;
    }

    @Nullable
    public List<String> getAdded() {
        return added;
    }

    @Nullable
    public List<String> getRemoved() {
        return removed;
    }

    public void putInAdded(List<String> elements) {
        if (this.added == null) {
            this.added = new ArrayList<>();
        }
        this.added.addAll(elements);
    }

    public void putInRemoved(List<String> elements) {
        if (this.removed == null) {
            this.removed = new ArrayList<>();
        }
        this.removed.addAll(elements);
    }

    @NonNull
    @Override
    public String toString() {
        return "ProfilePartialUpdateAttribute{" + "added=" + added + ", removed=" + removed + '}';
    }
}
