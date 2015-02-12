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

package org.apache.kylin.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.kylin.cube.CubeInstance;
import org.apache.kylin.cube.model.CubeDesc;
import org.apache.kylin.metadata.model.DataModelDesc;
import org.apache.kylin.rest.request.CubeRequest;
import org.apache.kylin.rest.service.CubeService;
import org.apache.kylin.rest.service.JobService;
import org.apache.kylin.rest.service.ServiceTestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

/**
 * @author xduo
 */
public class CubeControllerTest extends ServiceTestBase {

    private CubeController cubeController;
    private CubeDescController cubeDescController;
    private ModelController modelController;

    @Autowired
    CubeService cubeService;
    @Autowired
    JobService jobService;

    @Before
    public void setup() throws Exception {
        super.setUp();

        cubeController = new CubeController();
        cubeController.setCubeService(cubeService);
        cubeController.setJobService(jobService);
        cubeDescController = new CubeDescController();
        cubeDescController.setCubeService(cubeService);

        modelController = new ModelController();
        modelController.setCubeService(cubeService);
    }

    @Test
    public void testBasics() throws IOException {
        CubeDesc[] cubes = (CubeDesc[]) cubeDescController.getCube("test_kylin_cube_with_slr_ready");
        Assert.assertNotNull(cubes);
        Assert.assertNotNull(cubeController.getSql("test_kylin_cube_with_slr_ready", "20130331080000_20131212080000"));
        Assert.assertNotNull(cubeController.getCubes(null, null, 0, 5));

        CubeDesc cube = cubes[0];
        CubeDesc newCube = new CubeDesc();
        String newCubeName = cube.getName() + "_test_save";

        try {
            cubeController.deleteCube(newCubeName);
        } catch (Exception e) {
            // it may not exist, ignore the exception
        }

        newCube.setName(newCubeName);
        newCube.setModelName(cube.getModelName());
        newCube.setModel(cube.getModel());
        newCube.setDimensions(cube.getDimensions());
        newCube.setHBaseMapping(cube.getHBaseMapping());
        newCube.setMeasures(cube.getMeasures());
        newCube.setConfig(cube.getConfig());
        newCube.setRowkey(cube.getRowkey());

        String newModelName = newCubeName + "_model_desc";
        newCube.getModel().setName(newModelName);//generate a random model
        newCube.getModel().setLastModified(0);

        ObjectMapper cubeDescMapper = new ObjectMapper();
        StringWriter cubeDescWriter = new StringWriter();
        cubeDescMapper.writeValue(cubeDescWriter, newCube);

        ObjectMapper modelDescMapper = new ObjectMapper();
        StringWriter modelDescWriter = new StringWriter();
        modelDescMapper.writeValue(modelDescWriter, newCube.getModel());

        CubeRequest cubeRequest = new CubeRequest();
        cubeRequest.setCubeDescData(cubeDescWriter.toString());
        cubeRequest.setModelDescData(modelDescWriter.toString());
        cubeRequest = cubeController.saveCubeDesc(cubeRequest);


        DataModelDesc model = modelController.getModel(newModelName);
        Assert.assertNotNull(model);

        List<String> notifyList = Lists.newArrayList();
        notifyList.add("john@example.com");
        cubeController.updateNotifyList(newCubeName, notifyList);
        cubeController.updateCubeCost(newCubeName, 80);

        List<CubeInstance> cubeInstances = cubeController.getCubes(newCubeName, "default", 1, 0);

        CubeInstance cubeInstance = cubeInstances.get(0);
        Assert.assertTrue(cubeInstance.getDescriptor().getNotifyList().contains("john@example.com"));
        Assert.assertTrue(cubeInstance.getCost() == 80);
        cubeController.deleteCube(newCubeName);
    }

}
