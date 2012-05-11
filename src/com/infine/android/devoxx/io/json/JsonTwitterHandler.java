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
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.infine.android.devoxx.data.TweetData;
import com.infine.android.devoxx.data.TweetsData;
import com.infine.android.devoxx.provider.ScheduleContract;
import com.infine.android.devoxx.provider.ScheduleContract.Tweets;
import com.infine.android.devoxx.util.Lists;


public class JsonTwitterHandler extends JsonHandler {
	
	
	public static final String NEXT_REFRESH_URL ="devoxx_twitter_refresh_url";
	public static final String TWITTER_MAX_ID ="devoxx_twitter_max_id";
	public static final String NO_MAX_ID ="0";
	
	// ex : Sat, 24 Mar 2012 21:16:30 +0000 
	// EEE, dd MMM yyyy HH:mm:ss Z
	private static final SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
	
	private SharedPreferences mPreferences;
	
	private static final String TAG = "JsonTwitterHandler";
	
    
    public JsonTwitterHandler(Context context) {
        super(ScheduleContract.CONTENT_AUTHORITY);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    
    @Override
    public ArrayList<ContentProviderOperation> deserialize(
            InputStream inputStream, ContentResolver resolver)
            throws IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        
        
        // on va lire les donnees du flux
        TweetsData tweets = readData(inputStream);
        
        if (tweets != null && tweets.getResults() != null  && tweets.getResults().size() > 0) {
        	String maxId = tweets.getMaxIdStr();
        	if (needUpdate(maxId)) {
        		
        		for (TweetData t : tweets.getResults()) {
        			batch.add( buildCPOperation(t, resolver) );
        		}
        		// met a jour les preferences
        		Editor editor = mPreferences.edit();
        		editor.putString(TWITTER_MAX_ID, maxId);
        		editor.putString(NEXT_REFRESH_URL, tweets.getRefreshUrl());
        		boolean success = editor.commit();
        		if (!success) {
        			Log.e(TAG, "Probleme lors de la sauvegarde des preferences de l'utilisateur");
        		}
        	}
        }
        return batch;
    }
    
  
    
    private TweetsData readData(InputStream input) {
    	TweetsData tweets = null;
        ObjectMapper mapper = MapperFactory.getMapperInstance();
        try {
        	tweets = mapper.readValue(input, TweetsData.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } 
        return tweets;
    }
    
    
    private ContentProviderOperation buildCPOperation(TweetData tweet, ContentResolver resolver) {
        
        final ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(Tweets.CONTENT_URI);

        // affecte les valeurs des colonnes
        builder.withValue(Tweets.TWEET_ID, tweet.getIdStr());
        builder.withValue(Tweets.CREATION_DATE, parseTime(tweet.getCreateAt()));
        builder.withValue(Tweets.IMAGE_URL, tweet.getProfileImageUrl());
        builder.withValue(Tweets.TEXT, tweet.getText());
        builder.withValue(Tweets.USER, tweet.getFromUser());
        builder.withValue(Tweets.USER_NAME, tweet.getFromUserName());

        return builder.build();
    }

   private boolean needUpdate(String maxId) {
		final String maxTwitterId = mPreferences.getString(TWITTER_MAX_ID, NO_MAX_ID);
		BigDecimal userMaxTwitterId = new BigDecimal(maxTwitterId);
		if (maxId != null && !"".equals(maxId.trim())) {
			BigDecimal newTwitterId = new BigDecimal(maxId);
			if (newTwitterId.compareTo(userMaxTwitterId) == 1) {
				return true;
			}
		}
		return false;
	}
 
    private long parseTime(String date) {
        long retour = 0L;
        try {
            retour =  formatter.parse(date).getTime();
        } catch (ParseException e) {
            System.out.println("Probleme lors du formattage de la date : " + date);
        }
        return retour;
    }
  
}
