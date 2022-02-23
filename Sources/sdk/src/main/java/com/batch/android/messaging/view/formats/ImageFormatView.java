package com.batch.android.messaging.view.formats;

import static android.widget.RelativeLayout.ALIGN_PARENT_RIGHT;
import static android.widget.RelativeLayout.ALIGN_PARENT_TOP;
import static android.widget.RelativeLayout.ALIGN_RIGHT;
import static android.widget.RelativeLayout.ALIGN_TOP;
import static android.widget.RelativeLayout.CENTER_IN_PARENT;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.batch.android.R;
import com.batch.android.core.Logger;
import com.batch.android.core.Promise;
import com.batch.android.core.ReflectionHelper;
import com.batch.android.messaging.AsyncImageDownloadTask;
import com.batch.android.messaging.Size2D;
import com.batch.android.messaging.css.DOMNode;
import com.batch.android.messaging.css.Document;
import com.batch.android.messaging.model.ImageMessage;
import com.batch.android.messaging.model.MessagingError;
import com.batch.android.messaging.view.AnimatedCloseButton;
import com.batch.android.messaging.view.DelegatedTouchEventViewGroup;
import com.batch.android.messaging.view.FixedRatioFrameLayout;
import com.batch.android.messaging.view.helper.ImageHelper;
import com.batch.android.messaging.view.helper.StyleHelper;
import com.batch.android.messaging.view.helper.ViewCompat;
import com.batch.android.messaging.view.roundimage.RoundedImageView;
import com.batch.android.module.MessagingModule;
import java.util.Map;

/**
 * View rendering the Image format
 * Not called ImageView for obvious reasons
 */
@SuppressLint("ViewConstructor")
public class ImageFormatView extends FrameLayout implements AsyncImageDownloadTask.ImageDownloadListener {

    private static final float CLOSE_SIZE_DP = 52;
    private static final float FULLSCREEN_CLOSE_BUTTON_MARGIN_DP = 20;
    private static final float CLOSE_PADDING_DP = 10;
    private static final float MODAL_CONTAINER_MARGIN_DP = 40;
    private static final int IMAGE_FADE_IN_ANIMATION_DURATION = 500;

    private final Context context;
    private final ImageMessage message;
    private final ImageHelper.Cache imageCache;
    private final Document style;
    private final Point screenSizeDP;

    private final AnimatedCloseButton closeButton;
    private final RelativeLayout rootContainerView;
    private final ImageContainerView imageContainerView;
    private final ProgressBar imageViewLoader;

    private RoundedImageView imageView;

    private OnActionListener actionListener;

    private long uptimeWhenShown = 0L;

    private Promise<Void> viewShownPromise;

    /**
     * Create an image format view from the given message
     *
     * @param context
     */
    public ImageFormatView(
        @NonNull Context context,
        @NonNull ImageMessage message,
        @Nullable Document cachedStyle,
        @NonNull ImageHelper.Cache imageCache
    ) {
        super(context);
        setId(R.id.com_batchsdk_messaging_root_view);

        viewShownPromise = new Promise<>();

        this.context = context;
        this.message = message;
        this.imageCache = imageCache;
        this.style = cachedStyle != null ? cachedStyle : StyleHelper.parseStyle(message.css);
        this.screenSizeDP = ViewCompat.getScreenSize(context);

        addBackgroundView();
        // The rootContainerView's job is to layout the image container and be there for visual effects
        // it could all be done in a single RelativeLayout (and it used to be)
        // but we need to architecture this way to be able to animate the touch panning
        // easily without complex view linking, which is way harder to do than on iOS.
        rootContainerView = addRootContainerView();
        imageContainerView = addImageContainer(rootContainerView);
        imageView = addImageView(imageContainerView);
        imageViewLoader = addImageLoader(imageContainerView);
        closeButton = addCloseButton(rootContainerView, imageContainerView);

        if (imageCache.get(message.imageURL) == null) {
            // Image not in cache, start download
            final AsyncImageDownloadTask imageDownloadTask = new AsyncImageDownloadTask(this);
            imageDownloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message.imageURL);
        }

