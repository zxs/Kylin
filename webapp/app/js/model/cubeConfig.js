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

KylinApp.constant('cubeConfig', {

    //~ Define metadata & class
    measureParamType : ['column', 'constant'],
    measureExpressions : ['SUM', 'MIN', 'MAX', 'COUNT', 'COUNT_DISTINCT'],
    dimensionDataTypes : ["string", "tinyint", "int", "bigint", "date"],
    cubeCapacities : ["SMALL", "MEDIUM","LARGE"],
//    cubePartitionTypes : ['APPEND', 'UPDATE_INSERT'],
    cubePartitionTypes : ['APPEND'],
    joinTypes : [
        {name: 'Left', value: 'left'},
        {name: 'Inner', value: 'inner'},
        {name: 'Right', value: 'right'}
    ],
    queryPriorities : [
        {name: 'NORMAL', value: 50},
        {name: 'LOW', value: 70},
        {name: 'HIGH', value: 30}
    ],
    measureDataTypes : [
        {name: 'INT', value: 'int'},
        {name: 'BIGINT', value: 'bigint'},
        {name: 'DECIMAL', value: 'decimal'},
        {name: 'DOUBLE', value: 'double'},
        {name: 'DATE', value: 'date'},
        {name: 'STRING', value: 'string'}
    ],
    distinctDataTypes : [
        {name: 'Error Rate < 9.75%', value: 'hllc10'},
        {name: 'Error Rate < 4.88%', value: 'hllc12'},
        {name: 'Error Rate < 2.44%', value: 'hllc14'},
        {name: 'Error Rate < 1.72%', value: 'hllc15'},
        {name: 'Error Rate < 1.22%', value: 'hllc16'}
    ],
    dftSelections : {
        measureExpression: 'SUM',
        measureParamType: 'column',
        measureDataType: {name: 'BIGINT', value: 'bigint'},
        distinctDataType: {name: 'Error Rate < 2.44%', value: 'hllc14'},
        cubeCapacity: 'MEDIUM',
        queryPriority: {name: 'NORMAL', value: 50},
        cubePartitionType: 'APPEND'
    },
    dictionaries : ["true", "false"],

//    cubes config
    theaditems : [
    {attr: 'name', name: 'Name'},
    {attr: 'status', name: 'Status'},
    {attr: 'size_kb', name: 'Cube Size'},
    {attr: 'source_records_count', name: 'Source Records'},
    {attr: 'last_build_time', name: 'Last Build Time'},
    {attr: 'owner', name: 'Owner'},
    {attr: 'create_time', name: 'Create Time'}
     ]
    });
