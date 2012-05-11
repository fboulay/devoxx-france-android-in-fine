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

package com.infine.android.devoxx.ui.widget;

import java.util.Calendar;
import java.util.TimeZone;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.widget.Button;

import com.infine.android.devoxx.R;
import com.infine.android.devoxx.provider.ScheduleContract.Blocks;
import com.infine.android.devoxx.util.UIUtils;

/**
 * Custom view that represents a {@link Blocks#BLOCK_ID} instance, including its
 * title and time span that it occupies. Usually organized automatically by
 * {@link BlocksLayout} to match up against a {@link TimeRulerView} instance.
 */
public class BlockView extends Button {
	private static final int TALK_DURATION = 1;

	private static final int CODE_STORY_DURATION = 6;

	private static final int QUICKY_DURATION = 16;

	private static final int BOF_START_HOUR = 19;

	private static final int PARTY_START_HOUR = 19;

	private static final int TIME_STRING_FLAGS = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY
			| DateUtils.FORMAT_ABBREV_WEEKDAY | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_TIME;

	// id du block
	private final String mBlockId;
	// titre du block
	private String mTitle;
	// debut du block
	private final long mStartTime;
	// fin du block
	private final long mEndTime;
	// contient une etoile
	private final boolean mContainsStarred;
	// type de colonne
	private final BlockColumnType mColumnType;

	public BlockView(Context context, String blockId, String title, long startTime, long endTime, int nbStar,
			String titleStarred, BlockColumnType columnType) {
		super(context);

		mBlockId = blockId;
		mTitle = title;
		mStartTime = startTime;
		mEndTime = endTime;
		mContainsStarred = nbStar != 0;
		// patch
		mColumnType = patchType(columnType);

		if (columnType.showStar()) {
			StringBuilder text = new StringBuilder(title);
			text.append(" - ").append(nbStar).append(" ☆\n");
			if (titleStarred != null && titleStarred.length() > 0) {
				String[] titles = titleStarred.split("#");
				for (String starTitle : titles) {
					text.append("☆ ").append(starTitle).append("\n");
				}
			}
			setText(text);
		} else {
			setText(mTitle);
		}

		setTextSize(13.0f);
		int textColor = Color.WHITE;
		int accentColor = -1;

		switch (mColumnType) {
		case REGISTRATION:
			accentColor = getResources().getColor(R.color.block_column_registration);
			break;
		case BREAKFAST:
			accentColor = getResources().getColor(R.color.block_column_breakfast);
			break;
		case LUNCH:
			accentColor = getResources().getColor(R.color.block_column_lunch);
			break;
		case TALK:
			accentColor = getResources().getColor(R.color.block_column_talk);
			break;
		case BOF:
			accentColor = getResources().getColor(R.color.block_column_bof);
			break;
		case QUICKY:
			accentColor = getResources().getColor(R.color.block_column_quicky);
			setText("");
			break;
		case UNIVERSITY:
			accentColor = getResources().getColor(R.color.block_column_university);
			break;
		case KEYNOTE:
			accentColor = getResources().getColor(R.color.block_column_keynote);
			break;
		case BREAK:
			accentColor = getResources().getColor(R.color.block_column_break);
			break;
		case PARTY:
			accentColor = getResources().getColor(R.color.block_column_party);
			break;
		case COFFE_BREAK:
			accentColor = getResources().getColor(R.color.block_column_coffeebreak);
			break;
		case CODE_STORY:
			accentColor = getResources().getColor(R.color.block_column_talk);
			break;
		default:
			accentColor = getResources().getColor(R.color.block_column_undefined);
			break;
		}

		LayerDrawable buttonDrawable = (LayerDrawable) context.getResources().getDrawable(R.drawable.btn_block);
		buttonDrawable.getDrawable(0).setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP);
		buttonDrawable.getDrawable(1).setAlpha(mContainsStarred ? 255 : 0);

		setTextColor(textColor);
		setBackgroundDrawable(buttonDrawable);
	}

	// TODO find a better way :-S
	private BlockColumnType patchType(BlockColumnType original) {
		long duration = mEndTime - mStartTime;
		Calendar startDate = Calendar.getInstance();
		startDate.setTimeInMillis(mStartTime);
		int startHour = startDate.get(Calendar.HOUR_OF_DAY);
		long durationInMinutes = duration / 1000 / 60;
		long durationInHours = durationInMinutes / 60;
		BlockColumnType retour = original;
		if (durationInMinutes <= QUICKY_DURATION) {
			retour = BlockColumnType.QUICKY;
		} else if (original == BlockColumnType.TALK) {
			if (durationInHours > TALK_DURATION) {
				retour = BlockColumnType.UNIVERSITY;
			} else if (startHour >= BOF_START_HOUR) {
				retour = BlockColumnType.BOF;
			}
		} else if (original == BlockColumnType.BREAK && startHour >= PARTY_START_HOUR) {
			retour = BlockColumnType.PARTY;
		}
		return retour;
	}

	public String getBlockId() {
		return mBlockId;
	}

	public String getTitle() {
		return mTitle;
	}

	public String getBlockTimeString() {
		TimeZone.setDefault(UIUtils.CONFERENCE_TIME_ZONE);
		return DateUtils.formatDateTime(getContext(), mStartTime, TIME_STRING_FLAGS);
	}

	public long getStartTime() {
		return mStartTime;
	}

	public long getEndTime() {
		return mEndTime;
	}

	public BlockColumnType getColumn() {
		return mColumnType;
	}
}
