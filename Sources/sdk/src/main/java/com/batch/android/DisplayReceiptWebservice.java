package com.batch.android;

import android.content.Context;
import com.batch.android.core.Logger;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.core.TaskRunnable;
import com.batch.android.core.Webservice;
import com.batch.android.displayreceipt.DisplayReceipt;
import com.batch.android.post.DisplayReceiptPostDataProvider;
import com.batch.android.post.PostDataProvider;
import com.batch.android.webservice.listener.DisplayReceiptWebserviceListener;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class DisplayReceiptWebservice extends Webservice implements TaskRunnable {

  private static final String TAG = "DisplayReceiptWebservice";
  private static final String MSGPACK_SCHEMA_VERSION = "1.0.0";

  private DisplayReceiptWebserviceListener listener;
  private DisplayReceiptPostDataProvider dataProvider;

  // -------------------------------------->

  /**
   * @param context
   * @param baseURLFormat an url to format with api key (ex : http://sample.com/%s/sample)
   * @throws MalformedURLException
   */
  protected DisplayReceiptWebservice(
    Context context,
    DisplayReceiptWebserviceListener listener,
    DisplayReceiptPostDataProvider dataProvider,
    String... parameters
  ) throws MalformedURLException {
    super(
      context,
      RequestType.POST,
      Parameters.DISPLAY_RECEIPT_WS_URL,
      addSchemaVersion(parameters)
    );
    if (listener == null) {
      throw new NullPointerException("listener==null");
    }

    if (dataProvider == null || dataProvider.isEmpty()) {
      throw new NullPointerException("receipt provider is empty");
    }

    this.listener = listener;
    this.dataProvider = dataProvider;
  }

  // -------------------------------------->

  /**
   * Prepend the schema version into the url parameters
   *
   * @param parameters
   * @return
   */
  private static String[] addSchemaVersion(String[] parameters) {
    final String[] retParams = new String[parameters.length + 1];
    retParams[0] = MSGPACK_SCHEMA_VERSION;
    System.arraycopy(parameters, 0, retParams, 1, parameters.length);
    return retParams;
  }

  @Override
  protected Map<String, String> getHeaders() {
    HashMap<String, String> header = new HashMap<>();
    header.put("x-batch-protocol-version", MSGPACK_SCHEMA_VERSION);
    header.put("x-batch-sdk-version", Parameters.SDK_VERSION);
    return header;
  }

  @Override
  protected PostDataProvider<Collection<DisplayReceipt>> getPostDataProvider() {
    return dataProvider;
  }

  // -------------------------------------->

  @Override
  public String getTaskIdentifier() {
    return "Batch/receiptws";
  }

  @Override
  public void run() {
    try {
      Logger.internal(TAG, "display receipt webservice started");
      executeRequest();
      listener.onSuccess();
    } catch (WebserviceError error) {
      listener.onFailure(error);
    }
  }

  // ----------------------------------------->

  @Override
  protected String getURLSorterPatternParameterKey() {
    return ParameterKeys.DISPLAY_RECEIPT_WS_URLSORTER_PATTERN_KEY;
  }

  @Override
  protected String getCryptorTypeParameterKey() {
    return ParameterKeys.DISPLAY_RECEIPT_WS_CRYPTORTYPE_KEY;
  }

  @Override
  protected String getCryptorModeParameterKey() {
    return ParameterKeys.DISPLAY_RECEIPT_WS_CRYPTORMODE_KEY;
  }

  @Override
  protected String getPostCryptorTypeParameterKey() {
    return ParameterKeys.DISPLAY_RECEIPT_WS_POST_CRYPTORTYPE_KEY;
  }

  @Override
  protected String getReadCryptorTypeParameterKey() {
    return ParameterKeys.DISPLAY_RECEIPT_WS_READ_CRYPTORTYPE_KEY;
  }

  @Override
  protected String getSpecificConnectTimeoutKey() {
    return ParameterKeys.DISPLAY_RECEIPT_WS_CONNECT_TIMEOUT_KEY;
  }

  @Override
  protected String getSpecificReadTimeoutKey() {
    return ParameterKeys.DISPLAY_RECEIPT_WS_READ_TIMEOUT_KEY;
  }

  @Override
  protected String getSpecificRetryCountKey() {
    return ParameterKeys.DISPLAY_RECEIPT_WS_RETRYCOUNT_KEY;
  }
}
