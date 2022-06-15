package com.batch.android.messaging.view.formats;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.R;
import com.batch.android.core.Logger;
import com.batch.android.core.ReflectionHelper;
import com.batch.android.messaging.AsyncImageDownloadTask;
import com.batch.android.messaging.css.DOMNode;
import com.batch.android.messaging.css.Document;
import com.batch.android.messaging.model.BannerMessage;
import com.batch.android.messaging.model.BaseBannerMessage;
import com.batch.android.messaging.model.CTA;
import com.batch.android.messaging.model.MessagingError;
import com.batch.android.messaging.view.CloseButton;
import com.batch.android.messaging.view.CountdownView;
import com.batch.android.messaging.view.FlexboxLayout;
import com.batch.android.messaging.view.helper.ImageHelper;
import com.batch.android.messaging.view.helper.StyleHelper;
import com.batch.android.messaging.view.helper.ViewCompat;
import com.batch.android.messaging.view.percent.PercentRelativeLayout;
import com.batch.android.messaging.view.roundimage.RoundedImageView;
import com.batch.android.messaging.view.styled.Button;
import com.batch.android.messaging.view.styled.SeparatedFlexboxLayout;
import com.batch.android.messaging.view.styled.Styleable;
import com.batch.android.messaging.view.styled.TextView;
import com.batch.android.module.MessagingModule;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * View displaying the banner messaging template.
 * It is capable of reading the message, extract the elements and CSS from it, create the native views and layout them correctly according to the style.
 * Meant to be used in {@link EmbeddedBannerContainer}.
 * Everything is done in code so it does not need a layout file to work.
 *
 */

