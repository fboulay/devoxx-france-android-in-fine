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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;

import com.infine.android.devoxx.data.Schedule;
import com.infine.android.devoxx.data.Schedules;
import com.infine.android.devoxx.provider.ScheduleContract.Blocks;
import com.infine.android.devoxx.util.Lists;


public class JsonScheduleHandler extends JsonVersionedHandler {
	
    public JsonScheduleHandler(Context context, String prefsKey, int lastVersion) {
        super(context, prefsKey, lastVersion);
        
    }

    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    @Override
    public ArrayList<ContentProviderOperation> deserialize(
            InputStream inputStream, ContentResolver resolver)
            throws IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        
        // Supprime les anciens schedules
//        final String selection = Blocks.BLOCK_CATEGORY + "=? OR " 
//                                + Blocks.BLOCK_CATEGORY +"=? OR " 
//                                + Blocks.BLOCK_CATEGORY +"=? ";
//        final String[] selectionArgs = {
//                Blocks.Categories.LUNCH,
//                Blocks.Categories.REGISTRATION,
//                Blocks.Categories.TALK
//        };

        // envoie une requete au content provider pour effacer les blocks
        batch.add(ContentProviderOperation.newDelete(Blocks.CONTENT_URI).build());
//                .withSelection(selection, selectionArgs)

        // on va lire les donnees du flux
        Schedules schedules = readData(inputStream);
        if (schedules != null && schedules.getSchedules() != null) {
            for (Schedule sched : schedules.getSchedules()) {
                batch.add( buildCPOperation(sched) );
            }
        }
        return batch;
    }
    
  
    
    private Schedules readData(InputStream input) {
        Schedules schedules = null;
        ObjectMapper mapper = MapperFactory.getMapperInstance();
        try {
            schedules = mapper.readValue(input, Schedules.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } 
        return schedules;
    }
    
    private ContentProviderOperation buildCPOperation(Schedule schedule) {
        
        final ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(Blocks.CONTENT_URI);

        // affecte les valeurs des colonnes
        builder.withValue(Blocks.BLOCK_ID, ""+schedule.getScheduleId());
        // TODO voir ce qu'on met dans le titre
        builder.withValue(Blocks.BLOCK_TITLE, schedule.getTitle());
        builder.withValue(Blocks.BLOCK_START, parseTime(schedule.getStart()));
        builder.withValue(Blocks.BLOCK_END, parseTime(schedule.getEnd()));
        builder.withValue(Blocks.BLOCK_CATEGORY, schedule.getCategory());

        return builder.build();
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
