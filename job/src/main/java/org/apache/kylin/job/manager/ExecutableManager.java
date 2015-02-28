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

package org.apache.kylin.job.manager;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.apache.kylin.common.KylinConfig;
import org.apache.kylin.common.util.ClassUtil;
import org.apache.kylin.job.dao.ExecutableDao;
import org.apache.kylin.job.dao.ExecutableOutputPO;
import org.apache.kylin.job.dao.ExecutablePO;
import org.apache.kylin.job.exception.IllegalStateTranferException;
import org.apache.kylin.job.exception.PersistentException;
import org.apache.kylin.job.execution.AbstractExecutable;
import org.apache.kylin.job.execution.DefaultChainedExecutable;
import org.apache.kylin.job.execution.DefaultOutput;
import org.apache.kylin.job.execution.ExecutableState;
import org.apache.kylin.job.execution.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Created by qianzhou on 12/16/14.
 */
public class ExecutableManager {

    private static final Logger logger = LoggerFactory.getLogger(ExecutableManager.class);
    private static final ConcurrentHashMap<KylinConfig, ExecutableManager> CACHE = new ConcurrentHashMap<KylinConfig, ExecutableManager>();
    @SuppressWarnings("unused")
    private final KylinConfig config;

    private ExecutableDao executableDao;

    public static ExecutableManager getInstance(KylinConfig config) {
        ExecutableManager r = CACHE.get(config);
        if (r == null) {
            r = new ExecutableManager(config);
            CACHE.put(config, r);
            if (CACHE.size() > 1) {
                logger.warn("More than one singleton exist");
            }

        }
        return r;
    }

    private ExecutableManager(KylinConfig config) {
        logger.info("Using metadata url: " + config);
        this.config = config;
        this.executableDao = ExecutableDao.getInstance(config);
    }

    public void addJob(AbstractExecutable executable) {
        try {
            executableDao.addJob(parse(executable));
            addJobOutput(executable);
        } catch (PersistentException e) {
            logger.error("fail to submit job:" + executable.getId(), e);
            throw new RuntimeException(e);
        }
    }

    private void addJobOutput(AbstractExecutable executable) throws PersistentException {
        ExecutableOutputPO executableOutputPO = new ExecutableOutputPO();
        executableOutputPO.setUuid(executable.getId());
        executableDao.addJobOutput(executableOutputPO);
        if (executable instanceof DefaultChainedExecutable) {
            for (AbstractExecutable subTask: ((DefaultChainedExecutable) executable).getTasks()) {
                addJobOutput(subTask);
            }
        }
    }

    //for ut
    public void deleteJob(String jobId) {
        try {
            executableDao.deleteJob(jobId);
        } catch (PersistentException e) {
            logger.error("fail to delete job:" + jobId, e);
            throw new RuntimeException(e);
        }
    }

    public AbstractExecutable getJob(String uuid) {
        try {
            return parseTo(executableDao.getJob(uuid));
        } catch (PersistentException e) {
            logger.error("fail to get job:" + uuid, e);
            throw new RuntimeException(e);
        }
    }

    public Output getOutput(String uuid) {
        try {
            final ExecutableOutputPO jobOutput = executableDao.getJobOutput(uuid);
            Preconditions.checkArgument(jobOutput != null, "there is no related output for job id:" + uuid);
            final DefaultOutput result = new DefaultOutput();
            result.setExtra(jobOutput.getInfo());
            result.setState(ExecutableState.valueOf(jobOutput.getStatus()));
            result.setVerboseMsg(jobOutput.getContent());
            result.setLastModified(jobOutput.getLastModified());
            return result;
        } catch (PersistentException e) {
            logger.error("fail to get job output:" + uuid, e);
            throw new RuntimeException(e);
        }
    }

    public List<AbstractExecutable> getAllExecutables() {
        try {
            return Lists.transform(executableDao.getJobs(), new Function<ExecutablePO, AbstractExecutable>() {
                @Nullable
                @Override
                public AbstractExecutable apply(ExecutablePO input) {
                        return parseTo(input);
                }
            });
        } catch (PersistentException e) {
            logger.error("error get All Jobs", e);
            throw new RuntimeException(e);
        }
    }

    public List<String> getAllJobIds() {
        try {
            return executableDao.getJobIds();
        } catch (PersistentException e) {
            logger.error("error get All Job Ids", e);
            throw new RuntimeException(e);
        }
    }

    public void updateAllRunningJobsToError() {
        try {
            final List<ExecutableOutputPO> jobOutputs = executableDao.getJobOutputs();
            for (ExecutableOutputPO executableOutputPO : jobOutputs) {
                if (executableOutputPO.getStatus().equalsIgnoreCase(ExecutableState.RUNNING.toString())) {
                    executableOutputPO.setStatus(ExecutableState.ERROR.toString());
                    executableDao.updateJobOutput(executableOutputPO);
                }
            }
        } catch (PersistentException e) {
            logger.error("error reset job status from RUNNING to ERROR", e);
            throw new RuntimeException(e);
        }
    }

