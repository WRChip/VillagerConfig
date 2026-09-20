package me.drex.villagerconfig.common.config;

import io.github.fablabsmc.fablabs.api.fiber.v1.annotation.AnnotatedSettings;
import io.github.fablabsmc.fablabs.api.fiber.v1.annotation.SettingNamingConvention;
import io.github.fablabsmc.fablabs.api.fiber.v1.exception.FiberException;
import io.github.fablabsmc.fablabs.api.fiber.v1.serialization.FiberSerialization;
import io.github.fablabsmc.fablabs.api.fiber.v1.serialization.JanksonValueSerializer;
import io.github.fablabsmc.fablabs.api.fiber.v1.tree.ConfigTree;
import me.drex.villagerconfig.common.VillagerConfig;
import me.drex.villagerconfig.common.mixin.GossipTypeAccessor;
import me.drex.villagerconfig.common.platform.PlatformHooks;
import net.minecraft.world.entity.ai.gossip.GossipType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    public static final Path CONFIG_PATH = PlatformHooks.PLATFORM_HELPER.getModConfigDir().resolve("villagerconfig.json5");
    private static final AnnotatedSettings ANNOTATED_SETTINGS = AnnotatedSettings.builder()
            .useNamingConvention(SettingNamingConvention.SNAKE_CASE)
            .build();
    public static final Config CONFIG = new Config();
    public static final ConfigTree TREE = ConfigTree.builder()
            .applyFromPojo(CONFIG, ANNOTATED_SETTINGS)
            .build();
    private static final JanksonValueSerializer serializer = new JanksonValueSerializer(false);
    private static int vanillaMajorCureGossip = -1;
    private static int vanillaMinorCureGossip = -1;

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                ANNOTATED_SETTINGS.applyToNode(TREE, CONFIG);
                FiberSerialization.deserialize(TREE, Files.newInputStream(CONFIG_PATH), serializer);
            } catch (IOException | FiberException e) {
                VillagerConfig.LOGGER.error("Failed to load config file!", e);
            }
        } else {
            saveModConfig();
        }
        applyCureStacking();
    }

    public static int scaleCureGossip(int vanillaAmount) {
        return Math.max(0, (int) Math.round(vanillaAmount * CONFIG.features.cureMultiplier));
    }

    // curing adds a fixed amount of gossip, so the caps have to grow with it for repeated or stronger cures to count
    public static void applyCureStacking() {
        if (vanillaMajorCureGossip < 0) {
            vanillaMajorCureGossip = GossipType.MAJOR_POSITIVE.max;
            vanillaMinorCureGossip = GossipType.MINOR_POSITIVE.max;
        }
        int cures = CONFIG.features.maxCures < 0 ? 1 : CONFIG.features.maxCures;
        ((GossipTypeAccessor) (Object) GossipType.MAJOR_POSITIVE).setMax(scaleCureGossip(vanillaMajorCureGossip) * cures);
        ((GossipTypeAccessor) (Object) GossipType.MINOR_POSITIVE).setMax(scaleCureGossip(vanillaMinorCureGossip) * cures);
    }

    public static void saveModConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            ANNOTATED_SETTINGS.applyToNode(TREE, CONFIG);
            FiberSerialization.serialize(TREE, Files.newOutputStream(CONFIG_PATH), serializer);
        } catch (IOException | FiberException e) {
            VillagerConfig.LOGGER.error("Failed to save config file!", e);
        }
        applyCureStacking();
    }

}
