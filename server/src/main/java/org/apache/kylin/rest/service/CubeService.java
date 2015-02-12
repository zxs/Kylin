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

package org.apache.kylin.rest.service;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.kylin.common.KylinConfig;
import org.apache.kylin.common.util.HBaseRegionSizeCalculator;
import org.apache.kylin.common.util.HadoopUtil;
import org.apache.kylin.cube.CubeInstance;
import org.apache.kylin.cube.CubeManager;
import org.apache.kylin.cube.CubeSegment;
import org.apache.kylin.cube.cuboid.CuboidCLI;
import org.apache.kylin.cube.model.CubeDesc;
import org.apache.kylin.job.common.HadoopShellExecutable;
import org.apache.kylin.job.cube.CubingJob;
import org.apache.kylin.job.exception.JobException;
import org.apache.kylin.job.execution.DefaultChainedExecutable;
import org.apache.kylin.job.execution.ExecutableState;
import org.apache.kylin.job.hadoop.cardinality.HiveColumnCardinalityJob;
import org.apache.kylin.job.hadoop.cardinality.HiveColumnCardinalityUpdateJob;
import org.apache.kylin.metadata.MetadataConstants;
import org.apache.kylin.metadata.MetadataManager;
import org.apache.kylin.metadata.model.SegmentStatusEnum;
import org.apache.kylin.metadata.model.TableDesc;
import org.apache.kylin.metadata.project.ProjectInstance;
import org.apache.kylin.metadata.project.ProjectManager;
import org.apache.kylin.metadata.project.RealizationEntry;
import org.apache.kylin.metadata.realization.RealizationStatusEnum;
import org.apache.kylin.metadata.realization.RealizationType;
import org.apache.kylin.metadata.tool.HiveSourceTableLoader;
import org.apache.kylin.rest.constant.Constant;
import org.apache.kylin.rest.controller.QueryController;
import org.apache.kylin.rest.exception.InternalErrorException;
import org.apache.kylin.rest.request.MetricsRequest;
import org.apache.kylin.rest.response.HBaseResponse;
import org.apache.kylin.rest.response.MetricsResponse;
import org.apache.kylin.rest.security.AclPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Stateless & lightweight service facade of cube management functions.
 *
 * @author yangli9
 */
@Component("cubeMgmtService")
public class CubeService extends BasicService {
    private static final String DESC_SUFFIX = "_desc";

    private static final Logger logger = LoggerFactory.getLogger(CubeService.class);

    @Autowired
    private AccessService accessService;

    @PostFilter(Constant.ACCESS_POST_FILTER_READ)
    public List<CubeInstance> listAllCubes(final String cubeName, final String projectName) {
        List<CubeInstance> cubeInstances = null;
        ProjectInstance project = (null != projectName) ? getProjectManager().getProject(projectName) : null;

        if (null == project) {
            cubeInstances = getCubeManager().listAllCubes();
        } else {
            cubeInstances = listAllCubes(projectName);
        }

        List<CubeInstance> filterCubes = new ArrayList<CubeInstance>();
        for (CubeInstance cubeInstance : cubeInstances) {
            boolean isCubeMatch = (null == cubeName) || cubeInstance.getName().toLowerCase().contains(cubeName.toLowerCase());

            if (isCubeMatch) {
                filterCubes.add(cubeInstance);
            }
        }

        return filterCubes;
    }

    public List<CubeInstance> getCubes(final String cubeName, final String projectName, final Integer limit, final Integer offset) {
        int climit = (null == limit) ? 30 : limit;
        int coffset = (null == offset) ? 0 : offset;

        List<CubeInstance> cubes;
        cubes = listAllCubes(cubeName, projectName);

        if (cubes.size() <= coffset) {
            return Collections.emptyList();
        }

        if ((cubes.size() - coffset) < climit) {
            return cubes.subList(coffset, cubes.size());
        }

        return cubes.subList(coffset, coffset + climit);
    }

    @PreAuthorize(Constant.ACCESS_HAS_ROLE_ADMIN)
    public CubeInstance updateCubeCost(String cubeName, int cost) throws IOException {
        CubeInstance cube = getCubeManager().getCube(cubeName);
        if (cube == null) {
            throw new IOException("Cannot find cube " + cubeName);
        }
        if (cube.getCost() == cost) {
            // Do nothing
            return cube;
        }
        cube.setCost(cost);

        String owner = SecurityContextHolder.getContext().getAuthentication().getName();
        cube.setOwner(owner);

        return getCubeManager().updateCube(cube);
    }

