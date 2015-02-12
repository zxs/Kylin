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

package org.apache.kylin.job.hadoop.invertedindex;

import java.io.IOException;
import java.util.HashSet;

import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.ShortWritable;
import org.apache.hadoop.io.Text;

import org.apache.kylin.common.mr.KylinReducer;
import org.apache.kylin.common.util.ByteArray;

/**
 * @author yangli9
 */
public class IIDistinctColumnsCombiner extends KylinReducer<ShortWritable, Text, ShortWritable, Text> {

    private Text outputValue = new Text();

    @Override
    protected void setup(Context context) throws IOException {
        super.publishConfiguration(context.getConfiguration());

    }

    @Override
    public void reduce(ShortWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

        HashSet<ByteArray> set = new HashSet<ByteArray>();
        for (Text textValue : values) {
            ByteArray value = new ByteArray(Bytes.copy(textValue.getBytes(), 0, textValue.getLength()));
            set.add(value);
        }

        for (ByteArray value : set) {
            outputValue.set(value.data);
            context.write(key, outputValue);
        }
    }

}
