/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.core.entity.player;

import net.kyori.adventure.text.Component;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.cause.entity.MovementTypes;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.entity.RotateEntityEvent;
import org.spongepowered.api.event.entity.living.player.KickPlayerEvent;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.entity.player.ServerPlayerEntityBridge;
import org.spongepowered.common.bridge.permissions.SubjectBridge;
import org.spongepowered.common.bridge.world.PlatformITeleporterBridge;
import org.spongepowered.common.bridge.world.WorldBridge;
import org.spongepowered.common.entity.EntityUtil;
import org.spongepowered.common.entity.living.human.HumanEntity;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.hooks.PlatformHooks;
import org.spongepowered.common.profile.SpongeGameProfile;
import org.spongepowered.common.user.SpongeUserManager;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.portal.WrappedITeleporterPortalType;
import org.spongepowered.math.vector.Vector3d;

import javax.annotation.Nullable;

// See also: SubjectMixin_API and SubjectMixin
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntityMixin implements SubjectBridge, ServerPlayerEntityBridge {

    // @formatter:off

    @Shadow public ServerPlayNetHandler connection;
    @Shadow @Final public PlayerInteractionManager interactionManager;
    @Shadow @Final public MinecraftServer server;

    @Shadow public abstract net.minecraft.world.server.ServerWorld shadow$getServerWorld();
    @Shadow public abstract void shadow$setSpectatingEntity(Entity p_175399_1_);
    @Shadow public abstract void shadow$stopRiding();

    @Shadow public abstract void shadow$closeContainer();

    // @formatter:on

    @Shadow private int lastExperience;
    private final User impl$user = this.impl$getUserObjectOnConstruction();
    private @Nullable ITextComponent impl$connectionMessage;
    @Nullable private Vector3d impl$velocityOverride = null;

    @Override
    public @Nullable ITextComponent bridge$getConnectionMessageToSend() {
        if (this.impl$connectionMessage == null) {
            return new StringTextComponent("");
        }
        return this.impl$connectionMessage;
    }

    @Override
    public void bridge$setConnectionMessageToSend(final ITextComponent message) {
        this.impl$connectionMessage = message;
    }

    @Override
    public String bridge$getSubjectCollectionIdentifier() {
        return PermissionService.SUBJECTS_USER;
    }

    private User impl$getUserObjectOnConstruction() {
        if (this.impl$isFake) {
            return this.bridge$getUserObject();
        }
        // Ensure that the game profile is up to date.
        return ((SpongeUserManager) SpongeCommon.getGame().getServer().getUserManager()).forceRecreateUser(SpongeGameProfile.of(this.shadow$getGameProfile()));
    }

    @Override
    public User bridge$getUserObject() {
        return this.impl$user;
    }

    @Override
    public User bridge$getUser() {
        return this.impl$user;
    }

    // TODO: this, properly.
    @Override
    public boolean bridge$isVanished() {
        return false;
    }

    @Override
    public Tristate bridge$permDefault(final String permission) {
        return Tristate.FALSE;
    }

    /*
    @Inject(method = "markPlayerActive()V", at = @At("HEAD"))
    private void impl$onPlayerActive(final CallbackInfo ci) {
        ((ServerPlayNetHandlerBridge) this.connection).bridge$resendLatestResourcePackRequest();
    }
*/

    // Used to restore original item received in a packet after canceling an event
    private ItemStack impl$packetItem = ItemStack.EMPTY;

    @Override
    public void bridge$setPacketItem(final ItemStack itemstack) {
        this.impl$packetItem = itemstack;
    }

    @Override
    public boolean bridge$setLocation(final ServerLocation location) {
        if (this.removed || ((WorldBridge) location.getWorld()).bridge$isFake()) {
            return false;
        }

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(SpongeCommon.getActivePlugin());
            frame.addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.PLUGIN);

            ServerWorld destinationWorld = (net.minecraft.world.server.ServerWorld) location.getWorld();

            if (this.shadow$getServerWorld() != destinationWorld) {
                final ChangeEntityWorldEvent.Pre event = SpongeEventFactory.createChangeEntityWorldEventPre(frame.getCurrentCause(),
                        (org.spongepowered.api.entity.Entity) this, (org.spongepowered.api.world.server.ServerWorld) this.shadow$getServerWorld(),
                        location.getWorld(), location.getWorld());
                if (SpongeCommon.postEvent(event) && ((WorldBridge) event.getDestinationWorld()).bridge$isFake()) {
                    return false;
                }

                final ChangeEntityWorldEvent.Reposition repositionEvent =
                        SpongeEventFactory.createChangeEntityWorldEventReposition(frame.getCurrentCause(),
                                (org.spongepowered.api.entity.Entity) this, (org.spongepowered.api.world.server.ServerWorld) this.shadow$getServerWorld(),
                                VecHelper.toVector3d(this.shadow$getPositionVector()), location.getPosition(), event.getOriginalDestinationWorld(),
                                location.getPosition(), event.getDestinationWorld());

                if (SpongeCommon.postEvent(repositionEvent)) {
                    return false;
                }

                destinationWorld = (net.minecraft.world.server.ServerWorld) event.getDestinationWorld();

                this.shadow$setPosition(repositionEvent.getDestinationPosition().getX(), repositionEvent.getDestinationPosition().getY(),
                        repositionEvent.getDestinationPosition().getZ());
            } else {
                final MoveEntityEvent event = SpongeEventFactory.createMoveEntityEvent(frame.getCurrentCause(),
                        (org.spongepowered.api.entity.Entity) this, VecHelper.toVector3d(this.shadow$getPositionVector()),
                        location.getPosition(), location.getPosition());
                if (SpongeCommon.postEvent(event)) {
                    return false;
                }

                this.shadow$setPosition(event.getDestinationPosition().getX(), event.getDestinationPosition().getY(), event.getDestinationPosition().getZ());
            }

            ((ServerPlayerEntity) (Object) this).stopRiding();
            ((ServerPlayerEntity) (Object) this).setSpectatingEntity((Entity) (Object) this);

            if (((ServerPlayerEntity) (Object) this).isSleeping()) {
                ((ServerPlayerEntity) (Object) this).stopSleepInBed(true, true);
            }

            final ChunkPos chunkPos = new ChunkPos((int) this.shadow$getPosX() >> 4, (int) this.shadow$getPosZ() >> 4);
            destinationWorld.getChunkProvider().registerTicket(TicketType.POST_TELEPORT, chunkPos, 1, ((ServerPlayerEntity) (Object) this).getEntityId());
            ((ServerPlayerEntity) (Object) this).stopRiding();

            if (this.shadow$getServerWorld() != destinationWorld) {
                EntityUtil.performPostChangePlayerWorldLogic((ServerPlayerEntity) (Object) this, this.shadow$getServerWorld(),
                        (net.minecraft.world.server.ServerWorld) location.getWorld(), destinationWorld, false);
            } else {
                this.connection.setPlayerLocation(this.shadow$getPosX(), this.shadow$getPosY(), this.shadow$getPosZ(), this.rotationYaw,
                        this.rotationPitch);
                this.connection.captureCurrentPosition();
            }
        }

        return true;
    }

    /**
     * @author Zidane
     * @reason Ensure that the teleport hook honors our events
     */
    @Overwrite
    public void teleport(net.minecraft.world.server.ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        final ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        double actualX;
        double actualY;
        double actualZ;
        double actualYaw = yaw;
        double actualPitch = pitch;

        boolean hasMovementContext = PhaseTracker.getCauseStackManager().getCurrentContext().containsKey(EventContextKeys.MOVEMENT_TYPE);

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            if (!hasMovementContext) {
                frame.pushCause(SpongeCommon.getActivePlugin());
                frame.addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.PLUGIN);
            }

            if (world == player.world) {
                final MoveEntityEvent posEvent = SpongeEventFactory.createMoveEntityEvent(frame.getCurrentCause(),
                        (org.spongepowered.api.entity.Entity) player, VecHelper.toVector3d(player.getPositionVector()),
                        new Vector3d(x, y, z), new Vector3d(x, y, z));

                if (SpongeCommon.postEvent(posEvent)) {
                    return;
                }

                final RotateEntityEvent rotateEvent = SpongeEventFactory.createRotateEntityEvent(frame.getCurrentCause(),
                        (org.spongepowered.api.entity.Entity) player, new Vector3d(actualPitch, actualYaw, 0),
                        new Vector3d(pitch, yaw, 0));

                SpongeCommon.postEvent(rotateEvent);

                actualX = posEvent.getDestinationPosition().getX();
                actualY = posEvent.getDestinationPosition().getY();
                actualZ = posEvent.getDestinationPosition().getZ();
                actualYaw = rotateEvent.isCancelled() ? player.rotationYaw : rotateEvent.getToRotation().getY();
                actualPitch = rotateEvent.isCancelled() ? player.rotationPitch : rotateEvent.getToRotation().getX();

                this.shadow$setSpectatingEntity(player);
                this.shadow$stopRiding();

                if (player.isSleeping()) {
                    player.stopSleepInBed(true, true);
                }

                player.connection.setPlayerLocation(actualX, actualY, actualZ, (float) actualYaw, (float) actualPitch);

                player.setRotationYawHead((float) actualYaw);

                ChunkPos chunkpos = new ChunkPos(new BlockPos(actualX, actualY, actualZ));
                world.getChunkProvider().registerTicket(TicketType.POST_TELEPORT, chunkpos, 1, player.getEntityId());
            } else {
                final ChangeEntityWorldEvent.Pre preEvent = PlatformHooks.getInstance().getEventHooks().callChangeEntityWorldEventPre(player, world);
                if (SpongeCommon.postEvent(preEvent)) {
                    return;
                }

                final MoveEntityEvent posEvent = SpongeEventFactory.createChangeEntityWorldEventReposition(frame.getCurrentCause(),
                        (org.spongepowered.api.entity.Entity) player, preEvent.getOriginalWorld(), VecHelper.toVector3d(player.getPositionVector()),
                        new Vector3d(x, y, z), preEvent.getOriginalDestinationWorld(), new Vector3d(x, y, z), preEvent.getDestinationWorld());

                final RotateEntityEvent rotateEvent = SpongeEventFactory.createRotateEntityEvent(frame.getCurrentCause(),
                        (org.spongepowered.api.entity.Entity) player, new Vector3d(actualYaw, actualPitch, 0),
                        new Vector3d(yaw, pitch, 0));

                if (SpongeCommon.postEvent(posEvent)) {
                    return;
                }

                this.shadow$setPosition(posEvent.getDestinationPosition().getX(), posEvent.getDestinationPosition().getY(),
                        posEvent.getDestinationPosition().getZ());

                if (!SpongeCommon.postEvent(rotateEvent)) {
                    this.rotationYaw = (float) rotateEvent.getToRotation().getX();
                    this.rotationPitch = (float) rotateEvent.getToRotation().getY();
                }

                EntityUtil.performPostChangePlayerWorldLogic(player, (net.minecraft.world.server.ServerWorld) preEvent.getOriginalWorld(),
                        (net.minecraft.world.server.ServerWorld) preEvent.getOriginalDestinationWorld(),
                        (net.minecraft.world.server.ServerWorld) preEvent.getDestinationWorld(), false);
            }
        }
    }

    /**
     * @author Zidane
     * @reason Call to EntityUtil to handle dimension changes
     */
    @Nullable
    @Overwrite
    public Entity changeDimension(DimensionType destination) {
        if (this.shadow$getEntityWorld().isRemote || this.removed) {
            return (ServerPlayerEntity) (Object) this;
        }

        final WrappedITeleporterPortalType portalType = new WrappedITeleporterPortalType((PlatformITeleporterBridge) this.shadow$getServer()
                .getWorld(destination).getDefaultTeleporter(), null);

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(this);
            frame.pushCause(portalType);
            frame.addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.PORTAL);

            EntityUtil.invokePortalTo((ServerPlayerEntity) (Object) this, portalType, destination);
            return (ServerPlayerEntity) (Object) this;
        }
    }

    @Override
    public void bridge$refreshExp() {
        this.lastExperience = -1;
    }

    @Override
    public boolean bridge$kick(final Component message) {
        final Component messageToSend;
        if (ShouldFire.KICK_PLAYER_EVENT) {
            final KickPlayerEvent kickEvent = SpongeEventFactory.createKickPlayerEvent(PhaseTracker.getCauseStackManager().getCurrentCause(),
                message,
                message,
                (ServerPlayer) this
                );
            if (Sponge.getEventManager().post(kickEvent)) {
                return false;
            }
            messageToSend = kickEvent.getMessage();
        } else {
            messageToSend = message;
        }
        final ITextComponent component = SpongeAdventure.asVanilla(messageToSend);
        this.connection.disconnect(component);
        return true;
    }

    @Redirect(
        method = {"openContainer", "openHorseInventory"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/ServerPlayerEntity;closeScreen()V"
        )
    )

    private void impl$closePreviousContainer(final ServerPlayerEntity self) {
        this.shadow$closeContainer();
    }

    @Override
    public void bridge$setVelocityOverride(@Nullable final Vector3d velocity) {
        this.impl$velocityOverride = velocity;
    }

    @Override
    @Nullable
    public Vector3d bridge$getVelocityOverride() {
        return this.impl$velocityOverride;
    }

    @Inject(method = "removeEntity", at = @At("RETURN"))
    private void impl$removeHumanFromPlayerClient(final Entity entityIn, final CallbackInfo ci) {
        if (entityIn instanceof HumanEntity) {
            ((HumanEntity) entityIn).untrackFrom((ServerPlayerEntity) (Object) this);
        }
    }
}
