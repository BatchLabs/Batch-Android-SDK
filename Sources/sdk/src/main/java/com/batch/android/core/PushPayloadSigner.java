package com.batch.android.core;

import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.di.providers.KVUserPreferencesStorageProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Authenticates the Batch push payload carried in an Intent.
 * <p>
 * Batch signs the payload with a per-installation secret when it builds a notification, and verifies that signature
 * before auto-displaying a landing.
 */
public final class PushPayloadSigner {

    private static final String TAG = "PushPayloadSigner";

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Length in bytes of the generated per-installation secret.
     */
    private static final int SECRET_LENGTH = 32;

    /**
     * KV storage key under which the per-installation secret is persisted.
     */
    private static final String SECRET_STORAGE_KEY =
        Parameters.PARAMETERS_KEY_PREFIX + ParameterKeys.PUSH_PAYLOAD_SIGNATURE_SECRET_KEY;

    private static final Object SECRET_LOCK = new Object();

    private PushPayloadSigner() {}

    /**
     * Sign the Batch payload contained in the given bundle in place.
     *
     * @param context Context to read the signing secret from, or null to fall back to the SDK's own
     */
    public static void signPushExtras(@Nullable Context context, @Nullable Bundle extras) {
        final Context resolved = context != null ? context : RuntimeManagerProvider.get().getContext();
        if (resolved == null) {
            Logger.error(
                TAG,
                "Could not sign the push payload because Batch is not started: the mobile landing it " +
                "carries will not be displayed. If you are building a notification from a push service " +
                "while the app is in the background, call Batch.onServiceCreate() before serializing the " +
                "payload, or use Batch.Push.makePendingIntent() which does not have this requirement."
            );
            return;
        }
        signPushExtras(KVUserPreferencesStorageProvider.get(resolved), extras);
    }

    /**
     * Sign the Batch payload contained in the given bundle in place.
     * <p>
     * Reads the {@link InternalPushData#BATCH_BUNDLE_KEY} string, computes its signature and stores it
     * under {@link InternalPushData#SIGNATURE_BUNDLE_KEY}. No-op if the bundle carries no Batch data or
     * if signing fails (in which case any landing it carries will simply be rejected on read).
     */
    public static void signPushExtras(@NonNull KVUserPreferencesStorage storage, @Nullable Bundle extras) {
        if (extras == null) {
            return;
        }
        final String batchData = extras.getString(InternalPushData.BATCH_BUNDLE_KEY);
        if (batchData == null) {
            return;
        }

        final byte[] secret = getOrCreateSecret(storage);
        if (secret == null) {
            return;
        }

        final String signature = sign(secret, batchData);
        if (signature != null) {
            extras.putString(InternalPushData.SIGNATURE_BUNDLE_KEY, signature);
        }
    }

    /**
     * Whether the Batch payload contained in the given bundle carries a valid signature, proving it was
     * produced by this SDK installation and not forged by a third-party app.
     *
     * @return true only if the payload contains Batch data and a matching signature.
     */
    public static boolean isPushExtrasAuthentic(@NonNull KVUserPreferencesStorage storage, @Nullable Bundle extras) {
        if (extras == null) {
            return false;
        }
        final String batchData = extras.getString(InternalPushData.BATCH_BUNDLE_KEY);
        final String signature = extras.getString(InternalPushData.SIGNATURE_BUNDLE_KEY);
        if (batchData == null || signature == null) {
            return false;
        }

        final byte[] secret = getSecret(storage);
        if (secret == null) {
            // No secret means this installation never built a Batch notification, so no genuine signed
            // payload can exist. Any signed payload claiming otherwise is forged.
            return false;
        }

        final String expected = sign(secret, batchData);
        if (expected == null) {
            return false;
        }
        return MessageDigest.isEqual(ByteArrayHelper.getUTF8Bytes(expected), ByteArrayHelper.getUTF8Bytes(signature));
    }

    @Nullable
    private static String sign(@NonNull byte[] secret, @NonNull String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(ByteArrayHelper.getUTF8Bytes(data));
            return Base64.encodeToString(signature, Base64.NO_WRAP);
        } catch (Exception e) {
            Logger.internal(TAG, "Could not compute push payload signature", e);
            return null;
        }
    }

    /**
     * Read the per-installation secret, or null if none has been generated yet.
     */
    @Nullable
    private static byte[] getSecret(@NonNull KVUserPreferencesStorage storage) {
        final String stored = storage.get(SECRET_STORAGE_KEY);
        if (stored == null) {
            return null;
        }
        try {
            return Base64.decode(stored, Base64.NO_WRAP);
        } catch (Exception e) {
            Logger.internal(TAG, "Could not decode push payload signature secret", e);
            return null;
        }
    }

    /**
     * Read the per-installation secret, generating and persisting one if needed.
     */
    @Nullable
    private static byte[] getOrCreateSecret(@NonNull KVUserPreferencesStorage storage) {
        synchronized (SECRET_LOCK) {
            byte[] existing = getSecret(storage);
            if (existing != null) {
                return existing;
            }

            try {
                byte[] secret = new byte[SECRET_LENGTH];
                new SecureRandom().nextBytes(secret);
                storage.persist(SECRET_STORAGE_KEY, Base64.encodeToString(secret, Base64.NO_WRAP));
                return secret;
            } catch (Exception e) {
                Logger.internal(TAG, "Could not generate push payload signature secret", e);
                return null;
            }
        }
    }
}
