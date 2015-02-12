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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.kylin.rest.exception.InternalErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import org.apache.kylin.common.KylinConfig;
import org.apache.kylin.job.JobInstance;
import org.apache.kylin.job.constant.JobStatusEnum;
import org.apache.kylin.job.engine.JobEngineConfig;
import org.apache.kylin.job.impl.threadpool.DefaultScheduler;
import org.apache.kylin.rest.constant.Constant;
import org.apache.kylin.rest.request.JobListRequest;
import org.apache.kylin.rest.service.JobService;

/**
 * @author ysong1
 * @author Jack
 * 
 */
@Controller
@RequestMapping(value = "jobs")
public class JobController extends BasicController implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(JobController.class);

    @Autowired
    private JobService jobService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        String timeZone = jobService.getKylinConfig().getTimeZone();
        TimeZone tzone = TimeZone.getTimeZone(timeZone);
        TimeZone.setDefault(tzone);

        final KylinConfig kylinConfig = KylinConfig.getInstanceFromEnv();
        String serverMode = kylinConfig.getServerMode();

        if (Constant.SERVER_MODE_JOB.equals(serverMode.toLowerCase()) || Constant.SERVER_MODE_ALL.equals(serverMode.toLowerCase())) {
            logger.info("Initializing Job Engine ....");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        DefaultScheduler scheduler = DefaultScheduler.getInstance();
                        scheduler.init(new JobEngineConfig(kylinConfig));
                        if (!scheduler.hasStarted()) {
                            logger.error("scheduler has not been started");
                            System.exit(1);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }
    }

    /**
     * get all cube jobs
     * 
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "", method = { RequestMethod.GET })
    @ResponseBody
    public List<JobInstance> list(JobListRequest jobRequest) {

        List<JobInstance> jobInstanceList = Collections.emptyList();
        List<JobStatusEnum> statusList = new ArrayList<JobStatusEnum>();

        if (null != jobRequest.getStatus()) {
            for (int status : jobRequest.getStatus()) {
                statusList.add(JobStatusEnum.getByCode(status));
            }
        }

        try {
            jobInstanceList = jobService.listAllJobs(jobRequest.getCubeName(), jobRequest.getProjectName(), statusList, jobRequest.getLimit(), jobRequest.getOffset());
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new InternalErrorException(e);
        }
        return jobInstanceList;
    }

    /**
     * Get a cube job
     * 
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/{jobId}", method = { RequestMethod.GET })
    @ResponseBody
    public JobInstance get(@PathVariable String jobId) {
        JobInstance jobInstance = null;
        try {
            jobInstance = jobService.getJobInstance(jobId);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new InternalErrorException(e);
        }

        return jobInstance;
    }

    /**
     * Get a job step output
     * 
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/{jobId}/steps/{stepId}/output", method = { RequestMethod.GET })
    @ResponseBody
    public Map<String, String> getStepOutput(@PathVariable String jobId, @PathVariable String stepId) {
        Map<String, String> result = new HashMap<String, String>();
        result.put("jobId", jobId);
        result.put("stepId", String.valueOf(stepId));
        result.put("cmd_output", jobService.getExecutableManager().getOutput(stepId).getVerboseMsg());
        return result;
    }

    /**
     * Resume a cube job
     * 
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/{jobId}/resume", method = { RequestMethod.PUT })
    @ResponseBody
    public JobInstance resume(@PathVariable String jobId) {
        try {
            jobService.resumeJob(jobId);
            return jobService.getJobInstance(jobId);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new InternalErrorException(e);
        }
    }

    /**
     * Cancel a job
     * 
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/{jobId}/cancel", method = { RequestMethod.PUT })
    @ResponseBody
    public JobInstance cancel(@PathVariable String jobId) {

        try {
            return jobService.cancelJob(jobId);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new InternalErrorException(e);
        }

    }

    public void setJobService(JobService jobService) {
        this.jobService = jobService;
    }

}
