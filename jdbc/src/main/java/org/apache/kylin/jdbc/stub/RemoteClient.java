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

package org.apache.kylin.jdbc.stub;

import java.sql.SQLException;

import net.hydromatic.avatica.AvaticaStatement;

import org.apache.kylin.jdbc.KylinMetaImpl.MetaProject;

/**
 * Remote query stub of kylin restful service
 * 
 * @author xduo
 * 
 */
public interface RemoteClient {

    /**
     * Connect to kylin restful service. ConnectionException will be thrown if
     * authentication failed.
     * 
     * @throws ConnectionException
     */
    public void connect() throws ConnectionException;

    /**
     * @param project
     * @return
     */
    public MetaProject getMetadata(String project) throws ConnectionException;

    /**
     * Run query
     * 
     * @param statement
     * @param sql
     * @return
     * @throws SQLException
     */
    public DataSet<Object[]> query(AvaticaStatement statement, String sql) throws SQLException;

}
