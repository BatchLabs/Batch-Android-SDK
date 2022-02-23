package com.batch.android.user;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility class to compute the diff between two sets of attributes and two sets
 * of tag collections
 */
public class UserDataDiff {

    public Result result;

    public UserDataDiff(
        @Nullable Map<String, UserAttribute> newAttributes,
        @Nullable Map<String, UserAttribute> oldAttributes,
        @Nullable Map<String, Set<String>> newTagCollections,
        @Nullable Map<String, Set<String>> oldTagCollections
    ) {
        result = new Result();
        if (newAttributes != null && oldAttributes != null) {
            computeAttributes(newAttributes, oldAttributes);
        } else {
            result.addedAttributes = new HashMap<>();
            result.removedAttributes = new HashMap<>();
        }

        if (newTagCollections != null && oldTagCollections != null) {
            computeTags(newTagCollections, oldTagCollections);
        } else {
            result.addedTags = new HashMap<>();
            result.removedTags = new HashMap<>();
        }
    }

    private void computeAttributes(
        @NonNull Map<String, UserAttribute> newAttributes,
        @NonNull Map<String, UserAttribute> oldAttributes
    ) {
        // First, copy old attributes in missingEntries
        // Then, iterate on new attributes, remove them from missingEntries if they have the same value
        // This will leave us with attributes missing from "newAttributes" without having to iterate
        // a second time
        // An updated attribute is both in missing and added
        Map<String, UserAttribute> missingEntries = new HashMap<>(oldAttributes);
        Map<String, UserAttribute> addedEntries = new HashMap<>();

        for (Map.Entry<String, UserAttribute> newEntry : newAttributes.entrySet()) {
            String key = newEntry.getKey();
            UserAttribute oldValue = oldAttributes.get(key);
            if (!newEntry.getValue().equals(oldValue)) {
                addedEntries.put(key, newEntry.getValue());
            } else {
                missingEntries.remove(key);
            }
        }

        result.addedAttributes = addedEntries;
        result.removedAttributes = missingEntries;
    }

    private void computeTags(
        @NonNull Map<String, Set<String>> newTagCollections,
        @NonNull Map<String, Set<String>> oldTagCollections
    ) {
        Map<String, Set<String>> addedResult = new HashMap<>();
        Map<String, Set<String>> removedResult = new HashMap<>(oldTagCollections);

        // [0] are the added tags
        // [1] are the removed tags
        @SuppressWarnings("unchecked")
        Set<String>[] setDiffOutput = new Set[2];
        for (Map.Entry<String, Set<String>> newEntry : newTagCollections.entrySet()) {
            String collectionName = newEntry.getKey();
            computeTagSetDiff(newEntry.getValue(), oldTagCollections.get(collectionName), setDiffOutput);

            if (setDiffOutput[0] != null) { // added tags
                addedResult.put(collectionName, setDiffOutput[0]);
            }

            if (setDiffOutput[1] != null) {
                removedResult.put(collectionName, setDiffOutput[1]);
            } else {
                removedResult.remove(collectionName);
            }
        }

        result.addedTags = addedResult;
        result.removedTags = removedResult;
    }

    private void computeTagSetDiff(
        @Nullable Set<String> newSet,
        @Nullable Set<String> previousSet,
        Set[] setDiffOutput
    ) {
        setDiffOutput[0] = null;
        setDiffOutput[1] = null;

        // Quick optimizations for common tag collection use cases
        if (newSet == null || newSet.isEmpty()) {
            if (previousSet == null || previousSet.isEmpty()) {
                return;
            } else {
                setDiffOutput[1] = new HashSet<>(previousSet);
                return;
            }
        } else if (previousSet == null || previousSet.isEmpty()) {
            setDiffOutput[0] = new HashSet<>(newSet);
            return;
        } else if (newSet.equals(previousSet)) {
            return;
        }

        Set<String> missingEntries = new HashSet<>(previousSet);
        Set<String> addedEntries = new HashSet<>();

        for (String entry : newSet) {
            if (!missingEntries.remove(entry)) {
                addedEntries.add(entry);
            }
        }

        if (!addedEntries.isEmpty()) {
            setDiffOutput[0] = addedEntries;
        }

        if (!missingEntries.isEmpty()) {
            setDiffOutput[1] = missingEntries;
        }
    }

    /**
     * Represents a diff's results
     */
    public static class Result {

        public Map<String, UserAttribute> addedAttributes;
        public Map<String, UserAttribute> removedAttributes;

        public Map<String, Set<String>> addedTags;
        public Map<String, Set<String>> removedTags;

        private Result() {}

        public boolean hasChanges() {
            return (
                addedAttributes.size() > 0 ||
                removedAttributes.size() > 0 ||
                addedTags.size() > 0 ||
                removedTags.size() > 0
            );
        }

        /**
         * Serialize the diff results in event parameters form
         */
        public JSONObject toEventParameters(long version) throws JSONException {
            JSONObject result = new JSONObject();

            result.put("version", version);
            result.put("added", convertToJson(addedAttributes, addedTags));
            result.put("removed", convertToJson(removedAttributes, removedTags));

            return result;
        }

        private JSONObject convertToJson(
            Map<String, UserAttribute> attributes,
            Map<String, Set<String>> tagCollections
        ) throws JSONException {
            JSONObject result = new JSONObject();

            if (!attributes.isEmpty()) {
                Map<String, Object> serverAttributesRepresentation = UserAttribute.getServerMapRepresentation(
                    attributes
                );
                for (Map.Entry<String, Object> attribute : serverAttributesRepresentation.entrySet()) {
                    result.put(attribute.getKey(), attribute.getValue());
                }
            }

            for (Map.Entry<String, Set<String>> tagCollection : tagCollections.entrySet()) {
                result.put("t." + tagCollection.getKey(), new JSONArray(tagCollection.getValue()));
            }

            return result;
        }
    }
}