        setFitsSystemWindows(true);
        ReflectionHelper.optOutOfDarkModeRecursively(this);
    }

    public void setActionListener(OnActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public ImageContainerView getPannableView() {
        return imageContainerView;
    }

    public View getPanEffectsView() {
        return rootContainerView;
    }

    public void onShown() {
        viewShownPromise.resolve(null);
    }

    public boolean canAutoClose() {
        return !ViewCompat.isTouchExplorationEnabled(context);
    }

    public void startAutoCloseCountdown() {
        if (closeButton != null && message.autoCloseDelay > 0) {
            final int autoCloseMS = message.autoCloseDelay;
            closeButton.animateForDuration(autoCloseMS);
        }
    }

    private Map<String, String> getRulesForView(DOMNode node) {
        return this.style.getFlatRules(node, screenSizeDP);
    }

    //region Layouting

    private void addBackgroundView() {
        final View backgroundView = new View(context);
        backgroundView.setId(R.id.com_batchsdk_messaging_background_view);
        final Map<String, String> rules = getRulesForView(new DOMNode("background"));
        StyleHelper.applyCommonRules(backgroundView, rules);
        backgroundView.setLayoutParams(
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        );
        addView(backgroundView);
    }

    private RelativeLayout addRootContainerView() {
        final RelativeLayout rootContainer = new RelativeLayout(context);
        rootContainer.setId(R.id.com_batchsdk_messaging_container_view);
        rootContainer.setLayoutParams(
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        );

        // Animate layout changes, such as the resizing of the image container
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            final LayoutTransition lt = new LayoutTransition();
            lt.enableTransitionType(LayoutTransition.CHANGING);
            rootContainer.setLayoutTransition(lt);
        }

        addView(rootContainer);

        return rootContainer;
    }

    private ImageContainerView addImageContainer(@NonNull RelativeLayout container) {
        final ImageContainerView imageContainer = new ImageContainerView(
            context,
            message.isFullscreen ? null : message.imageSize
        );
        imageContainer.setId(R.id.com_batchsdk_messaging_image_container_view);
        final Map<String, String> rules = getRulesForView(new DOMNode("container"));
        StyleHelper.applyCommonRules(imageContainer, rules);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        if (!message.isFullscreen) {
            int margin = StyleHelper.dpToPixels(getResources(), MODAL_CONTAINER_MARGIN_DP);
            lp.setMargins(margin, margin, margin, margin);
        }
        lp.addRule(CENTER_IN_PARENT);
        imageContainer.setLayoutParams(lp);
        imageContainer.setContentDescription(message.imageDescription);

        // Add the on press effect on the container
        TypedArray ta = getContext().obtainStyledAttributes(new int[] { android.R.attr.selectableItemBackground });
        Drawable foregroundDrawable = ta.getDrawable(0);
        if (foregroundDrawable != null) {
            imageContainer.setForeground(foregroundDrawable);
        }
        ta.recycle();

        imageContainer.setOnClickListener(v -> onGlobalTap());

        container.addView(imageContainer);
        return imageContainer;
    }

    private RoundedImageView addImageView(@NonNull FrameLayout container) {
        final RoundedImageView imageView = new RoundedImageView(context);
        imageView.setId(R.id.com_batchsdk_messaging_image_view);
        final Map<String, String> rules = getRulesForView(new DOMNode("image"));
        imageView.applyStyleRules(rules);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );

        imageView.setLayoutParams(lp);

        container.addView(imageView);
        return imageView;
    }

    private ProgressBar addImageLoader(@NonNull FrameLayout container) {
        final ProgressBar imageLoader = new ProgressBar(context);
        imageLoader.setId(ViewCompat.generateViewId());
        imageLoader.setIndeterminate(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            imageLoader.setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));
        }

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.gravity = Gravity.CENTER;
        imageLoader.setLayoutParams(lp);

        container.addView(imageLoader);
        return imageLoader;
    }

    private AnimatedCloseButton addCloseButton(@NonNull RelativeLayout container, @NonNull View imageContainer) {
        final AnimatedCloseButton closeButton = new AnimatedCloseButton(context);
        closeButton.setId(R.id.com_batchsdk_messaging_close_button);
        closeButton.setShowBorder(true);
        final Map<String, String> closeButtonRules = getRulesForView(new DOMNode("close"));
        closeButton.applyStyleRules(closeButtonRules);

        int buttonSize = StyleHelper.dpToPixels(getResources(), CLOSE_SIZE_DP);

        int padding = StyleHelper.dpToPixels(getResources(), CLOSE_PADDING_DP);
        closeButton.setPadding(padding);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(buttonSize, buttonSize);

        if (message.isFullscreen) {
            lp.addRule(ALIGN_PARENT_TOP);
            lp.addRule(ALIGN_PARENT_RIGHT);
            int margin = StyleHelper.dpToPixels(getResources(), FULLSCREEN_CLOSE_BUTTON_MARGIN_DP);
            lp.setMargins(0, margin, margin, 0);
        } else {
            lp.addRule(ALIGN_TOP, imageContainer.getId());
            lp.addRule(ALIGN_RIGHT, imageContainer.getId());
            lp.setMargins(0, -buttonSize / 2, -buttonSize / 2, 0);
        }
        closeButton.setLayoutParams(lp);

        if (canAutoClose() && message.autoCloseDelay > 0) {
            closeButton.setCountdownProgress(1.0f);
        }

        closeButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onCloseAction();
            }
        });
        container.addView(closeButton);
        return closeButton;
    }

    //endregion

    //region Global tap

    private boolean mustWaitTapDelay() {
        return SystemClock.uptimeMillis() < uptimeWhenShown + message.globalTapDelay;
    }

    private void onGlobalTap() {
        if (message.globalTapDelay > 0 && mustWaitTapDelay()) {
            Logger.info(
                MessagingModule.TAG,
                "Global tap action has been triggered, but the accidental touch prevention delay hasn't elapsed: rejecting tap."
            );
            return;
        }
        if (actionListener != null) {
            actionListener.onGlobalAction();
        }
    }

    //endregion

    //region Image download callbacks

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        AsyncImageDownloadTask.Result result = imageCache.get(message.imageURL);
        if (result != null) {
            // Display image from cache when view is attached to parent
            displayImage(result);
        }
    }

    @Override
    public void onImageDownloadStart() {}

    @Override
    public void onImageDownloadSuccess(AsyncImageDownloadTask.Result result) {
        // Add image in cache then display it
        imageCache.put(result);
        displayImage(result);
    }

    @Override
    public void onImageDownloadError(@NonNull MessagingError cause) {
        ((ViewGroup) imageViewLoader.getParent()).removeView(imageViewLoader);

        if (actionListener != null) {
            actionListener.onErrorAction(cause);
        }
    }

    private void displayImage(AsyncImageDownloadTask.Result result) {
        ViewGroup imageViewLoaderParent = ((ViewGroup) imageViewLoader.getParent());
        if (imageViewLoaderParent != null) {
            imageViewLoaderParent.removeView(imageViewLoader);
        }

        if (imageView != null) {
            final ObjectAnimator a = ObjectAnimator.ofInt(imageView, "alpha", 0, 255);
            a.setDuration(IMAGE_FADE_IN_ANIMATION_DURATION);
            a.setInterpolator(new LinearInterpolator());
            a.start();

            ImageHelper.setDownloadResultInImage(imageView, result);

            // Only set the target size if not fullscreen
            if (!message.isFullscreen) {
                Drawable d = imageView.getDrawable();
                if (d != null && imageContainerView != null) {
                    int intrinsicWidth = d.getIntrinsicWidth();
                    int intrinsicHeight = d.getIntrinsicHeight();

                    imageContainerView.setTargetSize(new Size2D(intrinsicWidth, intrinsicHeight));
                }
            }
        }

        viewShownPromise.then(value -> {
            if (uptimeWhenShown == 0) {
                uptimeWhenShown = SystemClock.uptimeMillis();
            }
            if (actionListener != null) {
                actionListener.onImageDisplayedAction();
            }
        });
    }

    //endregion

    //region System inset handling

    @Override
    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        // Apparently, the relative layout does not really like to apply the insets, so convert it as
        // margin
        if (rootContainerView != null) {
            FrameLayout.LayoutParams lp = (LayoutParams) rootContainerView.getLayoutParams();
            lp.setMargins(
                insets.getSystemWindowInsetLeft(),
                insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),
                insets.getSystemWindowInsetBottom()
            );
            rootContainerView.setLayoutParams(lp);
        }

        return super.onApplyWindowInsets(insets.replaceSystemWindowInsets(0, 0, 0, 0));
    }

    //endregion

    public interface OnActionListener {
        void onCloseAction();

        void onGlobalAction();

        void onErrorAction(@NonNull MessagingError cause);

        // Called when the image content is displayed
        // This is not the same as when the view is shown: this is when the download has finished
        // (if we were waiting for one) and the image has been or is about to be displayed.
        // It is the appropriate time to start the auto close countdown
        void onImageDisplayedAction();
    }

    /**
     * Simple class that makes a {@link FixedRatioFrameLayout} implement {@link DelegatedTouchEventViewGroup}
     */
    @SuppressLint("ViewConstructor")
    public static class ImageContainerView extends FixedRatioFrameLayout implements DelegatedTouchEventViewGroup {

        private DelegatedTouchEventViewGroup.Delegate delegate;

        public ImageContainerView(@NonNull Context context, @Nullable Size2D targetSize) {
            super(context, targetSize);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (delegate != null) {
                return delegate.onInterceptTouchEvent(ev, this);
            } else {
                return super.onInterceptTouchEvent(ev);
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (delegate != null) {
                return delegate.onTouchEvent(ev, this, getForeground() != null);
            } else {
                return super.onTouchEvent(ev);
            }
        }

        @Override
        public void setTouchEventDelegate(@Nullable Delegate delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean superOnInterceptTouchEvent(MotionEvent ev) {
            return super.onInterceptTouchEvent(ev);
        }

        @Override
        public boolean superOnTouchEvent(MotionEvent ev) {
            return super.onTouchEvent(ev);
        }
    }
}
