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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import android.util.Log;

import com.infine.android.devoxx.data.VersionData;

public class VersionReader {

	private static final String TAG = "VersionReader";
	
	private static final String ERR_MSG = "Probleme lors de la lecture du flux JSON";
	

	public static int getVersion(String url) {
		int version = 0;
		ObjectMapper mapper = MapperFactory.getMapperInstance();
		try {
			VersionData versionData = mapper.readValue(new URL(url), VersionData.class);
			version = versionData.getVersion();
		} catch (JsonParseException e) {
			Log.e(TAG, ERR_MSG);
		} catch (JsonMappingException e) {
			Log.e(TAG, ERR_MSG);
		} catch (MalformedURLException e) {
			Log.e(TAG, ERR_MSG);
		} catch (IOException e) {
			Log.e(TAG, ERR_MSG);
		}
		return version;
	}
}
