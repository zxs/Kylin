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

KylinApp.constant('jobConfig', {
    allStatus : [
        {name: 'NEW', value: 0},
        {name: 'PENDING', value: 1},
        {name: 'RUNNING', value: 2},
        {name: 'FINISHED', value: 4},
        {name: 'ERROR', value: 8},
        {name: 'DISCARDED', value: 16}
    ],
    theaditems : [
        {attr: 'name', name: 'Job Name'},
        {attr: 'related_cube', name: 'Cube'},
        {attr: 'progress', name: 'Progress'},
        {attr: 'last_modified', name: 'Last Modified Time'},
        {attr: 'duration', name: 'Duration'}
    ]

});
