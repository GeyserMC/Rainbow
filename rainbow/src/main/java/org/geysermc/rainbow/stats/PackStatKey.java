/*
 * Copyright (c) 2026 GeyserMC. https://geysermc.org
 *
 * This file is part of Rainbow.
 *
 * Rainbow is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Rainbow is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * Rainbow. If not, see <https://www.gnu.org/licenses/>.
 */

package org.geysermc.rainbow.stats;

import org.apache.commons.lang3.StringUtils;

@FunctionalInterface
public interface PackStatKey {

    String toString(PackStats stats);

    record Unit() implements PackStatKey {

        @Override
        public String toString(PackStats stats) {
            return "";
        }
    }

    record Single(String humanName, TaskType taskType) implements PackStatKey {

        public String toString(PackStats stats) {
            return StringUtils.capitalize(humanName) + (taskType == TaskType.NONE ? "" : " " + taskType.humanName) + ": " + stats.get(this);
        }

        public enum TaskType {
            NONE(""),
            WRITTEN("written"),
            EXPORTED("exported");

            private final String humanName;

            TaskType(String humanName) {
                this.humanName = humanName;
            }
        }
    }

    record AssetCacheStatKey(String humanName, PackStatKey size, PackStatKey hits) implements PackStatKey {

        public AssetCacheStatKey(String humanName) {
            this(humanName, new Unit(), new Unit());
        }

        @Override
        public String toString(PackStats stats) {
            return StringUtils.capitalize(humanName) + " cache: " + stats.get(size) + " written, " + stats.get(hits) + " cache hits";
        }
    }
}
