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

package org.apache.kylin.query.enumerator;

import net.hydromatic.linq4j.AbstractEnumerable;
import net.hydromatic.linq4j.Enumerable;
import net.hydromatic.linq4j.Enumerator;
import net.hydromatic.optiq.DataContext;

import org.apache.kylin.query.relnode.OLAPContext;

/**
 * 
 * @author xjiang
 * 
 */
public class OLAPQuery extends AbstractEnumerable<Object[]> implements Enumerable<Object[]> {

    public static final String PROP_SCAN_THRESHOLD = "scan_threshold";

    public enum EnumeratorTypeEnum {
        INDEX, LOOKUP_TABLE, HIVE
    }

    private final DataContext optiqContext;
    private final EnumeratorTypeEnum type;
    private final int contextId;

    public OLAPQuery(DataContext optiqContext, EnumeratorTypeEnum type, int ctxId) {
        this.optiqContext = optiqContext;
        this.type = type;
        this.contextId = ctxId;
    }

    public OLAPQuery(EnumeratorTypeEnum type, int ctxSeq) {
        this(null, type, ctxSeq);
    }

    public Enumerator<Object[]> enumerator() {
        OLAPContext olapContext = OLAPContext.getThreadLocalContextById(contextId);
        switch (type) {
        case INDEX:
            return new CubeEnumerator(olapContext, optiqContext);
        case LOOKUP_TABLE:
            return new LookupTableEnumerator(olapContext);
        case HIVE:
            return new HiveEnumerator(olapContext);
        default:
            throw new IllegalArgumentException("Wrong type " + type + "!");
        }
    }
}
