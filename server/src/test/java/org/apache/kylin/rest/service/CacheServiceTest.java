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

import org.apache.kylin.common.KylinConfig;
import org.apache.kylin.common.restclient.Broadcaster;
import org.apache.kylin.common.util.LocalFileMetadataTestCase;
import org.apache.kylin.cube.CubeDescManager;
import org.apache.kylin.cube.CubeInstance;
import org.apache.kylin.cube.CubeManager;
import org.apache.kylin.cube.CubeSegment;
import org.apache.kylin.cube.model.CubeDesc;
import org.apache.kylin.metadata.MetadataManager;
import org.apache.kylin.metadata.model.DataModelDesc;
import org.apache.kylin.metadata.model.LookupDesc;
import org.apache.kylin.metadata.model.TableDesc;
import org.apache.kylin.metadata.project.ProjectInstance;
import org.apache.kylin.metadata.project.ProjectManager;
import org.apache.kylin.metadata.realization.IRealization;
import org.apache.kylin.metadata.realization.RealizationType;
import org.apache.kylin.rest.broadcaster.BroadcasterReceiveServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.*;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Created by qianzhou on 1/16/15.
 */

public class CacheServiceTest extends LocalFileMetadataTestCase {

    private static Server server;

    private static KylinConfig configA;
    private static KylinConfig configB;

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(CacheServiceTest.class);

    private static AtomicLong counter = new AtomicLong();

