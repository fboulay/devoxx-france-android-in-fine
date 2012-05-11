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
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.infine.android.devoxx.data.SessionData;
import com.infine.android.devoxx.data.SessionsData;
import com.infine.android.devoxx.provider.ScheduleContract.Sessions;
import com.infine.android.devoxx.util.Lists;


public class JsonSessionHandler extends JsonVersionedHandler {
    
    // TODO mettre les vrais valeurs
    private static final String URL_FEEDBACK_SESSION = "URL_FEEDBACK_SESSION/";
    private static final String URL_NATATION_SESSION = "URL_NOTATION_SESSION/";
    private static final String URL_DETAIL_SESSION = "URL_DETAIL_SESSION/";

    
    public JsonSessionHandler(Context context, String prefsKey, int lastVersion) {
    	super(context, prefsKey, lastVersion);
    }

    
    @Override
    public ArrayList<ContentProviderOperation> deserialize(
            InputStream inputStream, ContentResolver resolver)
            throws IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        
        
        // on va lire les donnees du flux
        SessionsData sessions = readData(inputStream);
        if (sessions != null && sessions.getSessions() != null) {
        	
        	if (sessions.getSessions().size() > 1 ) {
                // ajoute une requete de suppression des anciennes sessions
        		
                batch.add(ContentProviderOperation.newDelete(Sessions.CONTENT_URI).build());

                for (SessionData s : sessions.getSessions()) {
                	batch.add( buildCPOperation(s, resolver) );
                }
        	}
        	
        }
        return batch;
    }
    
    private SessionsData readData(InputStream input) {
        SessionsData sessions = null;
        ObjectMapper mapper = MapperFactory.getMapperInstance();
        try {
            sessions = mapper.readValue(input, SessionsData.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } 
        return sessions;
    }
    
    private ContentProviderOperation buildCPOperation(SessionData session, ContentResolver resolver) {
        
        final ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(Sessions.CONTENT_URI);

        // affecte les valeurs des colonnes
        String sessionId = ""+session.getSessionId();
        builder.withValue(Sessions.BLOCK_ID, session.getScheduleId());
        builder.withValue(Sessions.SESSION_ID, sessionId);
        builder.withValue(Sessions.UPDATED, 0);
        builder.withValue(Sessions.SESSION_EXPERIENCE, session.getExperience());
        builder.withValue(Sessions.SESSION_FEEDBACK_URL, URL_FEEDBACK_SESSION + sessionId);
        builder.withValue(Sessions.SESSION_NOTES_URL, URL_NATATION_SESSION + sessionId);
        builder.withValue(Sessions.ROOM_ID, ""+session.getRoomId());
        builder.withValue(Sessions.SESSION_SPEAKERS, getListAsString(session.getSpeakerIds()));
        builder.withValue(Sessions.SESSION_SUMMARY, session.getSummary());
        builder.withValue(Sessions.SESSION_TAGS, getListAsString(session.getTags()));
        builder.withValue(Sessions.SESSION_THEME, session.getTheme());
        builder.withValue(Sessions.SESSION_TITLE, session.getTitle());
        builder.withValue(Sessions.SESSION_TYPE, session.getType());
        builder.withValue(Sessions.SESSION_SPONSORED, session.isSponsored());
        builder.withValue(Sessions.SESSION_URL, URL_DETAIL_SESSION + sessionId);
        
        // Propagate any existing starred value
        final Uri sessionUri = Sessions.buildSessionUri(sessionId);
        final int starred = querySessionStarred(sessionUri, resolver);
        if (starred != -1) {
            builder.withValue(Sessions.SESSION_STARRED, starred);
        }

        return builder.build();
    }

    private String getListAsString(List<String> values) {
        StringBuilder b = new StringBuilder();
        if (values != null) {
            for (String v : values) {
                b.append(v).append(',');                
            }
        }
        if (b.length()>0){
            b.deleteCharAt(b.length()-1);
        }
        return b.toString();
    }
  
    public static int querySessionStarred(Uri uri, ContentResolver resolver) {
        final String[] projection = { Sessions.SESSION_STARRED };
        final Cursor cursor = resolver.query(uri, projection, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            } else {
                return -1;
            }
        } finally {
            cursor.close();
        }
    }
 
}
