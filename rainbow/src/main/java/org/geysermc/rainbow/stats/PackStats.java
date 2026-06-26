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

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

@FunctionalInterface
public interface PackStats {

    int get(PackStatKey stat);

    static Collector collector() {
        return new Collector();
    }

    @FunctionalInterface
    interface Holder {

        int stat();
    }

    @FunctionalInterface
    interface Aggregator {

        void collectStats(Collector collector);
    }

    class Collector {
        private final Reference2IntMap<PackStatKey> map = new Reference2IntOpenHashMap<>();

        public Collector collect(PackStatKey key, int count) {
            map.mergeInt(key, count, Integer::sum);
            return this;
        }

        public Collector collect(PackStatKey key, Holder holder) {
            return collect(key, holder.stat());
        }

        public Collector collect(Aggregator aggregator) {
            aggregator.collectStats(this);
            return this;
        }

        public PackStats finish() {
            Reference2IntMap<PackStatKey> copy = new Reference2IntOpenHashMap<>(map);
            return stat -> copy.getOrDefault(stat, 0);
        }
    }
}
