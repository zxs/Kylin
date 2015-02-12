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

package org.apache.kylin.invertedindex.index;

import com.google.common.collect.Lists;
import org.apache.kylin.dict.DateStrDictionary;
import org.apache.commons.lang.ObjectUtils;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.io.LongWritable;

import java.util.List;

/**
 * @author yangli9, honma
 *         <p/>
 *         TableRecord extends RawTableRecord by decorating it with a
 *         TableRecordInfo
 */
public class TableRecord implements Cloneable {

    private TableRecordInfo info;
    private RawTableRecord rawRecord;

    public TableRecord(RawTableRecord rawRecord, TableRecordInfo info) {
        this.info = info;
        this.rawRecord = rawRecord;
    }

    public TableRecord(TableRecord another) {
        this.info = another.info;
        this.rawRecord = (RawTableRecord) another.rawRecord.clone();
    }

    @Override
    public Object clone() {
        return new TableRecord(this);
    }

    public void reset() {
        rawRecord.reset();
    }

    public byte[] getBytes() {
        return rawRecord.getBytes();
    }

    public void setBytes(byte[] bytes, int offset, int length) {
        rawRecord.setBytes(bytes, offset, length);
    }

    public long getTimestamp() {
        String str = getValueString(info.getTimestampColumn());
        return DateStrDictionary.stringToMillis(str);
    }

    public int length(int col) {
        return rawRecord.length(col);
    }

    public List<String> getOriginTableColumnValues() {
        List<String> ret = Lists.newArrayList();
        for (int i = 0; i < info.nColumns; ++i) {
            ret.add(getValueString(i));
        }
        return ret;
    }

    public void setValueString(int col, String value) {
        if (rawRecord.isMetric(col)) {
            LongWritable v = rawRecord.codec(col).valueOf(value);
            setValueMetrics(col, v);
        } else {
            int id = info.dict(col).getIdFromValue(value);
            rawRecord.setValueID(col, id);
        }
    }

    /**
     * get value of columns which belongs to the original table columns.
     * i.e. columns like min_xx, max_yy will never appear
     */
    public String getValueString(int col) {
        if (rawRecord.isMetric(col))
            return getValueMetric(col);
        else
            return info.dict(col).getValueFromId(rawRecord.getValueID(col));
    }

    public void getValueBytes(int col, ImmutableBytesWritable bytes) {
        rawRecord.getValueBytes(col, bytes);
    }

    private void setValueMetrics(int col, LongWritable value) {
        rawRecord.setValueMetrics(col, value);
    }

    private String getValueMetric(int col) {
        return rawRecord.getValueMetric(col);
    }

    public short getShard() {
        int timestampID = rawRecord.getValueID(info.getTimestampColumn());
        return (short) (Math.abs(ShardingHash.hashInt(timestampID)) % info.getDescriptor().getSharding());
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("[");
        for (int col = 0; col < rawRecord.getColumnCount(); col++) {
            if (col > 0)
                buf.append(",");
            buf.append(getValueString(col));
        }
        buf.append("]");
        return buf.toString();
    }

    @Override
    public int hashCode() {
        if (rawRecord != null) {
            return rawRecord.hashCode();
        } else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TableRecord other = (TableRecord) obj;
        return ObjectUtils.equals(other.rawRecord, this.rawRecord);
    }

}
