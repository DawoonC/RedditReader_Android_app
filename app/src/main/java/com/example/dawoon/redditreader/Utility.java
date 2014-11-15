package com.example.dawoon.redditreader;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Calendar;
import java.util.Date;

/**
 * Utility class, contains helper methods for the app.
 */
public class Utility {
    public static String getPreferredSubreddit(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_subreddit_key),
                context.getString(R.string.pref_subreddit_default));
    }

    public static String formatTimeAndAuthor(double time, String author) {
        Calendar postingDate = Calendar.getInstance();
        Calendar currentDate = Calendar.getInstance();
        postingDate.setTime(new Date((long) time * 1000L));
        currentDate.setTime(new Date());

        int pDay = postingDate.get(postingDate.DAY_OF_YEAR);
        int cDay = currentDate.get(currentDate.DAY_OF_YEAR);
        int pHour = postingDate.get(postingDate.HOUR_OF_DAY);
        int cHour = currentDate.get(currentDate.HOUR_OF_DAY);
        int pMinute = postingDate.get(postingDate.MINUTE);
        int cMinute = currentDate.get(currentDate.MINUTE);

        int difference;
        String differenceStr;

        if (cDay - pDay < 1) {
            if (cHour - pHour < 1) {
                difference = cMinute - pMinute;
                differenceStr = Integer.toString(difference) + " minutes ago";
            } else {
                difference = cHour - pHour;
                differenceStr = Integer.toString(difference) + " hours ago";
            }
        } else {
            int dayDiff = cDay - pDay;
            if (dayDiff > 1) {
                differenceStr = Integer.toString(dayDiff) + " days ago";
            } else {
                difference = (cHour + 24) - pHour;
                differenceStr = Integer.toString(difference) + " hours ago";
            }
        }
        String outputFormat = differenceStr + " by " + author;
        return outputFormat;
    }

    public static String formatNumComments(int numComments) {
        String numCommentsStr = Integer.toString(numComments);
        return numCommentsStr + " comments";
    }
}
