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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.hadoop.hbase.util.Bytes;
import org.apache.kylin.common.hll.HyperLogLogPlusCounter;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yangli9
 * 
 */
public class HyperLogLogCounterTest {

    ByteBuffer buf = ByteBuffer.allocate(1024 * 1024);
    Random rand1 = new Random(1);
    Random rand2 = new Random(2);
    Random rand3 = new Random(3);
    int errorCount1 = 0;
    int errorCount2 = 0;
    int errorCount3 = 0;

    private Set<String> generateTestData(int n) {
        Set<String> testData = new HashSet<String>();
        for (int i = 0; i < n; i++) {
            String[] samples = generateSampleData();
            for (String sample : samples) {
                testData.add(sample);
            }
        }
        return testData;
    }

    // simulate the visit (=visitor+id)
    private String[] generateSampleData() {

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < 19; i++) {
            buf.append(Math.abs(rand1.nextInt()) % 10);
        }
        String header = buf.toString();

        int size = Math.abs(rand3.nextInt()) % 9 + 1;
        String[] samples = new String[size];
        for (int k = 0; k < size; k++) {
            buf = new StringBuilder(header);
            buf.append("-");
            for (int i = 0; i < 10; i++) {
                buf.append(Math.abs(rand3.nextInt()) % 10);
            }
            samples[k] = buf.toString();
        }

        return samples;
    }

    @Test
    public void countTest() throws IOException {
        int n = 10;
        for (int i = 0; i < 5; i++) {
            count(n);
            n *= 10;
        }
    }

    private void count(int n) throws IOException {
        Set<String> testSet = generateTestData(n);

        HyperLogLogPlusCounter hllc = newHLLC();
        for (String testData : testSet) {
            hllc.add(Bytes.toBytes(testData));
        }
        long estimate = hllc.getCountEstimate();
        double errorRate = hllc.getErrorRate();
        double actualError = (double) Math.abs(testSet.size() - estimate) / testSet.size();
        System.out.println(estimate);
        System.out.println(testSet.size());
        System.out.println(errorRate);
        System.out.println("=" + actualError);
        Assert.assertTrue(actualError < errorRate * 3.0);

        checkSerialize(hllc);
    }

    private void checkSerialize(HyperLogLogPlusCounter hllc) throws IOException {
        long estimate = hllc.getCountEstimate();
        buf.clear();
        hllc.writeRegisters(buf);
        buf.flip();
        hllc.readRegisters(buf);
        Assert.assertEquals(estimate, hllc.getCountEstimate());
    }

    @Test
    public void mergeTest() throws IOException {
        double error = 0;
        double absError = 0;
        int n = 100;
        for (int i = 0; i < n; i++) {
            System.out.println("============" + i);
            double e = merge();
            error += e;
            absError += Math.abs(e);
        }
        System.out.println("Total average error is " + error / n + " and absolute error is " + absError / n);

        System.out.println("errorCount1 is " + errorCount1 + "!");
        System.out.println("errorCount2 is " + errorCount2 + "!");
        System.out.println("errorCount3 is " + errorCount3 + "!");

        Assert.assertTrue(errorCount1 <= n * 0.40);
        Assert.assertTrue(errorCount2 <= n * 0.08);
        Assert.assertTrue(errorCount3 <= n * 0.02);
    }

    private double merge() throws IOException {

        int ln = 50;
        int dn = 300;
        Set<String> testSet = new HashSet<String>();
        HyperLogLogPlusCounter[] hllcs = new HyperLogLogPlusCounter[ln];
        for (int i = 0; i < ln; i++) {
            hllcs[i] = newHLLC();
            for (int k = 0; k < dn; k++) {
                String[] samples = generateSampleData();
                for (String data : samples) {
                    testSet.add(data);
                    hllcs[i].add(Bytes.toBytes(data));
                }
            }
        }
        HyperLogLogPlusCounter mergeHllc = newHLLC();
        for (HyperLogLogPlusCounter hllc : hllcs) {
            mergeHllc.merge(hllc);
            checkSerialize(mergeHllc);
        }

        double errorRate = mergeHllc.getErrorRate();
        long estimate = mergeHllc.getCountEstimate();
        double actualError = (double) (testSet.size() - estimate) / testSet.size();

        System.out.println(testSet.size() + "-" + estimate);

        System.out.println("=" + actualError);
        if (Math.abs(actualError) > errorRate) {
            errorCount1++;
        }
        if (Math.abs(actualError) > 2 * errorRate) {
            errorCount2++;
        }
        if (Math.abs(actualError) > 3 * errorRate) {
            errorCount3++;
        }

        return actualError;
    }

    @Test
    public void testPerformance() throws IOException {
        int N = 3; // reduce N HLLC into one
        int M = 1000; // for M times, use 100000 for real perf test

        HyperLogLogPlusCounter samples[] = new HyperLogLogPlusCounter[N];
        for (int i = 0; i < N; i++) {
            samples[i] = newHLLC();
            for (String str : generateTestData(10000))
                samples[i].add(str);
        }

        System.out.println("Perf test running ... ");
        long start = System.currentTimeMillis();
        HyperLogLogPlusCounter sum = newHLLC();
        for (int i = 0; i < M; i++) {
            sum.clear();
            for (int j = 0; j < N; j++) {
                sum.merge(samples[j]);
                checkSerialize(sum);
            }
        }
        long duration = System.currentTimeMillis() - start;
        System.out.println("Perf test result: " + duration / 1000 + " seconds");
    }

    @Test
    public void testEquivalence() {
        byte[] a = new byte[] { 0, 3, 4, 42, 2, 2 };
        byte[] b = new byte[] { 3, 4, 42 };
        HyperLogLogPlusCounter ha = new HyperLogLogPlusCounter();
        HyperLogLogPlusCounter hb = new HyperLogLogPlusCounter();
        ha.add(a, 1, 3);
        hb.add(b);

        Assert.assertTrue(ha.getCountEstimate()==hb.getCountEstimate());
    }

    private HyperLogLogPlusCounter newHLLC() {
        return new HyperLogLogPlusCounter(16);
    }
}
