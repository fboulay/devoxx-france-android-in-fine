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
package com.infine.android.devoxx.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.util.Log;

import com.infine.android.devoxx.R;
import com.infine.android.devoxx.io.json.JsonHandler.HandlerException;
import com.infine.android.devoxx.io.json.JsonRoomHandler;
import com.infine.android.devoxx.io.json.JsonScheduleHandler;
import com.infine.android.devoxx.io.json.JsonSessionHandler;
import com.infine.android.devoxx.io.json.JsonSpeakerHandler;
import com.infine.android.devoxx.io.json.LocalJsonExecutor;
import com.infine.android.devoxx.io.json.RemoteJsonExecutor;
import com.infine.android.devoxx.io.json.VersionReader;
import com.infine.android.devoxx.util.HttpHelper;

/**
 * Created by IntelliJ IDEA. User: Kris Date: 09/03/12 Time: 00:33 To change
 * this template use File | Settings | File Templates.
 */
public class RestService extends IntentService {

	private static final String PREFS_SCHEDULE_VERSION = "com.infine.android.devoxx.restservice.schedule.version";
	private static final String PREFS_SESSION_VERSION = "com.infine.android.devoxx.restservice.session.version";
	private static final String PREFS_SPEAKER_VERSION = "com.infine.android.devoxx.restservice.speaker.version";

	public static final String EXTRA_STATUS_RECEIVER = "com.infine.android.devoxx.extra.STATUS_RECEIVER";

	public static final String EXTRA_LASTEST_VERSION = "com.infine.android.devoxx.extra.LASTEST_VERSION";

	private static final String INFINE_SERVER_URL = "http://devoxx.infine.com";

	private static final String SERVER_PATH_SCHEDULE = INFINE_SERVER_URL + "/schedule";
	private static final String SERVER_PATH_SCHEDULE_VERSION = SERVER_PATH_SCHEDULE + "/version";

	private static final String SERVER_PATH_SESSION = INFINE_SERVER_URL + "/session";
	private static final String SERVER_PATH_SESSION_VERSION = SERVER_PATH_SESSION + "/version";

	private static final String SERVER_PATH_SPEAKER = INFINE_SERVER_URL + "/speaker";
	private static final String SERVER_PATH_SPEAKER_VERSION = SERVER_PATH_SESSION + "/version";

	private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String ENCODING_GZIP = "gzip";

	private static final String TAG = "RestService";

	public static final int STATUS_RUNNING = 0x1;
	public static final int STATUS_ERROR = 0x2;
	public static final int STATUS_FINISHED = 0x3;

	private LocalJsonExecutor mLocalExecutor;
	private RemoteJsonExecutor mRemoteExecutor;
	private Context mApplicationContext;

	public RestService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		final HttpClient httpClient = HttpHelper.getHttpClient(this);
		final ContentResolver resolver = getContentResolver();

