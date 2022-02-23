package com.batch.android.messaging;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import com.batch.android.core.ByteArrayHelper;
import com.batch.android.core.ForwardReadableInputStream;
import com.batch.android.core.Logger;
import com.batch.android.core.TLSSocketFactory;
import com.batch.android.messaging.gif.GifHelper;
import com.batch.android.messaging.model.MessagingError;
import com.batch.android.module.MessagingModule;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;

/**
 * Simple AsyncTask that will handle the downloading of the hero image.
 * No need for all of Batch's Webservice stuff here, simply download the image and call it a day.
 *
 */
public class AsyncImageDownloadTask extends AsyncTask<String, Void, AsyncImageDownloadTask.Result> {

    private static final String TAG = "AsyncImageDownloadTask";
    //region Inner classes/interfaces

    private MessagingError lastError = null;

    public abstract static class Result<T> {

        private String key;
        private T value;

        Result(String key, T value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public T get() {
            return value;
        }
    }

    public static class BitmapResult extends Result<Bitmap> {

        BitmapResult(String key, Bitmap value) {
            super(key, value);
        }
    }

    public static class GIFResult extends Result<byte[]> {

        GIFResult(String key, byte[] value) {
            super(key, value);
        }
    }

    public interface ImageDownloadListener {
        void onImageDownloadStart();

        void onImageDownloadSuccess(Result result);

        void onImageDownloadError(@NonNull MessagingError errorCause);
    }

    //endregion

    private WeakReference<ImageDownloadListener> weakListener;

    public AsyncImageDownloadTask(ImageDownloadListener listener) {
        weakListener = new WeakReference<>(listener);
    }

    @Override
    protected void onPreExecute() {
        ImageDownloadListener listener = weakListener.get();
        if (listener != null) {
            listener.onImageDownloadStart();
        }
    }

    @Override
    protected Result doInBackground(String... params) {
        if (params == null || params[0] == null) {
            return null;
        }

        Logger.internal(TAG, "Downloading image for URL: " + params[0]);

        URL imageURL;
        try {
            imageURL = new URL(params[0]);
        } catch (MalformedURLException e) {
            Logger.internal(TAG, "Error while reading the image URL (" + params[0] + ")", e);
            return null;
        }

        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            connection = (HttpURLConnection) imageURL.openConnection();

            if (connection instanceof HttpsURLConnection) {
                // If HTTPS is enable, we enforce the TLS versions
                ((HttpsURLConnection) connection).setSSLSocketFactory(new TLSSocketFactory());
            }

            connection.setConnectTimeout(20000);
            connection.setReadTimeout((int) (MessagingModule.DEFAULT_IMAGE_DOWNLOAD_TIMEOUT * 1000));
            connection.setRequestMethod("GET");

            connection.connect();
            int statusCode = connection.getResponseCode();
            if (statusCode >= 200 && statusCode < 300) {
                inputStream = connection.getInputStream();

                // Using this input stream wrapper means that we can't download valid bitmaps that are less than 6 bytes
                // Since that's not really a thing, it's an acceptable edge case that doesn't warrant a fix
                ForwardReadableInputStream fris = new ForwardReadableInputStream(
                    inputStream,
                    GifHelper.NEEDED_BYTES_FOR_TYPE_CHECK
                );
                if (GifHelper.isPotentiallyAGif(fris.getFirstBytes())) {
                    return new GIFResult(params[0], ByteArrayHelper.fromInputStream(fris));
                } else {
                    try {
                        return new BitmapResult(params[0], BitmapFactory.decodeStream(fris));
                    } catch (OutOfMemoryError e) {
                        Logger.internal(TAG, "Out of memory while creating the bitmap", e);
                        return null;
                    }
                }
            } else {
                Logger.internal(
                    TAG,
                    "Server returned an invalid status code (" + statusCode + ") for URL (" + params[0] + ")"
                );
                lastError = MessagingError.SERVER_FAILURE;
                return null;
            }
        } catch (SSLException e) {
            Logger.internal(TAG, "Unexpected error while downloading", e);
            lastError = MessagingError.SERVER_FAILURE;
        } catch (IOException e1) {
            Logger.internal(TAG, "Unexpected error while downloading", e1);
            lastError = MessagingError.CLIENT_NETWORK;
        } catch (ClassCastException | NoSuchAlgorithmException | KeyManagementException e2) {
            Logger.internal(TAG, "Unexpected error while downloading", e2);
            lastError = MessagingError.UNKNOWN;
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception ignored) {}
            }

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {}
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(Result result) {
        ImageDownloadListener listener = weakListener.get();
        if (listener != null) {
            if (result != null) {
                listener.onImageDownloadSuccess(result);
            } else {
                listener.onImageDownloadError(lastError != null ? lastError : MessagingError.UNKNOWN);
            }
        }
    }
}
