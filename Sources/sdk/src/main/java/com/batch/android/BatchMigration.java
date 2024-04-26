package com.batch.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.annotation.PublicSDK;
import java.util.EnumSet;

@PublicSDK
public enum BatchMigration {
    /**
     * No migrations disabled
     */
    NONE(0),

    /**
     * Whether Bath should automatically identify logged-in user when running the SDK V2 for the first time.
     * This mean user with a custom_user_id will be automatically attached a to a Profile and can be targeted within a Project scope.
     */
    CUSTOM_ID(1),

    /**
     * Whether Bath should automatically attach current installation's data (language/region/customDataAttributes...)
     * to the User's Profile when running the SDK for the first time.
     */
    CUSTOM_DATA(1 << 1);

    /**
     * The integer value
     */
    private final int value;

    /**
     * Constructor
     *
     * @param value Integer value of the migration
     */
    BatchMigration(int value) {
        this.value = value;
    }

    /**
     * Convert integer value in enum set of migrations
     *
     * @param value The integer value of migrations
     * @return An enumSet of Migration
     */
    @NonNull
    public static EnumSet<BatchMigration> fromValue(int value) {
        EnumSet<BatchMigration> migrations = EnumSet.noneOf(BatchMigration.class);
        for (BatchMigration migration : values()) {
            if (migration != NONE && (value & migration.value) == migration.value) {
                migrations.add(migration);
            }
        }
        if (migrations.isEmpty()) {
            migrations.add(BatchMigration.NONE);
        }
        return migrations;
    }

    /**
     * Convert an EnumSet of migration to an integer value
     *
     * @param migrations EnumSet of migrations to convert
     * @return The integer value of migrations
     */
    public static int toValue(@NonNull EnumSet<BatchMigration> migrations) {
        int val = 0;
        for (BatchMigration migration : migrations) {
            val |= migration.value;
        }
        return val;
    }

    /**
     * Whether the Custom User ID migration is disabled
     *
     * @param value migrations value
     * @return true if its disabled, false otherwise
     */
    public static boolean isCustomIDMigrationDisabled(@Nullable Integer value) {
        return (value != null && (fromValue(value).contains(BatchMigration.CUSTOM_ID)));
    }

    /**
     * Whether the Custom Data migration is disabled
     *
     * @param value migrations value
     * @return true if its disabled, false otherwise
     */
    public static boolean isCustomDataMigrationDisabled(@Nullable Integer value) {
        return (value != null && (fromValue(value).contains(BatchMigration.CUSTOM_DATA)));
    }
}
