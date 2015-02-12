/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.apache.kylin.common.persistence;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang.time.FastDateFormat;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Marks the root entity of JSON persistence. Unit of read, write, cache, and
 * refresh.
 * 
 * - CubeInstance - CubeDesc - SourceTable - JobMeta - Dictionary (not JSON but
 * also top level persistence)
 * 
 * @author yangli9
 */
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
abstract public class RootPersistentEntity implements AclEntity {

    static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss z";
    static FastDateFormat format = FastDateFormat.getInstance(DATE_PATTERN);
    static DateFormat df = new SimpleDateFormat(DATE_PATTERN);
  
    public static String formatTime(long millis) {
        return format.format(millis);
    }
    
    public static long parseTime(String timeString) {
        if(timeString == null)
            return 0;
        try {
            Date dt = df.parse(timeString);
            return dt.getTime();
        } catch (ParseException e) {
        }
        return 0l;
    }

    // ============================================================================

    @JsonProperty("uuid")
    protected String uuid;

    @JsonProperty("last_modified")
    protected long lastModified;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getId() {
        return uuid;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public void updateRandomUuid() {
        setUuid(UUID.randomUUID().toString());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (lastModified ^ (lastModified >>> 32));
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RootPersistentEntity other = (RootPersistentEntity) obj;
        if (lastModified != other.lastModified)
            return false;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

}
