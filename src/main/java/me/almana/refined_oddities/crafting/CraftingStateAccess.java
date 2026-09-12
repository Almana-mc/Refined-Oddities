package me.almana.refined_oddities.crafting;

import java.util.List;

public interface CraftingStateAccess {
    void refinedOddities$initialize(List<CompressionStorageHandle> handles);

    void refinedOddities$copyFrom(CompressionPlanningState pools);
}
