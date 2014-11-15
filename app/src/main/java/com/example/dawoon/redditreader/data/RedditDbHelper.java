package com.example.dawoon.redditreader.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.dawoon.redditreader.data.RedditContract.PostingEntry;
import com.example.dawoon.redditreader.data.RedditContract.SubredditEntry;

/**
 * Created by Dawoon on 2014-11-14.
 */
public class RedditDbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "reddit.db";

    public RedditDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        final String SQL_CREATE_SUBREDDIT_TABLE = "CREATE TABLE " + SubredditEntry.TABLE_NAME + " (" +
                SubredditEntry._ID + " INTEGER PRIMARY KEY, " +
                SubredditEntry.COLUMN_SUBREDDIT_SETTING + " TEXT UNIQUE NOT NULL, " +
                SubredditEntry.COLUMN_AFTER + " TEXT NOT NULL, " +
                "UNIQUE (" + SubredditEntry.COLUMN_SUBREDDIT_SETTING + ") ON CONFLICT IGNORE" +
                " );";


        final String SQL_CREATE_POSTING_TABLE = "CREATE TABLE " + PostingEntry.TABLE_NAME + " (" +
                PostingEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                // the ID of the subreddit entry associated with this posting data
                PostingEntry.COLUMN_SR_KEY + " INTEGER NOT NULL, " +
                PostingEntry.COLUMN_POSTING_ID + " TEXT NOT NULL, " +
                PostingEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
                PostingEntry.COLUMN_AUTHOR + " TEXT NOT NULL," +

                PostingEntry.COLUMN_SCORE + " INTEGER NOT NULL, " +
                PostingEntry.COLUMN_CREATED_UTC + " REAL NOT NULL, " +

                PostingEntry.COLUMN_PERMALINK + " TEXT NOT NULL, " +
                PostingEntry.COLUMN_NUM_COMMENTS + " INTEGER NOT NULL, " +
                PostingEntry.COLUMN_SUBREDDIT_CODE + " TEXT NOT NULL, " +
                PostingEntry.COLUMN_SUBREDDIT_NAME + " TEXT NOT NULL, " +
                PostingEntry.COLUMN_THUMBNAIL + " TEXT NOT NULL, " +

                // Set up the subreddit column as a foreign key to posting table.
                " FOREIGN KEY (" + PostingEntry.COLUMN_SR_KEY + ") REFERENCES " +
                SubredditEntry.TABLE_NAME + " (" + SubredditEntry._ID + "), " +

                // To assure the application have just one weather entry per day
                // per location, it's created a UNIQUE constraint with REPLACE strategy
                " UNIQUE (" + PostingEntry.COLUMN_POSTING_ID + ", " +
                PostingEntry.COLUMN_SR_KEY + ") ON CONFLICT REPLACE);";

        sqLiteDatabase.execSQL(SQL_CREATE_SUBREDDIT_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_POSTING_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SubredditEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PostingEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}