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

package org.apache.kylin.dict;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class DateStrDictionaryTest {

    DateStrDictionary dict;

    @Before
    public void setup() {
        dict = new DateStrDictionary();
    }

    @Test
    public void testNull() {
        int nullId = dict.getIdFromValue(null);
        assertNull(dict.getValueFromId(nullId));
        int nullId2 = dict.getIdFromValueBytes(null, 0, 0);
        assertEquals(dict.getValueBytesFromId(nullId2, null, 0), 0);
        assertEquals(nullId, nullId2);
    }

    @Test
    public void test() {
        checkPair("0001-01-01");
        checkPair("1970-01-02");
        checkPair("1975-06-24");
        checkPair("2024-10-04");
        checkPair("9999-12-31");
    }

    @Test
    public void testIllegalArgument() {
        try {
            dict.getIdFromValue("abcd");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // good
        }

        try {
            dict.getValueFromId(-2);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // good
        }
    }

    private void checkPair(String dateStr) {
        int id = dict.getIdFromValue(dateStr);
        String dateStrBack = dict.getValueFromId(id);
        assertEquals(dateStr, dateStrBack);
    }
}
