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

package org.apache.kylin.query.routing;

import java.util.Iterator;
import java.util.List;

import org.apache.kylin.query.routing.RoutingRules.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import org.apache.kylin.metadata.realization.IRealization;
import org.apache.kylin.metadata.realization.RealizationType;
import org.apache.kylin.query.relnode.OLAPContext;

/**
 * Created by Hongbin Ma(Binmahone) on 1/5/15.
 */
public abstract class RoutingRule {
    private static final Logger logger = LoggerFactory.getLogger(QueryRouter.class);
    private static List<RoutingRule> rules = Lists.newLinkedList();

    //TODO: two rules are left out:
    //1. simple query use II prior to cube
    //2. exact match prior to week match
    static {
        rules.add(new RealizationPriorityRule());
        rules.add(new RemoveUncapableRealizationsRule());
        rules.add(new SimpleQueryMoreColumnsCubeFirstRule());
        rules.add(new CubesSortRule());
        rules.add(new AdjustForWeeklyMatchedRealization());//this rule might modify olapcontext content, better put it at last
    }

    public static void applyRules(List<IRealization> realizations, OLAPContext olapContext) {
        for (RoutingRule rule : rules) {
            logger.info("Initial realizations order:");
            logger.info(getPrintableText(realizations));
            logger.info("Applying rule " + rule);

            rule.apply(realizations, olapContext);

            logger.info(getPrintableText(realizations));
            logger.info("===================================================");
        }
    }

    public static String getPrintableText(List<IRealization> realizations) {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (IRealization r : realizations) {
            sb.append(r.getName());
            sb.append(",");
        }
        if (sb.charAt(sb.length() - 1) != '[')
            sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

    /**
     *
     * @param rule
     * @param applyOrder RoutingRule are applied in order, latter rules can override previous rules
     */
    public static void registerRule(RoutingRule rule, int applyOrder) {
        if (applyOrder > rules.size()) {
            logger.warn("apply order " + applyOrder + "  is larger than rules size " + rules.size() + ", will put the new rule at the end");
            rules.add(rule);
        }

        rules.add(applyOrder, rule);
    }

    public static void removeRule(RoutingRule rule) {
        for (Iterator<RoutingRule> iter = rules.iterator(); iter.hasNext();) {
            RoutingRule r = iter.next();
            if (r.getClass() == rule.getClass()) {
                iter.remove();
            }
        }
    }

    protected List<Integer> findRealizationsOf(List<IRealization> realizations, RealizationType type) {
        List<Integer> itemIndexes = Lists.newArrayList();
        for (int i = 0; i < realizations.size(); ++i) {
            if (realizations.get(i).getType() == type) {
                itemIndexes.add(i);
            }
        }
        return itemIndexes;
    }

    @Override
    public String toString() {
        return this.getClass().toString();
    }

    public abstract void apply(List<IRealization> realizations, OLAPContext olapContext);

}
