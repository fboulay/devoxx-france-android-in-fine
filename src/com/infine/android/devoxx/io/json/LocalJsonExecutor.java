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

import android.content.ContentResolver;
import android.content.res.Resources;

import com.infine.android.devoxx.io.json.JsonHandler.HandlerException;

/**
 * Charge les fichiers statiques et ceux du cache
 */
public class LocalJsonExecutor {
    private Resources mRes;
    private ContentResolver mResolver;

    public LocalJsonExecutor(Resources res, ContentResolver resolver) {
        mRes = res;
        mResolver = resolver;
    }

    public void execute(int resId, JsonHandler handler) throws HandlerException {
        final InputStream inputStream = mRes.openRawResource(resId);
        try {
            try {
                handler.parseAndApply(inputStream, mResolver);
            } finally {
                if (inputStream != null) inputStream.close();
            }
        } catch (IOException e) {
            throw new HandlerException("Problem parsing local resource id : " + resId, e);
        }
    }
}
