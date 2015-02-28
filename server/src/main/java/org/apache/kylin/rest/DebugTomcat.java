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

package org.apache.kylin.rest;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.catalina.Context;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.startup.Tomcat;
import org.apache.hadoop.util.Shell;

import org.apache.kylin.common.KylinConfig;
import org.apache.kylin.rest.util.ClasspathUtil;

public class DebugTomcat {

    public static void main(String[] args) throws Exception {
        int port = 7070;
        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }
        
        // test_case_data/sandbox/ contains HDP 2.2 site xmls which is dev sandbox
        ClasspathUtil.addClasspath(new File("../examples/test_case_data/sandbox").getAbsolutePath());
        System.setProperty(KylinConfig.KYLIN_CONF, "../examples/test_case_data/sandbox");
        System.setProperty("hdp.version", "2.2.0.0-2041"); // mapred-site.xml ref this

        // workaround for job submission from win to linux -- https://issues.apache.org/jira/browse/MAPREDUCE-4052
        if (Shell.WINDOWS) {
            {
                Field field = Shell.class.getDeclaredField("WINDOWS");
                field.setAccessible(true);
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                field.set(null, false);
            }
            {
                Field field = java.io.File.class.getDeclaredField("pathSeparator");
                field.setAccessible(true);
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                field.set(null, ":");
            }
        }

        System.setProperty("spring.profiles.active", "testing");
        String webBase = new File("../webapp/app").getAbsolutePath();
        if (new File(webBase, "WEB-INF").exists() == false) {
            throw new RuntimeException("In order to launch Kylin web app from IDE, please make a symblink from webapp/app/WEB-INF to server/src/main/webapp/WEB-INF");
        }

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.setBaseDir(".");

        // Add AprLifecycleListener
        StandardServer server = (StandardServer) tomcat.getServer();
        AprLifecycleListener listener = new AprLifecycleListener();
        server.addLifecycleListener(listener);

        Context webContext = tomcat.addWebapp("/kylin", webBase);
        ErrorPage notFound = new ErrorPage();
        notFound.setErrorCode(404);
        notFound.setLocation("/index.html");
        webContext.addErrorPage(notFound);
        webContext.addWelcomeFile("index.html");

        // tomcat start
        tomcat.start();
        tomcat.getServer().await();
    }

}
