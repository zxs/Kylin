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

KylinApp.service('JobList',function(JobService,$q){
    var _this = this;
    this.jobs=[];

    this.list = function(jobRequest){

        var defer = $q.defer();
        JobService.list(jobRequest, function (jobs) {
            angular.forEach(jobs, function (job) {
                var id = job.uuid;
                if (angular.isDefined(_this.jobs[id])) {
                    if (job.last_modified != _this.jobs[id].last_modified) {
                        _this.jobs[id] = job;
                    } else {
                    }
                } else {
                    _this.jobs[id] = job;
                }
            });

//            $scope.state.loading = false;
//            if (angular.isDefined($scope.state.selectedJob)) {
//                $scope.state.selectedJob = $scope.jobs[selectedJob.uuid];
//            }
            defer.resolve(jobs.length);
        });

        return defer.promise;

    };

//    this.removeCube = function(cube){
//        var cubeIndex = _this.cubes.indexOf(cube);
//        if (cubeIndex > -1) {
//            _this.cubes.splice(cubeIndex, 1);
//        }
//    }

    this.removeAll = function(){
        _this.jobs=[];
    };

});
