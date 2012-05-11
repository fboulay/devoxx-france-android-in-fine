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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.infine.android.devoxx.R;
import com.infine.android.devoxx.service.RestService;
import com.infine.android.devoxx.service.ServiceStatus;
import com.infine.android.devoxx.util.AnalyticsUtils;
import com.infine.android.devoxx.util.DetachableResultReceiver;
import com.infine.android.devoxx.util.EulaHelper;

/**
 * Front-door {@link Activity} that displays high-level features the schedule
 * application offers to users. Depending on whether the device is a phone or an
 * Android 3.0+ tablet, different layouts will be used. For example, on a phone,
 * the primary content is a {@link DashboardFragment}, whereas on a tablet, both
 * a {@link DashboardFragment} and a {@link TagStreamFragment} are displayed.
 */
public class HomeActivity extends BaseActivity {
	private static final String TAG = "HomeActivity";
	
	// 1 minute between 2 refresh, unless force=true in #triggerRefresh()
	private static final long REFRESH_INTERVAL = 60000;

	private TagStreamFragment mTagStreamFragment;
	private SyncStatusUpdaterFragment mSyncStatusUpdaterFragment;

	private long lastRefresh = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// TODO a virer
		// mode debug -- reset EULA
		// EulaHelper.resetEula(this);

		if (!EulaHelper.hasAcceptedEula(this)) {
			EulaHelper.showEula(false, this);
		}

		AnalyticsUtils.getInstance(this).trackPageView("/Home");

		setContentView(R.layout.activity_home);
		getActivityHelper().setupActionBar(null, 0);

		FragmentManager fm = getSupportFragmentManager();

		mTagStreamFragment = (TagStreamFragment) fm.findFragmentById(R.id.fragment_tag_stream);

		mSyncStatusUpdaterFragment = (SyncStatusUpdaterFragment) fm.findFragmentByTag(SyncStatusUpdaterFragment.TAG);
		if (mSyncStatusUpdaterFragment == null) {
			mSyncStatusUpdaterFragment = new SyncStatusUpdaterFragment();
			fm.beginTransaction().add(mSyncStatusUpdaterFragment, SyncStatusUpdaterFragment.TAG).commit();

			triggerRefresh(true);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		triggerRefresh(false);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		getActivityHelper().setupHomeActivity();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.home_menu_items, menu);
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			triggerRefresh(true);
			return true;
		} else if (item.getItemId() == R.id.menu_info) {
			showAbout();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Execute a refresh of data.
	 * @param forceRefresh bypass the interval between 2 refresh
	 */
	private void triggerRefresh(boolean forceRefresh) {
		long now = System.currentTimeMillis();
		
		if (forceRefresh || now - lastRefresh > REFRESH_INTERVAL) {
			final Intent intent = new Intent(Intent.ACTION_SYNC, null, this, RestService.class);

			intent.putExtra(RestService.EXTRA_STATUS_RECEIVER, mSyncStatusUpdaterFragment.mReceiver);

			startService(intent);

			if (mTagStreamFragment != null) {
				mTagStreamFragment.refresh();
			}
		}
		lastRefresh = now;
	}

	private void updateRefreshStatus(boolean refreshing) {
		getActivityHelper().setRefreshActionButtonCompatState(refreshing);
	}

	private void showAbout() {
		AlertDialog about = new AlertDialog.Builder(this).setTitle(R.string.description_info)
				.setIcon(android.R.drawable.ic_dialog_info).setMessage(R.string.dialog_about).setCancelable(true)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).create();
		about.show();
	}

	/**
	 * A non-UI fragment, retained across configuration changes, that updates
	 * its activity's UI when sync status changes.
	 */
	public static class SyncStatusUpdaterFragment extends Fragment implements DetachableResultReceiver.Receiver {
		public static final String TAG = SyncStatusUpdaterFragment.class.getName();

		private boolean mSyncing = false;
		private DetachableResultReceiver mReceiver;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			mReceiver = new DetachableResultReceiver(new Handler());
			mReceiver.setReceiver(this);
		}

		/** {@inheritDoc} */
		public void onReceiveResult(int resultCode, Bundle resultData) {
			HomeActivity activity = (HomeActivity) getActivity();
			if (activity == null) {
				return;
			}

			switch (resultCode) {
			case ServiceStatus.STATUS_RUNNING: {
				mSyncing = true;
				break;
			}
			case ServiceStatus.STATUS_FINISHED: {
				mSyncing = false;
				break;
			}
			case ServiceStatus.STATUS_ERROR: {
				// Error happened down in SyncService, show as toast.
				mSyncing = false;
				final String errorText = getString(R.string.toast_sync_error, resultData.getString(Intent.EXTRA_TEXT));
				Toast.makeText(activity, errorText, Toast.LENGTH_LONG).show();
				break;
			}
			}

			activity.updateRefreshStatus(mSyncing);
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			((HomeActivity) getActivity()).updateRefreshStatus(mSyncing);
		}
	}
}
