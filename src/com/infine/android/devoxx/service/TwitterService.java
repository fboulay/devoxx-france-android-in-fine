/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.infine.android.devoxx.service;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.infine.android.devoxx.io.json.RemoteJsonExecutor;
import com.infine.android.devoxx.provider.ScheduleContract;

import java.util.ArrayList;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterService extends IntentService {

	private static final String TAG = "TwitterService";

	public static final String TWITTER_USER_PREFS = "devoxx_twitter_prefs";

	public static final String EXTRA_STATUS_RECEIVER = "com.infine.android.devoxx.twitter.extra.STATUS_RECEIVER";

    private static final String LAST_TWEET_ID = "com.infine.android.devoxx.twitter.extra.last.tweet.id";

    private ContentResolver mResolver;

    private Twitter mTwitter;

    private static final String TWEETS_QUERY = "DevoxxFR";

    public TwitterService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey("************")
                .setOAuthConsumerSecret("****************")
                .setOAuthAccessToken("***************")
                .setOAuthAccessTokenSecret("**************");
        TwitterFactory tf = new TwitterFactory(cb.build());
        mTwitter = tf.getInstance();
		mResolver = getContentResolver();

    }

	@Override
	protected void onHandleIntent(Intent intent) {
		final long startRemote = System.currentTimeMillis();
		triggerRefresh();
		if (Log.isLoggable(TAG, Log.DEBUG)) {
			Log.d(TAG, "remote sync took " + (System.currentTimeMillis() - startRemote) + "ms");
		}

	}

	public void triggerRefresh() {
		try {
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			long lastTweetId = prefs.getLong(LAST_TWEET_ID, 0);
            Query query = new Query(TWEETS_QUERY);
            if (lastTweetId > 0) {
                query.setSinceId(lastTweetId);
			}
            QueryResult result = mTwitter.search(query);
            Log.i("Twitter","nombre de tweets recu:"+result.getTweets().size());
            ArrayList<ContentProviderOperation> databaseOps = new ArrayList<ContentProviderOperation>();
            long maxId = 0;
            for (Status status : result.getTweets()) {
                maxId = Math.max(maxId,status.getId());
                databaseOps.add(buildCPOperation(status));
            }
            // batch update
            if (! databaseOps.isEmpty()) {
                mResolver.applyBatch(ScheduleContract.CONTENT_AUTHORITY, databaseOps);
            }
            if (maxId > 0) {
                prefs.edit().putLong(LAST_TWEET_ID,maxId);
            }

        } catch (TwitterException e) {
			// pas de message c'est qui doit pas avoir de reseau
			// Toast.makeText(this, "Erreur durant la récupération des Tweets",
			// Toast.LENGTH_SHORT).show();
        } catch (RemoteException e) {
            // pas de message
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            // pas de message
            e.printStackTrace();
        }
    }

    private ContentProviderOperation buildCPOperation(Status tweet) {

        final ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(ScheduleContract.Tweets.CONTENT_URI);

        // affecte les valeurs des colonnes
        builder.withValue(ScheduleContract.Tweets.TWEET_ID, tweet.getId());
        builder.withValue(ScheduleContract.Tweets.CREATION_DATE, tweet.getCreatedAt().getTime());
        builder.withValue(ScheduleContract.Tweets.IMAGE_URL, tweet.getUser().getProfileImageURL());
        builder.withValue(ScheduleContract.Tweets.TEXT, tweet.getText());
        builder.withValue(ScheduleContract.Tweets.USER, tweet.getUser().getName());
        builder.withValue(ScheduleContract.Tweets.USER_NAME, tweet.getUser().getScreenName());

        return builder.build();
    }

}
