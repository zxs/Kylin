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

package org.apache.kylin.job.tools;

import java.io.IOException;

import org.apache.commons.cli.Options;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.kylin.job.hadoop.AbstractHadoopJob;

/**
 * Created by honma on 11/11/14.
 */
public class CleanHtableCLI extends AbstractHadoopJob {

    protected static final Logger log = LoggerFactory.getLogger(CleanHtableCLI.class);

    String tableName;

    @Override
    public int run(String[] args) throws Exception {
        Options options = new Options();
        try {

            clean();

            return 0;
        } catch (Exception e) {
            printUsage(options);
            throw e;
        }
    }

    private void clean() throws IOException {
        Configuration conf = HBaseConfiguration.create();
        HBaseAdmin hbaseAdmin = new HBaseAdmin(conf);

        for (HTableDescriptor descriptor : hbaseAdmin.listTables()) {
            String name = descriptor.getNameAsString().toLowerCase();
            if (name.startsWith("kylin") || name.startsWith("_kylin")) {
                String x = descriptor.getValue("KYLIN_HOST");
                System.out.println("table name " + descriptor.getNameAsString() + " host: " + x);
                System.out.println(descriptor);
                System.out.println();
            }
        }
        hbaseAdmin.close();
    }

    public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new CleanHtableCLI(), args);
        System.exit(exitCode);
    }
}
