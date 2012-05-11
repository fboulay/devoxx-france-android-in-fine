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

import android.content.ContentResolver;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import com.infine.android.devoxx.io.json.JsonHandler.HandlerException;

import java.io.IOException;
import java.io.InputStream;

public class RemoteJsonExecutor {

    private final HttpClient mHttpClient;
    private final ContentResolver mResolver;

    public RemoteJsonExecutor(HttpClient httpClient, ContentResolver resolver) {
        mHttpClient = httpClient;
        mResolver = resolver;
    }

    public void executeGet(String url, JsonHandler handler) throws HandlerException {
        final HttpUriRequest request = new HttpGet(url);
        execute(request, handler);
    }

    private void execute(HttpUriRequest request,JsonHandler handler) throws HandlerException {
        try {
            final HttpResponse resp = mHttpClient.execute(request);
            final int status = resp.getStatusLine().getStatusCode();
            if (status != HttpStatus.SC_OK) {
                throw new HandlerException("Unexpected server response " + resp.getStatusLine()
                        + " for " + request.getRequestLine());
            }

            final InputStream input = resp.getEntity().getContent();
            try {
                handler.parseAndApply(input, mResolver);
            } finally {
                if (input != null) input.close();
            }
        } catch (HandlerException e) {
            throw e;
        } catch (IOException e) {
            throw new HandlerException("Problem reading remote response for "
                    + request.getRequestLine(), e);
        }
    }
}


