package com.batch.android;

import android.content.Context;
import com.batch.android.core.ParameterKeys;
import com.batch.android.di.providers.ParametersProvider;
import java.util.Date;
import java.util.UUID;

/**
 * Object that encapsulate install data
 *
 * @hide
 */
final class Install {

    /**
     * Install ID
     */
    private String installID;
    /**
     * Date of the install
     */
    private Date installDate;

    // -------------------------------------------->

    /**
     * @param context
     */
    protected Install(Context context) {
        if (context == null) {
            throw new NullPointerException("Null context");
        }

        installID = getInstallID(context.getApplicationContext());
        installDate = getInstallDate(context.getApplicationContext());
    }

    // -------------------------------------------->

    /**
     * Return the install ID
     *
     * @return
     */
    public String getInstallID() {
        return installID;
    }

    /**
     * Return the installation date
     *
     * @return
     */
    public Date getInstallDate() {
        return installDate;
    }

    // -------------------------------------------->

    /**
     * retrieve the install ID and generate one if not available
     *
     * @param context
     * @return
     */
    private String getInstallID(Context context) {
        String value = ParametersProvider.get(context).get(ParameterKeys.INSTALL_ID_KEY);
        if (value == null) {
            value = generateInstallID(context);
            ParametersProvider.get(context).set(ParameterKeys.INSTALL_ID_KEY, value, true);
        }

        return value;
    }

    /**
     * Generate an installation ID
     *
     * @param context
     * @return
     */
    private String generateInstallID(Context context) {
        return UUID.randomUUID().toString();
    }

    // -------------------------------------------->

    /**
     * retrieve the install date
     *
     * @param context
     * @return
     */
    private Date getInstallDate(Context context) {
        String value = ParametersProvider.get(context).get(ParameterKeys.INSTALL_TIMESTAMP_KEY);
        if (value == null) {
            Date date = new Date();
            ParametersProvider
                .get(context)
                .set(ParameterKeys.INSTALL_TIMESTAMP_KEY, String.valueOf(date.getTime()), true);
            return date;
        }

        return new Date(Long.valueOf(value));
    }
}
