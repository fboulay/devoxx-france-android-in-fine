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

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

public class TwitterRemoteExecutor {

    private final HttpClient mHttpClient;


    public TwitterRemoteExecutor(HttpClient httpClient) {
        mHttpClient = httpClient;
    }

    public <T> T executeGetAndDeserialize(String url, Class<T> mappingClass) throws JsonProcessingException, IOException {
        final HttpUriRequest request = new HttpGet(url);
        return execute(request, mappingClass);
    }

    private <T> T execute(HttpUriRequest request, Class<T> mappingClass) throws JsonProcessingException, IOException {
    	T jsonObjects = null;
   
        final HttpResponse resp = mHttpClient.execute(request);
        final int status = resp.getStatusLine().getStatusCode();
        if (status != HttpStatus.SC_OK) {
            throw new IOException("Unexpected server response " + resp.getStatusLine()
                    + " for " + request.getRequestLine());
        }
        final InputStream input = resp.getEntity().getContent();
        try {
        	jsonObjects = readData(input, mappingClass);
        } finally {
            if (input != null) input.close();
        }
        return jsonObjects;
        
    }
    
    private <T> T readData(InputStream input, Class<T> mappingClass) throws JsonProcessingException, IOException {
    	T jsonObjects = null;
        ObjectMapper mapper = MapperFactory.getMapperInstance();
        jsonObjects = mapper.readValue(input, mappingClass);
        return jsonObjects;
    }


}
