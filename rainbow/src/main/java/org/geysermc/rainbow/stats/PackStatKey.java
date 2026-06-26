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