		mLocalExecutor = new LocalJsonExecutor(getResources(), resolver);
		mRemoteExecutor = new RemoteJsonExecutor(httpClient, resolver);
		mApplicationContext = getApplicationContext();

	}

	@Override
	protected void onHandleIntent(Intent intent) {

		final ResultReceiver receiver = intent.getParcelableExtra(EXTRA_STATUS_RECEIVER);

		// si passage du numero de version
		// final int latestVersion = intent.getIntExtra(EXTRA_LASTEST_VERSION,
		// 1);

		if (receiver != null)
			receiver.send(ServiceStatus.STATUS_RUNNING, Bundle.EMPTY);

		// final Context context = this;
		// final SharedPreferences prefs =
		// getSharedPreferences(Prefs.DEVOXX_SCHEDULE_SYNC,
		// Context.MODE_PRIVATE);
		// final int localVersion = prefs.getInt(Prefs.LOCAL_VERSION,
		// VERSION_NONE);

		try {

			// Bulk of sync work, performed by executing several fetches from
			// local and online sources.
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
			int scheduleVersion = prefs.getInt(PREFS_SCHEDULE_VERSION, -1);
			int sessionVersion = prefs.getInt(PREFS_SESSION_VERSION, -1);
			int speakerVersion = prefs.getInt(PREFS_SPEAKER_VERSION, -1);

			if (scheduleVersion + sessionVersion + speakerVersion < 0) {
				// on charge les fichiers statiques que la premiere fois
				// ou quand un jeu de données schedule ou session est pourri
				// verion = -1
				loadStaticFiles();
			}

			// Always hit remote spreadsheet for any updates
			loadRemoteData();

		} catch (Exception e) {
			Log.e(TAG, "Problem while syncing", e);
			if (receiver != null) {
				// Pass back error to surface listener
				final Bundle bundle = new Bundle();
				bundle.putString(Intent.EXTRA_TEXT, e.toString());
				receiver.send(STATUS_ERROR, bundle);
			}
		}

		// Announce success to any surface listener
		if (receiver != null)
			receiver.send(STATUS_FINISHED, Bundle.EMPTY);
	}

	/**
	 * Charge les fichiers de donnees statiques
	 * 
	 * @param localVersion
	 * @param latestVersion
	 * @param prefs
	 * @throws HandlerException
	 */
	private void loadStaticFiles() throws HandlerException {
		final long startLocal = System.currentTimeMillis();
		// final boolean localParse = localVersion < latestVersion;
		// if (localParse || localVersion == VERSION_NONE) {
		// Load static local data
		// chargement des blocks
		mLocalExecutor.execute(R.raw.schedule, new JsonScheduleHandler(mApplicationContext, PREFS_SCHEDULE_VERSION, 0));
		// chargement des sessions
		mLocalExecutor.execute(R.raw.session, new JsonSessionHandler(mApplicationContext, PREFS_SESSION_VERSION, 0));
		// Room loading
		mLocalExecutor.execute(R.raw.room, new JsonRoomHandler());
		// Speaker loading
		mLocalExecutor.execute(R.raw.speaker, new JsonSpeakerHandler(mApplicationContext, PREFS_SPEAKER_VERSION, 0));

		// Save local parsed version
		// if (localVersion > VERSION_NONE) {
		// prefs.edit().putInt(Prefs.LOCAL_VERSION, latestVersion).commit();
		// }
		// }
		if (Log.isLoggable(TAG, Log.DEBUG)) {
			Log.d(TAG, "local sync took " + (System.currentTimeMillis() - startLocal) + "ms");
		}
	}

	private void loadRemoteData() throws HandlerException {
		final long startRemote = System.currentTimeMillis();
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);

		// gestion des schedules
		int localScheduleVersion = prefs.getInt(PREFS_SCHEDULE_VERSION, 0);
		int lastScheduleVersion = VersionReader.getVersion(SERVER_PATH_SCHEDULE_VERSION);
		if (localScheduleVersion < lastScheduleVersion) {
			mRemoteExecutor.executeGet(SERVER_PATH_SCHEDULE, new JsonScheduleHandler(mApplicationContext,
					PREFS_SCHEDULE_VERSION, lastScheduleVersion));
		}

		// gestion des sessions
		int localSessionVersion = prefs.getInt(PREFS_SESSION_VERSION, 0);
		int lastSessionVersion = VersionReader.getVersion(SERVER_PATH_SESSION_VERSION);
		if (localSessionVersion < lastSessionVersion) {
			mRemoteExecutor.executeGet(SERVER_PATH_SESSION, new JsonSessionHandler(mApplicationContext,
					PREFS_SESSION_VERSION, lastSessionVersion));
		}

		// speaker management
		int localSpeakerVersion = prefs.getInt(PREFS_SPEAKER_VERSION, 0);
		int lastSpeakerVersion = VersionReader.getVersion(SERVER_PATH_SPEAKER_VERSION);
		if (localSpeakerVersion < lastSpeakerVersion) {
			mRemoteExecutor.executeGet(SERVER_PATH_SPEAKER, new JsonSpeakerHandler(mApplicationContext,
					PREFS_SPEAKER_VERSION, lastSpeakerVersion));
		}
		if (Log.isLoggable(TAG, Log.DEBUG)) {
			Log.d(TAG, "remote sync took " + (System.currentTimeMillis() - startRemote) + "ms");
		}
	}

	/**
	 * Generate and return a {@link HttpClient} configured for general use,
	 * including setting an application-specific user-agent string.
	 */
	public static HttpClient getHttpClient(Context context) {
		final HttpParams params = new BasicHttpParams();

		// Use generous timeouts for slow mobile networks
		HttpConnectionParams.setConnectionTimeout(params, 20 * 1000);
		HttpConnectionParams.setSoTimeout(params, 20 * 1000);

		HttpConnectionParams.setSocketBufferSize(params, 8192);
		HttpProtocolParams.setUserAgent(params, buildUserAgent(context));

		final DefaultHttpClient client = new DefaultHttpClient(params);

		client.addRequestInterceptor(new HttpRequestInterceptor() {
			public void process(HttpRequest request, HttpContext context) {
				// Add header to accept gzip content
				if (!request.containsHeader(HEADER_ACCEPT_ENCODING)) {
					request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
				}
			}
		});

		client.addResponseInterceptor(new HttpResponseInterceptor() {
			public void process(HttpResponse response, HttpContext context) {
				// Inflate any responses compressed with gzip
				final HttpEntity entity = response.getEntity();
				final Header encoding = entity.getContentEncoding();
				if (encoding != null) {
					for (HeaderElement element : encoding.getElements()) {
						if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
							response.setEntity(new InflatingEntity(response.getEntity()));
							break;
						}
					}
				}
			}
		});

		return client;
	}

	/**
	 * Build and return a user-agent string that can identify this application
	 * to remote servers. Contains the package name and version code.
	 */
	private static String buildUserAgent(Context context) {
		try {
			final PackageManager manager = context.getPackageManager();
			final PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);

			// Some APIs require "(gzip)" in the user-agent string.
			return info.packageName + "/" + info.versionName + " (" + info.versionCode + ") (gzip)";
		} catch (NameNotFoundException e) {
			return null;
		}
	}

	/**
	 * Simple {@link HttpEntityWrapper} that inflates the wrapped
	 * {@link HttpEntity} by passing it through {@link GZIPInputStream}.
	 */
	private static class InflatingEntity extends HttpEntityWrapper {
		public InflatingEntity(HttpEntity wrapped) {
			super(wrapped);
		}

		@Override
		public InputStream getContent() throws IOException {
			return new GZIPInputStream(wrappedEntity.getContent());
		}

		@Override
		public long getContentLength() {
			return -1;
		}
	}

	// private interface Prefs {
	// String DEVOXX_SCHEDULE_SYNC = "devoxx_schedule_sync";
	// String LOCAL_VERSION = "local_version";
	// }

}
