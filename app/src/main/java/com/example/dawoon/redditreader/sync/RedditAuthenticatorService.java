package com.example.dawoon.redditreader.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by Dawoon on 2014-11-14.
 */
public class RedditAuthenticatorService extends Service {
    // Instance field that stores the authenticator object
    private RedditAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new RedditAuthenticator(this);
    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
