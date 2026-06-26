package com.batch.android.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ProfilePartialUpdateAttribute {

    @Nullable
    private List<String> added;

    @Nullable
    private List<String> removed;

    public ProfilePartialUpdateAttribute(@Nullable List<String> add) {
        if (add != null) {
            this.added = add;
        }
    }

    public ProfilePartialUpdateAttribute(@Nullable List<String> add, @Nullable List<String> remove) {
        if (add != null) {
            this.added = new ArrayList<>(add);
        }
        if (remove != null) {
            this.removed = new ArrayList<>(remove);
        }
    }

    public ProfilePartialUpdateAttribute(@NonNull ProfilePartialUpdateAttribute update) {
        if (update.added != null) {
            this.added = new ArrayList<>(update.added);
        }
        if (update.removed != null) {
            this.removed = new ArrayList<>(update.removed);
        }
    }

    @Nullable
    public List<String> getAdded() {
        return added;
    }

    @Nullable
    public List<String> getRemoved() {
        return removed;
    }

    public void putInAdded(@NonNull List<String> elements) {
        if (this.added == null) {
            this.added = new ArrayList<>();
        }
        this.added.addAll(elements);
        this.added = ProfileDataHelper.deduplicateKeepLast(this.added);
    }

    public void putInRemoved(@NonNull List<String> elements) {
        if (this.removed == null) {
            this.removed = new ArrayList<>();
        }
        this.removed.addAll(elements);
        this.removed = ProfileDataHelper.deduplicateKeepLast(this.removed);
    }

    public boolean isEmpty() {
        return (added == null || added.isEmpty()) && (removed == null || removed.isEmpty());
    }

    @NonNull
    public JSONObject getSerializedValue() throws JSONException {
        JSONObject json = new JSONObject();
        if (added != null && !added.isEmpty()) {
            json.put("$add", new JSONArray(added));
        }
        if (removed != null && !removed.isEmpty()) {
            json.put("$remove", new JSONArray(removed));
        }
        return json;
    }

    @NonNull
    @Override
    public String toString() {
        return "ProfilePartialUpdateAttribute{" + "added=" + added + ", removed=" + removed + '}';
    }
}