@SuppressLint("ViewConstructor")
public class BannerView
    extends RelativeLayout
    implements SeparatedFlexboxLayout.SeparatorStyleProvider, AsyncImageDownloadTask.ImageDownloadListener {

    private static final int IMAGE_FADE_IN_ANIMATION_DURATION = 500;
    private static final int BODY_MIN_HEIGHT_DP = 28;
    private static final int BODY_MAX_HEIGHT_DP = 150;

    private final Context context;
    private final BaseBannerMessage message;
    private final ImageHelper.Cache imageCache;
    private final Document style;
    private final Point screenSizeDP;

    private VerticalEdge pinnedVerticalEdge;

    private final SeparatedFlexboxLayout contentLayout;

    private CountdownView countdownView;

    private RoundedImageView imageView;

    private OnActionListener actionListener;

    private long uptimeWhenShown;

    /**
     * Create a banner view from the given message
     *
     * @param context     Context
     * @param message     Message describing the banner
     * @param cachedStyle Style, if it has already been computed
     * @param rootNode    Root DOM Node. Allows you to pick which rules the BannerView will use for itself
     *                    Gives some flexibility regarding what it will be contained in
     */
    public BannerView(
        @NonNull Context context,
        @NonNull BaseBannerMessage message,
        @Nullable Document cachedStyle,
        @NonNull DOMNode rootNode,
        @Nullable ImageHelper.Cache imageCache
    ) {
        super(context);
        setId(R.id.com_batchsdk_messaging_root_view);

        this.context = context;
        this.message = message;
        this.imageCache = imageCache;
        this.style = cachedStyle != null ? cachedStyle : StyleHelper.parseStyle(message.css);
        this.screenSizeDP = ViewCompat.getScreenSize(context);

        Map<String, String> rootRules = getRulesForView(rootNode);
        this.pinnedVerticalEdge = getPinnedVerticalEdge(rootRules);

        StyleHelper.applyCommonRules(this, rootRules);
        setLayoutParams(
            StyleHelper.getFrameLayoutParams(
                this.context,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
                rootRules
            )
        );

        Map<String, String> contentRules = getRulesForView(new DOMNode("content"));
        contentLayout = makeContentLayout(contentRules);
        contentLayout.setId(R.id.com_batchsdk_messaging_content_view);
        RelativeLayout.LayoutParams lp = StyleHelper.getRelativeLayoutParams(
            context,
            new PercentRelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ),
            contentRules,
            RelativeLayout.ALIGN_PARENT_LEFT,
            null
        );

        // Override the CSS' vertical-align, to fix the BannerView's measurement.
        // As the vertical align will set the pinnedVerticalEdge, the BannerView itself will be attached to the
        // bottom if needed, making these rules not only useless but harmful to additional behaviour
        //removeRule(rule) is equivalent to addRule(rule, 0)
        lp.addRule(RelativeLayout.ALIGN_BOTTOM, 0);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);

        if (this.message.globalTapAction != null) {
            contentLayout.setOnClickListener(v -> onGlobalTap());
        }

        addView(contentLayout, lp);
        addImage();
        addCloseButton();
        addCountdownView();

        ReflectionHelper.optOutOfDarkModeRecursively(this);
    }

    public void setActionListener(OnActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public SeparatedFlexboxLayout getContentView() {
        return contentLayout;
    }

    public void onShown() {
        uptimeWhenShown = SystemClock.uptimeMillis();
    }

    public boolean canAutoClose() {
        return !ViewCompat.isTouchExplorationEnabled(context);
    }

    public void startAutoCloseCountdown() {
        if (countdownView != null && message.autoCloseDelay > 0) {
            countdownView.animateForDuration(message.autoCloseDelay);
        }
    }

    private SeparatedFlexboxLayout makeContentLayout(Map<String, String> contentStyleRules) {
        final LayoutInflater inflater = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        final Resources resources = getResources();

        final SeparatedFlexboxLayout layout = new SeparatedFlexboxLayout(context, "cnt", this);
        layout.setFlexDirection(FlexboxLayout.FLEX_DIRECTION_COLUMN);
        layout.applyStyleRules(contentStyleRules);
        layout.setClipChildren(false);
        layout.setClipToPadding(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            layout.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        VerticalEdge contentPinnedEdge = getPinnedVerticalEdge(contentStyleRules);

        if (contentPinnedEdge != null) {
            this.pinnedVerticalEdge = contentPinnedEdge;
        }

        final List<Pair<View, DOMNode>> views = new LinkedList<>();

        // Title text view
        View titleTv;
        DOMNode titleDOMNode = new DOMNode("title");
        final FlexboxLayout.LayoutParams titleLp = new FlexboxLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleLp.flexGrow = 1;
        titleLp.flexShrink = 0;
        if (!TextUtils.isEmpty(message.titleText)) {
            titleTv = new TextView(context);
            ((TextView) titleTv).setText(message.titleText);
            titleDOMNode.classes.add("text");
        } else {
            // Insert a dummy view if there is no title, as the text view is used as an anchor
            // for layouting some styles.
            // We could improve this using container views/reworking the styles, but this
            // allows special styling to work without issue.
            titleTv = new View(context);
            titleLp.height = 0;
            titleLp.maxHeight = 0;
        }
        titleTv.setLayoutParams(titleLp);
        views.add(new Pair<>(titleTv, titleDOMNode));

        // Use an xml, as android:scrollbars is not doable in pure code
        final TextView bodyTv = (TextView) inflater.inflate(
            R.layout.com_batchsdk_messaging_scrollabletextview,
            layout,
            false
        );
        bodyTv.setText(message.getBody());
        bodyTv.makeScrollable();

        if (this.message.globalTapAction != null) {
            // We set the global action on the body TV because the view override onTouch
            bodyTv.setOnClickListener(v -> onGlobalTap());
        }

        final FlexboxLayout.LayoutParams bodyLp = new FlexboxLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bodyLp.flexGrow = 1;
        bodyLp.flexShrink = 0;
        bodyLp.minHeight = StyleHelper.dpToPixels(resources, (float) BODY_MIN_HEIGHT_DP);
        bodyLp.maxHeight = StyleHelper.dpToPixels(resources, (float) BODY_MAX_HEIGHT_DP);
        bodyTv.setLayoutParams(bodyLp);
        views.add(new Pair<>(bodyTv, new DOMNode("body", "text")));

        for (Pair<View, DOMNode> viewPair : views) {
            final View view = getStyledFlexboxSubview(viewPair);
            layout.addView(view);
        }

        final Map<String, String> ctaRules = getRulesForView(new DOMNode("ctas"));
        final View ctaLayout = makeCTALayout(ctaRules);
        if (ctaLayout != null) {
            ctaLayout.setId(ViewCompat.generateViewId());

            FlexboxLayout.LayoutParams ctaLP = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );

            ctaLP = StyleHelper.getFlexLayoutParams(context, ctaLP, ctaRules);
            layout.addView(ctaLayout, ctaLP);
        }

        return layout;
    }

    private SeparatedFlexboxLayout makeCTALayout(Map<String, String> rules) {
        if (message.ctas == null || message.ctas.size() == 0) {
            return null;
        }

        final SeparatedFlexboxLayout layout = new SeparatedFlexboxLayout(context, "ctas", this);
        layout.setClipChildren(false);
        layout.setClipToPadding(false);

        if (message.ctaDirection == BannerMessage.CTADirection.HORIZONTAL) {
            layout.setFlexDirection(FlexboxLayout.FLEX_DIRECTION_ROW_REVERSE);
        } else {
            layout.setFlexDirection(FlexboxLayout.FLEX_DIRECTION_COLUMN);
        }

        // Since we're in column mode, this is the default h-align
        layout.setAlignItems(FlexboxLayout.ALIGN_ITEMS_CENTER);
        layout.setJustifyContent(FlexboxLayout.JUSTIFY_CONTENT_CENTER);
        layout.applyStyleRules(rules);

        final List<Pair<View, DOMNode>> views = new LinkedList<>();

        final List<CTA> ctas = new LinkedList<>(message.ctas);

        for (int i = 0; i < ctas.size(); i++) {
            final CTA cta = ctas.get(i);
            final Button ctaView = new Button(context);
            ctaView.setText(cta.label);
            ctaView.setTag(cta);
            ctaView.setMaxLines(1);
            ctaView.setSingleLine(true); // This works around https://code.google.com/p/android/issues/detail?id=33868
            ctaView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            ctaView.setAllCaps(false);
            final int ctaIndex = i; // Thanks, java
            ctaView.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onCTAAction(ctaIndex, cta);
                }
            });

            final String btnDirectionalClass =
                "btn-" + (message.ctaDirection == BannerMessage.CTADirection.HORIZONTAL ? "h" : "v");

            final View view = getStyledFlexboxSubview(
                new Pair<>(ctaView, new DOMNode("cta" + (i + 1), "btn", btnDirectionalClass))
            );
            if (message.ctaDirection == BannerMessage.CTADirection.HORIZONTAL) {
                final FlexboxLayout.LayoutParams lp = (FlexboxLayout.LayoutParams) view.getLayoutParams();
                lp.flexGrow = 1;
                lp.flexShrink = 0;
                lp.flexBasisPercent = (float) 0.1; // This hack works. Don't really know why. Note that you'll need a different basis if you want separators (0.1 should work)
                view.setLayoutParams(lp); // Is not necessary, but that's an implementation detail, so do it to be sure.
            }
            layout.addView(view);
        }

        return layout;
    }

    private View getStyledFlexboxSubview(Pair<View, DOMNode> viewPair) {
        final View view = viewPair.first;
        final DOMNode node = viewPair.second;
        final Map<String, String> rules = getRulesForView(node);
        if (view instanceof Styleable) {
            ((Styleable) view).applyStyleRules(rules);
        }

        FlexboxLayout.LayoutParams lp = null;
        if (view.getLayoutParams() instanceof FlexboxLayout.LayoutParams) {
            lp = (FlexboxLayout.LayoutParams) view.getLayoutParams();
        }

        lp = StyleHelper.getFlexLayoutParams(context, lp, rules);
        view.setLayoutParams(lp);
        return view;
    }

    private void addImage() {
        if (message.imageURL != null) {
            imageView = new RoundedImageView(context);
            final Map<String, String> rules = getRulesForView(new DOMNode("img"));
            imageView.applyStyleRules(rules);
            imageView.setId(R.id.com_batchsdk_messaging_image_view);
            PercentRelativeLayout.LayoutParams lp = StyleHelper.getRelativeLayoutParams(
                context,
                new PercentRelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ),
                rules,
                RelativeLayout.ALIGN_PARENT_RIGHT,
                contentLayout
            );
            lp.addRule(RelativeLayout.ALIGN_TOP, contentLayout.getId());
            imageView.setLayoutParams(lp);

            imageView.setContentDescription(message.imageDescription);
            imageView.setAlpha(0);
            addView(imageView);

            if (imageCache == null || imageCache.get(message.imageURL) == null) {
                // Image not in cache, start download
                final AsyncImageDownloadTask imageDownloadTask = new AsyncImageDownloadTask(this);
                imageDownloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message.imageURL);
            }
        }
    }

    private void addCloseButton() {
        // Force close button if auto close is enabled
        if (message.showCloseButton || (!canAutoClose() && message.autoCloseDelay > 0)) {
            final CloseButton closeButton = new CloseButton(context);
            final Map<String, String> closeButtonRules = getRulesForView(new DOMNode("close"));
            closeButton.applyStyleRules(closeButtonRules);
            closeButton.setId(R.id.com_batchsdk_messaging_close_button);
            closeButton.setLayoutParams(
                StyleHelper.getRelativeLayoutParams(
                    context,
                    new PercentRelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ),
                    closeButtonRules,
                    RelativeLayout.ALIGN_PARENT_RIGHT,
                    contentLayout
                )
            );
            closeButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onCloseAction();
                }
            });
            addView(closeButton);
        }
    }

    private void addCountdownView() {
        if (canAutoClose() && message.autoCloseDelay > 0) {
            final Map<String, String> countdownRules = getRulesForView(new DOMNode("countdown"));
            countdownView = new CountdownView(this.context);
            countdownView.applyStyleRules(countdownRules);
            countdownView.setId(R.id.com_batchsdk_messaging_countdown_progress);

            RelativeLayout.LayoutParams lp = StyleHelper.getRelativeLayoutParams(
                context,
                new PercentRelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ),
                countdownRules,
                RelativeLayout.ALIGN_PARENT_LEFT,
                contentLayout
            );

            addView(countdownView, lp);
        }
    }

    private VerticalEdge getPinnedVerticalEdge(Map<String, String> rules) {
        for (Map.Entry<String, String> rule : rules.entrySet()) {
            if ("vertical-align".equalsIgnoreCase(rule.getKey())) {
                final String value = rule.getValue();
                if ("top".equalsIgnoreCase(value)) {
                    return VerticalEdge.TOP;
                } else if ("bottom".equalsIgnoreCase(value)) {
                    return VerticalEdge.BOTTOM;
                }
            }
        }

        return null;
    }

    public VerticalEdge getPinnedVerticalEdge() {
        return pinnedVerticalEdge != null ? pinnedVerticalEdge : VerticalEdge.BOTTOM;
    }

    private Map<String, String> getRulesForView(DOMNode node) {
        return this.style.getFlatRules(node, screenSizeDP);
    }

    @Override
    public Map<String, String> getRulesForSeparator(SeparatedFlexboxLayout layout, String separatorID) {
        return getRulesForView(
            new DOMNode(separatorID, layout.getSeparatorPrefix() + "-" + (layout.isHorizontal() ? "h-sep" : "sep"))
        );
    }

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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (imageView != null && imageCache != null) {
            AsyncImageDownloadTask.Result result = imageCache.get(message.imageURL);
            if (result != null) {
                // Display image from cache when view is attached to parent
                displayImage(result);
            }
        }
    }

    @Override
    public void onImageDownloadStart() {}

    @Override
    public void onImageDownloadSuccess(AsyncImageDownloadTask.Result result) {
        // Add image in cache then display it
        if (imageCache != null) {
            imageCache.put(result);
        }
        displayImage(result);
    }

    @Override
    public void onImageDownloadError(@NonNull MessagingError ignored) {}

    private void displayImage(AsyncImageDownloadTask.Result result) {
        if (imageView != null) {
            final ObjectAnimator a = ObjectAnimator.ofInt(imageView, "alpha", 0, 255);
            a.setDuration(IMAGE_FADE_IN_ANIMATION_DURATION);
            a.setInterpolator(new LinearInterpolator());
            a.start();

            ImageHelper.setDownloadResultInImage(imageView, result);
        }
    }

    public enum VerticalEdge {
        TOP,
        BOTTOM,
    }

    public interface OnActionListener {
        void onCloseAction();

        void onCTAAction(int index, @NonNull CTA cta);

        void onGlobalAction();
    }
}
