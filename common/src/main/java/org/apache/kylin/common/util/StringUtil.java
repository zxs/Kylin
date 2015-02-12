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

package org.apache.kylin.common.util;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA. User: lukhan Date: 12/2/13 Time: 11:43 AM To
 * change this template use File | Settings | File Templates.
 */
public class StringUtil {

    public static String[] filterSystemArgs(String args[]) {
        ArrayList<String> whatsLeft = new ArrayList<String>();
        for (String a : args) {
            if (a.startsWith("-D")) {
                String key;
                String value;
                int cut = a.indexOf('=');
                if (cut < 0) {
                    key = a.substring(2);
                    value = "";
                } else {
                    key = a.substring(2, cut);
                    value = a.substring(cut + 1);
                }
                System.setProperty(key, value);
            } else {
                whatsLeft.add(a);
            }
        }
        return (String[]) whatsLeft.toArray(new String[whatsLeft.size()]);
    }

    public static void toUpperCaseArray(String[] source, String[] target) {
        for (int i = 0; i < source.length; i++) {
            if (source[i] != null) {
                target[i] = source[i].toUpperCase();
            }
        }
    }

    public static String dropSuffix(String str, String suffix) {
        if (str.endsWith(suffix))
            return str.substring(0, str.length() - suffix.length());
        else
            return str;
    }

    public static String min(Collection<String> strs) {
        String min = null;
        for (String s : strs) {
            if (min == null || min.compareTo(s) > 0)
                min = s;
        }
        return min;
    }

    public static String max(Collection<String> strs) {
        String max = null;
        for (String s : strs) {
            if (max == null || max.compareTo(s) < 0)
                max = s;
        }
        return max;
    }

    public static String min(String s1, String s2) {
        if (s1 == null)
            return s2;
        else if (s2 == null)
            return s1;
        else
            return s1.compareTo(s2) < 0 ? s1 : s2;
    }

    public static String max(String s1, String s2) {
        if (s1 == null)
            return s2;
        else if (s2 == null)
            return s1;
        else
            return s1.compareTo(s2) > 0 ? s1 : s2;
    }

}
