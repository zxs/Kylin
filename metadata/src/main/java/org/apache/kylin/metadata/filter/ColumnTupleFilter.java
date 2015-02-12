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

package org.apache.kylin.metadata.filter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.kylin.common.util.BytesUtil;
import org.apache.kylin.metadata.model.ColumnDesc;
import org.apache.kylin.metadata.model.TableDesc;
import org.apache.kylin.metadata.model.TblColRef;
import org.apache.kylin.metadata.tuple.ITuple;

/**
 * 
 * @author xjiang
 * 
 */
public class ColumnTupleFilter extends TupleFilter {

    private TblColRef columnRef;
    private Object tupleValue;
    private List<String> values;

    public ColumnTupleFilter(TblColRef column) {
        super(Collections.<TupleFilter> emptyList(), FilterOperatorEnum.COLUMN);
        this.columnRef = column;
        this.values = new ArrayList<String>(1);
        this.values.add(null);
    }

    public TblColRef getColumn() {
        return columnRef;
    }

    public void setColumn(TblColRef col) {
        this.columnRef = col;
    }

    @Override
    public void addChild(TupleFilter child) {
        throw new UnsupportedOperationException("This is " + this + " and child is " + child);
    }

    @Override
    public String toString() {
        return "ColumnFilter [column=" + columnRef + "]";
    }

    @Override
    public boolean evaluate(ITuple tuple) {
        this.tupleValue = tuple.getValue(columnRef);
        return true;
    }

    @Override
    public boolean isEvaluable() {
        return true;
    }

    @Override
    public Collection<String> getValues() {
        this.values.set(0, (String) this.tupleValue);
        return this.values;
    }

    @Override
    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        String table = columnRef.getTable();
        BytesUtil.writeUTFString(table, buffer);

        String columnName = columnRef.getName();
        BytesUtil.writeUTFString(columnName, buffer);

        String dataType = columnRef.getDatatype();
        BytesUtil.writeUTFString(dataType, buffer);

        byte[] result = new byte[buffer.position()];
        System.arraycopy(buffer.array(), 0, result, 0, buffer.position());
        return result;
    }

    @Override
    public void deserialize(byte[] bytes) {
        ColumnDesc column = new ColumnDesc();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        String tableName = BytesUtil.readUTFString(buffer);
        if (tableName != null) {
            TableDesc table = new TableDesc();
            table.setName(tableName);
            column.setTable(table);
        }

        column.setName(BytesUtil.readUTFString(buffer));
        column.setDatatype(BytesUtil.readUTFString(buffer));

        this.columnRef = new TblColRef(column);
    }
}
