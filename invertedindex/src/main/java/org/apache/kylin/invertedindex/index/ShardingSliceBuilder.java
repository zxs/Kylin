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

package org.apache.kylin.invertedindex.index;

import java.util.List;

import com.google.common.collect.Lists;

public class ShardingSliceBuilder {

	SliceBuilder[] builders;

	public ShardingSliceBuilder(TableRecordInfo info) {
		int sharding = info.getDescriptor().getSharding();
		builders = new SliceBuilder[sharding];
		for (short i = 0; i < sharding; i++) {
			builders[i] = new SliceBuilder(info, i);
		}
	}

	// NOTE: record must be appended in time order
	public Slice append(TableRecord rec) {
		short shard = rec.getShard();
		return builders[shard].append(rec);
	}

	public List<Slice> close() {
		List<Slice> result = Lists.newArrayList();
		for (SliceBuilder builder : builders) {
			Slice slice = builder.close();
			if (slice != null)
				result.add(slice);
		}
		return result;
	}

}
