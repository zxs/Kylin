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

package org.apache.kylin.query.test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.apache.kylin.storage.hbase.coprocessor.observer.ObserverEnabler;

/**
 * Created by honma on 7/2/14.
 */
@RunWith(Parameterized.class)
public class CombinationTest extends KylinQueryTest {

    @BeforeClass
    public static void setUp() throws SQLException {
        System.out.println("setUp in CombinationTest");
    }

    @AfterClass
    public static void tearDown() {
        clean();
    }

    /**
     * return all config combinations, where first setting specifies join type
     * (inner or left), and the second setting specifies whether to force using
     * coprocessors(on, off or unset).
     */
    @Parameterized.Parameters
    public static Collection<Object[]> configs() {
        return Arrays.asList(new Object[][] { { "inner", "unset" }, { "left", "unset" }, { "inner", "off" }, { "left", "off" }, { "inner", "on" }, { "left", "on" }, });
    }

    public CombinationTest(String joinType, String coprocessorToggle) throws Exception {

        KylinQueryTest.clean();

        KylinQueryTest.joinType = joinType;
        KylinQueryTest.setupAll();
        KylinQueryTest.preferCubeOf(joinType);

        if (coprocessorToggle.equals("on")) {
            ObserverEnabler.forceCoprocessorOn();
        } else if (coprocessorToggle.equals("off")) {
            ObserverEnabler.forceCoprocessorOff();
        } else if (coprocessorToggle.equals("unset")) {
            // unset
        }
    }
}
