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

import java.io.IOException;

import org.apache.kylin.rest.service.CubeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import org.apache.kylin.common.KylinConfig;
import org.apache.kylin.metadata.MetadataManager;
import org.apache.kylin.metadata.model.DataModelDesc;

/**
 * @author jiazhong
 * 
 */
@Controller
@RequestMapping(value = "/model")
public class ModelController {

    @Autowired
    private CubeService cubeService;

    /**
     * Get detail information of the "Cube ID"
     * 
     * @param cubeDescName
     *            Cube ID
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/{model_name}", method = { RequestMethod.GET })
    @ResponseBody
    public DataModelDesc getModel(@PathVariable String model_name) {
        MetadataManager metaManager= MetadataManager.getInstance(KylinConfig.getInstanceFromEnv());
        DataModelDesc modeDesc = metaManager.getDataModelDesc(model_name);
        return modeDesc;
            
    }

    public void setCubeService(CubeService cubeService) {
        this.cubeService = cubeService;
    }

}
