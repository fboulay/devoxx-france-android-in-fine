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

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.util.Log;

import com.infine.android.devoxx.data.RoomData;
import com.infine.android.devoxx.data.RoomsData;
import com.infine.android.devoxx.provider.ScheduleContract;
import com.infine.android.devoxx.provider.ScheduleContract.Rooms;
import com.infine.android.devoxx.util.Lists;

/**
 * Handler to manage room from JSON InputStream.
 * 
 * @author florian
 *
 */
public class JsonRoomHandler extends JsonHandler {
    
	private static final String TAG = "JsonRoomHandler";

    public JsonRoomHandler() {
        super(ScheduleContract.CONTENT_AUTHORITY);
    }
    
    @Override
    public ArrayList<ContentProviderOperation> deserialize(
            InputStream inputStream, ContentResolver resolver)
            throws IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        
        // Read rooms from JSON InputStream
        RoomsData rooms = readData(inputStream);
        if (rooms != null && rooms.getRooms() != null) {
            for (RoomData r : rooms.getRooms()) {
                batch.add( buildCPOperation(r) );
            }
        }
        return batch;
    }
    
    /**
     * From the {@link InputStream} it returns an {@link RoomsData} object. Yeah !
     * @param input
     * @return
     */
    private RoomsData readData(InputStream input) {
        RoomsData rooms = null;
        ObjectMapper mapper = MapperFactory.getMapperInstance();
        try {
        	rooms = mapper.readValue(input, RoomsData.class);
        } catch (JsonParseException e) {
           Log.e(TAG, "error parsing JSON", e);
        } catch (JsonMappingException e) {
        	Log.e(TAG, "error mapping JSON", e);
        } catch (IOException e) {
        	Log.e(TAG, "error when reading the JSON stream", e);
        } 
        return rooms;
    }
    
    /**
     * Insertion of a room from a {@link RoomData}
     * @param room
     * @return
     */
    private ContentProviderOperation buildCPOperation(RoomData room) {
        
        final ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(Rooms.CONTENT_URI);

        // Fill in the columns from the RoomData JSON Object
        builder.withValue(Rooms.ROOM_ID, room.getRoomId());
        builder.withValue(Rooms.ROOM_NAME, room.getRoomName());
        builder.withValue(Rooms.ROOM_FLOOR, 0);

        return builder.build();
    }
 
}
