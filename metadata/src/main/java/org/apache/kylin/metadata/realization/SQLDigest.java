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

package org.apache.kylin.metadata.realization;

import java.util.Collection;

import org.apache.kylin.metadata.filter.TupleFilter;
import org.apache.kylin.metadata.model.FunctionDesc;
import org.apache.kylin.metadata.model.JoinDesc;
import org.apache.kylin.metadata.model.TblColRef;

/**
 * Created by Hongbin Ma(Binmahone) on 1/8/15.
 */
public class SQLDigest {
    public String factTable;
    public TupleFilter filter;
    public Collection<JoinDesc> joinDescs;
    public Collection<TblColRef> allColumns;
    public Collection<TblColRef> groupbyColumns;
    public Collection<TblColRef> filterColumns;
    public Collection<TblColRef> metricColumns;
    public Collection<FunctionDesc> aggregations;

    public SQLDigest(String factTable, TupleFilter filter, Collection<JoinDesc> joinDescs, Collection<TblColRef> allColumns, //
            Collection<TblColRef> groupbyColumns, Collection<TblColRef> filterColumns, Collection<TblColRef> aggregatedColumns, Collection<FunctionDesc> aggregateFunnc) {
        this.factTable = factTable;
        this.filter = filter;
        this.joinDescs = joinDescs;
        this.allColumns = allColumns;
        this.groupbyColumns = groupbyColumns;
        this.filterColumns = filterColumns;
        this.metricColumns = aggregatedColumns;
        this.aggregations = aggregateFunnc;
    }

    @Override
    public String toString() {
        return "fact table " + this.factTable + "," + //
                "group by " + this.groupbyColumns + "," + //
                "filter on " + this.filterColumns + "," + //
                "with aggregates" + this.aggregations + ".";
    }
}
