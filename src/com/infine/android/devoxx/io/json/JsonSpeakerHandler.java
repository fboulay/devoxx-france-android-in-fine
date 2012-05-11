/*
 *
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;

import com.infine.android.devoxx.data.RoomData;
import com.infine.android.devoxx.data.RoomsData;
import com.infine.android.devoxx.data.SpeakerData;
import com.infine.android.devoxx.data.SpeakersData;
import com.infine.android.devoxx.provider.ScheduleContract.Speakers;
import com.infine.android.devoxx.util.Lists;

/**
 * Handler to manage speakers from JSON InputStream.
 * 
 * @author Florian Boulay
 * 
 */
public class JsonSpeakerHandler extends JsonVersionedHandler {

	private static final String TAG = "JsonSpeakerHandler";

	public JsonSpeakerHandler(Context context, String prefsKey, int lastVersion) {
		super(context, prefsKey, lastVersion);
	}

	@Override
	public ArrayList<ContentProviderOperation> deserialize(InputStream inputStream, ContentResolver resolver)
			throws IOException {
		final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

		// Read rooms from JSON InputStream
		SpeakersData speakers = readData(inputStream);
		if (speakers != null && speakers.getSpeakers() != null) {
			for (SpeakerData s : speakers.getSpeakers()) {
				batch.add(buildCPOperation(s));
			}
		}
		return batch;
	}

	/**
	 * From the {@link InputStream} it returns an {@link RoomsData} object. Yeah
	 * !
	 * 
	 * @param input
	 * @return
	 */
	private SpeakersData readData(InputStream input) {
		SpeakersData speakers = null;
		ObjectMapper mapper = MapperFactory.getMapperInstance();
		try {
			speakers = mapper.readValue(input, SpeakersData.class);
		} catch (JsonParseException e) {
			Log.e(TAG, "error parsing JSON", e);
		} catch (JsonMappingException e) {
			Log.e(TAG, "error mapping JSON", e);
		} catch (IOException e) {
			Log.e(TAG, "error when reading the JSON stream", e);
		}
		return speakers;
	}

	/**
	 * Insertion of a room from a {@link RoomData}
	 * 
	 * @param speaker
	 * @return
	 */
	private ContentProviderOperation buildCPOperation(SpeakerData speaker) {

		final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(Speakers.CONTENT_URI);
		// Fill in the columns from the SpeakerData JSON Object
		builder.withValue(Speakers.SPEAKER_ID, speaker.getSpeakerId());
		builder.withValue(Speakers.SPEAKER_NAME, speaker.getName());
		builder.withValue(Speakers.SPEAKER_COMPANY, speaker.getCompany());
		builder.withValue(Speakers.SPEAKER_IMAGE_URL, speaker.getImageUri());
		builder.withValue(Speakers.SPEAKER_ABSTRACT, speaker.getBio());
		builder.withValue(Speakers.SPEAKER_SESSIONS, getListAsString(speaker.getSessionIds()));
		builder.withValue(Speakers.SPEAKER_URL, "N/A");
		builder.withValue(Speakers.UPDATED, 0);

		return builder.build();
	}

	private String getListAsString(List<String> values) {
		StringBuilder b = new StringBuilder();
		if (values != null) {
			for (String v : values) {
				b.append(v).append(',');
			}
		}
		if (b.length() > 0) {
			b.deleteCharAt(b.length() - 1);
		}
		return b.toString();
	}

}
