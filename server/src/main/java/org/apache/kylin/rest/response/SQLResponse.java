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

package org.apache.kylin.rest.response;

import java.io.Serializable;
import java.util.List;

import org.apache.kylin.rest.model.SelectedColumnMeta;

public class SQLResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    // private static final Logger logger =
    // LoggerFactory.getLogger(SQLResponse.class);

    // the data type for each column
    private List<SelectedColumnMeta> columnMetas;

    // the results rows, each row contains several columns
    private List<List<String>> results;

    private String cube;

    // if not select query, only return affected row count
    private int affectedRowCount;

    // flag indicating whether an exception occurred
    private boolean isException;

    // if isException, the detailed exception message
    private String exceptionMessage;

    private long duration;

    private boolean isPartial = false;

    private long totalScanCount;

    private boolean hitCache = false;

    public SQLResponse() {
    }

    public SQLResponse(List<SelectedColumnMeta> columnMetas, List<List<String>> results, int affectedRowCount, boolean isException, String exceptionMessage) {
        this.columnMetas = columnMetas;
        this.results = results;
        this.affectedRowCount = affectedRowCount;
        this.isException = isException;
        this.exceptionMessage = exceptionMessage;
    }

    public SQLResponse(List<SelectedColumnMeta> columnMetas, List<List<String>> results, String cube, int affectedRowCount, boolean isException, String exceptionMessage) {
        this.columnMetas = columnMetas;
        this.results = results;
        this.cube = cube;
        this.affectedRowCount = affectedRowCount;
        this.isException = isException;
        this.exceptionMessage = exceptionMessage;
    }

    public SQLResponse(List<SelectedColumnMeta> columnMetas, List<List<String>> results, String cube, int affectedRowCount, boolean isException, String exceptionMessage, boolean isPartial) {
        this.columnMetas = columnMetas;
        this.results = results;
        this.cube = cube;
        this.affectedRowCount = affectedRowCount;
        this.isException = isException;
        this.exceptionMessage = exceptionMessage;
        this.isPartial = isPartial;
    }

    public List<SelectedColumnMeta> getColumnMetas() {
        return columnMetas;
    }

    public List<List<String>> getResults() {
        return results;
    }

    public void setResults(List<List<String>> results) {
        this.results = results;
    }

    public String getCube() {
        return cube;
    }

    public int getAffectedRowCount() {
        return affectedRowCount;
    }

    public boolean getIsException() {
        return isException;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public boolean isPartial() {

        return isPartial;
    }

    public long getTotalScanCount() {
        return totalScanCount;
    }

    public void setTotalScanCount(long totalScanCount) {
        this.totalScanCount = totalScanCount;
    }

    public boolean isHitCache() {
        return hitCache;
    }

    public void setHitCache(boolean hitCache) {
        this.hitCache = hitCache;
    }

}
