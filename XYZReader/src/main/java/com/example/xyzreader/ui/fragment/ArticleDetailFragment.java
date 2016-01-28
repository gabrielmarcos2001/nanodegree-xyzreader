package com.example.xyzreader.ui.fragment;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.ui.customViews.DrawInsetsFrameLayout;
import com.example.xyzreader.ui.customViews.ObservableScrollView;
import com.example.xyzreader.ui.activity.ArticleDetailActivity;
import com.example.xyzreader.ui.activity.ArticleListActivity;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    private static final float PARALLAX_FACTOR = 1.25f;

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private ObservableScrollView mScrollView;

    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    private ColorDrawable mStatusBarColorDrawable;

    private int mTopInset;
    private View mPhotoContainerView;
    private View mContentView;
    private ImageView mPhotoView;
    private View mMetaBar;

    private int mScrollY;
    private boolean mIsCard = false;
    private boolean mShouldTriggerAnimation = false;
    private int mStatusBarFullOpacityBottom;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {

        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
                R.dimen.detail_card_top_margin);
        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mDrawInsetsFrameLayout = (DrawInsetsFrameLayout)
                mRootView.findViewById(R.id.draw_insets_frame_layout);

        mDrawInsetsFrameLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {
            @Override
            public void onInsetsChanged(Rect insets) {
                mTopInset = insets.top;
            }
        });

        mScrollView = (ObservableScrollView) mRootView.findViewById(R.id.scrollview);
        mScrollView.setCallbacks(new ObservableScrollView.Callbacks() {
            @Override
            public void onScrollChanged() {
                mScrollY = mScrollView.getScrollY();
                getActivityCast().onUpButtonFloorChanged(mItemId, ArticleDetailFragment.this);
                mPhotoContainerView.setTranslationY((int) (mScrollY - mScrollY / PARALLAX_FACTOR));
                updateStatusBar();
            }
        });

        mContentView = mRootView.findViewById(R.id.article_body);
        mContentView.setVisibility(View.INVISIBLE);
        mMetaBar = mRootView.findViewById(R.id.meta_bar);
        mMetaBar.setVisibility(View.INVISIBLE);

        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mPhotoView.setTransitionName(String.valueOf(mItemId));
        }

        mPhotoContainerView = mRootView.findViewById(R.id.photo_container);

        mStatusBarColorDrawable = new ColorDrawable(0);

        bindViews();
        updateStatusBar();

        return mRootView;
    }

    private void updateStatusBar() {

        int color = 0;

        if (mPhotoView != null && mTopInset != 0 && mScrollY > 0) {

            float f = progress(mScrollY,
                    mStatusBarFullOpacityBottom - mTopInset * 3,
                    mStatusBarFullOpacityBottom - mTopInset);

            color = Color.argb((int) (1 * f),
                    (int) (Color.red(mMutedColor) * 0.9),
                    (int) (Color.green(mMutedColor) * 0.9),
                    (int) (Color.blue(mMutedColor) * 0.9));
        }

        mStatusBarColorDrawable.setColor(color);
        mDrawInsetsFrameLayout.setInsetBackground(mStatusBarColorDrawable);

    }

    static float progress(float v, float min, float max) {
        return constrain((v - min) / (max - min), 0, 1);
    }

    static float constrain(float val, float min, float max) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }

    private void bindViews() {

        if (mRootView == null) {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);

        if (mCursor != null) {

            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);

            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

            bylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <b>"
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</b>"));

            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));

            bodyView.setClickable(true);
            bodyView.setMovementMethod(LinkMovementMethod.getInstance());
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));

            loadImageThumb();

        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A");
            bodyView.setText("N/A");
        }
    }

    /**
     * Loads the item detail low-res thumb image
     */
    private void loadImageThumb() {

        if (mCursor == null) return;
        if (!isAdded()) return;

        // We want to unlock the scheduled animations only from the fragment
        // which is currently visible
        if (mShouldTriggerAnimation) {
            ((ArticleDetailActivity) getActivity()).scheduleStartPostponedTransition(mPhotoView);
        }

        Picasso.with(getActivity())
                .load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                .into(mPhotoView, new Callback() {
                    @Override
                    public void onSuccess() {

                        if (!isAdded()) return;

                        Bitmap bitmap = ((BitmapDrawable) mPhotoView.getDrawable()).getBitmap();

                        if (mShouldTriggerAnimation) {

                            Animation anim = AnimationUtils.loadAnimation(getActivity(),R.anim.show_from_bottom);
                            mContentView.setVisibility(View.VISIBLE);
                            anim.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    Animation anim = AnimationUtils.loadAnimation(getActivity(),R.anim.show_from_bottom);
                                    mMetaBar.setVisibility(View.VISIBLE);
                                    mMetaBar.startAnimation(anim);
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {

                                }
                            });

                            mContentView.startAnimation(anim);

                        } else {
                            mMetaBar.setVisibility(View.VISIBLE);
                            mContentView.setVisibility(View.VISIBLE);
                        }

                        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {

                            public void onGenerated(Palette p) {

                                if (isAdded()) {

                                    int colorFrom = getResources().getColor(R.color.theme_primary_dark);
                                    int colorTo = p.getDarkVibrantColor(getResources().getColor(R.color.theme_primary_dark));

                                    ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                                    colorAnimation.setDuration(300);
                                    colorAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
                                    colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                                        @Override
                                        public void onAnimationUpdate(ValueAnimator animator) {
                                            mRootView.findViewById(R.id.meta_bar).setBackgroundColor((int) animator.getAnimatedValue());
                                        }

                                    });

                                    colorAnimation.start();
                                }
                            }
                        });

                        // Once we have the Thumb loaded and the animation is smooth and awesome
                        // we start loading the full-res image
                        loadImageFullRes();

                    }

                    @Override
                    public void onError() {
                        // Show Empty image
                        mPhotoView.setImageResource(R.drawable.empty_detail);
                    }
                });

    }

    /**
     * Loads the item detail full ress image
     */
    private void loadImageFullRes() {

        if (mCursor == null) return;
        if (!isAdded()) return;

        Picasso.with(getActivity())
                .load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {

                        if (!isAdded()) return;
                        mPhotoView.setImageBitmap(bitmap);

                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {

                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Snackbar.make(mRootView,getString(R.string.error_default),Snackbar.LENGTH_SHORT).show();
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    public int getUpButtonFloor() {
        if (mPhotoContainerView == null || mPhotoView.getHeight() == 0) {
            return Integer.MAX_VALUE;
        }

        // account for parallax
        return mIsCard
                ? (int) mPhotoContainerView.getTranslationY() + mPhotoView.getHeight() - mScrollY
                : mPhotoView.getHeight() - mScrollY;
    }

    public void setmShouldTriggerAnimation(boolean mShouldTriggerAnimation) {
        this.mShouldTriggerAnimation = mShouldTriggerAnimation;
    }

}
