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
package com.infine.android.devoxx.ui.adapter;

import java.util.Date;
import java.util.Locale;

import org.ocpsoft.pretty.time.PrettyTime;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.infine.android.devoxx.R;
import com.infine.android.devoxx.provider.query.TweetsQuery;
import com.infine.android.devoxx.util.UIUtils;

public class TwitterCursorAdapter extends CursorAdapter {

	private PrettyTime pt;

	public TwitterCursorAdapter(Context context) {
		super(context, null, false);
		if (Locale.getDefault().getISO3Country().equals("FRA")) {
			pt = new PrettyTime(new Locale("fr"));
		} else {
			pt = new PrettyTime(new Locale("en"));
		}
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		final TextView tweetTextView = (TextView) view.findViewById(R.id.list_item_tweet_text);
		final ImageView tweetIconView = (ImageView) view.findViewById(R.id.list_item_tweet_icon);
		final TextView tweetTextViewUsername = (TextView) view.findViewById(R.id.list_item_tweet_username);
		final TextView tweetTextViewElapsed = (TextView) view.findViewById(R.id.list_item_tweet_elapsed_time);

		tweetTextView.setText(cursor.getString(TweetsQuery.TEXT));
		tweetTextViewUsername.setText(cursor.getString(TweetsQuery.USER));
		tweetTextViewElapsed.setText(pt.format(new Date(cursor.getLong(TweetsQuery.CREATION_DATE))));
		UIUtils.displayImageLazily(context, cursor.getString(TweetsQuery.IMAGE_URL), tweetIconView);

	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return LayoutInflater.from(context).inflate(R.layout.list_item_tweet, parent, false);
	}


}