    public void resumeJob(String jobId) {
        AbstractExecutable job = getJob(jobId);
        if (job == null) {
            return;
        }
        updateJobOutput(jobId, ExecutableState.READY, null, null);
        if (job instanceof DefaultChainedExecutable) {
            List<AbstractExecutable> tasks = ((DefaultChainedExecutable) job).getTasks();
            for (AbstractExecutable task : tasks) {
                if (task.getStatus() == ExecutableState.ERROR) {
                    updateJobOutput(task.getId(), ExecutableState.READY, null, null);
                    break;
                }
            }
        }
    }

    public void discardJob(String jobId) {
        AbstractExecutable job = getJob(jobId);
        if (job instanceof DefaultChainedExecutable) {
            List<AbstractExecutable> tasks = ((DefaultChainedExecutable) job).getTasks();
            for (AbstractExecutable task : tasks) {
                if (!task.getStatus().isFinalState()) {
                    updateJobOutput(task.getId(), ExecutableState.DISCARDED, null, null);
                }
            }
        }
        updateJobOutput(jobId, ExecutableState.DISCARDED, null, null);
    }

    public void updateJobOutput(String jobId, ExecutableState newStatus, Map<String, String> info, String output) {
        try {
            final ExecutableOutputPO jobOutput = executableDao.getJobOutput(jobId);
            Preconditions.checkArgument(jobOutput != null, "there is no related output for job id:" + jobId);
            ExecutableState oldStatus = ExecutableState.valueOf(jobOutput.getStatus());
            if (newStatus != null && oldStatus != newStatus) {
                if (!ExecutableState.isValidStateTransfer(oldStatus, newStatus)) {
                    throw new IllegalStateTranferException("there is no valid state transfer from:" + oldStatus + " to:" + newStatus);
                }
                jobOutput.setStatus(newStatus.toString());
            }
            if (info != null) {
                jobOutput.setInfo(info);
            }
            if (output != null) {
                jobOutput.setContent(output);
            }
            executableDao.updateJobOutput(jobOutput);
            logger.info("job id:" + jobId + " from " + oldStatus + " to " + newStatus);
        } catch (PersistentException e) {
            logger.error("error change job:" + jobId + " to " + newStatus.toString());
            throw new RuntimeException(e);
        }
    }

    //for migration only
    //TODO delete when migration finished
    public void resetJobOutput(String jobId, ExecutableState state, String output) {
        try {
            final ExecutableOutputPO jobOutput = executableDao.getJobOutput(jobId);
            jobOutput.setStatus(state.toString());
            if (output != null) {
                jobOutput.setContent(output);
            }
            executableDao.updateJobOutput(jobOutput);
        } catch (PersistentException e) {
            throw new RuntimeException(e);
        }
    }

    public void addJobInfo(String id, Map<String, String> info) {
        if (info == null) {
            return;
        }
        try {
            ExecutableOutputPO output = executableDao.getJobOutput(id);
            Preconditions.checkArgument(output != null, "there is no related output for job id:" + id);
            output.getInfo().putAll(info);
            executableDao.updateJobOutput(output);
        } catch (PersistentException e) {
            logger.error("error update job info, id:" + id + "  info:" + info.toString());
            throw new RuntimeException(e);
        }
    }

    public void addJobInfo(String id, String key, String value) {
        Map<String, String> info = Maps.newHashMap();
        info.put(key, value);
        addJobInfo(id, info);
    }

    private static ExecutablePO parse(AbstractExecutable executable) {
        ExecutablePO result = new ExecutablePO();
        result.setName(executable.getName());
        result.setUuid(executable.getId());
        result.setType(executable.getClass().getName());
        result.setParams(executable.getParams());
        if (executable instanceof DefaultChainedExecutable) {
            List<ExecutablePO> tasks = Lists.newArrayList();
            for (AbstractExecutable task : ((DefaultChainedExecutable) executable).getTasks()) {
                tasks.add(parse(task));
            }
            result.setTasks(tasks);
        }
        return result;
    }

    private static AbstractExecutable parseTo(ExecutablePO executablePO) {
        if (executablePO == null) {
            return null;
        }
        String type = executablePO.getType();
        try {
            Class<? extends AbstractExecutable> clazz = ClassUtil.forName(type, AbstractExecutable.class);
            Constructor<? extends AbstractExecutable> constructor = clazz.getConstructor();
            AbstractExecutable result = constructor.newInstance();
            result.setId(executablePO.getUuid());
            result.setName(executablePO.getName());
            result.setParams(executablePO.getParams());
            List<ExecutablePO> tasks = executablePO.getTasks();
            if (tasks != null && !tasks.isEmpty()) {
                Preconditions.checkArgument(result instanceof DefaultChainedExecutable);
                for (ExecutablePO subTask: tasks) {
                    ((DefaultChainedExecutable) result).addTask(parseTo(subTask));
                }
            }
            return result;
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("cannot parse this job:" + executablePO.getId(), e);
        }
    }

}
