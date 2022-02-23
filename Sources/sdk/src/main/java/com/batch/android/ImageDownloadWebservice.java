package com.batch.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.batch.android.core.GenericHelper;
import com.batch.android.core.Logger;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Webservice;
import com.batch.android.post.PostDataProvider;
import java.net.MalformedURLException;
import java.util.List;

/**
 * @hide
 */
public class ImageDownloadWebservice extends Webservice {

    private static final String TAG = "ImageDownloadWebservice";

    private String url;

    protected ImageDownloadWebservice(Context context, String imageURL, List<Double> availableDensities)
        throws MalformedURLException {
        super(context, RequestType.GET, buildImageURL(context, imageURL, availableDensities));
        this.url = buildImageURL(context, imageURL, availableDensities);
    }

    // ---------------------------------------------->

    /**
     * Build image url based on the device density and available ones
     *
     * @param context
     * @param imageURL
     * @param availableDensities
     * @return
     */
    public static String buildImageURL(Context context, String imageURL, List<Double> availableDensities) {
        if (availableDensities == null) {
            return imageURL;
        }

        Float density = GenericHelper.getScreenDensity(context);
        if (density == null) {
            return imageURL;
        }

        Double densityDouble = density.doubleValue();

        if (availableDensities.contains(densityDouble)) {
            return appendDensityToImageURL(imageURL, densityDouble);
        } else {
            for (Double availableDensity : availableDensities) {
                if (densityDouble < availableDensity) {
                    return appendDensityToImageURL(imageURL, availableDensity);
                }
            }

            return imageURL;
        }
    }

    /**
     * Append density to the image URL
     *
     * @param imageURL
     * @param density
     * @return
     */
    private static String appendDensityToImageURL(String imageURL, Double density) {
        try {
            int lastDotIndex = imageURL.lastIndexOf(".");
            String start = imageURL.substring(0, lastDotIndex);
            String end = imageURL.substring(lastDotIndex + 1);

            return start + "-" + density + "." + end;
        } catch (Exception e) {
            Logger.internal(TAG, "Error while appending density to image url", e);
            return imageURL;
        }
    }

    public Bitmap run() {
        try {
            Logger.internal(TAG, "Image download webservice started [" + url + "]");

            /*
             * Read response
             */
            byte[] response = executeRequest();
            if (response == null) {
                Logger.internal(TAG, "Error while downloading image [" + url + "]");
                return null;
            }

            /*
             * Instanciate Bitmap
             */
            Bitmap btp = BitmapFactory.decodeByteArray(response, 0, response.length);
            if (btp == null) {
                throw new RuntimeException("Unable to decode bitmap");
            }

            Logger.internal(TAG, "Image download webservice ended [" + url + "]");

            return btp;
        } catch (Throwable e) { // Throwable to catch executeRequest Error
            Logger.internal(TAG, "Error while downloading image [" + url + "]", e);
            return null;
        }
    }

    // ----------------------------------------->

    @Override
    protected PostDataProvider<?> getPostDataProvider() {
        return null;
    }

    @Override
    protected String getURLSorterPatternParameterKey() {
        return ParameterKeys.IMAGE_WS_URLSORTER_PATTERN_KEY;
    }

    @Override
    protected String getCryptorTypeParameterKey() {
        return ParameterKeys.IMAGE_WS_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getCryptorModeParameterKey() {
        return ParameterKeys.IMAGE_WS_CRYPTORMODE_KEY;
    }

    @Override
    protected String getPostCryptorTypeParameterKey() {
        return ParameterKeys.IMAGE_WS_POST_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getReadCryptorTypeParameterKey() {
        return ParameterKeys.IMAGE_WS_READ_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getSpecificConnectTimeoutKey() {
        return ParameterKeys.IMAGE_WS_CONNECT_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificReadTimeoutKey() {
        return ParameterKeys.IMAGE_WS_READ_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificRetryCountKey() {
        return ParameterKeys.IMAGE_WS_RETRYCOUNT_KEY;
    }
}
