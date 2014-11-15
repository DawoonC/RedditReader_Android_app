package com.example.dawoon.redditreader.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.example.dawoon.redditreader.R;
import com.example.dawoon.redditreader.Utility;
import com.example.dawoon.redditreader.data.RedditContract.PostingEntry;
import com.example.dawoon.redditreader.data.RedditContract.SubredditEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

/**
 * Created by Dawoon on 2014-11-14.
 */
public class RedditSyncAdapter extends AbstractThreadedSyncAdapter {
    private final String LOG_TAG = RedditSyncAdapter.class.getSimpleName();
    // Interval at which to sync with the weather, in milliseconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;

    public RedditSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        String subredditQuery = Utility.getPreferredSubreddit(getContext());

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String listingJsonStr = null;
        Uri builtUri;
        URL url;
        InputStream inputStream;
        StringBuffer buffer;

        String format = ".json";
        String subredditTag = "r";

        // Construct the URL for the Reddit query
        final String REDDIT_BASE_URL =
                "http://www.reddit.com/";

        try {
            builtUri = Uri.parse(REDDIT_BASE_URL).buildUpon()
                    .appendPath(subredditTag)
                    .appendPath(subredditQuery)
                    .appendPath(format)
                    .build();

            url = new URL(builtUri.toString());

            // Create the request to Reddit.com, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            inputStream = urlConnection.getInputStream();
            buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return;
            }
            listingJsonStr = buffer.toString();

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        // These are the names of the JSON objects that need to be extracted.

        // data is a parent of children but also a child of children.
        final String R_P_DATA = "data";
        final String R_CHILDREN = "children";
        final String R_C_DATA = "data";
        final String R_AFTER = "after";
        final String R_POSTING_ID = "name";
        final String R_SUBREDDIT_ID = "subreddit_id";
        final String R_TITLE = "title";
        final String R_AUTHOR = "author";
        final String R_SCORE = "score";
        final String R_PERMALINK = "permalink";
        final String R_CREATED_UTC = "created_utc";
        final String R_NUM_COMMENTS = "num_comments";
        final String R_SR_NAME = "subreddit";
        final String R_THUMBNAIL = "thumbnail";

        try {
            if (listingJsonStr != null) {
                // Deletes existing data in DB before inserting new data.
                getContext().getContentResolver().delete(PostingEntry.CONTENT_URI, null, null);
            }

            JSONObject listingJson = new JSONObject(listingJsonStr);
            JSONObject pDataJson = listingJson.getJSONObject(R_P_DATA);
            JSONArray childrenArray = pDataJson.getJSONArray(R_CHILDREN);
            String afterId = pDataJson.getString(R_AFTER);

            long subredditId = addSubreddit(subredditQuery, afterId);

            // Get and insert the new listing information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(childrenArray.length());

            for(int i = 0; i < childrenArray.length(); i++) {
                // These are the values that will be collected.

                String title;
                String author;
                String permalink;
                String postingId;
                String subredditCode;
                String subredditName;
                String thumbnail;
                int score;
                int numComments;
                double createdUtc;

                // Get the JSON object representing the posting
                JSONObject postingJson = childrenArray.getJSONObject(i);
                JSONObject cDataJson = postingJson.getJSONObject(R_C_DATA);

                title = cDataJson.getString(R_TITLE);
                author = cDataJson.getString(R_AUTHOR);
                permalink = cDataJson.getString(R_PERMALINK);
                postingId = cDataJson.getString(R_POSTING_ID);
                subredditCode = cDataJson.getString(R_SUBREDDIT_ID);
                subredditName = cDataJson.getString(R_SR_NAME);
                thumbnail = cDataJson.getString(R_THUMBNAIL);
                score = cDataJson.getInt(R_SCORE);
                numComments = cDataJson.getInt(R_NUM_COMMENTS);
                createdUtc = cDataJson.getDouble(R_CREATED_UTC);

                ContentValues postingValues = new ContentValues();

                postingValues.put(PostingEntry.COLUMN_SR_KEY, subredditId);
                postingValues.put(PostingEntry.COLUMN_POSTING_ID, postingId);
                postingValues.put(PostingEntry.COLUMN_TITLE, title);
                postingValues.put(PostingEntry.COLUMN_AUTHOR, author);
                postingValues.put(PostingEntry.COLUMN_SCORE, score);
                postingValues.put(PostingEntry.COLUMN_CREATED_UTC, createdUtc);
                postingValues.put(PostingEntry.COLUMN_PERMALINK, permalink);
                postingValues.put(PostingEntry.COLUMN_NUM_COMMENTS, numComments);
                postingValues.put(PostingEntry.COLUMN_SUBREDDIT_CODE, subredditCode);
                postingValues.put(PostingEntry.COLUMN_SUBREDDIT_NAME, subredditName);
                postingValues.put(PostingEntry.COLUMN_THUMBNAIL, thumbnail);

                cVVector.add(postingValues);
            }

            if (cVVector.size() > 0) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                getContext().getContentResolver().bulkInsert(PostingEntry.CONTENT_URI, cvArray);
            }
            Log.d(LOG_TAG, "FetchRedditTask Complete. " + cVVector.size() + " Inserted");

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        return;
    }

    /**
     * Helper method to handle insertion of a new subreddit in the RedditReader database.
     *
     * @param subredditSetting The subreddit string used to request updates from the server.
     * @param afterId Posting ID of the last list item.
     * @return the row ID of the added subreddit.
     */
    private long addSubreddit(String subredditSetting, String afterId) {

        // First, check if the subreddit exists in the db
        Cursor cursor = getContext().getContentResolver().query(
                SubredditEntry.CONTENT_URI,
                new String[]{SubredditEntry._ID},
                SubredditEntry.COLUMN_SUBREDDIT_SETTING + " = ?",
                new String[]{subredditSetting},
                null);

        if (cursor.moveToFirst()) {
            Log.v(LOG_TAG, "Found it in the database!");
            int subredditIdIndex = cursor.getColumnIndex(SubredditEntry._ID);
            return cursor.getLong(subredditIdIndex);
        } else {
            Log.v(LOG_TAG, "Didn't find it in the database, inserting now!");
            ContentValues subredditValues = new ContentValues();
            subredditValues.put(SubredditEntry.COLUMN_SUBREDDIT_SETTING, subredditSetting);
            subredditValues.put(SubredditEntry.COLUMN_AFTER, afterId);

            Uri subredditInsertUri = getContext().getContentResolver()
                    .insert(SubredditEntry.CONTENT_URI, subredditValues);

            return ContentUris.parseId(subredditInsertUri);
        }
    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context An app context
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder()
                    .syncPeriodic(syncInterval, flexTime)
                    .setSyncAdapter(account, authority).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name),
                context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if (accountManager.getPassword(newAccount) == null) {
            // Add the account and account type, no password or user data
            // If successful, return the Account object, otherwise report an error.
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        // Since we've created an account
        RedditSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        // Without calling setSyncAutomatically, out periodic sync will not be enabled.
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        // Finally, let's do a sync to get things started
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }
}
