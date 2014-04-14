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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import com.infine.android.devoxx.R;
import com.infine.android.devoxx.provider.ScheduleContract;
import com.infine.android.devoxx.provider.ScheduleContract.Speakers;
import com.infine.android.devoxx.ui.phone.MapActivity;
import com.infine.android.devoxx.util.FractionalTouchDelegate;
import com.infine.android.devoxx.util.NotifyingAsyncQueryHandler;
import com.infine.android.devoxx.util.UIUtils;

/**
 * A fragment that shows detail information for a session, including session
 * title, abstract, time information, speaker photos and bios, etc.
 */
public class SessionDetailFragment extends Fragment implements NotifyingAsyncQueryHandler.AsyncQueryListener,
		CompoundButton.OnCheckedChangeListener {

	private static final String TAG = "SessionDetailFragment";

	/**
	 * Since sessions can belong tracks, the parent activity can send this extra
	 * specifying a track URI that should be used for coloring the title-bar.
	 */
	public static final String EXTRA_TRACK = "com.infine.android.devoxx.extra.TRACK";

	private static final String TAG_SUMMARY = "summary";
	private static final String TAG_SPEAKERS = "speakers";

	private String mSessionId;
	private Uri mSessionUri;

	private String mTitleString;
	private String mRoomId;

	private ViewGroup mRootView;
	private TabHost mTabHost;
	private TextView mTitle;
	private TextView mSubtitle;
	private TextView mExperience;
	private TextView mSponsored;
	private CompoundButton mStarred;

	private TextView mSummary;

	private String mSpeakerList;

	private NotifyingAsyncQueryHandler mHandler;

	private boolean mSessionCursor = false;
	private boolean mSpeakersCursor = false;
	private boolean mHasSummaryContent = false;

	private CursorAdapter mAdapter;
	private ListView mListView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());
		mSessionUri = intent.getData();

		if (mSessionUri == null) {
			return;
		}

		mSessionId = ScheduleContract.Sessions.getSessionId(mSessionUri);
		mAdapter = new SpeakerAdapter(getActivity());
		setHasOptionsMenu(false);
	}

	@Override
	public void onResume() {
		super.onResume();
		queryForSpeakers();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (mSessionUri == null) {
			return;
		}

		// Start background queries to load session and track details
		final Uri speakersUri = ScheduleContract.Sessions.buildSpeakersDirUri(mSessionId);

		mHandler = new NotifyingAsyncQueryHandler(getActivity().getContentResolver(), this);
		mHandler.startQuery(SessionsQuery._TOKEN, mSessionUri, SessionsQuery.PROJECTION);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_session_detail, null);
		mTabHost = (TabHost) mRootView.findViewById(android.R.id.tabhost);
		mTabHost.setup();

		mTitle = (TextView) mRootView.findViewById(R.id.session_title);
		mSubtitle = (TextView) mRootView.findViewById(R.id.session_subtitle);
		mSponsored = (TextView) mRootView.findViewById(R.id.session_sponsored);
		mStarred = (CompoundButton) mRootView.findViewById(R.id.star_button);

		mStarred.setFocusable(true);
		mStarred.setClickable(true);

		// Larger target triggers star toggle
		final View starParent = mRootView.findViewById(R.id.header_session);
		FractionalTouchDelegate.setupDelegate(starParent, mStarred, new RectF(0.6f, 0f, 1f, 0.8f));

		mSummary = (TextView) mRootView.findViewById(R.id.session_abstract);

		setupSummaryTab();

		return mRootView;
	}

	/**
	 * Build and add "summary" tab.
	 */
	private void setupSummaryTab() {
		// Summary content comes from existing layout
		mTabHost.addTab(mTabHost.newTabSpec(TAG_SUMMARY).setIndicator(buildIndicator(R.string.session_summary))
				.setContent(R.id.tab_session_summary));
	}

	/**
	 * Build and add "summary" tab.
	 */
	private void setupSpeakersTab() {
		// Summary content comes from existing layout
		mTabHost.addTab(mTabHost.newTabSpec(TAG_SPEAKERS).setIndicator(buildIndicator(R.string.session_speakers))
				.setContent(R.id.tab_session_speakers));
		mListView = (ListView) mRootView.findViewById(R.id.tab_session_speakers);
		mListView.setClickable(true);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> list, View view, int position, long id) {
				// Launch viewer for specific session
				final Cursor cursor = (Cursor) mAdapter.getItem(position);
				final String speakerId = cursor.getString(SpeakersQuery.SPEAKER_ID);
				final Uri speakerUri = ScheduleContract.Speakers.buildSpeakerUri(speakerId);
				final Intent intent = new Intent(Intent.ACTION_VIEW, speakerUri);
				((BaseActivity) getActivity()).openActivityOrFragment(intent);
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

		if (token == SessionsQuery._TOKEN) {
			onSessionQueryComplete(cursor);
		} else if (token == SpeakersQuery._TOKEN) {
			onSpeakersQueryComplete(cursor);
		} else {
			Log.i("SessionDetailFragment/onQueryComplete", "Query complete, Not Actionable: " + token);
			cursor.close();
		}
	}

	/**
	 * Handle {@link SessionsQuery} {@link Cursor}.
	 */
	private void onSessionQueryComplete(Cursor cursor) {
		try {
			mSessionCursor = true;
			if (!cursor.moveToFirst()) {
				return;
			}

			setupSpeakersTab();
			// execute 2nd request
			mSpeakerList = cursor.getString(SessionsQuery.SESSION_SPEAKERS);
			queryForSpeakers();

			// Format time block this session occupies
			final long blockStart = cursor.getLong(SessionsQuery.BLOCK_START);
			final long blockEnd = cursor.getLong(SessionsQuery.BLOCK_END);
			final String roomName = cursor.getString(SessionsQuery.ROOM_NAME);
			final Spanned subtitle = UIUtils.formatSessionSubtitle(blockStart, blockEnd, roomName, getActivity());

			mTitleString = cursor.getString(SessionsQuery.TITLE);
			mTitle.setText(mTitleString);
			mSubtitle.setText(subtitle);

			mSubtitle.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					Intent intent = new Intent(getActivity(), MapActivity.class);
					intent.putExtra(MapHotelFragment.EXTRA_ROOM, MapHotelFragment.roomToFloor(roomName));
					startActivity(intent);
				}
			});

