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

package org.apache.kylin.job.hadoop.hive;

import java.util.HashMap;
import java.util.Map;

/**
 * @author George Song (ysong1)
 * 
 */
public class SqlHiveDataTypeMapping {

    private static final Map<String, String> sqlToHiveDataTypeMapping = new HashMap<String, String>();

    static {
        sqlToHiveDataTypeMapping.put("short", "smallint");
        sqlToHiveDataTypeMapping.put("long", "bigint");
        sqlToHiveDataTypeMapping.put("byte", "tinyint");
        sqlToHiveDataTypeMapping.put("datetime", "date");
    }

    public static String getHiveDataType(String javaDataType) {
        String hiveDataType = sqlToHiveDataTypeMapping.get(javaDataType.toLowerCase());
        if (hiveDataType == null) {
            hiveDataType = javaDataType;
        }
        return hiveDataType.toLowerCase();
    }
}
