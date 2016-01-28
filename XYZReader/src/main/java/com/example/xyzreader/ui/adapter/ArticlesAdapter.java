package com.example.xyzreader.ui.adapter;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.ui.customViews.DynamicHeightNetworkImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

/**
 * Created by gabrielmarcos on 1/28/16.
 */
public class ArticlesAdapter extends RecyclerView.Adapter<ArticlesAdapter.ViewHolder> {

    public interface ArticlesListener {
        void onItemClicked(View view, int position);
    }

    private ArticlesListener mListener;
    private Cursor mCursor;
    private Context mContext;

    public ArticlesAdapter(Context context, Cursor cursor) {
        mCursor = cursor;
        mContext = context;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view =  ((LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.list_item_article, parent, false);
        final ViewHolder vh = new ViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onItemClicked(vh.thumbnailView,vh.getAdapterPosition());
                }
            }
        });

        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        mCursor.moveToPosition(position);
        holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

        holder.subtitleView.setText(
                DateUtils.getRelativeTimeSpanString(
                        mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString()
                        + " by "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR));

        Picasso.with(mContext)
                .load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                .into(holder.thumbnailView, new Callback() {
                    @Override
                    public void onSuccess() {

                        Bitmap bitmap = ((BitmapDrawable)holder.thumbnailView.getDrawable()).getBitmap();

                        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                            public void onGenerated(Palette p) {

                                ColorDrawable color = (ColorDrawable)holder.background.getBackground();

                                int colorFrom;
                                if (color != null) {
                                    colorFrom = color.getColor();
                                }else {
                                    colorFrom = mContext.getResources().getColor(R.color.theme_primary_dark);
                                }

                                int colorTo = p.getDarkVibrantColor(mContext.getResources().getColor(R.color.theme_primary_dark));

                                ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                                colorAnimation.setDuration(300);
                                colorAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
                                colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                                    @Override
                                    public void onAnimationUpdate(ValueAnimator animator) {
                                        holder.background.setBackgroundColor((int) animator.getAnimatedValue());
                                    }

                                });

                                colorAnimation.start();
                            }
                        });

                    }

                    @Override
                    public void onError() {
                        // Show Empty image
                        holder.thumbnailView.setImageResource(R.drawable.empty_detail);

                    }
                });

        holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    public void setmListener(ArticlesListener mListener) {
        this.mListener = mListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;
        public View background;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
            background = view.findViewById(R.id.background);
        }
    }

}


