package com.example.dawoon.redditreader.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.example.dawoon.redditreader.data.RedditContract.PostingEntry;
import com.example.dawoon.redditreader.data.RedditContract.SubredditEntry;

/**
 * Created by Dawoon on 2014-11-14.
 */
public class RedditProvider extends ContentProvider {
    private static final int POSTING = 100;
    private static final int POSTING_WITH_SUBREDDIT = 101;
    private static final int POSTING_WITH_SUBREDDIT_AND_LINK = 102;
    private static final int SUBREDDIT = 300;
    private static final int SUBREDDIT_ID = 301;

    private static final String LOG_TAG = RedditProvider.class.getSimpleName();

    // The URI matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private RedditDbHelper mOpenHelper;
    private static final SQLiteQueryBuilder sPostingBySubredditSettingQueryBuilder;

    static{
        sPostingBySubredditSettingQueryBuilder = new SQLiteQueryBuilder();
        sPostingBySubredditSettingQueryBuilder.setTables(
                PostingEntry.TABLE_NAME + " INNER JOIN " +
                        SubredditEntry.TABLE_NAME +
                        " ON " + PostingEntry.TABLE_NAME +
                        "." + PostingEntry.COLUMN_SR_KEY +
                        " = " + SubredditEntry.TABLE_NAME +
                        "." + SubredditEntry._ID);
    }

    private static final String sSubredditSettingSelection =
            SubredditEntry.TABLE_NAME +
                    "." + SubredditEntry.COLUMN_SUBREDDIT_SETTING + " = ? ";
    private static final String sSubredditSettingWithPermalinkSelection =
            SubredditEntry.TABLE_NAME +
                    "." + SubredditEntry.COLUMN_SUBREDDIT_SETTING + " = ? AND " +
                    PostingEntry.COLUMN_PERMALINK + " = ? ";

    private Cursor getPostingBySubredditSetting(Uri uri, String[] projection, String sortOrder) {
        String subredditSetting = PostingEntry.getSubredditSettingFromUri(uri);

        String[] selectionArgs;
        String selection;

        selection = sSubredditSettingSelection;
        selectionArgs = new String[]{subredditSetting};

        return sPostingBySubredditSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getPostingBySubredditSettingWithPermalink(Uri uri, String[] projection, String sortOrder) {
        String permalink = PostingEntry.getLinkFromUri(uri);
        String subredditSetting = PostingEntry.getSubredditSettingFromUri(uri);

        return sPostingBySubredditSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sSubredditSettingWithPermalinkSelection,
                new String[]{subredditSetting, permalink},
                null,
                null,
                sortOrder
        );
    }

    private static UriMatcher buildUriMatcher() {
        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found. The code passed into the constructor represents the code to return for the root
        // URI. It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = RedditContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, RedditContract.PATH_POSTING, POSTING);
        matcher.addURI(authority, RedditContract.PATH_POSTING + "/*", POSTING_WITH_SUBREDDIT);
        matcher.addURI(authority, RedditContract.PATH_POSTING + "/*/*", POSTING_WITH_SUBREDDIT_AND_LINK);

        matcher.addURI(authority, RedditContract.PATH_SUBREDDIT, SUBREDDIT);
        matcher.addURI(authority, RedditContract.PATH_SUBREDDIT + "/#", SUBREDDIT_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new RedditDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "posting/*/*"
            case POSTING_WITH_SUBREDDIT_AND_LINK: {
                retCursor = getPostingBySubredditSettingWithPermalink(uri, projection, sortOrder);
                break;
            }
            // "posting/*"
            case POSTING_WITH_SUBREDDIT: {
                retCursor = getPostingBySubredditSetting(uri, projection, sortOrder);
                break;
            }
            // "posting"
            case POSTING: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        RedditContract.PostingEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "subreddit/*"
            case SUBREDDIT_ID: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        RedditContract.SubredditEntry.TABLE_NAME,
                        projection,
                        RedditContract.SubredditEntry._ID + " = '" + ContentUris.parseId(uri) + "'",
                        null,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "subreddit"
            case SUBREDDIT: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        RedditContract.SubredditEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);

        return retCursor;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case POSTING_WITH_SUBREDDIT_AND_LINK:
                return PostingEntry.CONTENT_ITEM_TYPE;
            case POSTING_WITH_SUBREDDIT:
                return PostingEntry.CONTENT_TYPE;
            case POSTING:
                return PostingEntry.CONTENT_TYPE;
            case SUBREDDIT_ID:
                return SubredditEntry.CONTENT_ITEM_TYPE;
            case SUBREDDIT:
                return SubredditEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case POSTING: {
                long _id = db.insert(PostingEntry.TABLE_NAME, null, contentValues);
                if (_id > 0)
                    returnUri = PostingEntry.buildPostingUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case SUBREDDIT: {
                long _id = db.insert(SubredditEntry.TABLE_NAME, null, contentValues);
                if (_id > 0)
                    returnUri = SubredditEntry.buildSubredditUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;

        switch (match) {
            case POSTING:
                rowsDeleted = db.delete(PostingEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case SUBREDDIT:
                rowsDeleted = db.delete(SubredditEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (null == selection || 0 != rowsDeleted) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case POSTING:
                rowsUpdated = db.update(PostingEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case SUBREDDIT:
                rowsUpdated = db.update(SubredditEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (0 != rowsUpdated) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int returnCount;

        switch (match) {
            case POSTING:
                db.beginTransaction(); // begin transaction
                returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(PostingEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful(); // commit successful transaction, otherwise it won't work
                } finally {
                    db.endTransaction(); // end transaction when it's done
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values); // default is default bulkInsert method
        }
    }
}
