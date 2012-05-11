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

package com.infine.android.devoxx.provider;

import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.infine.android.devoxx.provider.ScheduleContract.Blocks;
import com.infine.android.devoxx.provider.ScheduleContract.BlocksColumns;
import com.infine.android.devoxx.provider.ScheduleContract.Rooms;
import com.infine.android.devoxx.provider.ScheduleContract.RoomsColumns;
import com.infine.android.devoxx.provider.ScheduleContract.Sessions;
import com.infine.android.devoxx.provider.ScheduleContract.SessionsColumns;
import com.infine.android.devoxx.provider.ScheduleContract.Speakers;
import com.infine.android.devoxx.provider.ScheduleContract.SpeakersColumns;
import com.infine.android.devoxx.provider.ScheduleContract.SyncColumns;
import com.infine.android.devoxx.provider.ScheduleContract.Tracks;
import com.infine.android.devoxx.provider.ScheduleContract.TracksColumns;
import com.infine.android.devoxx.provider.ScheduleContract.TweetsColumns;
import com.infine.android.devoxx.provider.ScheduleContract.Vendors;
import com.infine.android.devoxx.provider.ScheduleContract.VendorsColumns;

/**
 * Helper for managing {@link SQLiteDatabase} that stores data for
 * {@link ScheduleProvider}.
 */
public class ScheduleDatabase extends SQLiteOpenHelper {
	private static final String TAG = "ScheduleDatabase";

	private static final String DATABASE_NAME = "schedule.db";

	// NOTE: carefully update onUpgrade() when bumping database versions to make
	// sure user data is saved.

	private static final int VER_LAUNCH = 1;
	private static final int VER_WITH_TWEETS = 2;
	private static final int VER_DB_REFACTORING = 3;

	private static final int DATABASE_VERSION = VER_DB_REFACTORING;

	interface Tables {
		String BLOCKS = "blocks";
		String TRACKS = "tracks";
		String TWEETS = "tweets";
		String ROOMS = "rooms";
		String SESSIONS = "sessions";
		String SPEAKERS = "speakers";
		// String SESSIONS_BOOKMARKS = "sessions_bookmarks";
		// String SESSIONS_SPEAKERS = "sessions_speakers";
		// String SESSIONS_TRACKS = "sessions_tracks";
		// String VENDORS = "vendors";

		String SESSIONS_SEARCH = "sessions_search";
		// String VENDORS_SEARCH = "vendors_search";

		// String SEARCH_SUGGEST = "search_suggest";

		String SESSIONS_JOIN_BLOCKS_ROOMS = "sessions "
				+ "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
				+ "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";

		// String VENDORS_JOIN_TRACKS = "vendors " +
		// "LEFT OUTER JOIN tracks ON vendors.track_id=tracks.track_id";

		// String SESSIONS_SPEAKERS_JOIN_SPEAKERS = "sessions_speakers "
		// +
		// "LEFT OUTER JOIN speakers ON sessions_speakers.speaker_id=speakers.speaker_id";

		// String SESSIONS_SPEAKERS_JOIN_SESSIONS_BLOCKS_ROOMS =
		// "sessions_speakers "
		// +
		// "LEFT OUTER JOIN sessions ON sessions_speakers.session_id=sessions.session_id "
		// + "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
		// + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";

		// String SESSIONS_TRACKS_JOIN_TRACKS = "sessions_tracks "
		// +
		// "LEFT OUTER JOIN tracks ON sessions_tracks.track_id=tracks.track_id";

		// String SESSIONS_TRACKS_JOIN_SESSIONS_BLOCKS_ROOMS =
		// "sessions_tracks "
		// +
		// "LEFT OUTER JOIN sessions ON sessions_tracks.session_id=sessions.session_id "
		// + "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
		// + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";

		String SESSIONS_SEARCH_JOIN_SESSIONS_BLOCKS_ROOMS = "sessions_search "
				+ "LEFT OUTER JOIN sessions ON sessions_search.session_id=sessions.session_id "
				+ "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
				+ "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";

		// String VENDORS_SEARCH_JOIN_VENDORS_TRACKS = "vendors_search "
		// +
		// "LEFT OUTER JOIN vendors ON vendors_search.vendor_id=vendors.vendor_id "
		// + "LEFT OUTER JOIN tracks ON vendors.track_id=tracks.track_id";

	}