    @BeforeClass
    public static void beforeClass() throws Exception {
        createTestMetadata(LOCALMETA_TEST_DATA);
        configA = KylinConfig.getInstanceFromEnv();
        configB = KylinConfig.getKylinConfigFromInputStream(KylinConfig.getKylinPropertiesAsInputSteam());
        configB.setMetadataUrl("../examples/test_metadata");

        server = new Server(7070);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(new BroadcasterReceiveServlet(new BroadcasterReceiveServlet.BroadcasterHandler() {
            @Override
            public void handle(String type, String name, String event) {
                final CacheService cacheService = new CacheService() {
                    @Override
                    public KylinConfig getConfig() {
                        return configB;
                    }
                };
                Broadcaster.TYPE wipeType = Broadcaster.TYPE.getType(type);
                Broadcaster.EVENT wipeEvent = Broadcaster.EVENT.getEvent(event);
                final String log = "wipe cache type: " + wipeType + " event:" + wipeEvent + " name:" + name;
                logger.info(log);
                counter.incrementAndGet();
                switch (wipeEvent) {
                    case CREATE:
                    case UPDATE:
                        cacheService.rebuildCache(wipeType, name);
                        break;
                    case DROP:
                        cacheService.removeCache(wipeType, name);
                        break;
                    default:
                        throw new RuntimeException("invalid type:" + wipeEvent);
                }
            }
        })), "/");
        server.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stop();
        cleanAfterClass();
    }

    @Before
    public void setUp() throws Exception {
        counter.set(0L);
        createTestMetadata();
    }

    @After
    public void after() throws Exception {
    }

    private void waitForCounterAndClear(long count) {
        int retryTimes = 0;
        while ((!counter.compareAndSet(count, 0L))) {
            if (++retryTimes > 30) {
                throw new RuntimeException("timeout");
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @BeforeClass
    public static void startServer() throws Exception {

    }

    @AfterClass
    public static void stopServer() throws Exception {

    }

    private static CubeManager getCubeManager(KylinConfig config) throws Exception {
        return CubeManager.getInstance(config);
    }
    private static ProjectManager getProjectManager(KylinConfig config) throws Exception {
        return ProjectManager.getInstance(config);
    }
    private static CubeDescManager getCubeDescManager(KylinConfig config) throws Exception {
        return CubeDescManager.getInstance(config);
    }
    private static MetadataManager getMetadataManager(KylinConfig config) throws Exception {
        return MetadataManager.getInstance(config);
    }

    @Test
    public void testBasic() throws Exception {
        assertTrue(!configA.equals(configB));

        assertNotNull(getCubeManager(configA));
        assertNotNull(getCubeManager(configB));
        assertNotNull(getCubeDescManager(configA));
        assertNotNull(getCubeDescManager(configB));
        assertNotNull(getProjectManager(configB));
        assertNotNull(getProjectManager(configB));
        assertNotNull(getMetadataManager(configB));
        assertNotNull(getMetadataManager(configB));

        assertTrue(!getCubeManager(configA).equals(getCubeManager(configB)));
        assertTrue(!getCubeDescManager(configA).equals(getCubeDescManager(configB)));
        assertTrue(!getProjectManager(configA).equals(getProjectManager(configB)));
        assertTrue(!getMetadataManager(configA).equals(getMetadataManager(configB)));

        assertEquals(getProjectManager(configA).listAllProjects().size(), getProjectManager(configB).listAllProjects().size());
    }

    @Test
    public void testCubeCRUD() throws Exception {
        final Broadcaster broadcaster = Broadcaster.getInstance();
        broadcaster.getCounterAndClear();

        getStore().deleteResource("/cube/a_whole_new_cube.json");

        //create cube

        final String cubeName = "a_whole_new_cube";
        final CubeManager cubeManager = getCubeManager(configA);
        final CubeManager cubeManagerB = getCubeManager(configB);
        final ProjectManager projectManager = getProjectManager(configA);
        final ProjectManager projectManagerB = getProjectManager(configB);
        final CubeDescManager cubeDescManager = getCubeDescManager(configA);
        final CubeDescManager cubeDescManagerB = getCubeDescManager(configB);
        final CubeDesc cubeDesc = getCubeDescManager(configA).getCubeDesc("test_kylin_cube_with_slr_desc");

        assertTrue(cubeManager.getCube(cubeName) == null);
        assertTrue(cubeManagerB.getCube(cubeName) == null);
        assertTrue(!containsRealization(projectManager.listAllRealizations(ProjectInstance.DEFAULT_PROJECT_NAME), RealizationType.CUBE, cubeName));
        assertTrue(!containsRealization(projectManagerB.listAllRealizations(ProjectInstance.DEFAULT_PROJECT_NAME), RealizationType.CUBE, cubeName));
        cubeManager.createCube(cubeName, ProjectInstance.DEFAULT_PROJECT_NAME, cubeDesc, null);
        assertNotNull(cubeManager.getCube(cubeName));
        //one for cube update, one for project update
        assertEquals(2, broadcaster.getCounterAndClear());
        waitForCounterAndClear(2);
        assertNotNull(cubeManagerB.getCube(cubeName));
        assertTrue(containsRealization(projectManager.listAllRealizations(ProjectInstance.DEFAULT_PROJECT_NAME), RealizationType.CUBE, cubeName));
        assertTrue(containsRealization(projectManagerB.listAllRealizations(ProjectInstance.DEFAULT_PROJECT_NAME), RealizationType.CUBE, cubeName));

        //update cube
        CubeInstance cube = cubeManager.getCube(cubeName);
        assertEquals(0, cube.getSegments().size());
        assertEquals(0, cubeManagerB.getCube(cubeName).getSegments().size());
        CubeSegment segment = new CubeSegment();
        segment.setName("test_segment");
        cube.getSegments().add(segment);
        cubeManager.updateCube(cube);
        //only one for update cube
        assertEquals(1, broadcaster.getCounterAndClear());
        waitForCounterAndClear(1);
        assertEquals(1, cubeManagerB.getCube(cubeName).getSegments().size());
        assertEquals(segment.getName(), cubeManagerB.getCube(cubeName).getSegments().get(0).getName());

        //delete cube
        cubeManager.dropCube(cubeName, false);
        assertTrue(cubeManager.getCube(cubeName) == null);
        assertTrue(!containsRealization(projectManager.listAllRealizations(ProjectInstance.DEFAULT_PROJECT_NAME), RealizationType.CUBE, cubeName));
        //one for cube update, one for project update
        assertEquals(2, broadcaster.getCounterAndClear());
        waitForCounterAndClear(2);
        assertTrue(cubeManagerB.getCube(cubeName) == null);
        assertTrue(!containsRealization(projectManagerB.listAllRealizations(ProjectInstance.DEFAULT_PROJECT_NAME), RealizationType.CUBE, cubeName));


        final String cubeDescName = "test_cube_desc";
        cubeDesc.setName(cubeDescName);
        cubeDesc.setLastModified(0);
        assertTrue(cubeDescManager.getCubeDesc(cubeDescName) == null);
        assertTrue(cubeDescManagerB.getCubeDesc(cubeDescName) == null);
        cubeDescManager.createCubeDesc(cubeDesc);
        //one for add cube desc
        assertEquals(1, broadcaster.getCounterAndClear());
        waitForCounterAndClear(1);
        assertNotNull(cubeDescManager.getCubeDesc(cubeDescName));
        assertNotNull(cubeDescManagerB.getCubeDesc(cubeDescName));


        cubeDesc.setNotifyList(Arrays.asList("test@email", "test@email", "test@email"));
        cubeDescManager.updateCubeDesc(cubeDesc);
        assertEquals(1, broadcaster.getCounterAndClear());
        waitForCounterAndClear(1);
        assertEquals(cubeDesc.getNotifyList(), cubeDescManagerB.getCubeDesc(cubeDescName).getNotifyList());

        cubeDescManager.removeCubeDesc(cubeDesc);
        //one for add cube desc
        assertEquals(1, broadcaster.getCounterAndClear());
        waitForCounterAndClear(1);
        assertTrue(cubeDescManager.getCubeDesc(cubeDescName) == null);
        assertTrue(cubeDescManagerB.getCubeDesc(cubeDescName) == null);



        getStore().deleteResource("/cube/a_whole_new_cube.json");
    }

    private TableDesc createTestTableDesc() {
        TableDesc tableDesc = new TableDesc();
        tableDesc.setDatabase("TEST_DB");
        tableDesc.setName("TEST_TABLE");
        tableDesc.setUuid(UUID.randomUUID().toString());
        tableDesc.setLastModified(0);
        return tableDesc;
    }


    @Test
    public void testMetaCRUD() throws Exception {
        final MetadataManager metadataManager = MetadataManager.getInstance(configA);
        final MetadataManager metadataManagerB = MetadataManager.getInstance(configB);
        final Broadcaster broadcaster = Broadcaster.getInstance();
        broadcaster.getCounterAndClear();

        TableDesc tableDesc = createTestTableDesc();
        assertTrue(metadataManager.getTableDesc(tableDesc.getIdentity()) == null);
        assertTrue(metadataManagerB.getTableDesc(tableDesc.getIdentity()) == null);
        metadataManager.saveSourceTable(tableDesc);
        //only one for table insert
        assertEquals(1, broadcaster.getCounterAndClear());
        waitForCounterAndClear(1);
        assertNotNull(metadataManager.getTableDesc(tableDesc.getIdentity()));
        assertNotNull(metadataManagerB.getTableDesc(tableDesc.getIdentity()));




        final String dataModelName = "test_data_model";
        DataModelDesc dataModelDesc = metadataManager.getDataModelDesc("test_kylin_ii_model_desc");
        dataModelDesc.setName(dataModelName);
        dataModelDesc.setLastModified(0);
        assertTrue(metadataManager.getDataModelDesc(dataModelName) == null);
        assertTrue(metadataManagerB.getDataModelDesc(dataModelName) == null);

        dataModelDesc.setName(dataModelName);
        metadataManager.createDataModelDesc(dataModelDesc);
        //only one for data model update
        assertEquals(1, broadcaster.getCounterAndClear());
        waitForCounterAndClear(1);
        assertEquals(dataModelDesc.getName(), metadataManagerB.getDataModelDesc(dataModelName).getName());

        final LookupDesc[] lookups = dataModelDesc.getLookups();
        assertTrue(lookups.length > 0);
        dataModelDesc.setLookups(new LookupDesc[]{lookups[0]});
        metadataManager.updateDataModelDesc(dataModelDesc);
        //only one for data model update
        assertEquals(1, broadcaster.getCounterAndClear());
        waitForCounterAndClear(1);
        assertEquals(dataModelDesc.getLookups().length, metadataManagerB.getDataModelDesc(dataModelName).getLookups().length);

    }

    private boolean containsRealization(Set<IRealization> realizations, RealizationType type, String name) {
        for (IRealization realization : realizations) {
            if (realization.getType() == type && realization.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
