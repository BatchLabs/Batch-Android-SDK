package com.batch.android.localcampaigns.persistence;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.batch.android.core.Logger;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.UUID;

public class LocalCampaignsFilePersistence implements LocalCampaignsPersistence {

    private static final String TAG = "LocalCampaignsFilePersistence";
    private static final String PERSISTENCE_TMP_FILE_PREFIX = "com.batch.tmp.";

    @VisibleForTesting
    static final String PERSISTENCE_SAVE_VERSION_KEY = "save_version";

    @VisibleForTesting
    static final int PERSISTENCE_CURRENT_FILE_VERSION = 1;

    @Override
    public boolean hasSavedData(@NonNull Context context, @NonNull String filename) throws PersistenceException {
        return new File(context.getCacheDir(), filename).exists();
    }

    @Override
    public void persistData(@NonNull Context context, @NonNull JSONObject json, @NonNull String filename)
        throws PersistenceException {
        final File cacheDir = context.getCacheDir();

        // Try to get the access to the file.
        FileOutputStream fos = null;
        OutputStreamWriter osr = null;

        File file = new File(cacheDir, filename);
        File tmpFile = new File(cacheDir, PERSISTENCE_TMP_FILE_PREFIX + UUID.randomUUID().toString() + ".json");

        try {
            // Create a temporary file, we will write in it
            if (!tmpFile.createNewFile()) {
                Logger.internal(TAG, "Unable to create a temporary file " + file.getName());
            }

            // Copy the output JSON so we don't pollute the source argument
            JSONObject outputJSON = new JSONObject(json);

            // Add version to the file if the response does not contain that information
            if (!outputJSON.has(PERSISTENCE_SAVE_VERSION_KEY)) {
                outputJSON.put(PERSISTENCE_SAVE_VERSION_KEY, PERSISTENCE_CURRENT_FILE_VERSION);
            }

            fos = new FileOutputStream(tmpFile);
            osr = new OutputStreamWriter(fos, "UTF-8");

            osr.append(outputJSON.toString());
            osr.close();
            osr = null;

            fos.close();
            fos = null;

            // If write success, remove previous file
            if (file.exists()) {
                if (!file.delete()) {
                    Logger.internal(TAG, "Unable to create previous cached file " + file.getName());
                }
            }

            // Rename temporary file to previous file
            if (tmpFile.exists()) {
                if (!tmpFile.renameTo(file)) {
                    String removedMsg = tmpFile.delete()
                        ? "Removed temporary cache file"
                        : "Tried to remove temporary cache file but unable to do it.";

                    throw new PersistenceException(
                        "unable to rename temporary cached file " +
                        tmpFile.getName() +
                        " to " +
                        file.getName() +
                        ". " +
                        removedMsg
                    );
                }
            } else {
                throw new PersistenceException(
                    "unable to rename temporary cached file " + tmpFile.getName() + ". File not found."
                );
            }
        } catch (Exception ex) {
            // Remove temporary file
            if (tmpFile.exists()) {
                if (!tmpFile.delete()) {
                    throw new PersistenceException("unable to delete temporary cached file " + tmpFile.getName(), ex);
                }
            }

            throw new PersistenceException("Can't save json file. " + ex.toString(), ex);
        } finally {
            try {
                if (osr != null) {
                    osr.close();
                }
            } catch (IOException ignored) {}
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ignored) {}
        }
    }

    @Nullable
    @Override
    public JSONObject loadData(@NonNull Context context, @NonNull String filename) throws PersistenceException {
        FileInputStream fis;
        InputStreamReader isr;
        BufferedReader bufferedReader;

        StringBuilder sb = new StringBuilder();

        // Read the content
        try {
            File file = new File(context.getCacheDir(), filename);
            fis = new FileInputStream(file);
            isr = new InputStreamReader(fis, "UTF-8");
            bufferedReader = new BufferedReader(isr);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception ex) {
            throw new PersistenceException("Can't read file. " + ex.toString());
        }

        try {
            bufferedReader.close();
            isr.close();
            fis.close();
        } catch (IOException ex) {
            throw new PersistenceException("Stream not closed. " + ex.toString());
        }

        try {
            JSONObject json = new JSONObject(sb.toString());

            // If the jsonVersion is different to the current supported file version, delete the file
            if (json.has(PERSISTENCE_SAVE_VERSION_KEY)) {
                if (json.getInt(PERSISTENCE_SAVE_VERSION_KEY) == PERSISTENCE_CURRENT_FILE_VERSION) {
                    return json;
                } else {
                    throw new PersistenceException("The loaded file has a wrong version. Dropping.");
                }
            }
        } catch (JSONException ex) {
            throw new PersistenceException("Can't parse loaded json response. " + ex.toString(), ex);
        }

        return null;
    }

    @Override
    public void deleteData(@NonNull Context context, @NonNull String filename) throws PersistenceException {
        final File cacheDir = context.getCacheDir();

        File file = new File(cacheDir, filename);
        if (file.exists()) {
            if (file.delete()) {
                Logger.internal(TAG, "Unable to delete file " + file.getName());
            }
        }
    }
}
