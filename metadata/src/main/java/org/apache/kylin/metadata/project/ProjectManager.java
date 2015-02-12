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

package org.apache.kylin.metadata.project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.kylin.metadata.model.ColumnDesc;
import org.apache.kylin.metadata.model.MeasureDesc;
import org.apache.kylin.metadata.model.TableDesc;
import org.apache.kylin.metadata.realization.IRealization;
import org.apache.kylin.metadata.realization.RealizationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import org.apache.kylin.common.KylinConfig;
import org.apache.kylin.common.persistence.JsonSerializer;
import org.apache.kylin.common.persistence.ResourceStore;
import org.apache.kylin.common.persistence.Serializer;
import org.apache.kylin.common.restclient.Broadcaster;
import org.apache.kylin.common.restclient.CaseInsensitiveStringCache;
import org.apache.kylin.metadata.MetadataManager;
import org.apache.kylin.metadata.realization.RealizationType;

public class ProjectManager {
    private static final Logger logger = LoggerFactory.getLogger(ProjectManager.class);
    private static final ConcurrentHashMap<KylinConfig, ProjectManager> CACHE = new ConcurrentHashMap<KylinConfig, ProjectManager>();
    public static final Serializer<ProjectInstance> PROJECT_SERIALIZER = new JsonSerializer<ProjectInstance>(ProjectInstance.class);

    public static ProjectManager getInstance(KylinConfig config) {
        ProjectManager r = CACHE.get(config);
        if (r != null) {
            return r;
        }

        synchronized (ProjectManager.class) {
            r = CACHE.get(config);
            if (r != null) {
                return r;
            }
            try {
                r = new ProjectManager(config);
                CACHE.put(config, r);
                if (CACHE.size() > 1) {
                    logger.warn("More than one singleton exist");
                }
                return r;
            } catch (IOException e) {
                throw new IllegalStateException("Failed to init ProjectManager from " + config, e);
            }
        }
    }

    public static void clearCache() {
        CACHE.clear();
    }

    // ============================================================================

    private KylinConfig config;
    private ProjectL2Cache l2Cache;
    // project name => ProjrectInstance
    private CaseInsensitiveStringCache<ProjectInstance> projectMap = new CaseInsensitiveStringCache<ProjectInstance>(Broadcaster.TYPE.PROJECT);

    private ProjectManager(KylinConfig config) throws IOException {
        logger.info("Initializing ProjectManager with metadata url " + config);
        this.config = config;
        this.l2Cache = new ProjectL2Cache(this);

        reloadAllProjects();
    }

    public void clearL2Cache() {
        l2Cache.clear();
    }

    private void reloadAllProjects() throws IOException {
        ResourceStore store = getStore();
        List<String> paths = store.collectResourceRecursively(ResourceStore.PROJECT_RESOURCE_ROOT, ".json");

        logger.debug("Loading Project from folder " + store.getReadableResourcePath(ResourceStore.PROJECT_RESOURCE_ROOT));

        for (String path : paths) {
            reloadProjectAt(path);
        }
        wireProjectAndRealizations(projectMap.values());
        logger.debug("Loaded " + projectMap.size() + " Project(s)");
    }

    public ProjectInstance reloadProject(String project) throws IOException {
        return reloadProjectAt(ProjectInstance.concatResourcePath(project));
    }

    private ProjectInstance reloadProjectAt(String path) throws IOException {
        ResourceStore store = getStore();

        ProjectInstance projectInstance = store.getResource(path, ProjectInstance.class, PROJECT_SERIALIZER);
        if (projectInstance == null) {
            logger.warn("reload project at path:" + path + " not found, this:" + this.toString());
            return null;
        }

        projectInstance.init();

        if (StringUtils.isBlank(projectInstance.getName()))
            throw new IllegalStateException("Project name must not be blank");

        projectMap.putLocal(projectInstance.getName(), projectInstance);

        clearL2Cache();
        return projectInstance;
    }

    private void wireProjectAndRealizations(Collection<ProjectInstance> projectInstances) {
        if (projectInstances.isEmpty())
            return;

        RealizationRegistry registry = RealizationRegistry.getInstance(config);
        for (ProjectInstance projectInstance : projectInstances) {
            for (RealizationEntry realization : projectInstance.getRealizationEntries()) {
                IRealization rel = registry.getRealization(realization.getType(), realization.getRealization());
                if (rel != null) {
                    rel.setProjectName(projectInstance.getName());
                } else {
                    logger.warn("Realization '" + realization + "' defined under project '" + projectInstance + "' is not found");
                }
            }
        }
    }

    public List<ProjectInstance> listAllProjects() {
        return new ArrayList<ProjectInstance>(projectMap.values());
    }

    public ProjectInstance getProject(String projectName) {
        projectName = norm(projectName);
        return projectMap.get(projectName);
    }

    public ProjectInstance createProject(String projectName, String owner, String description) throws IOException {
        logger.info("Creating project '" + projectName);

        ProjectInstance currentProject = getProject(projectName);
        if (currentProject == null) {
            currentProject = ProjectInstance.create(projectName, owner, description, null);
        } else {
            throw new IllegalStateException("The project named " + projectName + "already exists");
        }

        saveResource(currentProject);

        return currentProject;
    }