    public CubeInstance createCubeAndDesc(String cubeName, String projectName, CubeDesc desc) throws IOException {
        if (getCubeManager().getCube(cubeName) != null) {
            throw new InternalErrorException("The cube named " + cubeName + " already exists");
        }

        String owner = SecurityContextHolder.getContext().getAuthentication().getName();
        CubeDesc createdDesc = null;
        CubeInstance createdCube = null;

        boolean isNew = false;
        if (getCubeDescManager().getCubeDesc(desc.getName()) == null) {
            createdDesc = getCubeDescManager().createCubeDesc(desc);
            isNew = true;
        } else {
            createdDesc = getCubeDescManager().updateCubeDesc(desc);
        }


        if (!createdDesc.getError().isEmpty()) {
            if (isNew) {
                getCubeDescManager().removeCubeDesc(createdDesc);
            }
            throw new InternalErrorException(createdDesc.getError().get(0));
        }

        try {
            int cuboidCount = CuboidCLI.simulateCuboidGeneration(createdDesc);
            logger.info("New cube " + cubeName + " has " + cuboidCount + " cuboids");
        } catch (Exception e) {
            getCubeDescManager().removeCubeDesc(createdDesc);
            throw new InternalErrorException("Failed to deal with the request.", e);
        }

        createdCube = getCubeManager().createCube(cubeName, projectName, createdDesc, owner);
        accessService.init(createdCube, AclPermission.ADMINISTRATION);

        ProjectInstance project = getProjectManager().getProject(projectName);
        accessService.inherit(createdCube, project);

        return createdCube;
    }

    private List<CubeInstance> listAllCubes(String projectName) {
        ProjectManager projectManager = getProjectManager();
        ProjectInstance project = projectManager.getProject(projectName);
        if (project == null) {
            return Collections.emptyList();
        }
        ArrayList<CubeInstance> result = new ArrayList<CubeInstance>();
        for (RealizationEntry projectDataModel : project.getRealizationEntries()) {
            if (projectDataModel.getType() == RealizationType.CUBE) {
                CubeInstance cube = getCubeManager().getCube(projectDataModel.getRealization());
                assert cube != null;
                result.add(cube);
            }
        }
        return result;
    }

