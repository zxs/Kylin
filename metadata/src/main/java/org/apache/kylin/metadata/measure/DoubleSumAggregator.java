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

package org.apache.kylin.metadata.measure;

import org.apache.hadoop.io.DoubleWritable;

/**
 * @author yangli9
 * 
 */
public class DoubleSumAggregator extends MeasureAggregator<DoubleWritable> {

    DoubleWritable sum = new DoubleWritable();

    @Override
    public void reset() {
        sum.set(0.0);
    }

    @Override
    public void aggregate(DoubleWritable value) {
        sum.set(sum.get() + value.get());
    }

    @Override
    public DoubleWritable getState() {
        return sum;
    }

    @Override
    public int getMemBytes() {
        return guessDoubleMemBytes();
    }

}
