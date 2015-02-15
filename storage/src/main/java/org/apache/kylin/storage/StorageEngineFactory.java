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

package org.apache.kylin.storage;

import org.apache.kylin.cube.CubeInstance;
import org.apache.kylin.invertedindex.IIInstance;
import org.apache.kylin.metadata.realization.IRealization;
import org.apache.kylin.metadata.realization.RealizationType;
import org.apache.kylin.storage.hbase.CubeStorageEngine;
import org.apache.kylin.storage.hbase.InvertedIndexStorageEngine;
import org.apache.kylin.storage.hybrid.HybridInstance;
import org.apache.kylin.storage.hybrid.HybridStorageEngine;

/**
 * @author xjiang
 */
public class StorageEngineFactory {

    public static IStorageEngine getStorageEngine(IRealization realization) {
        if (realization.getType() == RealizationType.INVERTED_INDEX) {
            return new InvertedIndexStorageEngine((IIInstance) realization);
        }

        if (realization.getType() == RealizationType.CUBE) {
            return new CubeStorageEngine((CubeInstance) realization);
        }

        return new HybridStorageEngine((HybridInstance) realization);
    }

}