//			String exp = cursor.getString(SessionsQuery.EXPERIENCE);
//			int resource = getResources().getIdentifier("session_exp_" + exp, "id",
//					getActivity().getApplicationContext().getPackageName());
//			mExperience = (TextView) mRootView.findViewById(resource);
//			mExperience.setVisibility(View.VISIBLE);
//			mExperience.setText(exp);

			boolean isSponsored = cursor.getInt(SessionsQuery.SESSION_SPONSORED) == 1 ? true : false;
			if (isSponsored) {
				mSponsored.setText(R.string.session_sponsored);
			} else {
				mSponsored.setVisibility(View.GONE);
			}

			mRoomId = cursor.getString(SessionsQuery.ROOM_ID);

			// Unregister around setting checked state to avoid triggering
			// listener since change isn't user generated.
			mStarred.setOnCheckedChangeListener(null);
			mStarred.setChecked(cursor.getInt(SessionsQuery.STARRED) != 0);
			mStarred.setOnCheckedChangeListener(this);

			final String sessionAbstract = cursor.getString(SessionsQuery.SUMMARY);
			if (!TextUtils.isEmpty(sessionAbstract)) {
				UIUtils.setTextMaybeHtml(mSummary, sessionAbstract);
				mSummary.setVisibility(View.VISIBLE);
				mHasSummaryContent = true;
			} else {
				mSummary.setVisibility(View.GONE);
			}

			// Show empty message when all data is loaded, and nothing to show
			if (mSpeakersCursor && !mHasSummaryContent) {
				mRootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
			}