	private interface Triggers {
		String SESSIONS_SEARCH_INSERT = "sessions_search_insert";
		String SESSIONS_SEARCH_DELETE = "sessions_search_delete";
		String SESSIONS_SEARCH_UPDATE = "sessions_search_update";

		// String VENDORS_SEARCH_INSERT = "vendors_search_insert";
		// String VENDORS_SEARCH_DELETE = "vendors_search_delete";
	}

	public interface SessionsSpeakers {
		String SESSION_ID = "session_id";
		String SPEAKER_ID = "speaker_id";
	}

	// public interface SessionsTracks {
	// String SESSION_ID = "session_id";
	// String TRACK_ID = "track_id";
	// }

	public interface SessionsBookmarks {
		String SESSION_ID = "session_id";
		String BOOKMARK_ID = "bookmark_id";
	}

	interface SessionsSearchColumns {
		String SESSION_ID = "session_id";
		String BODY = "body";
	}

	// interface VendorsSearchColumns {
	// String VENDOR_ID = "vendor_id";
	// String BODY = "body";
	// }

	/** Fully-qualified field names. */
	private interface Qualified {
		String SESSIONS_SEARCH_SESSION_ID = Tables.SESSIONS_SEARCH + "." + SessionsSearchColumns.SESSION_ID;
		// String VENDORS_SEARCH_VENDOR_ID = Tables.VENDORS_SEARCH + "." +
		// VendorsSearchColumns.VENDOR_ID;

		String SESSIONS_SEARCH = Tables.SESSIONS_SEARCH + "(" + SessionsSearchColumns.SESSION_ID + ","
				+ SessionsSearchColumns.BODY + ")";
		// String VENDORS_SEARCH = Tables.VENDORS_SEARCH + "(" +
		// VendorsSearchColumns.VENDOR_ID + ","
		// + VendorsSearchColumns.BODY + ")";
	}

	/** {@code REFERENCES} clauses. */
	private interface References {
		String BLOCK_ID = "REFERENCES " + Tables.BLOCKS + "(" + Blocks.BLOCK_ID + ")";
		// String TRACK_ID = "REFERENCES " + Tables.TRACKS + "(" +
		// Tracks.TRACK_ID + ")";
		String ROOM_ID = "REFERENCES " + Tables.ROOMS + "(" + Rooms.ROOM_ID + ")";
		String SESSION_ID = "REFERENCES " + Tables.SESSIONS + "(" + Sessions.SESSION_ID + ")";
		String SPEAKER_ID = "REFERENCES " + Tables.SPEAKERS + "(" + Speakers.SPEAKER_ID + ")";
		// String VENDOR_ID = "REFERENCES " + Tables.VENDORS + "(" +
		// Vendors.VENDOR_ID + ")";
	}

	private interface Subquery {
		/**
		 * Subquery used to build the {@link SessionsSearchColumns#BODY} string
		 * used for indexing {@link Sessions} content.
		 */
		String SESSIONS_BODY = "(new." + Sessions.SESSION_TITLE + "||'; '||new." + Sessions.SESSION_SUMMARY
				+ "||'; '||" + "coalesce(new." + Sessions.SESSION_TAGS + ", '')" + ")";

		/**
		 * Subquery used to build the {@link VendorsSearchColumns#BODY} string
		 * used for indexing {@link Vendors} content.
		 */
		String VENDORS_BODY = "(new." + Vendors.VENDOR_NAME + "||'; '||new." + Vendors.VENDOR_DESC + "||'; '||new."
				+ Vendors.VENDOR_PRODUCT_DESC + ")";
	}

	public ScheduleDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		if (!isTableExists(Tables.BLOCKS, db)) {
			db.execSQL("CREATE TABLE " + Tables.BLOCKS + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ BlocksColumns.BLOCK_ID + " TEXT NOT NULL," + BlocksColumns.BLOCK_TITLE + " TEXT NOT NULL,"
					+ BlocksColumns.BLOCK_CATEGORY + " TEXT NOT NULL," + BlocksColumns.BLOCK_START
					+ " INTEGER NOT NULL," + BlocksColumns.BLOCK_END + " INTEGER NOT NULL," + "UNIQUE ("
					+ BlocksColumns.BLOCK_ID + ") ON CONFLICT REPLACE)");
		}

