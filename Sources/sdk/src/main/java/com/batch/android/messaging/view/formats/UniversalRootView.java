package com.batch.android.messaging.view.formats;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.batch.android.R;
import com.batch.android.core.ReflectionHelper;
import com.batch.android.messaging.AsyncImageDownloadTask;
import com.batch.android.messaging.css.DOMNode;
import com.batch.android.messaging.css.Document;
import com.batch.android.messaging.fragment.UniversalTemplateFragment;
import com.batch.android.messaging.model.CTA;
import com.batch.android.messaging.model.UniversalMessage;
import com.batch.android.messaging.view.AnimatedCloseButton;
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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * View displaying the universal messaging template.
 * It is capable of reading the message, extract the elements and CSS from it, create the native views and layout them correctly according to the style.
 * Meant to be used in {@link UniversalTemplateFragment}.
 * Everything is done in code so it does not need a layout file to work.
 *
 */
@SuppressLint("ViewConstructor")
public class UniversalRootView extends PercentRelativeLayout implements SeparatedFlexboxLayout.SeparatorStyleProvider {

    private static final double DEFAULT_HERO_SPLIT_RATIO = 0.4;

    private static final int HERO_LAYOUT_ID = 1;

    /**
     * The delay we have to wait before handling events
     */
    private static final long TAP_DELAY_MILLIS = 0;

    private boolean landscape = true;
    private boolean childRelayoutingNeeded = false;

    private Context context;
    private FrameLayout heroLayout;
    private SeparatedFlexboxLayout contentLayout;
    private SeparatedFlexboxLayout ctasLayout;

    private Map<String, String> contentStyleRules;
    private Map<String, String> ctasStyleRules;

    private AnimatedCloseButton closeButton;
    private TextureView videoView;
    private RoundedImageView heroImageView;
    private View heroPlaceholder;
    private ProgressBar heroLoader;

    private UniversalMessage message;
    private Document style;

    private boolean waitForHeroImage;
    private AsyncImageDownloadTask.Result heroDownloadResult;

    private OnActionListener actionListener;
    private TextureView.SurfaceTextureListener surfaceHolderCallback;

    private Point screenSizeDP;
    private int topInset = 0;
    private int originalContentPaddingTop = 0;
    private int originalCloseMarginTop = 0;

    // The time we displayed this view
    private long drawTimeMillis = 0;

    public UniversalRootView(
        @NonNull Context context,
        @NonNull UniversalMessage message,
        @NonNull Document style,
        AsyncImageDownloadTask.Result heroDownloadResult,
        boolean waitForHeroImage
    ) {
        super(context);
        setId(R.id.com_batchsdk_messaging_root_view);

        this.context = context;
        this.message = message;
        this.waitForHeroImage = waitForHeroImage;
        this.style = style;
        this.heroDownloadResult = heroDownloadResult;

        // Use the system's "fit system windows" feature
        // Thought, the top value will be consumed before calling super
        // So that we can insert a padding view for the content view
        setFitsSystemWindows(true);

        screenSizeDP = ViewCompat.getScreenSize(context);

        createViews();
        ReflectionHelper.optOutOfDarkModeRecursively(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        boolean oldLandscape = landscape;
        landscape = (w > h);

        if (landscape != oldLandscape || (oldw == 0 && oldh == 0)) {
            childRelayoutingNeeded = true;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawTimeMillis = SystemClock.uptimeMillis();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (childRelayoutingNeeded) {
            childRelayoutingNeeded = false;
            setupVariableLayoutParameters();
        }
    }

    private void createViews() {
        setLayoutParams(
            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        );
        StyleHelper.applyCommonRules(this, getRulesForView(new DOMNode("root")));

        if (waitForHeroImage || heroDownloadResult != null || message.videoURL != null) {
            setupHeroLayout();
        }

        setupCtaLayoutIfNeeded();
        setupContentLayout();

        if (message.showCloseButton != null && message.showCloseButton) {
            closeButton = new AnimatedCloseButton(context);
            closeButton.setId(R.id.com_batchsdk_messaging_close_button);
            final Map<String, String> closeButtonRules = getRulesForView(new DOMNode("close"));
            closeButton.applyStyleRules(closeButtonRules);
            PercentRelativeLayout.LayoutParams closeButtonLp = StyleHelper.getRelativeLayoutParams(
                context,
                new PercentRelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ),
                closeButtonRules,
                RelativeLayout.ALIGN_PARENT_RIGHT,
                null
            );
            closeButton.setLayoutParams(closeButtonLp);
            closeButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onCloseAction();
                }
            });

