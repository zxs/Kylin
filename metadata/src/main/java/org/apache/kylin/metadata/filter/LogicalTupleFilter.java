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

package org.apache.kylin.metadata.filter;

import org.apache.kylin.metadata.tuple.ITuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


public class LogicalTupleFilter extends TupleFilter {

    public LogicalTupleFilter(FilterOperatorEnum op) {
        super(new ArrayList<TupleFilter>(2), op);
        boolean opGood = (op == FilterOperatorEnum.AND || op == FilterOperatorEnum.OR || op == FilterOperatorEnum.NOT);
        if (opGood == false)
            throw new IllegalArgumentException("Unsupported operator " + op);
    }

    private LogicalTupleFilter(List<TupleFilter> filters, FilterOperatorEnum op) {
        super(filters, op);
    }

    @Override
    public TupleFilter copy() {
        List<TupleFilter> cloneChildren = new LinkedList<TupleFilter>(children);
        TupleFilter cloneTuple = new LogicalTupleFilter(cloneChildren, operator);
        return cloneTuple;
    }

    @Override
    public TupleFilter reverse() {
        switch (operator) {
        case NOT:
            assert (children.size() == 1);
            return children.get(0);
        case AND:
        case OR:
            LogicalTupleFilter reverse = new LogicalTupleFilter(REVERSE_OP_MAP.get(operator));
            for (TupleFilter child : children) {
                reverse.addChild(child.reverse());
            }
            return reverse;
        default:
            throw new IllegalStateException();
        }
    }

    @Override
    public String toString() {
        return "LogicalFilter [operator=" + operator + ", children=" + children + "]";
    }

    @Override
    public boolean evaluate(ITuple tuple) {
        switch (this.operator) {
        case AND:
            return evalAnd(tuple);
        case OR:
            return evalOr(tuple);
        case NOT:
            return evalNot(tuple);
        default:
            return false;
        }
    }

    private boolean evalAnd(ITuple tuple) {
        for (TupleFilter filter : this.children) {
            if (!filter.evaluate(tuple)) {
                return false;
            }
        }
        return true;
    }

    private boolean evalOr(ITuple tuple) {
        for (TupleFilter filter : this.children) {
            if (filter.evaluate(tuple)) {
                return true;
            }
        }
        return false;
    }

    private boolean evalNot(ITuple tuple) {
        return !this.children.get(0).evaluate(tuple);
    }

    @Override
    public Collection<String> getValues() {
        return Collections.emptyList();
    }

    @Override
    public boolean isEvaluable() {
        return true;
    }

    @Override
    public byte[] serialize() {
        return new byte[0];
    }

    @Override
    public void deserialize(byte[] bytes) {
    }

}