		if (!isTableExists(Tables.TWEETS, db)) {
		db.execSQL("CREATE TABLE " + Tables.TWEETS + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ TweetsColumns.TWEET_ID + " TEXT NOT NULL," + TweetsColumns.USER + " TEXT," + TweetsColumns.USER_NAME
				+ " TEXT," + TweetsColumns.IMAGE_URL + " TEXT," + TweetsColumns.TEXT + " TEXT,"
				+ TweetsColumns.CREATION_DATE + " INTEGER," + "UNIQUE (" + TweetsColumns.TWEET_ID
				+ ") ON CONFLICT REPLACE)");
		}
		if (!isTableExists(Tables.ROOMS, db)) {
		db.execSQL("CREATE TABLE " + Tables.ROOMS + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ RoomsColumns.ROOM_ID + " TEXT NOT NULL," + RoomsColumns.ROOM_NAME + " TEXT,"
				+ RoomsColumns.ROOM_FLOOR + " TEXT," + "UNIQUE (" + RoomsColumns.ROOM_ID + ") ON CONFLICT REPLACE)");
		}
		if (!isTableExists(Tables.SESSIONS, db)) {
		db.execSQL("CREATE TABLE " + Tables.SESSIONS + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ SyncColumns.UPDATED + " INTEGER NOT NULL," + SessionsColumns.SESSION_ID + " TEXT NOT NULL,"
				+ Sessions.BLOCK_ID + " TEXT " + References.BLOCK_ID + "," + Sessions.ROOM_ID + " TEXT "
				+ References.ROOM_ID + "," + SessionsColumns.SESSION_EXPERIENCE + " TEXT,"
				+ SessionsColumns.SESSION_TITLE + " TEXT," + SessionsColumns.SESSION_TYPE + " TEXT,"
				+ SessionsColumns.SESSION_THEME + " TEXT," + SessionsColumns.SESSION_ROOM + " TEXT,"
				+ SessionsColumns.SESSION_SUMMARY + " TEXT," + SessionsColumns.SESSION_SPEAKERS + " TEXT,"
				+ SessionsColumns.SESSION_TAGS + " TEXT," + SessionsColumns.SESSION_URL + " TEXT,"
				+ SessionsColumns.SESSION_FEEDBACK_URL + " TEXT," + SessionsColumns.SESSION_NOTES_URL + " TEXT,"
				+ SessionsColumns.SESSION_SPONSORED + " INTEGER," + SessionsColumns.SESSION_STARRED
				+ " INTEGER NOT NULL DEFAULT 0," + "UNIQUE (" + SessionsColumns.SESSION_ID + ") ON CONFLICT REPLACE)");
		}
		if (!isTableExists(Tables.SPEAKERS, db)) {
		db.execSQL("CREATE TABLE " + Tables.SPEAKERS + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ SyncColumns.UPDATED + " INTEGER NOT NULL," + SpeakersColumns.SPEAKER_ID + " TEXT NOT NULL,"
				+ SpeakersColumns.SPEAKER_NAME + " TEXT," + SpeakersColumns.SPEAKER_IMAGE_URL + " TEXT,"
				+ SpeakersColumns.SPEAKER_COMPANY + " TEXT," + SpeakersColumns.SPEAKER_ABSTRACT + " TEXT,"
				+ SpeakersColumns.SPEAKER_URL + " TEXT," + SpeakersColumns.SPEAKER_SESSIONS + " TEXT," + "UNIQUE ("
				+ SpeakersColumns.SPEAKER_ID + ") ON CONFLICT REPLACE)");
		}
		createSessionsSearch(db);
	}

	/**
	 * Create triggers that automatically build {@link Tables#SESSIONS_SEARCH}
	 * as values are changed in {@link Tables#SESSIONS}.
	 */
	private static void createSessionsSearch(SQLiteDatabase db) {
		// Using the "porter" tokenizer for simple stemming, so that
		// "frustration" matches "frustrated."

		db.execSQL("CREATE VIRTUAL TABLE " + Tables.SESSIONS_SEARCH + " USING fts3(" + BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT," + SessionsSearchColumns.BODY + " TEXT NOT NULL,"
				+ SessionsSearchColumns.SESSION_ID + " TEXT NOT NULL " + References.SESSION_ID + "," + "UNIQUE ("
				+ SessionsSearchColumns.SESSION_ID + ") ON CONFLICT REPLACE," + "tokenize=porter)");

		// TODO: handle null fields in body, which cause trigger to fail
		// TODO: implement update trigger, not currently exercised

		db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SEARCH_INSERT + " AFTER INSERT ON " + Tables.SESSIONS
				+ " BEGIN INSERT INTO " + Qualified.SESSIONS_SEARCH + " " + " VALUES(new." + Sessions.SESSION_ID + ", "
				+ Subquery.SESSIONS_BODY + ");" + " END;");

		db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SEARCH_DELETE + " AFTER DELETE ON " + Tables.SESSIONS
				+ " BEGIN DELETE FROM " + Tables.SESSIONS_SEARCH + " " + " WHERE "
				+ Qualified.SESSIONS_SEARCH_SESSION_ID + "=old." + Sessions.SESSION_ID + ";" + " END;");

		db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SEARCH_UPDATE + " AFTER UPDATE ON " + Tables.SESSIONS
				+ " BEGIN UPDATE sessions_search SET " + SessionsSearchColumns.BODY + " = " + Subquery.SESSIONS_BODY
				+ " WHERE session_id = old.session_id" + "; END;");

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (Log.isLoggable(TAG, Log.DEBUG)) {
			Log.d(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);
		}
		// NOTE: This switch statement is designed to handle cascading database
		// updates, starting at the current version and falling through to all
		// future upgrade cases. Only use "break;" when you want to drop and
		// recreate the entire database.
		int version = oldVersion;
		switch (version) {
		case VER_WITH_TWEETS:
		case VER_LAUNCH:
			if (newVersion == VER_DB_REFACTORING) {
				db.execSQL("DROP TABLE IF EXISTS " + Tables.TRACKS);
				db.execSQL("DROP TABLE IF EXISTS vendors");
				db.execSQL("DROP TABLE IF EXISTS vendors_search");
				db.execSQL("DROP TABLE IF EXISTS VENDORS_SEARCH_CONTENT");
				db.execSQL("DROP TABLE IF EXISTS VENDORS_SEARCH_SEGMENTS");
				db.execSQL("DROP TABLE IF EXISTS VENDORS_SEARCH_SEGDIR");
				db.execSQL("DROP TABLE IF EXISTS sessions_speakers");
				db.execSQL("DROP TABLE IF EXISTS sessions_tracks");
				db.execSQL("DROP TABLE IF EXISTS search_suggest");
				db.execSQL("DROP TRIGGER IF EXISTS vendors_search_insert");
				db.execSQL("DROP TRIGGER IF EXISTS vendors_search_delete");
			}
		}
		if (Log.isLoggable(TAG, Log.DEBUG)) {
			Log.d(TAG, "after upgrade logic, at version " + version);
		}
		if (version != DATABASE_VERSION) {
			Log.w(TAG, "Destroying old data during upgrade");

//			db.execSQL("DROP TABLE IF EXISTS " + Tables.BLOCKS);
//			db.execSQL("DROP TABLE IF EXISTS " + Tables.TRACKS);
//			db.execSQL("DROP TABLE IF EXISTS " + Tables.TWEETS);
//			db.execSQL("DROP TABLE IF EXISTS " + Tables.ROOMS);
//			db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS);
//			db.execSQL("DROP TABLE IF EXISTS " + Tables.SPEAKERS);
//			db.execSQL("DROP TABLE IF EXISTS sessions_speakers");
//			db.execSQL("DROP TABLE IF EXISTS sessions_tracks");
//			db.execSQL("DROP TABLE IF EXISTS vendors");
//
//			db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_SEARCH_INSERT);
//			db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_SEARCH_DELETE);
//			db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_SEARCH);
//
//			db.execSQL("DROP TRIGGER IF EXISTS vendors_search_insert");
//			db.execSQL("DROP TRIGGER IF EXISTS vendors_search_delete");
//			db.execSQL("DROP TABLE IF EXISTS vendors_search");
//
//			db.execSQL("DROP TABLE IF EXISTS search_suggest");

//			onCreate(db);
		}
	}

	public boolean isTableExists(String tableName, SQLiteDatabase db) {

		Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + tableName + "'",
				null);
		if (cursor != null) {
			if (cursor.getCount() > 0) {
				return true;
			}
		}
		return false;
	}

}