    private boolean isCubeInProject(String projectName, CubeInstance target) {
        ProjectManager projectManager = getProjectManager();
        ProjectInstance project = projectManager.getProject(projectName);
        if (project == null) {
            return false;
        }
        for (RealizationEntry projectDataModel : project.getRealizationEntries()) {
            if (projectDataModel.getType() == RealizationType.CUBE) {
                CubeInstance cube = getCubeManager().getCube(projectDataModel.getRealization());
                assert cube != null;
                if (cube.equals(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    @PreAuthorize(Constant.ACCESS_HAS_ROLE_ADMIN + " or hasPermission(#cube, 'ADMINISTRATION') or hasPermission(#cube, 'MANAGEMENT')")
    public CubeDesc updateCubeAndDesc(CubeInstance cube, CubeDesc desc, String newProjectName) throws UnknownHostException, IOException, JobException {
        final List<CubingJob> cubingJobs = listAllCubingJobs(cube.getName(), null, EnumSet.of(ExecutableState.READY, ExecutableState.RUNNING));
        if (!cubingJobs.isEmpty()) {
            throw new JobException("Cube schema shouldn't be changed with running job.");
        }

        try {
            if (!cube.getDescriptor().calculateSignature().equals(cube.getDescriptor().getSignature())) {
                this.releaseAllSegments(cube);
            }

            CubeDesc updatedCubeDesc = getCubeDescManager().updateCubeDesc(desc);

            int cuboidCount = CuboidCLI.simulateCuboidGeneration(updatedCubeDesc);
            logger.info("Updated cube " + cube.getName() + " has " + cuboidCount + " cuboids");

            ProjectManager projectManager = getProjectManager();
            if (!isCubeInProject(newProjectName, cube)) {
                String owner = SecurityContextHolder.getContext().getAuthentication().getName();
                ProjectInstance newProject = projectManager.moveRealizationToProject(RealizationType.CUBE, cube.getName(), newProjectName, owner);
                accessService.inherit(cube, newProject);
            }

            return updatedCubeDesc;
        } catch (IOException e) {
            throw new InternalErrorException("Failed to deal with the request.", e);
        }
    }

    @PreAuthorize(Constant.ACCESS_HAS_ROLE_ADMIN + " or hasPermission(#cube, 'ADMINISTRATION') or hasPermission(#cube, 'MANAGEMENT')")
    public void deleteCube(CubeInstance cube) throws IOException, JobException {
        final List<CubingJob> cubingJobs = listAllCubingJobs(cube.getName(), null, EnumSet.of(ExecutableState.READY, ExecutableState.RUNNING));
        if (!cubingJobs.isEmpty()) {
            throw new JobException("The cube " + cube.getName() + " has running job, please discard it and try again.");
        }

        this.releaseAllSegments(cube);
        getCubeManager().dropCube(cube.getName(), true);
        accessService.clean(cube, true);
    }

    public boolean isCubeEditable(CubeInstance ci) {
        return ci.getStatus() == RealizationStatusEnum.DISABLED;
    }

    public boolean isCubeDescEditable(CubeDesc cd) {
        List<CubeInstance> list = getCubeManager().getCubesByDesc(cd.getName());
        if (list.isEmpty()) {
            return true;
        }
        Iterator<CubeInstance> it = list.iterator();
        while (it.hasNext()) {
            if (!isCubeEditable(it.next())) {
                return false;
            }
        }
        return true;
    }

    public static String getCubeDescNameFromCube(String cubeName) {
        return cubeName + DESC_SUFFIX;
    }

    public static String getCubeNameFromDesc(String descName) {
        if (descName.toLowerCase().endsWith(DESC_SUFFIX)) {
            return descName.substring(0, descName.toLowerCase().indexOf(DESC_SUFFIX));
        } else {
            return descName;
        }
    }

    public void reloadCubeCache(String cubeName) {
        CubeManager.getInstance(this.getConfig()).loadCubeCache(cubeName);
    }

    public void removeCubeCache(String cubeName) {
        CubeManager.getInstance(this.getConfig()).removeCubeCacheLocal(cubeName);
    }

    /**
     * Stop all jobs belonging to this cube and clean out all segments
     *
     * @param cube
     * @return
     * @throws IOException
     * @throws CubeIntegrityException
     * @throws JobException
     */
    @PreAuthorize(Constant.ACCESS_HAS_ROLE_ADMIN + " or hasPermission(#cube, 'ADMINISTRATION') or hasPermission(#cube, 'OPERATION') or hasPermission(#cube, 'MANAGEMENT')")
    @Caching(evict = {@CacheEvict(value = QueryController.SUCCESS_QUERY_CACHE, allEntries = true), @CacheEvict(value = QueryController.EXCEPTION_QUERY_CACHE, allEntries = true)})
    public CubeInstance purgeCube(CubeInstance cube) throws IOException, JobException {
        String cubeName = cube.getName();

        RealizationStatusEnum ostatus = cube.getStatus();
        if (null != ostatus && !RealizationStatusEnum.DISABLED.equals(ostatus)) {
            throw new InternalErrorException("Only disabled cube can be purged, status of " + cubeName + " is " + ostatus);
        }

        try {
            this.releaseAllSegments(cube);
            return cube;
        } catch (IOException e) {
            throw e;
        }

    }

    /**
     * Update a cube status from ready to disabled.
     *
     * @return
     * @throws CubeIntegrityException
     * @throws IOException
     * @throws JobException
     */
    @PreAuthorize(Constant.ACCESS_HAS_ROLE_ADMIN + " or hasPermission(#cube, 'ADMINISTRATION') or hasPermission(#cube, 'OPERATION') or hasPermission(#cube, 'MANAGEMENT')")
    @Caching(evict = {@CacheEvict(value = QueryController.SUCCESS_QUERY_CACHE, allEntries = true), @CacheEvict(value = QueryController.EXCEPTION_QUERY_CACHE, allEntries = true)})
    public CubeInstance disableCube(CubeInstance cube) throws IOException, JobException {
        String cubeName = cube.getName();

        RealizationStatusEnum ostatus = cube.getStatus();
        if (null != ostatus && !RealizationStatusEnum.READY.equals(ostatus)) {
            throw new InternalErrorException("Only ready cube can be disabled, status of " + cubeName + " is " + ostatus);
        }

        cube.setStatus(RealizationStatusEnum.DISABLED);

        try {
            return getCubeManager().updateCube(cube);
        } catch (IOException e) {
            cube.setStatus(ostatus);
            throw e;
        }
    }

    /**
     * Update a cube status from disable to ready.
     *
     * @return
     * @throws CubeIntegrityException
     * @throws IOException
     * @throws JobException
     */
    @PreAuthorize(Constant.ACCESS_HAS_ROLE_ADMIN + " or hasPermission(#cube, 'ADMINISTRATION') or hasPermission(#cube, 'OPERATION')  or hasPermission(#cube, 'MANAGEMENT')")
    public CubeInstance enableCube(CubeInstance cube) throws IOException, JobException {
        String cubeName = cube.getName();

        RealizationStatusEnum ostatus = cube.getStatus();
        if (!cube.getStatus().equals(RealizationStatusEnum.DISABLED)) {
            throw new InternalErrorException("Only disabled cube can be enabled, status of " + cubeName + " is " + ostatus);
        }

        if (cube.getSegments(SegmentStatusEnum.READY).size() == 0) {
            throw new InternalErrorException("Cube " + cubeName + " dosen't contain any READY segment");
        }

        final List<CubingJob> cubingJobs = listAllCubingJobs(cube.getName(), null, EnumSet.of(ExecutableState.READY, ExecutableState.RUNNING));
        if (!cubingJobs.isEmpty()) {
            throw new JobException("Enable is not allowed with a running job.");
        }
        if (!cube.getDescriptor().calculateSignature().equals(cube.getDescriptor().getSignature())) {
            this.releaseAllSegments(cube);
        }

        cube.setStatus(RealizationStatusEnum.READY);
        try {
            return getCubeManager().updateCube(cube);
        } catch (IOException e) {
            cube.setStatus(ostatus);
            throw e;
        }
    }

    public MetricsResponse calculateMetrics(MetricsRequest request) {
        List<CubeInstance> cubes = this.getCubeManager().listAllCubes();
        MetricsResponse metrics = new MetricsResponse();
        Date startTime = (null == request.getStartTime()) ? new Date(-1) : request.getStartTime();
        Date endTime = (null == request.getEndTime()) ? new Date() : request.getEndTime();
        metrics.increase("totalCubes", (float) 0);
        metrics.increase("totalStorage", (float) 0);

        for (CubeInstance cube : cubes) {
            Date createdDate = new Date(-1);
            createdDate = (cube.getCreateTimeUTC() == 0) ? createdDate : new Date(cube.getCreateTimeUTC());

            if (createdDate.getTime() > startTime.getTime() && createdDate.getTime() < endTime.getTime()) {
                metrics.increase("totalCubes");
            }
        }

        metrics.increase("aveStorage", (metrics.get("totalCubes") == 0) ? 0 : metrics.get("totalStorage") / metrics.get("totalCubes"));

        return metrics;
    }

    /**
     * Calculate size of each region for given table and other info of the
     * table.
     *
     * @param tableName The table name.
     * @return The HBaseResponse object contains table size, region count. null
     * if error happens.
     * @throws IOException Exception when HTable resource is not closed correctly.
     */
    public HBaseResponse getHTableInfo(String tableName) throws IOException {
        // Get HBase storage conf.
        String hbaseUrl = KylinConfig.getInstanceFromEnv().getStorageUrl();
        Configuration hconf = HadoopUtil.newHBaseConfiguration(hbaseUrl);

        HTable table = null;
        HBaseResponse hr = null;
        long tableSize = 0;
        int regionCount = 0;

        try {
            table = new HTable(hconf, tableName);

            HBaseRegionSizeCalculator cal = new HBaseRegionSizeCalculator(table);
            Map<byte[], Long> sizeMap = cal.getRegionSizeMap();

            for (long s : sizeMap.values()) {
                tableSize += s;
            }

            regionCount = sizeMap.size();

            // Set response.
            hr = new HBaseResponse();
            hr.setTableSize(tableSize);
            hr.setRegionCount(regionCount);
        } finally {
            if (null != table) {
                table.close();
            }
        }

        return hr;
    }

    /**
     * Generate cardinality for table This will trigger a hadoop job
     * The result will be merged into table exd info
     *
     * @param tableName
     */
    public void calculateCardinality(String tableName, String submitter) {
        String[] dbTableName = HadoopUtil.parseHiveTableName(tableName);
        tableName = dbTableName[0] + "." + dbTableName[1];
        TableDesc table = getMetadataManager().getTableDesc(tableName);
        final Map<String, String> tableExd = getMetadataManager().getTableDescExd(tableName);
        if (tableExd == null || table == null) {
            IllegalArgumentException e = new IllegalArgumentException("Cannot find table descirptor " + tableName);
            logger.error("Cannot find table descirptor " + tableName, e);
            throw e;
        }

        DefaultChainedExecutable job = new DefaultChainedExecutable();
        job.setName("Hive Column Cardinality calculation for table '" + tableName + "'");
        job.setSubmitter(submitter);

        String outPath = HiveColumnCardinalityJob.OUTPUT_PATH + "/" + tableName;
        String param = "-table " + tableName + " -output " + outPath;

        HadoopShellExecutable step1 = new HadoopShellExecutable();

        step1.setJobClass(HiveColumnCardinalityJob.class);
        step1.setJobParams(param);

        job.addTask(step1);

        HadoopShellExecutable step2 = new HadoopShellExecutable();

        step2.setJobClass(HiveColumnCardinalityUpdateJob.class);
        step2.setJobParams(param);
        job.addTask(step2);

        getExecutableManager().addJob(job);
    }


    @PreAuthorize(Constant.ACCESS_HAS_ROLE_ADMIN + " or hasPermission(#cube, 'ADMINISTRATION') or hasPermission(#cube, 'OPERATION')  or hasPermission(#cube, 'MANAGEMENT')")
    public void updateCubeNotifyList(CubeInstance cube, List<String> notifyList) throws IOException {
        CubeDesc desc = cube.getDescriptor();
        desc.setNotifyList(notifyList);
        getCubeDescManager().updateCubeDesc(desc);
    }

    public CubeInstance rebuildLookupSnapshot(String cubeName, String segmentName, String lookupTable) throws IOException {
        CubeManager cubeMgr = getCubeManager();
        CubeInstance cube = cubeMgr.getCube(cubeName);
        CubeSegment seg = cube.getSegment(segmentName, SegmentStatusEnum.READY);
        cubeMgr.buildSnapshotTable(seg, lookupTable);

        return cube;
    }

    /**
     * purge the cube
     *
     * @throws IOException
     * @throws JobException
     * @throws CubeIntegrityException
     */
    private void releaseAllSegments(CubeInstance cube) throws IOException, JobException {
        final List<CubingJob> cubingJobs = listAllCubingJobs(cube.getName(), null);
        for (CubingJob cubingJob : cubingJobs) {
            final ExecutableState status = cubingJob.getStatus();
            if (status != ExecutableState.SUCCEED && status != ExecutableState.STOPPED && status != ExecutableState.DISCARDED) {
                getExecutableManager().discardJob(cubingJob.getId());
            }
        }
        cube.getSegments().clear();
        CubeManager.getInstance(getConfig()).updateCube(cube);
    }

    @PreAuthorize(Constant.ACCESS_HAS_ROLE_MODELER + " or " + Constant.ACCESS_HAS_ROLE_ADMIN)
    public String[] reloadHiveTable(String tables) throws IOException {
        Set<String> loaded = HiveSourceTableLoader.reloadHiveTables(tables.split(","), getConfig());
        return (String[]) loaded.toArray(new String[loaded.size()]);
    }

    @PreAuthorize(Constant.ACCESS_HAS_ROLE_ADMIN)
    public void syncTableToProject(String[] tables, String project) throws IOException {
        getProjectManager().addTableDescToProject(tables, project);
    }


    @PreAuthorize(Constant.ACCESS_HAS_ROLE_MODELER + " or " + Constant.ACCESS_HAS_ROLE_ADMIN)
    public void calculateCardinalityIfNotPresent(String[] tables, String submitter) throws IOException {
        MetadataManager metaMgr = getMetadataManager();
        for (String table : tables) {
            Map<String, String> exdMap = metaMgr.getTableDescExd(table);
            if (exdMap == null || !exdMap.containsKey(MetadataConstants.TABLE_EXD_CARDINALITY)) {
                calculateCardinality(table, submitter);
            }
        }
    }


}
