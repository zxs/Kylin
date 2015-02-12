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

package org.apache.kylin.invertedindex.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.kylin.common.util.StringUtil;

/**
 * Created by Hongbin Ma(Binmahone) on 12/26/14.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class IIDimension {
    @JsonProperty("table")
    private String table;
    @JsonProperty("columns")
    private String[] columns;

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String[] getColumns() {
        return columns;
    }

    public void setColumns(String[] columns) {
        this.columns = columns;
    }


    public static void capicalizeStrings(List<IIDimension> dimensions) {
        for (IIDimension iiDimension : dimensions) {
            iiDimension.setTable(iiDimension.getTable().toUpperCase());
            StringUtil.toUpperCaseArray(iiDimension.getColumns(), iiDimension.getColumns());
        }
    }

    public static int getColumnCount(List<IIDimension> iiDimensions) {
        int count = 0;
        for (IIDimension iiDimension : iiDimensions) {
            count += iiDimension.getColumns().length;
        }
        return count;
    }

}
