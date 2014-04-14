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
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.infine.android.devoxx.io.json.JsonHandler.HandlerException;
import com.infine.android.devoxx.io.json.JsonTwitterHandler;
import com.infine.android.devoxx.io.json.RemoteJsonExecutor;

import org.apache.http.client.HttpClient;

public class TwitterService extends IntentService {

	private static final String TAG = "TwitterService";

	public static final String TWITTER_USER_PREFS = "devoxx_twitter_prefs";

	public static final String EXTRA_STATUS_RECEIVER = "com.infine.android.devoxx.twitter.extra.STATUS_RECEIVER";

	private RemoteJsonExecutor mExecutor;

	private static final String TWITTER_SEARCH_API_URL = "https://api.twitter.com/1.1/search/tweets.json";
	private static final String DEFAULT_QUERY = TWITTER_SEARCH_API_URL + "?q=devoxxfr";

	public TwitterService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		final HttpClient httpClient = RestService.getHttpClient(this);
		final ContentResolver resolver = getContentResolver();
		mExecutor = new RemoteJsonExecutor(httpClient, resolver);

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
			String refreshUrl = prefs.getString(JsonTwitterHandler.NEXT_REFRESH_URL, null);
			if (refreshUrl == null) {
				mExecutor.executeGet(DEFAULT_QUERY, new JsonTwitterHandler(getApplicationContext()));
			} else {
				mExecutor.executeGet(TWITTER_SEARCH_API_URL + refreshUrl, new JsonTwitterHandler(
						getApplicationContext()));
			}
		} catch (HandlerException e) {
			// pas de message c'est qui doit pas avoir de reseau
			// Toast.makeText(this, "Erreur durant la récupération des Tweets",
			// Toast.LENGTH_SHORT).show();
		}
	}

}
