package com.example.dawoon.redditreader;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * An adapter for handling view binding.
 */
public class RedditAdapter extends CursorAdapter {

    public RedditAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_reddit, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        int score = cursor.getInt(RedditFragment.COL_SCORE);
        String title = cursor.getString(RedditFragment.COL_TITLE);
        double createdUtc = cursor.getDouble(RedditFragment.COL_CREATED_UTC);
        String author = cursor.getString(RedditFragment.COL_AUTHOR);
        int numComments = cursor.getInt(RedditFragment.COL_NUM_COMMENTS);

        viewHolder.scoreView.setText(Integer.toString(score));
        viewHolder.titleView.setText(title);
        viewHolder.authorView.setText(Utility.formatTimeAndAuthor(createdUtc, author));
        viewHolder.commentsView.setText(Utility.formatNumComments(numComments));
    }

    /**
     * Cache of the children views for a reddit list item.
     */
    public static class ViewHolder {
        public final TextView scoreView;
        public final TextView titleView;
        public final TextView authorView;
        public final TextView commentsView;

        public ViewHolder(View view) {
            scoreView = (TextView) view.findViewById(R.id.list_item_score_textview);
            titleView = (TextView) view.findViewById(R.id.list_item_title_textview);
            authorView = (TextView) view.findViewById(R.id.list_item_author_textview);
            commentsView = (TextView) view.findViewById(R.id.list_item_num_comments_textview);
        }
    }
}
