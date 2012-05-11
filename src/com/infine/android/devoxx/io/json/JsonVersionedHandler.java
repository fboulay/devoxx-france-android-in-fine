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
package com.infine.android.devoxx.io.json;

import com.infine.android.devoxx.provider.ScheduleContract;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public abstract class JsonVersionedHandler extends JsonHandler {

	private Context mContext;
	private String mPrefsKey;
	private int mVersion;

	public JsonVersionedHandler(Context context, String prefsKey, int lastVersion) {
		super(ScheduleContract.CONTENT_AUTHORITY);
		mContext = context;
		mPrefsKey = prefsKey;
		mVersion = lastVersion;
	}

	@Override
	protected void onBatchSucceed() {
		updatePrefs();
	}

	private void updatePrefs() {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		final Editor editor = prefs.edit();
		editor.putInt(mPrefsKey, mVersion);
		editor.commit();
	}
}
