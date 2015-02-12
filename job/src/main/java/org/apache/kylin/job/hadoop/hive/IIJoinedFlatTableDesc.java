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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.Lists;

import org.apache.kylin.invertedindex.model.IIDesc;
import org.apache.kylin.metadata.model.DataModelDesc;
import org.apache.kylin.metadata.model.JoinDesc;
import org.apache.kylin.metadata.model.LookupDesc;
import org.apache.kylin.metadata.model.TblColRef;

/**
 * Created by Hongbin Ma(Binmahone) on 12/30/14.
 */
public class IIJoinedFlatTableDesc implements IJoinedFlatTableDesc {

    private IIDesc iiDesc;
    private String tableName;
    private List<IntermediateColumnDesc> columnList = Lists.newArrayList();
    private Map<String, String> tableAliasMap;

    public IIJoinedFlatTableDesc(IIDesc iiDesc) {
        this.iiDesc = iiDesc;
        parseIIDesc();
    }

    private void parseIIDesc() {
        this.tableName = "kylin_intermediate_ii_" + iiDesc.getName();

        int columnIndex = 0;
        for (TblColRef col : iiDesc.listAllColumns()) {
            columnList.add(new IntermediateColumnDesc(String.valueOf(columnIndex), col));
            columnIndex++;
        }
        buildTableAliasMap();
    }

    private void buildTableAliasMap() {
        tableAliasMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        tableAliasMap.put(iiDesc.getFactTableName(), FACT_TABLE_ALIAS);

        int i = 1;
        for (LookupDesc lookupDesc : iiDesc.getModel().getLookups()) {
            JoinDesc join = lookupDesc.getJoin();
            if (join != null) {
                tableAliasMap.put(lookupDesc.getTable(), LOOKUP_TABLE_ALAIS_PREFIX + i);
                i++;
            }
        }
    }

    @Override
    public String getTableName(String jobUUID) {
        return tableName + "_" + jobUUID.replace("-", "_");
    }

    public List<IntermediateColumnDesc> getColumnList() {
        return columnList;
    }

    @Override
    public DataModelDesc getDataModel() {
        return iiDesc.getModel();
    }

    @Override
    public DataModelDesc.RealizationCapacity getCapacity() {
        return DataModelDesc.RealizationCapacity.SMALL;
    }

    @Override
    public String getTableAlias(String tableName) {
        return tableAliasMap.get(tableName);
    }
}
