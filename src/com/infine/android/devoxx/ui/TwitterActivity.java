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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

import com.infine.android.devoxx.R;
import com.infine.android.devoxx.provider.ScheduleContract;
import com.infine.android.devoxx.service.TwitterService;

public class TwitterActivity extends BaseSinglePaneActivity {
	
	@Override
	protected Fragment onCreatePane() {
		triggerRefresh();
		final Intent intent = new Intent(Intent.ACTION_VIEW, ScheduleContract.Tweets.CONTENT_URI);
		TwitterFragment2 fragment =  new TwitterFragment2();
		fragment.setArguments(intentToFragmentArguments(intent));
		return fragment;
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		getActivityHelper().setupSubActivity();
	}
	
	
	private void triggerRefresh() {
		final Uri tweetsUri = ScheduleContract.Tweets.CONTENT_URI;
        final Intent intent = new Intent(Intent.ACTION_SYNC, tweetsUri, this, TwitterService.class);
        startService(intent);
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.refresh_menu_items, menu);
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			triggerRefresh();
			return true;
		} 
		return super.onOptionsItemSelected(item);
	}
		
}
