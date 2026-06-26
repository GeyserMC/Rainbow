package org.geysermc.rainbow.mapping;

import org.geysermc.rainbow.pack.BedrockBlock;
import org.geysermc.rainbow.pack.BedrockItem;
import org.geysermc.rainbow.pack.BedrockWaypointStyle;

public interface BedrockAssetConsumer {

    void acceptBlock(BedrockBlock block);

    void acceptItem(BedrockItem item);

    void acceptWaypointStyle(BedrockWaypointStyle style);
}
