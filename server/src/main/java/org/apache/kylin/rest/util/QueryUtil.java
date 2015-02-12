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

package org.apache.kylin.rest.util;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.kylin.rest.model.SelectedColumnMeta;
import org.apache.kylin.rest.response.SQLResponse;

/**
 * @author xduo
 * 
 */
public class QueryUtil {

    private static final String S0 = "\\s*";
    private static final String S1 = "\\s";
    private static final String SM = "\\s+";
    private static final Pattern PTN_GROUP_BY = Pattern.compile(S1 + "GROUP" + SM + "BY" + S1, Pattern.CASE_INSENSITIVE);
    private static final Pattern PTN_HAVING_COUNT_GREATER_THAN_ZERO = Pattern.compile(S1 + "HAVING" + SM + "[(]?" + S0 + "COUNT" + S0 + "[(]" + S0 + "1" + S0 + "[)]" + S0 + ">" + S0 + "0" + S0 + "[)]?", Pattern.CASE_INSENSITIVE);
    private static final Pattern PTN_SUM_1 = Pattern.compile(S1 + "SUM" + S0 + "[(]" + S0 + "[1]" + S0 + "[)]" + S1, Pattern.CASE_INSENSITIVE);

    // private static final Pattern PTN_HAVING_ESCAPE_FUNCTION =
    // Pattern.compile("\\{fn" + "(" + S0 + ")" + "\\}",
    // Pattern.CASE_INSENSITIVE);
    private static final Pattern PTN_HAVING_ESCAPE_FUNCTION = Pattern.compile("\\{fn" + "(.*?)" + "\\}", Pattern.CASE_INSENSITIVE);

    private static String[] tableauTestQueries = new String[] { "SELECT 1",//
            "CREATE LOCAL TEMPORARY TABLE \"XTableau_B_Connect\" ( \"COL\" INTEGER ) ON COMMIT PRESERVE ROWS",//
            "DROP TABLE \"XTableau_B_Connect\"",//
            "SELECT \"COL\" FROM (SELECT 1 AS \"COL\") AS \"SUBQUERY\"",//
            "SELECT TOP 1 \"COL\" FROM (SELECT 1 AS \"COL\") AS \"CHECKTOP\"", "SELECT \"COL\" FROM (SELECT 1 AS \"COL\") AS \"CHECKTOP\" LIMIT 1",//
            "SELECT \"SUBCOL\" AS \"COL\"  FROM (   SELECT 1 AS \"SUBCOL\" ) \"SUBQUERY\" GROUP BY 1", "SELECT \"SUBCOL\" AS \"COL\" FROM (   SELECT 1 AS \"SUBCOL\" ) \"SUBQUERY\" GROUP BY 2", "INSERT INTO \"XTableau_C_Connect\" SELECT * FROM (SELECT 1 AS COL) AS CHECKTEMP LIMIT 1", "DROP TABLE \"XTableau_C_Connect\"", "INSERT INTO \"XTableau_B_Connect\" SELECT * FROM (SELECT 1 AS COL) AS CHECKTEMP LIMIT 1" };

    private static SQLResponse temp = new SQLResponse(new LinkedList<SelectedColumnMeta>() {
        private static final long serialVersionUID = -8086728462624901359L;

        {
            add(new SelectedColumnMeta(false, false, true, false, 2, true, 11, "COL", "COL", "", "", "", 10, 0, 4, "int4", false, true, false));
        }
    }, new LinkedList<List<String>>() {
        private static final long serialVersionUID = -470083340592928073L;

        {
            add(new LinkedList<String>() {
                private static final long serialVersionUID = -3673192785838230054L;

                {
                    add("1");
                }
            });
        }
    }, 0, false, null);

    private static SQLResponse[] fakeResponses = new SQLResponse[] { temp, new SQLResponse(null, null, 0, false, null), //
            new SQLResponse(null, null, 0, false, null), //
            temp, //
            new SQLResponse(null, null, 0, true, "near 1 syntax error"), //
            temp, //
            new SQLResponse(null, null, 0, true, "group by 1????"), //
            new SQLResponse(null, null, 0, true, "group by 2????"), //
            new SQLResponse(null, null, 0, true, "XTableau_C_Connect not exist"), //
            new SQLResponse(null, null, 0, true, "XTableau_C_Connect not exist"), new SQLResponse(null, null, 0, true, "XTableau_B_Connect not exist"), };

    private static ArrayList<HashSet<String>> tableauTestQueriesInToken = new ArrayList<HashSet<String>>();

    static {
        for (String q : tableauTestQueries) {
            HashSet<String> temp = new HashSet<String>();
            for (String token : q.split("[\r\n\t \\(\\)]")) {
                temp.add(token);
            }
            temp.add("");
            tableauTestQueriesInToken.add(temp);
        }
    }

    // correct sick / invalid SQL
    public static String healSickSql(String sql) {
        Matcher m;

        // Case fn{ EXTRACT(...) }
        // Use non-greedy regrex matching to remove escape functions
        while (true) {
            m = PTN_HAVING_ESCAPE_FUNCTION.matcher(sql);
            if (!m.find())
                break;
            sql = sql.substring(0, m.start()) + m.group(1) + sql.substring(m.end());
        }

        // Case: HAVING COUNT(1)>0 without Group By
        // Tableau generates: SELECT SUM(1) AS "COL" FROM "VAC_SW" HAVING
        // COUNT(1)>0
        m = PTN_HAVING_COUNT_GREATER_THAN_ZERO.matcher(sql);
        if (m.find() && PTN_GROUP_BY.matcher(sql).find() == false) {
            sql = sql.substring(0, m.start()) + " " + sql.substring(m.end());
        }

        // Case: SUM(1)
        // Replace it with COUNT(1)
        while (true) {
            m = PTN_SUM_1.matcher(sql);
            if (!m.find())
                break;
            sql = sql.substring(0, m.start()) + " COUNT(1) " + sql.substring(m.end());
        }

        return sql;
    }

    public static SQLResponse tableauIntercept(String sql) {

        String[] tokens = sql.split("[\r\n\t \\(\\)]");
        for (int i = 0; i < tableauTestQueries.length; ++i) {
            if (isTokenWiseEqual(tokens, tableauTestQueriesInToken.get(i))) {
                printInfo("Hit fake response " + i);
                return fakeResponses[i];
            }
        }

        // if(sql.trim().endsWith("LIMIT 1"))
        // {
        // //queries end with LIMIT 1 is test queries sent by Tableau
        // return temp;
        // }

        return null;
    }

    /**
     * adjust error message order
     * 
     * @param errorMsg
     * @return
     */
    public static String makeErrorMsgUserFriendly(String errorMsg) {
        try {
            errorMsg = errorMsg.replaceAll("\\s", " ");// replace all invisible
                                                       // characters
            Pattern pattern = Pattern.compile("error while executing SQL \"(.*)\":(.*)");
            Matcher matcher = pattern.matcher(errorMsg);
            if (matcher.find()) {
                return matcher.group(2).trim() + "\n" + "while executing SQL: \"" + matcher.group(1).trim() + "\"";
            } else
                return errorMsg;
        } catch (Exception e) {
            return errorMsg;
        }
    }

    private static boolean isTokenWiseEqual(String[] tokens, HashSet<String> tokenSet) {
        for (String token : tokens) {
            if (!tokenSet.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private static void printInfo(String info) {
        System.out.println(new Timestamp(System.currentTimeMillis()) + " - " + info);
    }

}
