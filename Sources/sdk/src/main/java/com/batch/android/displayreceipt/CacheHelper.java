package com.batch.android.displayreceipt;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.core.Logger;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public abstract class CacheHelper {

    private static final String TAG = "CacheHelper";

    private static final String CACHE_DIR = "com.batch.displayreceipts";
    private static final String CACHE_FILE_FORMAT = "%d-%s.bin";
    private static final int MAX_READ_RECEIPT_FROM_CACHE = 5;
    private static final long MAX_AGE_FROM_CACHE = 2592000L; // 30 days in seconds

    /**
     * Get the display receipt directory
     *
     * @param context
     * @return
     */
    @Nullable
    private static File getCacheDir(@NonNull Context context) {
        File dir = new File(context.getCacheDir(), CACHE_DIR);
        if (!dir.exists() && !dir.mkdir()) {
            Logger.internal(TAG, "Could not create display receipt cache directory");
            return null;
        }
        return dir;
    }

    @NonNull
    static String generateNewFilename(long timestamp) {
        String randomId = UUID.randomUUID().toString();
        return String.format(Locale.US, CACHE_FILE_FORMAT, timestamp, randomId);
    }

    @Nullable
    private static Long getTimestampFromFilename(String filename) {
        int firstDash = filename.indexOf('-');
        if (firstDash != -1) {
            try {
                String timestamp = filename.substring(0, firstDash);
                return Long.parseLong(timestamp);
            } catch (Exception e) {
                // Ignore
            }
        }
        return null;
    }

    @Nullable
    public static byte[] read(@NonNull File inputFile) {
        int size = (int) inputFile.length();
        if (size > 0L) {
            byte[] bytes = new byte[size];
            try (BufferedInputStream buf = new BufferedInputStream(new FileInputStream(inputFile))) {
                buf.read(bytes, 0, bytes.length);
            } catch (Exception e) {
                Logger.internal(TAG, "Could not read cached display receipt", e);
            }
            return bytes;
        }
        return null;
    }

    public static File write(@NonNull Context context, long timestamp, byte[] data) {
        File cacheDir = getCacheDir(context);
        if (cacheDir == null) {
            return null;
        }

        File outputFile = new File(cacheDir, generateNewFilename(timestamp));
        if (write(outputFile, data)) {
            return outputFile;
        }

        return null;
    }

    public static boolean write(File outputFile, byte[] data) {
        try (OutputStream out = new FileOutputStream(outputFile, false)) {
            out.write(data);
        } catch (Exception e) {
            Logger.internal(TAG, "Could not write receipt", e);
            return false;
        }

        Logger.internal(TAG, "Successfully wrote " + outputFile.getAbsolutePath());
        return true;
    }

    /**
     * Recursively delete all files in a directory, then delete the directory itself
     *
     * @param dir
     * @return
     */
    private static boolean deleteDirectory(@NonNull File dir) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return dir.delete();
    }

    /**
     * Delete cached display receipt
     *
     * @param context
     * @return
     */
    public static boolean deleteAll(@NonNull Context context) {
        File dir = new File(context.getCacheDir(), CACHE_DIR);
        return deleteDirectory(dir);
    }

    /**
     * Return a sorted list of File matching available cached display receipt.
     * Don't load the cached file in memory, only list them.
     *
     * @param context
     * @param debug   If true return all cached files in any order
     * @return
     */
    @Nullable
    public static List<File> getCachedFiles(@NonNull Context context, boolean debug) {
        File cacheDir = getCacheDir(context);
        if (cacheDir == null) {
            return null;
        }

        File[] files = cacheDir.listFiles();
        if (files != null) {
            List<File> output;

            if (debug) {
                output = new ArrayList<>();
                Collections.addAll(output, files);
            } else {
                output = filterCachedFiles(files);
            }
            return output;
        }
        return null;
    }

    private static List<File> filterCachedFiles(File[] files) {
        Map<File, Long> cachedFiles = new LinkedHashMap<>();
        for (File file : files) {
            Long timestamp = getTimestampFromFilename(file.getName());
            if (timestamp != null && timestamp + MAX_AGE_FROM_CACHE <= System.currentTimeMillis() / 1000L) {
                // Receipt too old - Delete file
                Logger.internal(TAG, "removing too old cached receipt");
                file.delete();
            } else if (timestamp != null) {
                cachedFiles.put(file, timestamp);
            } else {
                // Filename is not a valid - Delete file
                Logger.internal(TAG, "removing too invalid cached receipt");
                file.delete();
            }
        }

        // Order file by timestamp value
        List<Map.Entry<File, Long>> sortedFile = new LinkedList<>(cachedFiles.entrySet());
        Collections.sort(sortedFile, (o1, o2) -> (o1.getValue()).compareTo(o2.getValue()));

        List<File> output = new ArrayList<>();
        for (int i = 0; i < sortedFile.size() && i < MAX_READ_RECEIPT_FROM_CACHE; ++i) {
            output.add(sortedFile.get(i).getKey());
        }
        return output;
    }
}
