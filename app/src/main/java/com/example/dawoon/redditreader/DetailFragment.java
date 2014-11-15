package com.example.dawoon.redditreader;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.dawoon.redditreader.data.RedditContract.PostingEntry;

/**
 * Fragment for DetailActivity. This fragment displays WebView called from MainActivity.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int DETAIL_LOADER = 0;
    public static final String SUBREDDIT_KEY = "subreddit";

    private String mSubreddit;

    public static WebView mWebView;

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mSubreddit = savedInstanceState.getString(SUBREDDIT_KEY);
        }
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(DetailActivity.LINK_KEY)) {
            getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(DetailActivity.LINK_KEY) &&
                mSubreddit != null &&
                !mSubreddit.equals(Utility.getPreferredSubreddit(getActivity()))) {
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        mWebView = (WebView) rootView.findViewById(R.id.webview_reddit);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // I decided not to use enabling zoom or initial zoom out mode.
//        webSettings.setUseWideViewPort(true);
//        webSettings.setLoadWithOverviewMode(true);
//        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
//        webSettings.setBuiltInZoomControls(true);
        mWebView.setWebViewClient(new WebViewClient());

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(SUBREDDIT_KEY, mSubreddit);
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new loader needs to be created. This
        // fragment only uses one loader, so we don't care about checking the id.
        String[] columns = {
                PostingEntry.TABLE_NAME + "." + PostingEntry._ID,
                PostingEntry.COLUMN_PERMALINK
        };

        String permalinkString = getArguments().getString(DetailActivity.LINK_KEY);

        mSubreddit = Utility.getPreferredSubreddit(getActivity());
        Uri postingUri = PostingEntry.buildPostingSubredditWithLink(mSubreddit, permalinkString);

        return new CursorLoader(
                getActivity(),
                postingUri,
                columns,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            String permalink = data.getString(
                        data.getColumnIndex(PostingEntry.COLUMN_PERMALINK));
            mWebView.loadUrl("http://www.reddit.com" + permalink);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
