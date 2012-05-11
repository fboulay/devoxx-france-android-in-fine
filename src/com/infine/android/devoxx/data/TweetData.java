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

public class TweetData {


	private String idStr;
	private String createdAt;
	private String fromUser;
	private String fromUserName;
	private String profileImageUrl;
	private String text;

	
	public String getIdStr() {
		return idStr;
	}
	
	public void setId_str(String idStr) {
		this.idStr = idStr;
	}
	
	public String getCreateAt() {
		return createdAt;
	}

	public void setCreated_at(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getFromUser() {
		return fromUser;
	}

	public void setFrom_user(String fromUser) {
		this.fromUser = fromUser;
	}

	public String getFromUserName() {
		return fromUserName;
	}

	public void setFrom_user_name(String fromUserName) {
		this.fromUserName = fromUserName;
	}

	public String getProfileImageUrl() {
		return profileImageUrl;
	}

	public void setProfile_image_url(String profileImageUrl) {
		this.profileImageUrl = profileImageUrl;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

}
