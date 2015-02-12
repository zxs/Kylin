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

package org.apache.kylin.metadata.measure.fixedlen;

import org.apache.kylin.metadata.model.DataType;


abstract public class FixedLenMeasureCodec<T> {

    public static FixedLenMeasureCodec<?> get(DataType type) {
        return new FixedPointLongCodec(type);
    }

    abstract public int getLength();

    abstract public DataType getDataType();

    abstract public T valueOf(String value);


    abstract public Object getValue();

    abstract public T read(byte[] buf, int offset);

    abstract public void write(T v, byte[] buf, int offset);

}