            if (canAutoClose() && message.autoCloseDelay > 0) {
                closeButton.setCountdownProgress(1.0f);
            }

            originalCloseMarginTop = closeButtonLp.topMargin;
            addView(closeButton);
        }
        //setupVariableLayoutParameters();
    }

    private void setupContentLayout() {
        contentStyleRules = getRulesForView(new DOMNode("content"));
        contentLayout = new SeparatedFlexboxLayout(context, "cnt", this);
        contentLayout.setId(R.id.com_batchsdk_messaging_content_view);
        contentLayout.setFlexDirection(FlexboxLayout.FLEX_DIRECTION_COLUMN);
        // Since we're in column mode, this is the default h-align
        contentLayout.setAlignItems(FlexboxLayout.ALIGN_ITEMS_CENTER);
        contentLayout.setJustifyContent(FlexboxLayout.JUSTIFY_CONTENT_CENTER);
        contentLayout.applyStyleRules(contentStyleRules);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            contentLayout.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        originalContentPaddingTop = contentLayout.getPaddingTop();

        addView(contentLayout);

        if (message == null) {
            return;
        }

        final List<Pair<View, DOMNode>> views = new LinkedList<>();
        final List<Pair<View, DOMNode>> ctaViews = new LinkedList<>();

        if (!TextUtils.isEmpty(message.headingText)) {
            final TextView headingTv = new TextView(context);
            headingTv.setText(message.headingText);
            views.add(new Pair<>(headingTv, new DOMNode("h1", "text")));
        }

        if (!TextUtils.isEmpty(message.titleText)) {
            final TextView titleTv = new TextView(context);
            titleTv.setText(message.titleText);
            views.add(new Pair<>(titleTv, new DOMNode("h2", "text")));
        }

        if (!TextUtils.isEmpty(message.subtitleText)) {
            final TextView subtitleTv = new TextView(context);
            subtitleTv.setText(message.subtitleText);
            views.add(new Pair<>(subtitleTv, new DOMNode("h3", "text")));
        }

        final DOMNode bodyTvNode = new DOMNode("body", "text");
        final Map<String, String> bodyTvRules = getRulesForView(bodyTvNode);
        final TextView bodyTv = new TextView(context);
        bodyTv.setText(message.getBody());

        bodyTv.applyStyleRules(bodyTvRules);
        bodyTv.setGravity(Gravity.CENTER);
        bodyTv.setLayoutParams(
            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        );

        final ScrollView bodyScrollWrapper = new ScrollView(context);
        bodyScrollWrapper.setFillViewport(true);
        bodyScrollWrapper.addView(bodyTv);

        final FlexboxLayout.LayoutParams bodyScrollWrapperLp = new FlexboxLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bodyScrollWrapperLp.flexGrow = 1;
        // Use the body's layout rules for the wrapper
        bodyScrollWrapper.setLayoutParams(StyleHelper.getFlexLayoutParams(context, bodyScrollWrapperLp, bodyTvRules));
        views.add(new Pair<>(bodyScrollWrapper, null));

        if (message.ctas != null) {
            int i = 0;
            for (final CTA cta : message.ctas) {
                final int index = i;
                i++;
                final Button ctaView = new Button(context);
                ctaView.setText(cta.label);
                ctaView.setTag(cta);
                ctaView.setMaxLines(1);
                ctaView.setSingleLine(true); // This works around https://code.google.com/p/android/issues/detail?id=33868
                ctaView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                ctaView.setAllCaps(false);
                ctaView.setOnClickListener(v -> {
                    if (actionListener != null && !mustWaitTapDelay()) {
                        actionListener.onCTAAction(index, cta);
                    }
                });
                ctaViews.add(new Pair<>(ctaView, new DOMNode("cta" + i, "btn")));
            }
        }

        for (Pair<View, DOMNode> viewPair : views) {
            final View view = getConfiguredView(viewPair);
            contentLayout.addView(view);
        }

        final ViewGroup targetCTALayout = ctasLayout != null ? ctasLayout : contentLayout;
        if (message.stackCTAsHorizontally != null && message.stackCTAsHorizontally) {
            // Horizontally stacked CTAs are added in the reverse order ([CTA 2]   [CTA 1])
            Collections.reverse(ctaViews);
        }

        for (Pair<View, DOMNode> ctaViewPair : ctaViews) {
            final View view = getConfiguredView(ctaViewPair);

            if (ctasLayout != null && message.stretchCTAsHorizontally != null && message.stretchCTAsHorizontally) {
                final FlexboxLayout.LayoutParams lp = (FlexboxLayout.LayoutParams) view.getLayoutParams();
                lp.flexGrow = 1;
                lp.flexShrink = 0;
                lp.flexBasisPercent = 1f / ctaViews.size(); // This hack works. Don't really know why. Note that you'll need a different basis if you want separators (0.1 should work)
                view.setLayoutParams(lp); // Is not necessary, but that's an implementation detail, so do it to be sure.
            }

            targetCTALayout.addView(view);
        }
    }

    // Configure a view style and layout options for a pair of view + DOM representation
    private View getConfiguredView(Pair<View, DOMNode> viewPair) {
        final View view = viewPair.first;
        final DOMNode node = viewPair.second;

        if (node != null) {
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
        }
        return view;
    }

    private void setupHeroLayout() {
        final List<Pair<View, DOMNode>> views = new LinkedList<>();

        heroLayout = new FrameLayout(context);
        heroLayout.setId(ViewCompat.generateViewId());
        addView(heroLayout);

        // Video takes priority over image
        if (message.videoURL != null) {
            videoView = new TextureView(context);
            if (surfaceHolderCallback != null) {
                videoView.setSurfaceTextureListener(surfaceHolderCallback);
            }
            views.add(new Pair<>(videoView, new DOMNode("video")));
        } else {
            //TODO: Fix a newly introduced bug where corner radius is fucked up after rotating the device
            heroImageView = new RoundedImageView(context);
            heroImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (!TextUtils.isEmpty(message.heroDescription)) {
                heroImageView.setContentDescription(message.heroDescription);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    heroImageView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
                }
            }
            views.add(new Pair<>(heroImageView, new DOMNode("image", "image")));

            if (heroDownloadResult == null) {
                heroPlaceholder = new View(context);
                final DOMNode placeholderNode = new DOMNode("placeholder");
                views.add(new Pair<>(heroPlaceholder, placeholderNode));

                boolean darkProgressBar = false;
                Map<String, String> rules = getRulesForView(placeholderNode);
                for (Map.Entry<String, String> rule : rules.entrySet()) {
                    if ("loader".equalsIgnoreCase(rule.getKey())) {
                        String ruleValue = rule.getValue();
                        if ("dark".equalsIgnoreCase(ruleValue)) {
                            darkProgressBar = true;
                        } else if ("light".equalsIgnoreCase(ruleValue)) {
                            darkProgressBar = false;
                        }
                    }
                }

                heroLoader = new ProgressBar(context);
                heroLoader.setIndeterminate(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (darkProgressBar) {
                        heroLoader.setIndeterminateTintList(ColorStateList.valueOf(Color.BLACK));
                    } else {
                        heroLoader.setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));
                    }
                }
                final FrameLayout.LayoutParams progressLayoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                progressLayoutParams.gravity = Gravity.CENTER;
                heroLoader.setLayoutParams(progressLayoutParams);
                heroLoader.setVisibility(GONE);
            } else {
                displayHero();
            }
        }

        for (Pair<View, DOMNode> viewPair : views) {
            final Map<String, String> rules = getRulesForView(viewPair.second);
            final View view = viewPair.first;
            view.setLayoutParams(StyleHelper.getFrameLayoutParams(context, null, rules));
            if (view instanceof Styleable) {
                ((Styleable) view).applyStyleRules(rules);
            } else {
                StyleHelper.applyCommonRules(view, rules);
            }

            heroLayout.addView(viewPair.first);
        }

        if (heroLoader != null) {
            heroLayout.addView(heroLoader);
        }
    }

    private void setupCtaLayoutIfNeeded() {
        if (message.attachCTAsBottom != null && message.attachCTAsBottom) {
            ctasStyleRules = getRulesForView(new DOMNode("ctas"));
            ctasLayout = new SeparatedFlexboxLayout(context, "ctas", this);
            ctasLayout.setId(ViewCompat.generateViewId());

            if (message.stackCTAsHorizontally != null && message.stackCTAsHorizontally) {
                ctasLayout.setFlexDirection(FlexboxLayout.FLEX_DIRECTION_ROW);
            } else {
                ctasLayout.setFlexDirection(FlexboxLayout.FLEX_DIRECTION_COLUMN);
            }

            // Since we're in column mode, this is the default h-align
            ctasLayout.setAlignItems(FlexboxLayout.ALIGN_ITEMS_CENTER);
            ctasLayout.setJustifyContent(FlexboxLayout.JUSTIFY_CONTENT_CENTER);
            ctasLayout.applyStyleRules(ctasStyleRules);
            addView(ctasLayout);
        }
    }

    /**
     * Here we setup any layout parameter that can change because of the payload, or on screen rotation
     */
    private void setupVariableLayoutParameters() {
        PercentRelativeLayout.LayoutParams contentLP = StyleHelper.getRelativeLayoutParams(
            context,
            new PercentRelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ),
            contentStyleRules,
            RelativeLayout.CENTER_HORIZONTAL,
            null
        );

        PercentRelativeLayout.LayoutParams ctasLP = null;
        if (ctasLayout != null) {
            ctasLP =
                StyleHelper.getRelativeLayoutParams(
                    context,
                    new PercentRelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ),
                    ctasStyleRules,
                    RelativeLayout.CENTER_HORIZONTAL,
                    null
                );
            ctasLP.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        }

        if (heroLayout != null) {
            PercentRelativeLayout.LayoutParams heroLP;
            if (!landscape) {
                // Configure the split ratio for the right orientation
                heroLP = new PercentRelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
                heroLP.getPercentLayoutInfo().heightPercent =
                    message.heroSplitRatio != null ? message.heroSplitRatio.floatValue() : 0.4f;

                // Set how the split will work
                if (message.flipHeroVertical != null && message.flipHeroVertical) {
                    if (ctasLP != null) {
                        heroLP.addRule(RelativeLayout.ABOVE, ctasLayout.getId());
                    } else {
                        heroLP.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    }
                    contentLP.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                    contentLP.addRule(RelativeLayout.ABOVE, heroLayout.getId());
                } else {
                    if (ctasLP != null) {
                        contentLP.addRule(RelativeLayout.ABOVE, ctasLayout.getId());
                    } else {
                        contentLP.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    }
                    contentLP.addRule(RelativeLayout.BELOW, heroLayout.getId());
                    heroLP.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                }
            } else {
                // Configure the split ratio for the right orientation
                heroLP = new PercentRelativeLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
                heroLP.getPercentLayoutInfo().widthPercent =
                    message.heroSplitRatio != null ? message.heroSplitRatio.floatValue() : 0.4f;

                // Set how the split will work
                if (message.flipHeroHorizontal != null && message.flipHeroHorizontal) {
                    heroLP.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    contentLP.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    contentLP.addRule(RelativeLayout.LEFT_OF, heroLayout.getId());

                    if (ctasLP != null) {
                        ctasLP.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        ctasLP.addRule(RelativeLayout.LEFT_OF, heroLayout.getId());
                    }
                } else {
                    heroLP.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    contentLP.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    contentLP.addRule(RelativeLayout.RIGHT_OF, heroLayout.getId());

                    if (ctasLP != null) {
                        ctasLP.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        ctasLP.addRule(RelativeLayout.RIGHT_OF, heroLayout.getId());
                    }
                }

                if (ctasLP != null) {
                    contentLP.addRule(RelativeLayout.ABOVE, ctasLayout.getId());
                }
            }
            heroLayout.setLayoutParams(heroLP);
        } else if (ctasLayout != null) {
            contentLP.addRule(RelativeLayout.ABOVE, ctasLayout.getId());
        }

        contentLayout.setLayoutParams(contentLP);
        if (ctasLayout != null && ctasLP != null) {
            ctasLayout.setLayoutParams(ctasLP);
        }

        updateLayoutInsets();

        // We need to request a layout so that the layout params are applied correctly
        // Newer platforms request a layout because of something else, add this so if the other bugs get fixed, layouting wont break
        requestLayout();
    }

    private void displayHero() {
        if (heroDownloadResult == null) {
            heroImageView.setImageDrawable(null);
        } else {
            ImageHelper.setDownloadResultInImage(heroImageView, heroDownloadResult);
        }
    }

    public boolean canAutoClose() {
        return !ViewCompat.isTouchExplorationEnabled(context);
    }

    public void startAutoCloseCountdown() {
        final int autoCloseMS = message.autoCloseDelay;
        if (closeButton != null && autoCloseMS > 0) {
            closeButton.animateForDuration(autoCloseMS);
        }
    }

    public void setActionListener(OnActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setSurfaceHolderCallback(TextureView.SurfaceTextureListener listener) {
        if (videoView != null) {
            videoView.setSurfaceTextureListener(listener);
        }

        surfaceHolderCallback = listener;
    }

    public void onHeroBitmapStartsDownloading() {
        heroLoader.setVisibility(VISIBLE);
    }

    public void onHeroDownloaded(AsyncImageDownloadTask.Result result) {
        heroLayout.removeView(heroLoader);
        heroDownloadResult = result;

        displayHero();

        if (result != null && heroLayout != null && heroPlaceholder != null) {
            heroLayout.removeView(heroPlaceholder);
            heroPlaceholder = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        topInset = insets.getSystemWindowInsetTop();
        updateLayoutInsets();
        insets =
            insets.replaceSystemWindowInsets(
                insets.getSystemWindowInsetLeft(),
                0,
                insets.getSystemWindowInsetRight(),
                insets.getSystemWindowInsetBottom()
            );
        return super.onApplyWindowInsets(insets);
    }

    /**
     * Update the content layout's insets to include the window's top inset
     */
    private void updateLayoutInsets() {
        if (contentLayout != null) {
            contentLayout.setPadding(
                contentLayout.getPaddingLeft(),
                shouldApplyWindowInsetToContent() ? originalContentPaddingTop + topInset : originalContentPaddingTop,
                contentLayout.getPaddingRight(),
                contentLayout.getPaddingBottom()
            );
        }

        if (closeButton != null) {
            PercentRelativeLayout.LayoutParams params = (LayoutParams) closeButton.getLayoutParams();
            params.topMargin = originalCloseMarginTop + topInset;
            closeButton.setLayoutParams(params);
        }
    }

    private boolean shouldApplyWindowInsetToContent() {
        // The only case where we want to skip this is when the hero it on top of the content
        return heroLayout == null || (message != null && message.flipHeroVertical == Boolean.TRUE) || landscape;
    }

    /**
     * @return true if we have to wait before handling events
     */
    private boolean mustWaitTapDelay() {
        return SystemClock.uptimeMillis() < drawTimeMillis + TAP_DELAY_MILLIS;
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

    public interface OnActionListener {
        void onCloseAction();

        void onCTAAction(int index, @NonNull CTA cta);
    }
}
