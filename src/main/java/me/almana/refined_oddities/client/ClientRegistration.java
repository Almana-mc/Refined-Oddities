package me.almana.refined_oddities.client;

import me.almana.refined_oddities.Refined_oddities;
import me.almana.refined_oddities.content.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Refined_oddities.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientRegistration {
    private ClientRegistration() {
    }

    @SubscribeEvent
    public static void registerScreens(final RegisterMenuScreensEvent event) {
        event.register(ModMenus.COMPRESSION_CONFIGURATION.get(), CompressionConfigurationScreen::new);
    }
}