    public ProjectInstance dropProject(String projectName) throws IOException {
        if (projectName == null)
            throw new IllegalArgumentException("Project name not given");

        ProjectInstance projectInstance = getProject(projectName);

        if (projectInstance == null) {
            throw new IllegalStateException("The project named " + projectName + " does not exist");
        }

        if (projectInstance.getRealizationCount(null) != 0) {
            throw new IllegalStateException("The project named " + projectName + " can not be deleted because there's still realizations in it. Delete them first.");
        }

        logger.info("Dropping project '" + projectInstance.getName() + "'");

        deleteResource(projectInstance);

        return projectInstance;
    }

    public ProjectInstance updateProject(ProjectInstance project, String newName, String newDesc) throws IOException {
        if (!project.getName().equals(newName)) {
            ProjectInstance newProject = this.createProject(newName, project.getOwner(), newDesc);
            // FIXME table lost??
            newProject.setCreateTimeUTC(project.getCreateTimeUTC());
            newProject.recordUpdateTime(System.currentTimeMillis());
            newProject.setRealizationEntries(project.getRealizationEntries());

            deleteResource(project);
            saveResource(newProject);

            return newProject;
        } else {
            project.setName(newName);
            project.setDescription(newDesc);

            if (project.getUuid() == null)
                project.updateRandomUuid();

            saveResource(project);

            return project;
        }
    }

    public ProjectInstance moveRealizationToProject(RealizationType type, String realizationName, String newProjectName, String owner) throws IOException {
        removeRealizationsFromProjects(type, realizationName);
        return addRealizationToProject(type, realizationName, newProjectName, owner);
    }

    private ProjectInstance addRealizationToProject(RealizationType type, String realizationName, String project, String user) throws IOException {
        String newProjectName = norm(project);
        ProjectInstance newProject = getProject(newProjectName);
        if (newProject == null) {
            newProject = this.createProject(newProjectName, user, "This is a project automatically added when adding realization " + realizationName + "(" + type + ")");
        }
        newProject.addRealizationEntry(type, realizationName);
        saveResource(newProject);

        return newProject;
    }

    public void removeRealizationsFromProjects(RealizationType type, String realizationName) throws IOException {
        for (ProjectInstance projectInstance : findProjects(type, realizationName)) {
            projectInstance.removeRealization(type, realizationName);
            saveResource(projectInstance);
        }
    }

    public ProjectInstance addTableDescToProject(String[] tableIdentities, String projectName) throws IOException {
        MetadataManager metaMgr = getMetadataManager();
        ProjectInstance projectInstance = getProject(projectName);
        for (String tableId : tableIdentities) {
            TableDesc table = metaMgr.getTableDesc(tableId);
            if (table == null) {
                throw new IllegalStateException("Cannot find table '" + table + "' in metadata manager");
            }
            projectInstance.addTable(table.getIdentity());
        }

        saveResource(projectInstance);
        return projectInstance;
    }

    private void saveResource(ProjectInstance prj) throws IOException {
        ResourceStore store = getStore();
        store.putResource(prj.getResourcePath(), prj, PROJECT_SERIALIZER);

        prj = reloadProjectAt(prj.getResourcePath());
        projectMap.put(norm(prj.getName()), prj); // triggers update broadcast
        clearL2Cache();
    }

    private void deleteResource(ProjectInstance proj) throws IOException {
        ResourceStore store = getStore();
        store.deleteResource(proj.getResourcePath());
        projectMap.remove(norm(proj.getName()));
        clearL2Cache();
    }

    private List<ProjectInstance> findProjects(RealizationType type, String realizationName) {
        List<ProjectInstance> result = Lists.newArrayList();
        for (ProjectInstance prj : projectMap.values()) {
            for (RealizationEntry entry : prj.getRealizationEntries()) {
                if (entry.getType().equals(type) && entry.getRealization().equalsIgnoreCase(realizationName)) {
                    result.add(prj);
                    break;
                }
            }
        }
        return result;
    }

    public List<TableDesc> listDefinedTables(String project) throws IOException {
        return l2Cache.listDefinedTables(norm(project));
    }

    public Set<TableDesc> listExposedTables(String project) {
        return l2Cache.listExposedTables(norm(project));
    }

    public Set<ColumnDesc> listExposedColumns(String project, String table) {
        return l2Cache.listExposedColumns(norm(project), table);
    }

    public boolean isExposedTable(String project, String table) {
        return l2Cache.isExposedTable(norm(project), table);
    }

    public boolean isExposedColumn(String project, String table, String col) {
        return l2Cache.isExposedColumn(norm(project), table, col);
    }

    public Set<IRealization> listAllRealizations(String project) {
        return l2Cache.listAllRealizations(norm(project));
    }

    public Set<IRealization> getRealizationsByTable(String project, String tableName) {
        return l2Cache.getRealizationsByTable(norm(project), tableName.toUpperCase());
    }

    public List<IRealization> getOnlineRealizationByFactTable(String project, String factTable) {
        return l2Cache.getOnlineRealizationByFactTable(norm(project), factTable.toUpperCase());
    }

    public List<MeasureDesc> listEffectiveRewriteMeasures(String project, String factTable) {
        return l2Cache.listEffectiveRewriteMeasures(norm(project), factTable.toUpperCase());
    }

    KylinConfig getConfig() {
        return config;
    }

    ResourceStore getStore() {
        return ResourceStore.getStore(this.config);
    }

    MetadataManager getMetadataManager() {
        return MetadataManager.getInstance(config);
    }

    private String norm(String project) {
        return project;
    }

}
