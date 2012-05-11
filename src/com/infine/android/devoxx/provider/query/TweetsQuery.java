package com.infine.android.devoxx.provider.query;

import android.provider.BaseColumns;

import com.infine.android.devoxx.provider.ScheduleContract;

public interface TweetsQuery {
	
    int _TOKEN = 0x1;

    String[] PROJECTION = {
            BaseColumns._ID,
            ScheduleContract.Tweets.TWEET_ID,
            ScheduleContract.Tweets.CREATION_DATE,
            ScheduleContract.Tweets.TEXT,
            ScheduleContract.Tweets.IMAGE_URL,
            ScheduleContract.Tweets.USER,
            ScheduleContract.Tweets.USER_NAME,
    };

    int _ID = 0;
    int TWEET_ID = 1;
    int CREATION_DATE = 2;
    int TEXT = 3;
    int IMAGE_URL = 4;
    int USER = 5;
    int USER_NAME = 6;
}
