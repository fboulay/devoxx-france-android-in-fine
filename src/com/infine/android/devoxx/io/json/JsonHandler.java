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

import org.codehaus.jackson.JsonProcessingException;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.os.RemoteException;

/**
 *
 */
public abstract class JsonHandler {

    private final String mAuthority;

    public JsonHandler(String authority) {
        mAuthority = authority;
    }

    public void parseAndApply(InputStream in, ContentResolver resolver)
            throws HandlerException {
        try {
            final ArrayList<ContentProviderOperation> batch = deserialize(in, resolver);
            resolver.applyBatch(mAuthority, batch);
            // si on arrive ici c'est que ca s'est bien passe, sinon on aurait eu une exception
            onBatchSucceed();
        } catch (HandlerException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new HandlerException("Problem parsing JSON response", e);
        } catch (IOException e) {
            throw new HandlerException("Problem reading response", e);
        } catch (RemoteException e) {
            // Failed binder transactions aren't recoverable
            throw new RuntimeException("Problem applying batch operation", e);
        } catch (OperationApplicationException e) {
            // Failures like constraint violation aren't recoverable
            // wrapping around to retry parsing again.
            throw new RuntimeException("Problem applying batch operation", e);
        }
    }

    /**
     * Methode executer si le batch est OK
     */
    protected void onBatchSucceed() { }
    
    /**
     * Deserialize the stream to objects
     * @param inputStream
     * @param resolver
     * @return
     * @throws IOException
     */
    public abstract ArrayList<ContentProviderOperation> deserialize(InputStream inputStream,
            ContentResolver resolver) throws IOException;

    /**
     * General {@link java.io.IOException} that indicates a problem occured while
     * parsing or applying an {@link org.xmlpull.v1.XmlPullParser}.
     */
    public static class HandlerException extends IOException {
        public HandlerException(String message) {
            super(message);
        }

        public HandlerException(String message, Throwable cause) {
            super(message);
            initCause(cause);
        }

        @Override
        public String toString() {
            if (getCause() != null) {
                return getLocalizedMessage() + ": " + getCause();
            } else {
                return getLocalizedMessage();
            }
        }
    }
}
