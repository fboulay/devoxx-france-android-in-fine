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

package com.infine.android.devoxx.ui;

import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.infine.android.devoxx.R;
import com.infine.android.devoxx.provider.ScheduleContract;
import com.infine.android.devoxx.provider.query.TweetsQuery;
import com.infine.android.devoxx.ui.adapter.TwitterCursorAdapter;
import com.infine.android.devoxx.util.NotifyingAsyncQueryHandler;

/**
 * A {@link ListFragment} showing a list of sessions.
 */
public class TwitterFragment2 extends ListFragment implements NotifyingAsyncQueryHandler.AsyncQueryListener {

	public static final String EXTRA_SCHEDULE_TIME_STRING = "com.infine.android.devoxx.extra.SCHEDULE_TIME_STRING";

	private static final String STATE_CHECKED_POSITION = "checkedPosition";

	private static final int REFRESH_INTERVAL_MINUTES = 5;

	private static final String TAG = "TwitterFragment2";

	private Cursor mCursor;
	private TwitterCursorAdapter mAdapter;
	private int mCheckedPosition = -1;
	private boolean mHasSetEmptyText = false;

	private NotifyingAsyncQueryHandler mHandler;

	private Handler mMessageQueueHandler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHandler = new NotifyingAsyncQueryHandler(getActivity().getContentResolver(), this);
		reloadFromArguments(getArguments());

	}

	public void reloadFromArguments(Bundle arguments) {
		// Teardown from previous arguments
		if (mCursor != null) {
			getActivity().stopManagingCursor(mCursor);
			mCursor = null;
		}

		mCheckedPosition = -1;
		setListAdapter(null);

		mHandler.cancelOperation(TweetsQuery._TOKEN);

		// Load new arguments
		// final Intent intent =
		// BaseActivity.fragmentArgumentsToIntent(arguments);
		// final Uri tweetsUri = intent.getData();
		final Uri tweetsUri = ScheduleContract.Tweets.CONTENT_URI;
		final int tweetQueryToken = TweetsQuery._TOKEN;

		String[] projection = TweetsQuery.PROJECTION;
		mAdapter = new TwitterCursorAdapter(getActivity());

		setListAdapter(mAdapter);

		// Start background query to load sessions
		mHandler.startQuery(tweetQueryToken, null, tweetsUri, projection, null, null,
				ScheduleContract.Tweets.DEFAULT_SORT);

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		getListView().setClickable(false);
		getListView().setOnItemClickListener(null);

		if (savedInstanceState != null) {
			mCheckedPosition = savedInstanceState.getInt(STATE_CHECKED_POSITION, -1);
		}

		if (!mHasSetEmptyText) {
			// Could be a bug, but calling this twice makes it become visible
			// when it shouldn't
			// be visible.
			setEmptyText(getString(R.string.empty_tweets));
			mHasSetEmptyText = true;
		}
	}

	/** {@inheritDoc} */
	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (token == TweetsQuery._TOKEN) {
			onTweetsQueryComplete(cursor);
		} else {
			if (Log.isLoggable(TAG, Log.DEBUG)) {
				Log.d("SessionsFragment/onQueryComplete", "Query complete, Not Actionable: " + token);
			}
			cursor.close();
		}
	}

	/**
	 * Handle {@link SessionsQuery} {@link Cursor}.
	 */
	private void onTweetsQueryComplete(Cursor cursor) {
		if (mCursor != null) {
			// In case cancelOperation() doesn't work and we end up with
			// consecutive calls to this
			// callback.
			getActivity().stopManagingCursor(mCursor);
			mCursor = null;
		}

		mCursor = cursor;
		getActivity().startManagingCursor(mCursor);
		mAdapter.changeCursor(mCursor);
		// if (mCheckedPosition >= 0 && getView() != null) {
		// getListView().setItemChecked(mCheckedPosition, true);
		// }
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// nothing for now
		// super.onListItemClick(l, v, position, id);
	}

	@Override
	public void onResume() {
		super.onResume();
		mMessageQueueHandler.post(mRefreshTweetsRunnable);
		getActivity().getContentResolver().registerContentObserver(ScheduleContract.Tweets.CONTENT_URI, true,
				mTweetChangesObserver);
		if (mCursor != null) {
			mCursor.requery();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		mMessageQueueHandler.removeCallbacks(mRefreshTweetsRunnable);
		getActivity().getContentResolver().unregisterContentObserver(mTweetChangesObserver);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_CHECKED_POSITION, mCheckedPosition);
	}

	private ContentObserver mTweetChangesObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			if (mCursor != null) {
				mCursor.requery();
			}
		}
	};

	private Runnable mRefreshTweetsRunnable = new Runnable() {
		public void run() {
			if (mAdapter != null) {
				mAdapter.notifyDataSetChanged();
			}

			// Check again on the next quarter hour, with some padding to
			// account for network
			// time differences.
			long nextQuarterHour = (SystemClock.uptimeMillis() / 900000 + 1) * 900000 + 5000;
			mMessageQueueHandler.postAtTime(mRefreshTweetsRunnable, nextQuarterHour);
		}
	};

}
