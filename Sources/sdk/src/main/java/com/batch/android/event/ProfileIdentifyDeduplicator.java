package com.batch.android.event;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.core.Logger;
import com.batch.android.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Removes duplicate consecutive {@code _PROFILE_IDENTIFY} events that carry the same
 * {@code custom_id} from an event list.
 *
 * <p>Non-identify events pass through unchanged and do not reset deduplication tracking,
 * so two identify events with the same custom_id are always collapsed even when other
 * event types appear between them.
 */
public final class ProfileIdentifyDeduplicator {

    private static final String TAG = "ProfileIdentifyDeduplicator";

    private ProfileIdentifyDeduplicator() {}

    /**
     * Returns a new list with duplicate {@code _PROFILE_IDENTIFY} events removed.
     */
    public static List<Event> deduplicate(@NonNull List<Event> events) {
        List<Event> result = new ArrayList<>(events.size());
        boolean hasSeenIdentify = false;
        String lastCustomId = null;

        for (Event event : events) {
            if (InternalEvents.PROFILE_IDENTIFY.equals(event.getName())) {
                String currentCustomId = extractCustomId(event.getParameters());
                if (hasSeenIdentify && Objects.equals(lastCustomId, currentCustomId)) {
                    Logger.internal(
                        TAG,
                        "Deduplicating consecutive _PROFILE_IDENTIFY event with custom_id: " + currentCustomId
                    );
                    continue;
                }
                hasSeenIdentify = true;
                lastCustomId = currentCustomId;
            }
            result.add(event);
        }

        return result;
    }

    /**
     * Extracts the {@code custom_id} value from a {@code _PROFILE_IDENTIFY} event's JSON
     * parameters string. Returns {@code null} when the identifier is absent, explicitly null in
     * the JSON payload, or when parsing fails.
     */
    @Nullable
    static String extractCustomId(@Nullable String parameters) {
        if (parameters == null) {
            return null;
        }
        try {
            JSONObject params = new JSONObject(parameters);
            JSONObject identifiers = params.optJSONObject("identifiers");
            if (identifiers == null) {
                return null;
            }
            return identifiers.reallyOptString("custom_id", null);
        } catch (Exception e) {
            return null;
        }
    }
}
