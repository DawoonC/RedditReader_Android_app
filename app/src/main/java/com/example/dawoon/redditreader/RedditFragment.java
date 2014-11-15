package com.example.dawoon.redditreader;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.dawoon.redditreader.data.RedditContract.PostingEntry;
import com.example.dawoon.redditreader.sync.RedditSyncAdapter;

/**
 * Fragment for MainActivity.
 * This fragment displays reddit post list.
 */
public class RedditFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener {

    private String mSubreddit;
    // Each loader has an ID, it allows a Fragment to have multiple loaders active at once.
    private static final int REDDIT_LOADER = 0;

    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION;
    private SwipeRefreshLayout swipeLayout;

    private static final String SELECTED_KEY = "selected_position";

    // For the reddit view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    private static final String[] REDDIT_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the subreddit & posting tables in the background
            // (both have an _id column)
            PostingEntry.TABLE_NAME + "." + PostingEntry._ID,
            PostingEntry.COLUMN_TITLE,
            PostingEntry.COLUMN_AUTHOR,
            PostingEntry.COLUMN_SCORE,
            PostingEntry.COLUMN_CREATED_UTC,
            PostingEntry.COLUMN_PERMALINK,
            PostingEntry.COLUMN_NUM_COMMENTS,
            PostingEntry.COLUMN_THUMBNAIL
    };

    // These indices are tied to REDDIT_COLUMNS. If REDDIT_COLUMNS changes,
    // these must change.
    public static final int COL_ENTRY_ID = 0;
    public static final int COL_TITLE = 1;
    public static final int COL_AUTHOR = 2;
    public static final int COL_SCORE = 3;
    public static final int COL_CREATED_UTC = 4;
    public static final int COL_PERMALINK = 5;
    public static final int COL_NUM_COMMENTS = 6;
    public static final int COL_THUMBNAIL = 7;

    private RedditAdapter mRedditAdapter;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(String permalink);
    }

    public RedditFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(REDDIT_LOADER, null, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // The RedditAdapter will take data from the database through the
        // Loader and use it to populate the ListView it's attached to.
        mRedditAdapter = new RedditAdapter(getActivity(), null, 0);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        mListView = (ListView) rootView.findViewById(R.id.listview_reddit);
        mListView.setAdapter(mRedditAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                //ForecastAdapter adapter = (ForecastAdapter) adapterView.getAdapter();
                Cursor cursor = mRedditAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    ((Callback) getActivity())
                            .onItemSelected(cursor.getString(COL_PERMALINK));
                }
                mPosition = position;
            }
        });

        // If there's instance state, mine it for useful information.
        // The end-goal here is that the user never knows that turning their device sideways
        // does crazy lifecycle related things. It should feel like some stuff stretched out,
        // actually *lost*.
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            // The listview probably hasn't even been populated yet. Actually perform the
            // swapout in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        swipeLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        return rootView;
    }

    @Override
    public void onRefresh() {
        updateListing();
        swipeLayout.setRefreshing(false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        // When no item is selected, mPosition will be set to Listview.INVALID_POSITION,
        // so check for that before storing.
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    private void updateListing() {
        RedditSyncAdapter.syncImmediately(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mSubreddit != null && !Utility.getPreferredSubreddit(getActivity()).equals(mSubreddit)) {
            getLoaderManager().restartLoader(REDDIT_LOADER, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.
        mSubreddit = Utility.getPreferredSubreddit(getActivity());
        Uri postingForSubredditUri = PostingEntry.buildPostingSubreddit(
                mSubreddit);

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                postingForSubredditUri,
                REDDIT_COLUMNS,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mRedditAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            // If we don't need to restart the loader, and there's a desired position to restore
            // to, do so now.
            mListView.setSelection(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRedditAdapter.swapCursor(null);
    }
}