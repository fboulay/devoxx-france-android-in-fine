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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import com.infine.android.devoxx.R;
import com.infine.android.devoxx.provider.ScheduleContract;
import com.infine.android.devoxx.provider.ScheduleContract.Sessions;
import com.infine.android.devoxx.util.AnalyticsUtils;
import com.infine.android.devoxx.util.NotifyingAsyncQueryHandler;
import com.infine.android.devoxx.util.UIUtils;

/**
 * A fragment that shows detail information for a speaker, including speaker bio
 * and photo
 */
public class SpeakerDetailFragment extends Fragment implements NotifyingAsyncQueryHandler.AsyncQueryListener {

	private static final String TAG = "SpeakerDetailFragment";

	private static final String TAG_BIO = "bio";
	private static final String TAG_SESSIONS = "sessions";

	private Uri mSpeakerUri;

	private String mTitleString;

	private ViewGroup mRootView;
	private TabHost mTabHost;
	private TextView mName;
	private TextView mCompany;
	private ImageView mPicture;
	private TextView mBio;

	private String mSessionList;

	private NotifyingAsyncQueryHandler mHandler;

	private boolean mSpeakerCursor = false;
	private boolean mSpeakerSessionCursor = false;
	private boolean mHasSpeakerBio = false;

	private CursorAdapter mAdapter;
	private ListView mListView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());
		mSpeakerUri = intent.getData();

		if (mSpeakerUri == null) {
			return;
		}

		mAdapter = new SessionsForSpeakerAdapter(getActivity());
		// setHasOptionsMenu(true);
		setHasOptionsMenu(false);
	}

	@Override
	public void onResume() {
		super.onResume();
		queryForSessions();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (mSpeakerUri == null) {
			return;
		}

		mHandler = new NotifyingAsyncQueryHandler(getActivity().getContentResolver(), this);
		mHandler.startQuery(SpeakerQuery._TOKEN, mSpeakerUri, SpeakerQuery.PROJECTION);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_speaker_detail, null);
		mTabHost = (TabHost) mRootView.findViewById(android.R.id.tabhost);
		mTabHost.setup();

		mName = (TextView) mRootView.findViewById(R.id.speaker_title);
		mCompany = (TextView) mRootView.findViewById(R.id.speaker_subtitle);
		mPicture = (ImageView) mRootView.findViewById(R.id.image_speaker);

		mBio = (TextView) mRootView.findViewById(R.id.speaker_bio);

		setupBioTab(inflater);
		// setupSessionsTab();

		return mRootView;
	}

	/**
	 * Build and add "bio" tab.
	 */
	private void setupBioTab(LayoutInflater inflater) {
		mTabHost.addTab(mTabHost.newTabSpec(TAG_BIO).setIndicator(buildIndicator(R.string.tab_speaker_bio))
				.setContent(R.id.tab_speaker_bio));
	}

	/**
	 * Build and add "sessions" tab.
	 */
	private void setupSessionsTab() {
		mTabHost.addTab(mTabHost.newTabSpec(TAG_SESSIONS).setIndicator(buildIndicator(R.string.tab_speaker_session))
				.setContent(R.id.tab_speaker_sessions));
		mListView = (ListView) mRootView.findViewById(R.id.tab_speaker_sessions);
		mListView.setClickable(true);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> list, View view, int position, long id) {
				// Launch viewer for specific session
				final Cursor cursor = (Cursor) mAdapter.getItem(position);
				final String sessionId = cursor.getString(SpeakerSessionQuery.SESSION_ID);
				final Uri sessionUri = ScheduleContract.Sessions.buildSessionUri(sessionId);
				final Intent intent = new Intent(Intent.ACTION_VIEW, sessionUri);
				// intent.putExtra(SessionDetailFragment.EXTRA_TRACK,
				// mTrackUri);
				((BaseActivity) getActivity()).openActivityOrFragment(intent);

				// view.setItemChecked(position, true);
				// mCheckedPosition = position;

			}
		});

	}

	/**
	 * Build a {@link View} to be used as a tab indicator, setting the requested
	 * string resource as its label.
	 * 
	 * @param textRes
	 * @return View
	 */
	private View buildIndicator(int textRes) {
		final TextView indicator = (TextView) getActivity().getLayoutInflater().inflate(R.layout.tab_indicator,
				(ViewGroup) mRootView.findViewById(android.R.id.tabs), false);
		indicator.setText(textRes);
		return indicator;
	}

	/**
	 * {@inheritDoc}
	 */
	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (token == SpeakerQuery._TOKEN) {
			onSpeakerQueryComplete(cursor);
		} else if (token == SpeakerSessionQuery._TOKEN) {
			onSpeakerSessionQueryComplete(cursor);
		} else {
			Log.i("SpeakerDetailFragment/onQueryComplete", "Query complete, Not Actionable: " + token);
			cursor.close();
		}
	}

	/**
	 * Handle {@link SessionsQuery} {@link Cursor}.
	 */
	private void onSpeakerQueryComplete(Cursor cursor) {
		try {
			mSpeakerCursor = true;
			if (!cursor.moveToFirst()) {
				return;
			}

			setupSessionsTab();
			// execute 2nd request
			mSessionList = cursor.getString(SpeakerQuery.SPEAKER_SESSIONS);
			queryForSessions();

			mTitleString = cursor.getString(SpeakerQuery.SPEAKER_NAME);
			mName.setText(mTitleString);
			mCompany.setText(cursor.getString(SpeakerQuery.SPEAKER_COMPANY));

			UIUtils.displayImageLazily(getActivity(), cursor.getString(SpeakerQuery.SPEAKER_IMAGE_URL), mPicture);

			final String speakerBio = cursor.getString(SpeakerQuery.SPEAKER_ABSTRACT);
			if (!TextUtils.isEmpty(speakerBio)) {
				UIUtils.setTextMaybeHtml(mBio, speakerBio);
				mBio.setVisibility(View.VISIBLE);
				mHasSpeakerBio = true;
			} else {
				mBio.setVisibility(View.GONE);
			}

			// Show empty message when all data is loaded, and nothing to show
			if (mSpeakerCursor && !mHasSpeakerBio) {
				mRootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
			}

			AnalyticsUtils.getInstance(getActivity()).trackPageView("/Speaker/" + mTitleString);

		} finally {
			cursor.close();
		}
	}

	private void onSpeakerSessionQueryComplete(Cursor cursor) {
		if (!cursor.moveToFirst()) {
			return;
		}
		mSpeakerSessionCursor = true;
		mAdapter.changeCursor(cursor);

	}

	private void queryForSessions() {
		mHandler.startQuery(SpeakerSessionQuery._TOKEN, Sessions.CONTENT_URI, SpeakerSessionQuery.PROJECTION,
				Sessions.SESSION_ID + " in (" + mSessionList + ")");
	}

	/*
	 * Event structure: Category -> "Session Details" Action -> "Create Note",
	 * "View Note", etc Label -> Session's Title Value -> 0.
	 */
	public void fireSpeakerEvent(int actionId) {
		AnalyticsUtils.getInstance(getActivity()).trackEvent("Speaker Details", getActivity().getString(actionId),
				mTitleString, 0);
	}

	private interface SpeakerQuery {
		int _TOKEN = 0x5;

		String[] PROJECTION = { BaseColumns._ID, ScheduleContract.Speakers.SPEAKER_ID,
				ScheduleContract.Speakers.SPEAKER_NAME, ScheduleContract.Speakers.SPEAKER_COMPANY,
				ScheduleContract.Speakers.SPEAKER_IMAGE_URL, ScheduleContract.Speakers.SPEAKER_ABSTRACT,
				ScheduleContract.Speakers.SPEAKER_SESSIONS };

		int _ID = 0;
		int SPEAKER_ID = 1;
		int SPEAKER_NAME = 2;
		int SPEAKER_COMPANY = 3;
		int SPEAKER_IMAGE_URL = 4;
		int SPEAKER_ABSTRACT = 5;
		int SPEAKER_SESSIONS = 6;
	}

	private interface SpeakerSessionQuery {
		int _TOKEN = 0x6;

		String[] PROJECTION = { BaseColumns._ID, ScheduleContract.Sessions.SESSION_ID,
				ScheduleContract.Sessions.SESSION_TITLE, ScheduleContract.Sessions.SESSION_THEME,
				ScheduleContract.Sessions.SESSION_STARRED};

		int _ID = 0;
		int SESSION_ID = 1;
		int SESSION_TITLE = 2;
		int SESSION_THEME = 3;
		int SESSION_STARRED = 4;
	}

	private class SessionsForSpeakerAdapter extends CursorAdapter {
		public SessionsForSpeakerAdapter(Context context) {
			super(context, null);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			((TextView) view.findViewById(R.id.session_title)).setText(cursor
					.getString(SpeakerSessionQuery.SESSION_TITLE));
			((TextView) view.findViewById(R.id.session_subtitle)).setText(cursor
					.getString(SpeakerSessionQuery.SESSION_THEME));
			((TextView) view.findViewById(R.id.session_subtitle_tag)).setText(cursor
					.getString(SpeakerSessionQuery.SESSION_THEME));
			
			final boolean starred = cursor.getInt(SpeakerSessionQuery.SESSION_STARRED) != 0;
			view.findViewById(R.id.star_button).setVisibility(starred ? View.VISIBLE : View.INVISIBLE);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return getActivity().getLayoutInflater().inflate(R.layout.list_item_session, parent, false);

		}
	}
}
