package org.apache.kylin.storage.hybrid;

import org.apache.kylin.common.KylinConfig;
import org.apache.kylin.common.persistence.ResourceStore;
import org.apache.kylin.common.util.JsonUtil;
import org.apache.kylin.common.util.LocalFileMetadataTestCase;
import org.apache.kylin.cube.CubeDescManager;
import org.apache.kylin.cube.CubeInstance;
import org.apache.kylin.cube.CubeManager;
import org.apache.kylin.cube.model.CubeDesc;
import org.apache.kylin.invertedindex.IIInstance;
import org.apache.kylin.metadata.project.ProjectInstance;
import org.apache.kylin.metadata.project.ProjectManager;
import org.apache.kylin.metadata.realization.IRealization;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by shaoshi on 2/13/15.
 */
public class HybridManagerTest extends LocalFileMetadataTestCase {

    @Before
    public void setUp() throws Exception {
        this.createTestMetadata();
    }

    @After
    public void after() throws Exception {
        this.cleanupTestMetadata();
    }

    @Test
    public void testBasics() throws Exception {
        HybridInstance cube = getHybridManager().getHybridInstance("test_kylin_hybrid");
        cube.init();
        System.out.println(JsonUtil.writeValueAsIndentString(cube));

        IRealization history = cube.getHistoryRealizationInstance();
        IRealization realTime = cube.getRealTimeRealizationInstance();

        Assert.assertTrue(history instanceof CubeInstance);
        Assert.assertTrue(realTime instanceof IIInstance);

    }


    public HybridManager getHybridManager() {
        return HybridManager.getInstance(getTestConfig());
    }
}
