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
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.kylin.common.util.BytesSplitter;
import org.apache.kylin.cube.CubeSegment;
import org.apache.kylin.cube.cuboid.Cuboid;
import org.apache.kylin.cube.model.CubeDesc;
import org.apache.kylin.cube.model.DimensionDesc;
import org.apache.kylin.metadata.model.*;

/**
 * @author George Song (ysong1)
 */
public class CubeJoinedFlatTableDesc implements IJoinedFlatTableDesc {

    private String tableName;
    private final CubeDesc cubeDesc;
    private final CubeSegment cubeSegment;

    private int columnCount;
    private int[] rowKeyColumnIndexes; // the column index on flat table
    private int[][] measureColumnIndexes; // [i] is the i.th measure related column index on flat table

    // Map for table alias:
    // key -> table name; 
    // value -> alias;
    private Map<String, String> tableAliasMap;
    private List<IntermediateColumnDesc> columnList = Lists.newArrayList();



    public CubeJoinedFlatTableDesc(CubeDesc cubeDesc, CubeSegment cubeSegment) {
        this.cubeDesc = cubeDesc;
        this.cubeSegment = cubeSegment;
        parseCubeDesc();
    }

    /**
     * @return the cubeSegment
     */
    public CubeSegment getCubeSegment() {
        return cubeSegment;
    }

    // check what columns from hive tables are required, and index them
    private void parseCubeDesc() {
        int rowkeyColCount = cubeDesc.getRowkey().getRowKeyColumns().length;
        long baseCuboidId = Cuboid.getBaseCuboidId(cubeDesc);
        Cuboid baseCuboid = Cuboid.findById(cubeDesc, baseCuboidId);

        if (cubeSegment == null) {
            this.tableName = "kylin_intermediate_" + cubeDesc.getName();
        } else {
            this.tableName = "kylin_intermediate_" + cubeDesc.getName() + "_" + cubeSegment.getName();
        }

        Map<String, Integer> dimensionIndexMap = Maps.newHashMap();
        int columnIndex = 0;
        for (TblColRef col : cubeDesc.listDimensionColumnsExcludingDerived()) {
            dimensionIndexMap.put(colName(col.getCanonicalName()), columnIndex);
            columnList.add(new IntermediateColumnDesc(String.valueOf(columnIndex), col));
            columnIndex++;
        }

        // build index
        List<TblColRef> cuboidColumns = baseCuboid.getColumns();
        rowKeyColumnIndexes = new int[rowkeyColCount];
        for (int i = 0; i < rowkeyColCount; i++) {
            String colName = colName(cuboidColumns.get(i).getCanonicalName());
            Integer dimIdx = dimensionIndexMap.get(colName);
            if (dimIdx == null) {
                throw new RuntimeException("Can't find column " + colName);
            }
            rowKeyColumnIndexes[i] = dimIdx;
        }

        List<MeasureDesc> measures = cubeDesc.getMeasures();
        int measureSize = measures.size();
        measureColumnIndexes = new int[measureSize][];
        for (int i = 0; i < measureSize; i++) {
            FunctionDesc func = measures.get(i).getFunction();
            List<TblColRef> colRefs = func.getParameter().getColRefs();
            if (colRefs == null) {
                measureColumnIndexes[i] = null;
            } else {
                measureColumnIndexes[i] = new int[colRefs.size()];
                for (int j = 0; j < colRefs.size(); j++) {
                    TblColRef c = colRefs.get(j);
                    measureColumnIndexes[i][j] = contains(columnList, c);
                    if (measureColumnIndexes[i][j] < 0) {
                        measureColumnIndexes[i][j] = columnIndex;
                        columnList.add(new IntermediateColumnDesc(String.valueOf(columnIndex), c));
                        columnIndex++;
                    }
                }
            }
        }

        columnCount = columnIndex;
        
        buildTableAliasMap();
    }

    private void buildTableAliasMap() {
        tableAliasMap = new HashMap<String, String>();

        tableAliasMap.put(cubeDesc.getFactTable().toUpperCase(), FACT_TABLE_ALIAS);

        int i = 1;
        for (DimensionDesc dim : cubeDesc.getDimensions()) {
            JoinDesc join = dim.getJoin();
            if (join != null) {
                tableAliasMap.put(dim.getTable().toUpperCase(), LOOKUP_TABLE_ALAIS_PREFIX + i);
                i++;
            }

        }
    }

    private int contains(List<IntermediateColumnDesc> columnList, TblColRef c) {
        for (int i = 0; i < columnList.size(); i++) {
            IntermediateColumnDesc col = columnList.get(i);

            if (col.isSameAs(c.getTable(), c.getName()))
                return i;
        }
        return -1;
    }

    // sanity check the input record (in bytes) matches what's expected
    public void sanityCheck(BytesSplitter bytesSplitter) {
        if (columnCount != bytesSplitter.getBufferSize()) {
            throw new IllegalArgumentException("Expect " + columnCount + " columns, but see " + bytesSplitter.getBufferSize() + " -- " + bytesSplitter);
        }
        
        // TODO: check data types here
    }

    public CubeDesc getCubeDesc() {
        return cubeDesc;
    }

    public int[] getRowKeyColumnIndexes() {
        return rowKeyColumnIndexes;
    }

    public int[][] getMeasureColumnIndexes() {
        return measureColumnIndexes;
    }

    @Override
    public String getTableName(String jobUUID) {
        return tableName + "_" + jobUUID.replace("-", "_");
    }

    @Override
    public List<IntermediateColumnDesc> getColumnList() {
        return columnList;
    }

    @Override
    public DataModelDesc getDataModel() {
        return cubeDesc.getModel();
    }

    @Override
    public DataModelDesc.RealizationCapacity getCapacity() {
        return cubeDesc.getModel().getCapacity();
    }

    @Override
    public String getTableAlias(String tableName) {
        return tableAliasMap.get(tableName.toUpperCase());
    }

    private static String colName(String canonicalColName) {
        return canonicalColName.replace(".", "_");
    }
}
