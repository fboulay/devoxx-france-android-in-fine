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

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.infine.android.devoxx.R;
import com.infine.android.devoxx.provider.ScheduleContract;
import com.infine.android.devoxx.provider.ScheduleContract.Speakers;
import com.infine.android.devoxx.util.NotifyingAsyncQueryHandler;
import com.infine.android.devoxx.util.UIUtils;

/**
 * A {@link ListFragment} showing a list of sessions.
 */
public class SpeakersFragment extends ListFragment implements NotifyingAsyncQueryHandler.AsyncQueryListener {

	public static final String EXTRA_SCHEDULE_TIME_STRING = "com.infine.android.devoxx.extra.SCHEDULE_TIME_STRING";

	private static final String STATE_CHECKED_POSITION = "checkedPosition";

	private static final String TAG = "SpeakersFragment";

	private Cursor mCursor;
	private CursorAdapter mAdapter;
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

		mHandler.cancelOperation(SpeakerQuery._TOKEN);

		// Load new arguments
		final Intent intent = BaseActivity.fragmentArgumentsToIntent(arguments);
		final Uri speakersUri = intent.getData();
		final int sessionQueryToken;

		if (speakersUri == null) {
			return;
		}

		String whereClause = "";
		if (ScheduleContract.Speakers.isSearchUri(speakersUri)) {
			String speaker = Speakers.getSearchQuery(speakersUri);
			whereClause = Speakers.SPEAKER_NAME + " like '%" + speaker + "%'";

		}
		mAdapter = new SpeakerAdapter(getActivity());
		String[] projection = SpeakerQuery.PROJECTION;
		sessionQueryToken = SpeakerQuery._TOKEN;

		setListAdapter(mAdapter);

		// Start background query to load sessions
		mHandler.startQuery(sessionQueryToken, null, speakersUri, projection, whereClause, null,
				ScheduleContract.Speakers.DEFAULT_SORT);

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		if (savedInstanceState != null) {
			mCheckedPosition = savedInstanceState.getInt(STATE_CHECKED_POSITION, -1);
		}
	}

	/** {@inheritDoc} */
	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		if (getActivity() == null) {
			setEmptyText(getString(R.string.empty_speakers));
			mHasSetEmptyText = true;
			return;
		}
		if (token == SpeakerQuery._TOKEN) {
			if (mCursor != null) {
				// In case cancelOperation() doesn't work and we end up with
				// consecutive calls to this
				// callback.
				getActivity().stopManagingCursor(mCursor);
				mCursor = null;
			}
			this.mCursor = cursor;
			
			if (mCursor != null) {
				setEmptyText(getString(R.string.empty_speakers));
			}
			
			getActivity().startManagingCursor(mCursor);
			mAdapter.changeCursor(mCursor);

		} else {
			Log.i("SpeakersFragment/onQueryComplete", "Query complete, Not Actionable: " + token);
			cursor.close();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		mMessageQueueHandler.post(mRefreshSessionsRunnable);
		getActivity().getContentResolver().registerContentObserver(ScheduleContract.Speakers.CONTENT_URI, true,
				mSessionChangesObserver);
		if (mCursor != null) {
			mCursor.requery();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		mMessageQueueHandler.removeCallbacks(mRefreshSessionsRunnable);
		getActivity().getContentResolver().unregisterContentObserver(mSessionChangesObserver);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_CHECKED_POSITION, mCheckedPosition);
	}

	/** {@inheritDoc} */
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// Launch viewer for specific speaker, passing along any track knowledge
		// that should influence the title-bar.
		final Cursor cursor = (Cursor) mAdapter.getItem(position);
		final String speakerId = cursor.getString(cursor.getColumnIndex(ScheduleContract.Speakers.SPEAKER_ID));
		final Uri speakerUri = ScheduleContract.Speakers.buildSpeakerUri(speakerId);
		final Intent intent = new Intent(Intent.ACTION_VIEW, speakerUri);
		// intent.putExtra(SessionDetailFragment.EXTRA_TRACK, mTrackUri);
		((BaseActivity) getActivity()).openActivityOrFragment(intent);

		getListView().setItemChecked(position, true);
		mCheckedPosition = position;
	}

	public void clearCheckedPosition() {
		if (mCheckedPosition >= 0) {
			getListView().setItemChecked(mCheckedPosition, false);
			mCheckedPosition = -1;
		}
	}

	private class SpeakerAdapter extends CursorAdapter {

		public SpeakerAdapter(Context context) {
			super(context, null);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			((TextView) view.findViewById(R.id.speaker_title)).setText(cursor.getString(SpeakerQuery.SPEAKER_NAME));
			((TextView) view.findViewById(R.id.speaker_subtitle)).setText(cursor
					.getString(SpeakerQuery.SPEAKER_COMPANY));
			final ImageView image = (ImageView) view.findViewById(R.id.image_item_speaker);
			UIUtils.displayImageLazily(context, cursor.getString(SpeakerQuery.SPEAKER_IMAGE_URL), image);

		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return getActivity().getLayoutInflater().inflate(R.layout.list_item_speaker, parent, false);
		}

	}

	private ContentObserver mSessionChangesObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			if (mCursor != null) {
				mCursor.requery();
			}
		}
	};

	private Runnable mRefreshSessionsRunnable = new Runnable() {
		public void run() {
			if (mAdapter != null) {
				// This is used to refresh session title colors.
				mAdapter.notifyDataSetChanged();
			}

			// Check again on the next quarter hour, with some padding to
			// account for network
			// time differences.
			long nextQuarterHour = (SystemClock.uptimeMillis() / 900000 + 1) * 900000 + 5000;
			mMessageQueueHandler.postAtTime(mRefreshSessionsRunnable, nextQuarterHour);
		}
	};

	private interface SpeakerQuery {
		int _TOKEN = 0x5;

		String[] PROJECTION = { BaseColumns._ID, ScheduleContract.Speakers.SPEAKER_ID,
				ScheduleContract.Speakers.SPEAKER_NAME, ScheduleContract.Speakers.SPEAKER_COMPANY,
				ScheduleContract.Speakers.SPEAKER_IMAGE_URL, };

		int _ID = 0;
		int SPEAKER_ID = 1;
		int SPEAKER_NAME = 2;
		int SPEAKER_COMPANY = 3;
		int SPEAKER_IMAGE_URL = 4;
	}

}
