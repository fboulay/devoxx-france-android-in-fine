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
package com.infine.android.devoxx.data;

import java.util.List;

public class TweetsData {

	private String maxIdStr;

	private String refreshUrl;

	private List<TweetData> results;

	public String getMaxIdStr() {
		return maxIdStr;
	}

	public void setMax_id_str(String maxIdStr) {
		this.maxIdStr = maxIdStr;
	}

	public String getRefreshUrl() {
		return refreshUrl;
	}

	public void setRefresh_url(String refreshUrl) {
		this.refreshUrl = refreshUrl;
	}

	public List<TweetData> getResults() {
		return results;
	}

	public void setResults(List<TweetData> results) {
		this.results = results;
	}

}
