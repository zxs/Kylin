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

'use strict';


KylinApp.controller('CubeEditCtrl', function ($scope, $q, $routeParams, $location, $templateCache, $interpolate, MessageService, TableService, CubeDescService, CubeService, loadingRequest, SweetAlert,$log,cubeConfig,CubeDescModel,ModelService,MetaModel,TableModel) {
    $scope.cubeConfig = cubeConfig;
    //add or edit ?
    var absUrl = $location.absUrl();
    $scope.cubeMode = absUrl.indexOf("/cubes/add")!=-1?'addNewCube':absUrl.indexOf("/cubes/edit")!=-1?'editExistCube':'default';


    $scope.getColumnsByTable = function (tableName) {
        var temp = [];
        angular.forEach(TableModel.selectProjectTables, function (table) {
            if (table.name == tableName) {
                temp = table.columns;
            }
        });
        return temp;
    };

    $scope.getPartitonColumns = function(tableName){
        var columns = _.filter($scope.getColumnsByTable(tableName),function(column){
            return column.datatype==="date";
        });
        return columns;
    };

    $scope.getColumnType = function (_column,table){
        var columns = $scope.getColumnsByTable(table);
        var type;
        angular.forEach(columns,function(column){
            if(_column === column.name){
                type = column.datatype;
                return;
            }
        });
        return type;
    };

    var ColFamily = function () {
        var index = 1;
        return function () {
            var newColFamily =
            {
                "name": "f" + index,
                "columns": [
                    {
                        "qualifier": "m",
                        "measure_refs": []
                    }
                ]
            };
            index += 1;

            return  newColFamily;
        }
    };


    // ~ Define data
    $scope.state = {
        "cubeSchema": ""
    };

    // ~ init
    if ($scope.isEdit = !!$routeParams.cubeName) {
        CubeDescService.get({cube_name: $routeParams.cubeName}, function (detail) {
            if (detail.length > 0) {
                $scope.cubeMetaFrame = detail[0];
                ModelService.get({model_name: $scope.cubeMetaFrame.model_name}, function (model) {
                    if (model) {
//                        $scope.metaModel = model;
                        MetaModel.setMetaModel(model);
                        $scope.metaModel = MetaModel;

                        //use
                        //convert GMT mills ,to make sure partition date show GMT Date
                        //should run only one time
                        if(model.partition_desc&&model.partition_desc.partition_date_start)
                        {
                            //$scope.metaModel.partition_desc.partition_date_start+=new Date().getTimezoneOffset()*60000;
                            MetaModel.converDateToGMT();
                        }
                    }
                });
                $scope.state.cubeSchema = angular.toJson($scope.cubeMetaFrame, true);
            }
        });

        //
        //            $scope.metaModel = null;

    } else {
        $scope.cubeMetaFrame = CubeDescModel.createNew();
        MetaModel.initModel();
        $scope.metaModel = MetaModel;
        $scope.cubeMetaFrame.project = $scope.projectModel.selectedProject;
        $scope.state.cubeSchema = angular.toJson($scope.cubeMetaFrame, true);
    }

    // ~ public methods
    $scope.aceChanged = function () {
    };

    $scope.aceLoaded = function(){
    };

    $scope.prepareCube = function () {
        // generate column family
        generateColumnFamily();


        if ($scope.metaModel.model.partition_desc.partition_date_column&&($scope.metaModel.model.partition_desc.partition_date_start|$scope.metaModel.model.partition_desc.partition_date_start==0)) {
            var dateStart = new Date($scope.metaModel.model.partition_desc.partition_date_start);
            dateStart = (dateStart.getFullYear() + "-" + (dateStart.getMonth() + 1) + "-" + dateStart.getDate());
            //switch selected time to utc timestamp
            $scope.metaModel.model.partition_desc.partition_date_start = new Date(moment.utc(dateStart, "YYYY-MM-DD").format()).getTime();


            if($scope.metaModel.model.partition_desc.partition_date_column.indexOf(".")==-1){
            $scope.metaModel.model.partition_desc.partition_date_column=$scope.metaModel.model.fact_table+"."+$scope.metaModel.model.partition_desc.partition_date_column;
            }

        }
        //use cubedesc name as model name
        if($scope.metaModel.model.name===""||angular.isUndefined($scope.metaModel.model.name)){
            $scope.metaModel.model.name = $scope.cubeMetaFrame.name;
        }

        //set model ref for cubeDesc
        if($scope.cubeMetaFrame.model_name===""||angular.isUndefined($scope.cubeMetaFrame.model_name)){
            $scope.cubeMetaFrame.model_name = $scope.cubeMetaFrame.name;
        }

        $scope.state.project = $scope.cubeMetaFrame.project;
//        delete $scope.cubeMetaFrame.project;

        $scope.state.cubeSchema = angular.toJson($scope.cubeMetaFrame, true);
        $scope.state.modelSchema = angular.toJson($scope.metaModel.model, true);
    };

    $scope.cubeResultTmpl = function (notification) {
        // Get the static notification template.
        var tmpl = notification.type == 'success' ? 'cubeResultSuccess.html' : 'cubeResultError.html';
        return $interpolate($templateCache.get(tmpl))(notification);
    };

    $scope.saveCube = function (design_form) {

        try {
            angular.fromJson($scope.state.cubeSchema);
        } catch (e) {
            SweetAlert.swal('Oops...', 'Invalid cube json format..', 'error');
            return;
        }

        SweetAlert.swal({
            title: '',
            text: 'Are you sure to save the cube ?',
            type: '',
            showCancelButton: true,
            confirmButtonColor: '#DD6B55',
            confirmButtonText: "Yes",
            closeOnConfirm: true
        }, function(isConfirm) {
            if(isConfirm){
                loadingRequest.show();

                if ($scope.isEdit) {
                    CubeService.update({}, {cubeDescData: $scope.state.cubeSchema,modelDescData:$scope.state.modelSchema, cubeName: $routeParams.cubeName, project: $scope.state.project}, function (request) {
                        if (request.successful) {
                            $scope.state.cubeSchema = request.cubeDescData;
                            MessageService.sendMsg($scope.cubeResultTmpl({'text':'Updated the cube successfully.',type:'success'}), 'success', {}, true, 'top_center');

                            if (design_form) {
                                design_form.$invalid = true;
                            }
                        } else {
                            $scope.saveCubeRollBack();
                            $scope.cubeMetaFrame.project = $scope.state.project;
                                var message =request.message;
                                var msg = !!(message) ? message : 'Failed to take action.';
                                MessageService.sendMsg($scope.cubeResultTmpl({'text':msg,'schema':$scope.state.cubeSchema}), 'error', {}, true, 'top_center');
                        }
                        //end loading
                        loadingRequest.hide();
                    }, function (e) {
                        $scope.saveCubeRollBack();

                        if(e.data&& e.data.exception){
                            var message =e.data.exception;
                            var msg = !!(message) ? message : 'Failed to take action.';
                            MessageService.sendMsg($scope.cubeResultTmpl({'text':msg,'schema':$scope.state.cubeSchema}), 'error', {}, true, 'top_center');
                        } else {
                            MessageService.sendMsg($scope.cubeResultTmpl({'text':'Failed to take action.','schema':$scope.state.cubeSchema}), 'error', {}, true, 'top_center');
                        }
                        loadingRequest.hide();
                    });
                } else {
                    CubeService.save({}, {cubeDescData: $scope.state.cubeSchema,modelDescData:$scope.state.modelSchema, project: $scope.state.project}, function (request) {
                        if(request.successful) {
                            $scope.state.cubeSchema = request.cubeDescData;

                            MessageService.sendMsg($scope.cubeResultTmpl({'text':'Created the cube successfully.',type:'success'}), 'success', {}, true, 'top_center');
                        } else {
                            $scope.saveCubeRollBack();
                            $scope.cubeMetaFrame.project = $scope.state.project;
                            var message =request.message;
                            var msg = !!(message) ? message : 'Failed to take action.';
                            MessageService.sendMsg($scope.cubeResultTmpl({'text':msg,'schema':$scope.state.cubeSchema}), 'error', {}, true, 'top_center');
                        }

                        //end loading
                        loadingRequest.hide();
                    }, function (e) {
                        $scope.saveCubeRollBack();

                        if (e.data && e.data.exception) {
                            var message =e.data.exception;
                            var msg = !!(message) ? message : 'Failed to take action.';
                            MessageService.sendMsg($scope.cubeResultTmpl({'text':msg,'schema':$scope.state.cubeSchema}), 'error', {}, true, 'top_center');
                        } else {
                            MessageService.sendMsg($scope.cubeResultTmpl({'text':"Failed to take action.",'schema':$scope.state.cubeSchema}), 'error', {}, true, 'top_center');
                        }
                        //end loading
                        loadingRequest.hide();

                    });
                }
            }
            else{
                $scope.saveCubeRollBack();
            }
        });
    };

//    reverse the date
    $scope.saveCubeRollBack = function (){
        if($scope.metaModel.model&&($scope.metaModel.model.partition_desc.partition_date_start||$scope.metaModel.model.partition_desc.partition_date_start==0))
        {
            $scope.metaModel.model.partition_desc.partition_date_start+=new Date().getTimezoneOffset()*60000;
        }
    }

    function reGenerateRowKey(){
        $log.log("reGen rowkey & agg group");
        var tmpRowKeyColumns = [];
        var tmpAggregationItems = [];
        var hierarchyItems = [];
        angular.forEach($scope.cubeMetaFrame.dimensions, function (dimension, index) {

            if(dimension.derived&&dimension.derived.length){
                var lookup = _.find($scope.metaModel.model.lookups,function(lookup){return lookup.table==dimension.table});
                angular.forEach(lookup.join.foreign_key, function (fk, index) {
                    for (var i = 0; i < tmpRowKeyColumns.length; i++) {
                        if(tmpRowKeyColumns[i].column == fk)
                            break;
                    }
                    // push to array if no duplicate value
                    if(i == tmpRowKeyColumns.length) {
                        tmpRowKeyColumns.push({
                            "column": fk,
                            "length": 0,
                            "dictionary": "true",
                            "mandatory": false
                        });

                        tmpAggregationItems.push(fk);
                    }
                })

            }
            else if (dimension.column&&dimension.column.length==1) {
                for (var i = 0; i < tmpRowKeyColumns.length; i++) {
                    if(tmpRowKeyColumns[i].column == dimension.column[0])
                        break;
                }
                if(i == tmpRowKeyColumns.length) {
                    tmpRowKeyColumns.push({
                        "column": dimension.column[0],
                        "length": 0,
                        "dictionary": "true",
                        "mandatory": false
                    });
                    tmpAggregationItems.push(dimension.column[0]);
                }
            }
            if(dimension.hierarchy && dimension.column.length){
                angular.forEach(dimension.column, function (hier_column, index) {
                    for (var i = 0; i < tmpRowKeyColumns.length; i++) {
                        if(tmpRowKeyColumns[i].column == hier_column)
                            break;
                    }
                    if(i == tmpRowKeyColumns.length) {
                        tmpRowKeyColumns.push({
                            "column": hier_column,
                            "length": 0,
                            "dictionary": "true",
                            "mandatory": false
                        });
                        tmpAggregationItems.push(hier_column);
                    }
                    if(hierarchyItems.indexOf(hier_column)==-1){
                        hierarchyItems.push(hier_column);
                    }
                });
            }

        });

        var rowkeyColumns = $scope.cubeMetaFrame.rowkey.rowkey_columns;
        var newRowKeyColumns = sortSharedData(rowkeyColumns,tmpRowKeyColumns);
        var increasedColumns = increasedColumn(rowkeyColumns,tmpRowKeyColumns);
        newRowKeyColumns = newRowKeyColumns.concat(increasedColumns);

        //! here get the latest rowkey_columns
        $scope.cubeMetaFrame.rowkey.rowkey_columns = newRowKeyColumns;

        if($scope.cubeMode==="editExistCube") {
            var aggregationGroups = $scope.cubeMetaFrame.rowkey.aggregation_groups;
            // rm unused item from group,will only rm when [edit] dimension
            angular.forEach(aggregationGroups, function (group, index) {
                if (group) {
                    for (var j = 0; j < group.length; j++) {
                        var elemStillExist = false;
                        for (var k = 0; k < tmpAggregationItems.length; k++) {
                            if (group[j] == tmpAggregationItems[k]) {
                                elemStillExist = true;
                                break;
                            }
                        }
                        if (!elemStillExist) {
                            group.splice(j, 1);
                            j--;
                        }
                    }
                    if (!group.length) {
                        aggregationGroups.splice(index, 1);
                        index--;
                    }
                }
                else {
                    aggregationGroups.splice(index, 1);
                    index--;
                }
            });
        }

        if($scope.cubeMode==="addNewCube"){

            var newUniqAggregationItem = [];
            angular.forEach(tmpAggregationItems, function (item, index) {
                if(newUniqAggregationItem.indexOf(item)==-1){
                    newUniqAggregationItem.push(item);
                }
            });

            var unHierarchyItems = increasedData(hierarchyItems,newUniqAggregationItem);
//            hierarchyItems
            var increasedDataGroups = sliceGroupItemToGroups(unHierarchyItems);
            if(hierarchyItems.length){
                increasedDataGroups.push(hierarchyItems);
            }

            //! here get the latest aggregation groups,only effect when add newCube
            $scope.cubeMetaFrame.rowkey.aggregation_groups = increasedDataGroups;
        }
    }

    function sortSharedData(oldArray,tmpArr){
        var newArr = [];
        for(var j=0;j<oldArray.length;j++){
            var unit = oldArray[j];
            for(var k=0;k<tmpArr.length;k++){
                if(unit.column==tmpArr[k].column){
                    newArr.push(unit);
                }
            }
        }
        return newArr;
    }

    function increasedData(oldArray,tmpArr){
        var increasedData = [];
        if(oldArray&&!oldArray.length){
            return   increasedData.concat(tmpArr);
        }

        for(var j=0;j<tmpArr.length;j++){
            var unit = tmpArr[j];
            var exist = false;
            for(var k=0;k<oldArray.length;k++){
                if(unit==oldArray[k]){
                    exist = true;
                    break;
                }
            }
            if(!exist){
                increasedData.push(unit);
            }
        }
        return increasedData;
    }

    function increasedColumn(oldArray,tmpArr){
        var increasedData = [];
        if(oldArray&&!oldArray.length){
         return   increasedData.concat(tmpArr);
        }

        for(var j=0;j<tmpArr.length;j++){
            var unit = tmpArr[j];
            var exist = false;
            for(var k=0;k<oldArray.length;k++){
                if(unit.column==oldArray[k].column){
                    exist = true;
                    break;
                }
            }
            if(!exist){
                increasedData.push(unit);
            }
        }
        return increasedData;
    }

    function sliceGroupItemToGroups(groupItems){
        if(!groupItems.length){
            return [];
        }
        var groups = [];
        var j = -1;
        for(var i = 0;i<groupItems.length;i++){
            if(i%10==0){
                j++;
                groups[j]=[];
            }
            groups[j].push(groupItems[i]);
        }
//        if(groups[groups.length-1].length<10){
//            groups.pop();
//        }
        return groups;
    }


    // ~ private methods
    function generateColumnFamily() {
        $scope.cubeMetaFrame.hbase_mapping.column_family = [];
        var colFamily = ColFamily();
        var normalMeasures = [], distinctCountMeasures=[];
        angular.forEach($scope.cubeMetaFrame.measures, function (measure, index) {
            if(measure.function.expression === 'COUNT_DISTINCT'){
                distinctCountMeasures.push(measure);
            }else{
                normalMeasures.push(measure);
            }
        });
        if(normalMeasures.length>0){
            var nmcf = colFamily();
            angular.forEach(normalMeasures, function(normalM, index){
                nmcf.columns[0].measure_refs.push(normalM.name);
            });
            $scope.cubeMetaFrame.hbase_mapping.column_family.push(nmcf);
        }

        if (distinctCountMeasures.length > 0){
            var dccf = colFamily();
            angular.forEach(distinctCountMeasures, function(dcm, index){
                dccf.columns[0].measure_refs.push(dcm.name);
            });
            $scope.cubeMetaFrame.hbase_mapping.column_family.push(dccf);
        }
    }

    $scope.$watch('projectModel.selectedProject', function (newValue, oldValue) {
        if(!newValue){
            return;
        }
        var param = {
            ext: true,
            project:newValue
        };
        if(newValue){
            TableModel.initTables();
            TableService.list(param, function (tables) {
                angular.forEach(tables, function (table) {
                    table.name = table.database+"."+table.name;
                    TableModel.addTable(table);
                });
            });
        }
    });

    $scope.$on('DimensionsEdited', function (event) {
        if ($scope.cubeMetaFrame) {
            reGenerateRowKey();
        }
    });
});
