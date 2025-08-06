package com.batch.android.messaging.view.formats.mep;

import android.os.SystemClock;
import android.util.LruCache;
import android.view.View;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.BatchMessage;
import com.batch.android.MessagingAnalyticsDelegate;
import com.batch.android.core.Logger;
import com.batch.android.core.ReflectionHelper;
import com.batch.android.di.providers.MessagingModuleProvider;
import com.batch.android.messaging.AsyncImageDownloadTask;
import com.batch.android.messaging.css.DOMNode;
import com.batch.android.messaging.model.CTA;
import com.batch.android.messaging.model.mep.BannerMessage;
import com.batch.android.messaging.view.formats.EmbeddedBannerContainer;
import com.batch.android.messaging.view.helper.ImageHelper;
import com.batch.android.module.MessagingModule;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;

/**
 * Container for Batch's banners built from the MEP.
 * Handles everything related to the banner: its lifecycle, window insets, etc...
 * <p>
 * Kinda similar to <a href="https://android.googlesource.com/platform/frameworks/support.git/+/master/design/src/android/support/design/widget/BaseTransientBottomBar.java">...</a>
 */
@Module
public class MEPBannerContainer
    extends EmbeddedBannerContainer
    implements BannerView.OnActionListener, ImageHelper.Cache {

    private final BannerMessage message;

    private final LruCache<String, AsyncImageDownloadTask.Result> imageCache;

    @Provide
    public static MEPBannerContainer provide(
        @NonNull View attachTo,
        @NonNull BatchMessage payloadMessage,
        @NonNull BannerMessage message,
        @NonNull MessagingAnalyticsDelegate analyticsDelegate,
        boolean embed
    ) {
        return new MEPBannerContainer(
            MessagingModuleProvider.get(),
            attachTo,
            payloadMessage,
            message,
            analyticsDelegate,
            embed
        );
    }

    private MEPBannerContainer(
        @NonNull MessagingModule messagingModule,
        @NonNull View attachTo,
        @NonNull BatchMessage payloadMessage,
        @NonNull BannerMessage message,
        @NonNull MessagingAnalyticsDelegate analyticsDelegate,
        boolean embed
    ) {
        super(attachTo, messagingModule, analyticsDelegate, payloadMessage, embed);
        this.message = message;
        this.imageCache = new LruCache<>(1); // We have max 1 image on a banner

        // Fits system windows to dodge the status bar and navigation bar
        this.rootView.setFitsSystemWindows(true);

        // Make the banner view, cannot be done in the super constructor
        // since child's methods are called
        super.makeView();

        // Opt out of dark mode
        ReflectionHelper.optOutOfDarkModeRecursively(rootView);
    }

    private BannerView getBannerView() {
        return ((BannerView) bannerView);
    }

    @NonNull
    @Override
    protected BannerView makeBannerView() {
        BannerView v = new BannerView(this.context, this.message, null, new DOMNode("root"), this);
        v.setActionListener(this);
        return v;
    }

    @Override
    protected boolean allowSwipeToDismiss() {
        return message.allowSwipeToDismiss;
    }

    @NonNull
    @Override
    protected VerticalEdge getPinnedVerticalEdge() {
        return getBannerView().getPinnedVerticalEdge();
    }

    @Override
    protected void scheduleAutoClose() {
        if (message.autoCloseDelay > 0 && getBannerView().canAutoClose()) {
            getBannerView().startAutoCloseCountdown();
            long when = SystemClock.uptimeMillis() + message.autoCloseDelay;
            mainThreadHandler.postAtTime(this::performAutoClose, autoCloseHandlerToken, when);
        }
    }

    @Nullable
    @Override
    protected ScrollView getScrollView() {
        return null;
    }

    @Override
    public void onCloseAction() {
        dismiss(true);
        analyticsDelegate.onClosed();
    }

    @Override
    public void onCTAAction(int index, @NonNull CTA cta) {
        dismiss(true);
        analyticsDelegate.onCTAClicked(index, cta);
        messagingModule.performAction(context, payloadMessage, cta);
    }

    @Override
    public void onGlobalAction() {
        dismiss(true);
        analyticsDelegate.onGlobalTap(message.globalTapAction);
        if (message.globalTapAction != null) {
            messagingModule.performAction(context, payloadMessage, message.globalTapAction);
        } else {
            Logger.internal(MessagingModule.TAG, "Could not perform global tap action. Internal error.");
        }
    }

    @Override
    public void put(@NonNull AsyncImageDownloadTask.Result result) {
        imageCache.put(result.getKey(), result);
    }

    @Nullable
    @Override
    public AsyncImageDownloadTask.Result get(@NonNull String key) {
        return imageCache.get(key);
    }
}