//			AnalyticsUtils.getInstance(getActivity()).trackPageView("/Sessions/" + mTitleString);

		} finally {
			cursor.close();
		}
	}

	private void onSpeakersQueryComplete(Cursor cursor) {

		if (!cursor.moveToFirst()) {
			return;
		}
		mSpeakersCursor = true;
		mAdapter.changeCursor(cursor);
	}

	private void queryForSpeakers() {
		mHandler.startQuery(SpeakersQuery._TOKEN, Speakers.CONTENT_URI, SpeakersQuery.PROJECTION, Speakers.SPEAKER_ID
				+ " in (" + mSpeakerList + ")");
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.session_detail_menu_items, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// final String shareString;
		final Intent intent;

		switch (item.getItemId()) {
		case R.id.menu_map:
			intent = new Intent(getActivity().getApplicationContext(), UIUtils.getMapActivityClass(getActivity()));
			intent.putExtra(MapFragment.EXTRA_ROOM, mRoomId);
			startActivity(intent);
			return true;

		case R.id.menu_share:
			// TODO: consider bringing in shortlink to session
			// shareString = getString(R.string.share_template, mTitleString,
			// getHashtagsString(),
			// mUrl);
			// intent = new Intent(Intent.ACTION_SEND);
			// intent.setType("text/plain");
			// intent.putExtra(Intent.EXTRA_TEXT, shareString);
			// startActivity(Intent.createChooser(intent,
			// getText(R.string.title_share)));
			// return true;
			return false;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Handle toggling of starred checkbox.
	 */
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		final ContentValues values = new ContentValues();
		values.put(ScheduleContract.Sessions.SESSION_STARRED, isChecked ? 1 : 0);
		mHandler.startUpdate(mSessionUri, values);

		// Because change listener is set to null during initialization, these
		// won't fire on
		// pageview.
//		AnalyticsUtils.getInstance(getActivity()).trackEvent("Sandbox", isChecked ? "Starred" : "Unstarred", mTitleString, 0);
	}

	/*
	 * Event structure: Category -> "Session Details" Action -> "Create Note",
	 * "View Note", etc Label -> Session's Title Value -> 0.
	 */
	public void fireNotesEvent(int actionId) {
//		AnalyticsUtils.getInstance(getActivity()).trackEvent("Session Details", getActivity().getString(actionId), mTitleString, 0);
	}

	/*
	 * Event structure: Category -> "Session Details" Action -> Link Text Label
	 * -> Session's Title Value -> 0.
	 */
	public void fireLinkEvent(int actionId) {
//		AnalyticsUtils.getInstance(getActivity()).trackEvent("Link Details", getActivity().getString(actionId), mTitleString, 0);
	}

	private class SpeakerAdapter extends CursorAdapter {

		public SpeakerAdapter(Context context) {
			super(context, null);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			((TextView) view.findViewById(R.id.speaker_title)).setText(cursor.getString(SpeakersQuery.SPEAKER_NAME));
			((TextView) view.findViewById(R.id.speaker_subtitle)).setText(cursor
					.getString(SpeakersQuery.SPEAKER_COMPANY));
			ImageView image = (ImageView) view.findViewById(R.id.image_item_speaker);
			UIUtils.displayImageLazily(context, cursor.getString(SpeakersQuery.SPEAKER_IMAGE_URL), image);

		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return getActivity().getLayoutInflater().inflate(R.layout.list_item_speaker, parent, false);
		}
	}

	/**
	 * {@link com.infine.android.devoxx.provider.ScheduleContract.Sessions}
	 * query parameters.
	 */
	private interface SessionsQuery {
		int _TOKEN = 0x1;

		String[] PROJECTION = { ScheduleContract.Blocks.BLOCK_START, ScheduleContract.Blocks.BLOCK_END,
				ScheduleContract.Sessions.SESSION_EXPERIENCE, ScheduleContract.Sessions.SESSION_TITLE,
				ScheduleContract.Sessions.SESSION_TYPE, ScheduleContract.Sessions.SESSION_THEME,
				ScheduleContract.Sessions.SESSION_SUMMARY, ScheduleContract.Sessions.SESSION_STARRED,
				ScheduleContract.Sessions.ROOM_ID, ScheduleContract.Rooms.ROOM_NAME,
				ScheduleContract.Sessions.SESSION_SPEAKERS, ScheduleContract.Sessions.SESSION_SPONSORED };

		int BLOCK_START = 0;
		int BLOCK_END = 1;
		int EXPERIENCE = 2;
		int TITLE = 3;
		int TYPE = 4;
		int THEME = 5;
		int SUMMARY = 6;
		int STARRED = 7;
		int ROOM_ID = 8;
		int ROOM_NAME = 9;
		int SESSION_SPEAKERS = 10;
		int SESSION_SPONSORED = 11;

	}

	private interface SpeakersQuery {
		int _TOKEN = 0x3;

		String[] PROJECTION = { ScheduleContract.Speakers._ID, ScheduleContract.Speakers.SPEAKER_ID,
				ScheduleContract.Speakers.SPEAKER_NAME, ScheduleContract.Speakers.SPEAKER_COMPANY,
				ScheduleContract.Speakers.SPEAKER_IMAGE_URL };

		int _ID = 0;
		int SPEAKER_ID = 1;
		int SPEAKER_NAME = 2;
		int SPEAKER_COMPANY = 3;
		int SPEAKER_IMAGE_URL = 4;
	}
}
