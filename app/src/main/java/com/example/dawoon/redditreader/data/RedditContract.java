package com.example.dawoon.redditreader.data;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Dawoon on 2014-11-14.
 */
public class RedditContract {
    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website. A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "com.example.dawoon.redditreader";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths (appended to base content URI for possible URI's)
    public static final String PATH_POSTING = "posting";
    public static final String PATH_NEWPOSTING = "newposting";
    public static final String PATH_SUBREDDIT = "subreddit";

    /* Inner class that defines the table contents of the posting table */
    public static final class PostingEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_POSTING).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_POSTING;

        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_POSTING;

        public static final String TABLE_NAME = "posting";

        // Column with the foreign key into the subreddit table
        public static final String COLUMN_SR_KEY = "subreddit_id";
        // Posting ID
        public static final String COLUMN_POSTING_ID = "posting_id";
        // Title of the posting
        public static final String COLUMN_TITLE = "title";
        // Author of the posting
        public static final String COLUMN_AUTHOR = "author";
        // Score of the posting
        public static final String COLUMN_SCORE = "score";
        // Created time of the posting in UTC
        public static final String COLUMN_CREATED_UTC = "created_utc";
        // Permalink of the posting
        public static final String COLUMN_PERMALINK = "permalink";
        // Number of comments on the posting
        public static final String COLUMN_NUM_COMMENTS = "num_comments";
        // Subreddit ID of the posting
        public static final String COLUMN_SUBREDDIT_CODE = "subreddit_code";
        // Human readable subreddit name for the posting
        public static final String COLUMN_SUBREDDIT_NAME = "subreddit_name";
        // Thumbnail of the posting
        public static final String COLUMN_THUMBNAIL = "thumbnail";

        public static Uri buildPostingUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildPostingSubreddit(String subredditSetting) {
            return CONTENT_URI.buildUpon().appendPath(subredditSetting).build();
        }

        public static Uri buildPostingSubredditWithLink(String subredditSetting, String permalink) {
            return CONTENT_URI.buildUpon().appendPath(subredditSetting).appendPath(permalink).build();
        }

        public static String getSubredditSettingFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String getLinkFromUri(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }


    /* Inner class that defines the table contents of the subreddit table */
    public static final class SubredditEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SUBREDDIT).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_SUBREDDIT;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_SUBREDDIT;

        // Table name
        public static final String TABLE_NAME = "subreddit";

        // The subreddit setting string is what will be sent to Reddit
        // as the subreddit query.
        public static final String COLUMN_SUBREDDIT_SETTING = "subreddit_setting";

        // Posting ID of the last list item.
        public static final String COLUMN_AFTER = "after";

        public static Uri buildSubredditUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }
}
