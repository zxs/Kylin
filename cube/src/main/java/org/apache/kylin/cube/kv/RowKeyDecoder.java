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

package org.apache.kylin.cube.kv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.kylin.cube.CubeSegment;
import org.apache.kylin.cube.common.RowKeySplitter;
import org.apache.kylin.common.util.SplittedBytes;
import org.apache.kylin.cube.cuboid.Cuboid;
import org.apache.kylin.cube.model.CubeDesc;
import org.apache.kylin.metadata.model.TblColRef;

/**
 * 
 * @author xjiang
 * 
 */
public class RowKeyDecoder {

    private final CubeDesc cubeDesc;
    private final RowKeyColumnIO colIO;
    private final RowKeySplitter rowKeySplitter;

    private Cuboid cuboid;
    private List<String> names;
    private List<String> values;

    public RowKeyDecoder(CubeSegment cubeSegment) {
        this.cubeDesc = cubeSegment.getCubeDesc();
        this.rowKeySplitter = new RowKeySplitter(cubeSegment, 65, 255);
        this.colIO = new RowKeyColumnIO(cubeSegment);
        this.values = new ArrayList<String>();
    }

    public long decode(byte[] bytes) throws IOException {
        this.values.clear();

        long cuboidId = rowKeySplitter.split(bytes, bytes.length);
        initCuboid(cuboidId);

        SplittedBytes[] splits = rowKeySplitter.getSplitBuffers();

        int offset = 1; // skip cuboid id part

        for (int i = 0; i < this.cuboid.getColumns().size(); i++) {
            TblColRef col = this.cuboid.getColumns().get(i);
            collectValue(col, splits[offset].value, splits[offset].length);
            offset++;
        }

        return cuboidId;
    }

    private void initCuboid(long cuboidID) {
        if (this.cuboid != null && this.cuboid.getId() == cuboidID) {
            return;
        }
        this.cuboid = Cuboid.findById(cubeDesc, cuboidID);
    }

    private void collectValue(TblColRef col, byte[] valueBytes, int length) throws IOException {
        String strValue = colIO.readColumnString(col, valueBytes, length);
        values.add(strValue);
    }

    public RowKeySplitter getRowKeySplitter() {
        return rowKeySplitter;
    }

    public void setCuboid(Cuboid cuboid) {
        this.cuboid = cuboid;
        this.names = null;
    }

    public List<String> getNames(Map<TblColRef, String> aliasMap) {
        if (names == null) {
            names = buildNameList(aliasMap);
        }
        return names;
    }

    private List<String> buildNameList(Map<TblColRef, String> aliasMap) {
        List<TblColRef> columnList = getColumns();
        List<String> result = new ArrayList<String>(columnList.size());
        for (TblColRef col : columnList)
            result.add(findName(col, aliasMap));
        return result;
    }

    private String findName(TblColRef column, Map<TblColRef, String> aliasMap) {
        String name = null;
        if (aliasMap != null) {
            name = aliasMap.get(column);
        }
        if (name == null) {
            name = column.getName();
        }
        return name;
    }

    public List<TblColRef> getColumns() {
        return cuboid.getColumns();
    }

    public List<String> getValues() {
        return values;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(cuboid.getId());
        for (Object value : values) {
            buf.append(",");
            buf.append(value);
        }
        return buf.toString();
    }

}
