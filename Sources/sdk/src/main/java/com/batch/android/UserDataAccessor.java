package com.batch.android;

import android.content.Context;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.SQLUserDatasourceProvider;
import com.batch.android.module.UserModule;
import com.batch.android.user.SQLUserDatasource;
import com.batch.android.user.UserAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @hide
 */
public class UserDataAccessor {

    @SuppressWarnings("ConstantConditions")
    public static void fetchTagCollections(
        @NonNull final Context context,
        @Nullable final BatchTagCollectionsFetchListener listener,
        boolean async
    ) {
        if (context == null) {
            throw new IllegalArgumentException("Context can't be null");
        }

        Runnable runnable = () -> {
            Map<String, Set<String>> tagCollections = null;
            final SQLUserDatasource datasource = SQLUserDatasourceProvider.get(context);

            if (datasource == null) {
                Logger.error(UserModule.TAG, "Datasource error.");
            } else {
                tagCollections = datasource.getTagCollections();
            }

            final Map<String, Set<String>> finalTagCollections = tagCollections;
            if (async) {
                Handler mainHandler = new Handler(context.getMainLooper());
                mainHandler.post(() -> {
                    if (listener != null) {
                        if (finalTagCollections == null) {
                            listener.onError();
                        } else {
                            listener.onSuccess(finalTagCollections);
                        }
                    }
                });
            } else {
                if (listener != null) {
                    if (finalTagCollections == null) {
                        listener.onError();
                    } else {
                        listener.onSuccess(finalTagCollections);
                    }
                }
            }
        };

        if (async) {
            UserModule.submitOnApplyQueue(0, runnable);
        } else {
            runnable.run();
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static void fetchAttributes(
        @NonNull final Context context,
        @Nullable final BatchAttributesFetchListener listener,
        boolean async
    ) {
        if (context == null) {
            throw new IllegalArgumentException("Context can't be null");
        }

        Runnable runnable = () -> {
            HashMap<String, BatchUserAttribute> publicAttributes = null;
            final SQLUserDatasource datasource = SQLUserDatasourceProvider.get(context);

            if (datasource == null) {
                Logger.error(UserModule.TAG, "Datasource error.");
            } else {
                HashMap<String, UserAttribute> privateAttributes = datasource.getAttributes();
                if (privateAttributes != null) {
                    publicAttributes = new HashMap<>();

                    BatchUserAttribute.Type publicType;
                    for (Map.Entry<String, UserAttribute> entry : privateAttributes.entrySet()) {
                        switch (entry.getValue().type) {
                            case BOOL:
                                publicType = BatchUserAttribute.Type.BOOL;
                                break;
                            case DATE:
                                publicType = BatchUserAttribute.Type.DATE;
                                break;
                            case LONG:
                                publicType = BatchUserAttribute.Type.LONGLONG;
                                break;
                            case DOUBLE:
                                publicType = BatchUserAttribute.Type.DOUBLE;
                                break;
                            case STRING:
                                publicType = BatchUserAttribute.Type.STRING;
                                break;
                            case URL:
                                publicType = BatchUserAttribute.Type.URL;
                                break;
                            default:
                                continue; // We skip attributes whose type is not dealt with above.
                        }

                        BatchUserAttribute publicAttribute = new BatchUserAttribute(entry.getValue().value, publicType);

                        // Clean the key so that it is equal to the one used when setting the attribute.
                        String cleanKey = entry.getKey().substring(2);
                        publicAttributes.put(cleanKey, publicAttribute);
                    }
                }
            }

            final HashMap<String, BatchUserAttribute> finalPublicAttributes = publicAttributes;
            if (async) {
                Handler mainHandler = new Handler(context.getMainLooper());
                mainHandler.post(() -> {
                    if (listener != null) {
                        if (finalPublicAttributes != null) {
                            listener.onSuccess(finalPublicAttributes);
                        } else {
                            listener.onError();
                        }
                    }
                });
            } else {
                if (listener != null) {
                    if (finalPublicAttributes != null) {
                        listener.onSuccess(finalPublicAttributes);
                    } else {
                        listener.onError();
                    }
                }
            }
        };

        if (async) {
            UserModule.submitOnApplyQueue(0, runnable);
        } else {
            runnable.run();
        }
    }
}
