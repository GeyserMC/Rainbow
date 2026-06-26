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
