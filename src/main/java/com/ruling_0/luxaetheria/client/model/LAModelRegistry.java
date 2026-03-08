package com.ruling_0.luxaetheria.client.model;

import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.gtnewhorizon.gtnhlib.client.model.loading.ResourceLoc;
import com.gtnewhorizon.gtnhlib.concurrent.ThreadsafeCache;

public class LAModelRegistry {

    private static final ResourceLoc.ModelLoc AETHER_RELAY_LOC = new ResourceLoc.ModelLoc("luxaetheria",
        "blocks/aether_relay");

    private static final ThreadsafeCache<ModelAetherRelay.RelayBakeData, BakedModel> AETHER_RELAY_CACHE = new ThreadsafeCache<>(
        64, key -> {
            final var jsonModel = ModelRegistry.getJSONModel(AETHER_RELAY_LOC);
            final var model = new ModelAetherRelay(jsonModel);
            return model.bake((ModelAetherRelay.RelayBakeData) key);
        }, false);

    public static BakedModel getAetherRelayModel(ModelAetherRelay.RelayBakeData data) {
        return AETHER_RELAY_CACHE.get(data);
    }
}
