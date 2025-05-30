package com.gmail.snorrethedev.customportalcreatetraincompat;

import com.mojang.logging.LogUtils;
import com.simibubi.create.api.contraption.train.PortalTrackProvider;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.BlockFace;
import net.kyrptonaught.customportalapi.CustomPortalApiRegistry;
import net.kyrptonaught.customportalapi.util.CustomPortalHelper;
import net.kyrptonaught.customportalapi.util.CustomTeleporter;
import net.kyrptonaught.customportalapi.util.PortalLink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.simibubi.create.content.trains.track.AllPortalTracks;

import javax.annotation.Nullable;
import java.util.function.Function;

@Mod(CustomPortalCreateTrainCompat.MODID)
public class CustomPortalCreateTrainCompat {

    public static final String MODID = "customportalcreatetraincompat";

    private static final Logger LOGGER = LogUtils.getLogger();

    public CustomPortalCreateTrainCompat() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
        CustomPortalApiRegistry.getAllPortalLinks().forEach(pl -> PortalTrackProvider.REGISTRY.register(pl.getPortalBlock(), (p,face) -> createPortalTrackProvider(p, face, pl)));
        });
    }

    private static PortalTrackProvider.Exit createPortalTrackProvider(ServerLevel inbound, BlockFace face, PortalLink portalLink) {
        ResourceKey<Level> trainDepot = ResourceKey.create(Registries.DIMENSION, portalLink.dimID);
        return CustomPortalCreateTrainCompat.standardPortalProvider(Pair.of(inbound,face), Level.OVERWORLD, trainDepot, (sl) -> CustomPortalCreateTrainCompat.wrapCustomTeleporter(Pair.of(inbound,face), portalLink));
    }

    public static ITeleporter wrapCustomTeleporter(Pair<ServerLevel, BlockFace> inbound, PortalLink portalLink) {
        return new ITeleporter() {

            @Override
            public @Nullable PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {

                return CustomTeleporter.customTPTarget(
                        destWorld,
                        entity,
                        inbound.getSecond().getConnectedPos(),
                        CustomPortalHelper.getPortalBase(inbound.getFirst(), entity.getOnPos()),
                        portalLink.getFrameTester());
            }

        };
    }

    /**
     * This method is nearly exactly the same as the on in {@link com.simibubi.create.content.trains.track.AllPortalTracks#standardPortalProvider}
     * with just one difference: instead of using <pre>BlockStateProperties.HORIZONTAL_AXIS</pre> it uses <pre>BlockStateProperties.AXIS</pre>
     * since the is what CustomPortalAPI uses internally
     * @param inbound
     * @param firstDimension
     * @param secondDimension
     * @param customPortalForcer
     * @return
     */
    public static PortalTrackProvider.Exit standardPortalProvider(Pair<ServerLevel, BlockFace> inbound,
                                                                  ResourceKey<Level> firstDimension, ResourceKey<Level> secondDimension,
                                                                  Function<ServerLevel, ITeleporter> customPortalForcer) {
        ServerLevel level = inbound.getFirst();
        ResourceKey<Level> resourcekey = level.dimension() == secondDimension ? firstDimension : secondDimension;
        MinecraftServer minecraftserver = level.getServer();
        ServerLevel otherLevel = minecraftserver.getLevel(resourcekey);

        if (otherLevel == null || !minecraftserver.isNetherEnabled())
            return null;

        BlockFace inboundTrack = inbound.getSecond();
        BlockPos portalPos = inboundTrack.getConnectedPos();
        BlockState portalState = level.getBlockState(portalPos);
        ITeleporter teleporter = customPortalForcer.apply(otherLevel);

        SuperGlueEntity probe = new SuperGlueEntity(level, new AABB(portalPos));
        probe.setYRot(inboundTrack.getFace()
                .toYRot());
        probe.setPortalEntrancePos();

        PortalInfo portalinfo = teleporter.getPortalInfo(probe, otherLevel, probe::findDimensionEntryPoint);
        if (portalinfo == null)
            return null;

        BlockPos otherPortalPos = BlockPos.containing(portalinfo.pos);
        BlockState otherPortalState = otherLevel.getBlockState(otherPortalPos);
        if (otherPortalState.getBlock() != portalState.getBlock())
            return null;

        Direction targetDirection = inboundTrack.getFace();
        if (targetDirection.getAxis() == otherPortalState.getValue(BlockStateProperties.AXIS))
            targetDirection = targetDirection.getClockWise();
        BlockPos otherPos = otherPortalPos.relative(targetDirection);
        return new PortalTrackProvider.Exit(otherLevel, new BlockFace(otherPos, targetDirection.getOpposite()));
    }

}