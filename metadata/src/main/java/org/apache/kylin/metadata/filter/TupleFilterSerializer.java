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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.kylin.common.util.BytesUtil;

/**
 * http://eli.thegreenplace.net/2011/09/29/an-interesting-tree-serialization-algorithm-from-dwarf
 * 
 * @author xjiang
 * 
 */
public class TupleFilterSerializer {

    public static interface Decorator {
        TupleFilter onSerialize(TupleFilter filter);
    }

    private static final int BUFFER_SIZE = 65536;
    private static final Map<Integer, TupleFilter.FilterOperatorEnum> ID_OP_MAP = new HashMap<Integer, TupleFilter.FilterOperatorEnum>();

    static {
        for (TupleFilter.FilterOperatorEnum op : TupleFilter.FilterOperatorEnum.values()) {
            ID_OP_MAP.put(op.getValue(), op);
        }
    }

    public static byte[] serialize(TupleFilter rootFilter) {
        return serialize(rootFilter, null);
    }

    public static byte[] serialize(TupleFilter rootFilter, Decorator decorator) {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        internalSerialize(rootFilter, decorator, buffer);
        byte[] result = new byte[buffer.position()];
        System.arraycopy(buffer.array(), 0, result, 0, buffer.position());
        return result;
    }

    private static void internalSerialize(TupleFilter filter, Decorator decorator, ByteBuffer buffer) {
        if (decorator != null) { // give decorator a chance to manipulate the
                                 // output filter
            filter = decorator.onSerialize(filter);
        }

        if (filter == null) {
            return;
        }

        if (filter.hasChildren()) {
            // serialize filter+true
            serializeFilter(1, filter, decorator, buffer);
            // serialize children
            for (TupleFilter child : filter.getChildren()) {
                internalSerialize(child, decorator, buffer);
            }
            // serialize none
            serializeFilter(-1, filter, decorator, buffer);
        } else {
            // serialize filter+false
            serializeFilter(0, filter, decorator, buffer);
        }
    }

    private static void serializeFilter(int flag, TupleFilter filter, Decorator decorator, ByteBuffer buffer) {
        if (flag < 0) {
            BytesUtil.writeVInt(-1, buffer);
        } else {
            byte[] bytes = filter.serialize();
            int opVal = filter.getOperator().getValue();
            BytesUtil.writeVInt(opVal, buffer);
            BytesUtil.writeByteArray(bytes, buffer);
            BytesUtil.writeVInt(flag, buffer);
        }
    }

    public static TupleFilter deserialize(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        TupleFilter rootFilter = null;
        Stack<TupleFilter> parentStack = new Stack<TupleFilter>();
        while (buffer.hasRemaining()) {
            int opVal = BytesUtil.readVInt(buffer);
            if (opVal < 0) {
                parentStack.pop();
                continue;
            }

            // deserialize filter
            TupleFilter filter = createTupleFilter(opVal);
            byte[] filetrBytes = BytesUtil.readByteArray(buffer);
            filter.deserialize(filetrBytes);

            if (rootFilter == null) {
                // push root to stack
                rootFilter = filter;
                parentStack.push(filter);
                BytesUtil.readVInt(buffer);
                continue;
            }

            // add filter to parent
            TupleFilter parentFilter = parentStack.peek();
            if (parentFilter != null) {
                parentFilter.addChild(filter);
            }

            // push filter to stack or not based on having children or not
            int hasChild = BytesUtil.readVInt(buffer);
            if (hasChild == 1) {
                parentStack.push(filter);
            }
        }
        return rootFilter;
    }

    private static TupleFilter createTupleFilter(int opVal) {
        TupleFilter.FilterOperatorEnum op = ID_OP_MAP.get(opVal);
        if (op == null) {
            throw new IllegalStateException("operator value is " + opVal);
        }
        TupleFilter filter = null;
        switch (op) {
        case AND:
        case OR:
        case NOT:
            filter = new LogicalTupleFilter(op);
            break;
        case EQ:
        case NEQ:
        case LT:
        case LTE:
        case GT:
        case GTE:
        case IN:
        case ISNULL:
        case ISNOTNULL:
            filter = new CompareTupleFilter(op);
            break;
        case EXTRACT:
            filter = new ExtractTupleFilter(op);
            break;
        case CASE:
            filter = new CaseTupleFilter();
            break;
        case COLUMN:
            filter = new ColumnTupleFilter(null);
            break;
        case CONSTANT:
            filter = new ConstantTupleFilter();
            break;
        case DYNAMIC:
            filter = new DynamicTupleFilter(null);
            break;
        default:
            throw new IllegalStateException("Error FilterOperatorEnum: " + op.getValue());
        }

        return filter;
    }
}
