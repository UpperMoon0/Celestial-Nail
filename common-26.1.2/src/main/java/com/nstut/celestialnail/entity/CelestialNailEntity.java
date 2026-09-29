package com.nstut.celestialnail.entity;

import com.nstut.celestialnail.CelestialNailMath;
import com.nstut.celestialnail.CelestialNailVisuals;
import com.nstut.celestialnail.FluidPurgeCursor;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CelestialNailEntity extends Entity {
    public static final float DEFAULT_POWER = 32.0F;
    public static final float MIN_POWER = 4.0F;
    public static final float MAX_POWER = 128.0F;
    private static final byte PHASE_IDLE = 0;
    private static final byte PHASE_DESCENDING = 1;
    private static final byte PHASE_IMPACT = 2;
    private static final int MAX_BLOCK_CHANGES_PER_TICK = 3000;
    private static final int MAX_SCAN_STEPS_PER_TICK = 45000;
    private static final EntityDataAccessor<String> DATA_NAIL_ID = SynchedEntityData.defineId(CelestialNailEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_POWER = SynchedEntityData.defineId(CelestialNailEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> DATA_PHASE = SynchedEntityData.defineId(CelestialNailEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_WAVE_RADIUS = SynchedEntityData.defineId(CelestialNailEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Float> DATA_SCALE = SynchedEntityData.defineId(CelestialNailEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> DATA_SUMMON_TIME = SynchedEntityData.defineId(CelestialNailEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_LAUNCH_TIME = SynchedEntityData.defineId(CelestialNailEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> DATA_PORTAL_Y = SynchedEntityData.defineId(CelestialNailEntity.class, EntityDataSerializers.FLOAT);
    public static java.util.function.Consumer<CelestialNailEntity> clientVisualTick = nail -> {};
    public boolean portalSoundStarted;
    private static final EntityDataAccessor<Long> DATA_IMPACT_TIME = SynchedEntityData.defineId(CelestialNailEntity.class, EntityDataSerializers.LONG);
    private long purgeIndex;
    private int purgePass, purgeWait;
    private double descentSpeed;
    private BlockPos impactCenter = BlockPos.ZERO;
    private int shellRadius;
    private int scanX;
    private int scanY;
    private int scanZ;
    private boolean ownsForcedChunk;
    private boolean fluidPurgeActive;
    private final Set<Long> ownedImpactForcedChunks = new HashSet<>();

    public CelestialNailEntity(EntityType<? extends CelestialNailEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_NAIL_ID, "");
        builder.define(DATA_POWER, DEFAULT_POWER);
        builder.define(DATA_PHASE, PHASE_IDLE);
        builder.define(DATA_WAVE_RADIUS, 0);
        builder.define(DATA_SCALE, 1.0F);
        builder.define(DATA_SUMMON_TIME, -10000L);
        builder.define(DATA_LAUNCH_TIME, -1L);
        builder.define(DATA_PORTAL_Y, 0.0F);
        builder.define(DATA_IMPACT_TIME, -1L);
    }

    public void configure(String id, float power) {
        this.entityData.set(DATA_NAIL_ID, id);
        this.entityData.set(DATA_POWER, power);
        this.setCustomName(Component.literal(id));
        this.setCustomNameVisible(true);
        this.setInvulnerable(true);
    }

    public String nailId() { return this.entityData.get(DATA_NAIL_ID); }
    public float power() { return this.entityData.get(DATA_POWER); }
    public boolean isLaunched() { return this.entityData.get(DATA_PHASE) != PHASE_IDLE; }
    public int waveRadius() { return this.entityData.get(DATA_WAVE_RADIUS); }

    public float nailScale() { return this.entityData.get(DATA_SCALE); }
    public float nailHeight() { return CelestialNailVisuals.height(nailScale()); }
    public float portalY() { return this.entityData.get(DATA_PORTAL_Y); }
    public float summonAge(float partial) { return (float)(this.level().getGameTime()-this.entityData.get(DATA_SUMMON_TIME))+partial; }
    public float launchAge(float partial) {
        long started=this.entityData.get(DATA_LAUNCH_TIME);
        return started < 0 ? -1 : (float)(this.level().getGameTime()-started)+partial;
    }
    public float impactAge(float partial) {
        long started=this.entityData.get(DATA_IMPACT_TIME);
        return started < 0 ? -1 : (float)(this.level().getGameTime()-started)+partial;
    }
    @Override
    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        // Tracking and client horizontal fade retain the world's normal chunk range.
        double dx=getX()-cameraX, dz=getZ()-cameraZ;
        return shouldRenderAtSqrDistance(dx*dx+dz*dz);
    }
    public boolean isImpacting() { return this.entityData.get(DATA_PHASE) == PHASE_IMPACT; }
    public void beginSummoning(float scale) {
        this.entityData.set(DATA_SCALE, CelestialNailVisuals.safeScale(scale));
        this.entityData.set(DATA_SUMMON_TIME, this.level().getGameTime());
        this.entityData.set(DATA_LAUNCH_TIME, -1L);
        this.entityData.set(DATA_PORTAL_Y, (float)(this.getY()+CelestialNailVisuals.portalHeight(nailHeight())));
        this.refreshDimensions();
    }
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(nailHeight()*.13F, nailHeight());
    }
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_SCALE.equals(key)) this.refreshDimensions();
    }
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range=Math.max(512, nailHeight()*12);
        return distance < range*range;
    }
    public AABB visualBounds() {
        if(isImpacting())return new AABB(getX()-768,getY()-power(),getZ()-768,getX()+768,getY()+nailHeight()*5,getZ()+768);
        double h=nailHeight(), r=h*.65;
        return new AABB(this.getX()-r, this.getY()-h*.02, this.getZ()-r,
                this.getX()+r, Math.max(this.getY()+h, portalY()+h*1.4), this.getZ()+r);
    }

    public boolean launch() {
        if (this.entityData.get(DATA_PHASE) != PHASE_IDLE || summonAge(0) < CelestialNailVisuals.READY_TICKS) return false;
        this.entityData.set(DATA_PHASE, PHASE_DESCENDING);
        this.entityData.set(DATA_LAUNCH_TIME, this.level().getGameTime());
        this.descentSpeed = 1.25;
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) clientVisualTick.accept(this);
        if (this.level() instanceof ServerLevel serverLevel) forceOwnChunk(serverLevel);
        byte phase = this.entityData.get(DATA_PHASE);
        if (phase == PHASE_IDLE) {
            this.setDeltaMovement(Vec3.ZERO);
        } else if (phase == PHASE_DESCENDING) {
            if (!this.level().isClientSide()) tickDescending();
        } else if (phase == PHASE_IMPACT && !this.level().isClientSide()) {
            tickImpactWave((ServerLevel)this.level());
        }
    }

    private void tickDescending() {
        if (launchAge(0) < CelestialNailVisuals.CLOSE_TICKS) return;
        this.descentSpeed = Math.min(8.0, Math.max(1.25, this.descentSpeed + 0.22));
        Vec3 from = this.position();
        Vec3 to = from.add(0.0, -this.descentSpeed, 0.0);
        BlockHitResult hit = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (!this.level().isClientSide() && hit.getType() != HitResult.Type.MISS) {
            beginImpact((ServerLevel)this.level(), hit.getBlockPos(), hit.getLocation());
            return;
        }
        this.setDeltaMovement(0.0, -this.descentSpeed, 0.0);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (!this.level().isClientSide()) {
            ServerLevel server = (ServerLevel)this.level();
            server.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + nailHeight()*.2, this.getZ(), 8, nailHeight()*.025, nailHeight()*.12, nailHeight()*.025, 0.02);
            if (this.getY() <= server.getMinY()) beginImpact(server, BlockPos.containing(this.getX(), server.getMinY(), this.getZ()), new Vec3(this.getX(), server.getMinY(), this.getZ()));
        }
    }

    private void beginImpact(ServerLevel level, BlockPos center, Vec3 tipPosition) {
        this.entityData.set(DATA_PHASE, PHASE_IMPACT);
        this.entityData.set(DATA_IMPACT_TIME, level.getGameTime());
        this.setDeltaMovement(Vec3.ZERO);
        this.impactCenter = center.immutable();
        this.setPos(tipPosition.x, tipPosition.y, tipPosition.z);
        this.shellRadius = 0;
        resetScanForShell();
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 6, 1.2, 1.2, 1.2, 0.0);
        damageEntities(level);
    }

    private void damageEntities(ServerLevel level) {
        double radius = this.power() * 1.25;
        AABB box = new AABB(this.impactCenter).inflate(radius);
        for (Entity entity : level.getEntities(this, box, e -> e instanceof LivingEntity)) {
            double distance = Math.sqrt(entity.distanceToSqr(Vec3.atCenterOf(this.impactCenter)));
            if (distance > radius) continue;
            float damage = (float)Math.max(8.0, (1.0 - distance / radius) * this.power() * 8.0);
            ((LivingEntity)entity).hurtServer(level, level.damageSources().magic(), damage);
        }
    }

    private void tickImpactWave(ServerLevel level) {
        if (this.fluidPurgeActive) { tickFluidPurge(level); return; }
        int targetRadius = CelestialNailMath.targetRadius(this.power());
        int changed = 0;
        int scanned = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        while (this.shellRadius <= targetRadius && changed < MAX_BLOCK_CHANGES_PER_TICK && scanned < MAX_SCAN_STEPS_PER_TICK) {
            int dx = this.scanX;
            int dy = this.scanY;
            int dz = this.scanZ;
            cursor.set(this.impactCenter.getX() + dx, this.impactCenter.getY() + dy, this.impactCenter.getZ() + dz);
            if (cursor.getY() >= level.getMinY() && cursor.getY() < level.getMaxY()) {
                if (!ensureImpactChunkReady(level, cursor)) {
                    this.entityData.set(DATA_WAVE_RADIUS, displayedWaveRadius());
                    return;
                }
                var state = level.getBlockState(cursor);
                // UPDATE_KNOWN_SHAPE prevents neighbor shape updates from scheduling new fluid
                // cascades behind an inside-out wave. No drops, including waterlogged containers.
                if (!state.isAir() && level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2 | 16 | 32)) changed++;
            }
            scanned++;
            advanceScan();
        }
        this.entityData.set(DATA_WAVE_RADIUS, displayedWaveRadius());
        if (this.shellRadius > targetRadius) {
            this.fluidPurgeActive = true;
            this.purgeIndex = 0;
            this.purgePass = 0;
            this.entityData.set(DATA_WAVE_RADIUS, targetRadius);
        }
    }

    private void tickFluidPurge(ServerLevel level) {
        if (this.purgeWait > 0) { this.purgeWait--; return; }
        FluidPurgeCursor scan = new FluidPurgeCursor(CelestialNailMath.targetRadius(power()), this.purgeIndex);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int scanned=0, changed=0;
        while (!scan.done() && scanned < MAX_SCAN_STEPS_PER_TICK && changed < MAX_BLOCK_CHANGES_PER_TICK) {
            pos.set(impactCenter.getX()+scan.x(), impactCenter.getY()+scan.y(), impactCenter.getZ()+scan.z());
            if (scan.inside() && pos.getY() >= level.getMinY() && pos.getY() < level.getMaxY()) {
                if (!ensureImpactChunkReady(level, pos)) { this.purgeIndex=scan.index(); return; }
                var state=level.getBlockState(pos);
                if (!state.getFluidState().isEmpty()) {
                    var dry=state.hasProperty(BlockStateProperties.WATERLOGGED)
                            ? state.setValue(BlockStateProperties.WATERLOGGED, false) : Blocks.AIR.defaultBlockState();
                    if (level.setBlock(pos, dry, 2 | 16 | 32)) changed++;
                }
            }
            scanned++;
            scan.advance();
        }
        this.purgeIndex=scan.index();
        if (!scan.done()) return;
        if (this.purgePass++ == 0) {
            // Let already queued water/lava ticks settle, then remove residual flowing states
            // in gravity order. This is bounded; surviving water outside the blast is untouched.
            this.purgeIndex=0;
            this.purgeWait=40;
            return;
        }
        level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 160,
                this.power()*.35, this.power()*.18, this.power()*.35, .12);
        releaseImpactForcedChunks(level);
        releaseForcedChunk(level);
        this.discard();
    }

    private int displayedWaveRadius() {
        return this.fluidPurgeActive ? CelestialNailMath.targetRadius(this.power()) : this.shellRadius;
    }

    private void resetScanForShell() {
        int r = this.shellRadius;
        this.scanX = -r;
        this.scanY = -r - 1;
        moveToNextColumn();
    }

    private void advanceScan() {
        int r = this.shellRadius;
        int outer = CelestialNailMath.outerZExtent(this.scanX, this.scanY, r);
        int inner = CelestialNailMath.innerZExtent(this.scanX, this.scanY, r);
        if (inner >= 0 && this.scanZ == -inner - 1) {
            this.scanZ = inner + 1;
            return;
        }
        if (this.scanZ < outer) {
            this.scanZ++;
            return;
        }
        moveToNextColumn();
    }

    private void moveToNextColumn() {
        while (true) {
            int r = this.shellRadius;
            if (++this.scanY > r) {
                this.scanY = -r;
                this.scanX++;
            }
            if (this.scanX > r) {
                this.shellRadius++;
                r = this.shellRadius;
                this.scanX = -r;
                this.scanY = -r - 1;
                continue;
            }
            int outer = CelestialNailMath.outerZExtent(this.scanX, this.scanY, r);
            int inner = CelestialNailMath.innerZExtent(this.scanX, this.scanY, r);
            if (outer > inner) {
                this.scanZ = -outer;
                return;
            }
        }
    }
    private boolean ensureImpactChunkReady(ServerLevel level, BlockPos pos) {
        if (level.isLoaded(pos)) return true;
        int chunkX = Math.floorDiv(pos.getX(), 16);
        int chunkZ = Math.floorDiv(pos.getZ(), 16);
        long chunkKey = CelestialNailMath.packChunk(chunkX, chunkZ);
        if (level.getForceLoadedChunks().contains(chunkKey)) return false;
        if (level.setChunkForced(chunkX, chunkZ, true)) this.ownedImpactForcedChunks.add(chunkKey);
        return false;
    }

    private boolean needsForcedChunk(long chunkKey) {
        long ownChunk = CelestialNailMath.packChunk(this.chunkPosition().x(), this.chunkPosition().z());
        if (chunkKey == ownChunk) return true;
        if (this.entityData.get(DATA_PHASE) != PHASE_IMPACT) return false;
        return CelestialNailMath.chunkIntersectsHorizontalRadius(CelestialNailMath.unpackChunkX(chunkKey), CelestialNailMath.unpackChunkZ(chunkKey), this.impactCenter.getX(), this.impactCenter.getZ(), CelestialNailMath.targetRadius(this.power()));
    }

    private void acceptForcedChunkOwnership(long chunkKey) {
        long ownChunk = CelestialNailMath.packChunk(this.chunkPosition().x(), this.chunkPosition().z());
        if (chunkKey == ownChunk) this.ownsForcedChunk = true;
        else this.ownedImpactForcedChunks.add(chunkKey);
    }
    private boolean transferForcedChunkOwnership(ServerLevel level, long chunkKey) {
        for (Entity e : level.getAllEntities()) {
            if (e != this && e instanceof CelestialNailEntity nail && !nail.isRemoved() && nail.needsForcedChunk(chunkKey)) {
                nail.acceptForcedChunkOwnership(chunkKey);
                return true;
            }
        }
        return false;
    }

    private void releaseImpactForcedChunks(ServerLevel level) {
        for (long chunkKey : new HashSet<>(this.ownedImpactForcedChunks)) {
            if (!transferForcedChunkOwnership(level, chunkKey)) {
                level.setChunkForced(CelestialNailMath.unpackChunkX(chunkKey), CelestialNailMath.unpackChunkZ(chunkKey), false);
            }
        }
        this.ownedImpactForcedChunks.clear();
    }
    public void forceOwnChunk(ServerLevel level) {
        var chunk = this.chunkPosition();
        if (!level.getForceLoadedChunks().contains(chunk.pack())) {
            this.ownsForcedChunk = level.setChunkForced(chunk.x(), chunk.z(), true);
        }
    }

    public void releaseForcedChunk(ServerLevel level) {
        if (!this.ownsForcedChunk) return;
        var chunk = this.chunkPosition();
        for (Entity e : level.getAllEntities()) {
            if (e != this && e instanceof CelestialNailEntity nail && !nail.isRemoved() && nail.chunkPosition().equals(chunk)) {
                nail.ownsForcedChunk = true;
                this.ownsForcedChunk = false;
                return;
            }
        }
        level.setChunkForced(chunk.x(), chunk.z(), false);
        this.ownsForcedChunk = false;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel server) {
            CelestialNailIndex.unregister(server.getServer(), this);
            if (reason.shouldDestroy() && !this.isRemoved()) {
                releaseImpactForcedChunks(server);
                releaseForcedChunk(server);
            }
        }
        super.remove(reason);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(DATA_IMPACT_TIME, input.getLongOr("ImpactTime", -1L));
        this.configure(input.getStringOr("NailId", ""), input.getFloatOr("Power", DEFAULT_POWER));
        this.entityData.set(DATA_PHASE, input.getByteOr("Phase", PHASE_IDLE));
        this.entityData.set(DATA_SCALE, CelestialNailVisuals.safeScale(input.getFloatOr("Scale", 1)));
        this.entityData.set(DATA_SUMMON_TIME, input.getLongOr("SummonTime", -10000L));
        this.entityData.set(DATA_LAUNCH_TIME, input.getLongOr("LaunchTime", this.isLaunched() ? this.level().getGameTime()-CelestialNailVisuals.CLOSE_TICKS : -1L));
        this.entityData.set(DATA_PORTAL_Y, input.getFloatOr("PortalY", (float)(this.getY()+CelestialNailVisuals.portalHeight(nailHeight()))));
        // Upgrade old floating anchors without moving a portal already closing during launch.
        if (input.getIntOr("PortalLayout", 0) < CelestialNailVisuals.PORTAL_LAYOUT_VERSION && !isLaunched())
            this.entityData.set(DATA_PORTAL_Y, (float)(this.getY()+CelestialNailVisuals.portalHeight(nailHeight())));
        this.purgeIndex=input.getLongOr("PurgeIndex", 0);
        this.purgePass=input.getIntOr("PurgePass", 0);
        this.purgeWait=input.getIntOr("PurgeWait", 0);
        this.refreshDimensions();
        this.descentSpeed = input.getDoubleOr("DescentSpeed", 0.0);
        this.impactCenter = new BlockPos(input.getIntOr("ImpactX", 0), input.getIntOr("ImpactY", 0), input.getIntOr("ImpactZ", 0));
        this.shellRadius = input.getIntOr("ShellRadius", 0);
        this.scanX = input.getIntOr("ScanX", -this.shellRadius);
        this.scanY = input.getIntOr("ScanY", -this.shellRadius);
        this.scanZ = input.getIntOr("ScanZ", -this.shellRadius);
        this.ownsForcedChunk = input.getBooleanOr("OwnsForcedChunk", false);
        this.fluidPurgeActive = input.getBooleanOr("FluidPurgeActive", false);
        this.ownedImpactForcedChunks.clear();
        for (ValueInput chunk : input.childrenListOrEmpty("OwnedImpactForcedChunks")) {
            chunk.getLong("Chunk").ifPresent(this.ownedImpactForcedChunks::add);
        }
        this.entityData.set(DATA_WAVE_RADIUS, displayedWaveRadius());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putLong("ImpactTime", this.entityData.get(DATA_IMPACT_TIME));
        output.putString("NailId", this.nailId());
        output.putFloat("Power", this.power());
        output.putFloat("Scale", nailScale());
        output.putLong("SummonTime", this.entityData.get(DATA_SUMMON_TIME));
        output.putLong("LaunchTime", this.entityData.get(DATA_LAUNCH_TIME));
        output.putFloat("PortalY", portalY());
        output.putInt("PortalLayout", CelestialNailVisuals.PORTAL_LAYOUT_VERSION);
        output.putLong("PurgeIndex", this.purgeIndex);
        output.putInt("PurgePass", this.purgePass);
        output.putInt("PurgeWait", this.purgeWait);
        output.putByte("Phase", this.entityData.get(DATA_PHASE));
        output.putDouble("DescentSpeed", this.descentSpeed);
        output.putInt("ImpactX", this.impactCenter.getX());
        output.putInt("ImpactY", this.impactCenter.getY());
        output.putInt("ImpactZ", this.impactCenter.getZ());
        output.putInt("ShellRadius", this.shellRadius);
        output.putInt("ScanX", this.scanX);
        output.putInt("ScanY", this.scanY);
        output.putInt("ScanZ", this.scanZ);
        output.putBoolean("OwnsForcedChunk", this.ownsForcedChunk);
        output.putBoolean("FluidPurgeActive", this.fluidPurgeActive);
        var forcedChunks = output.childrenList("OwnedImpactForcedChunks");
        for (long chunkKey : this.ownedImpactForcedChunks) forcedChunks.addChild().putLong("Chunk", chunkKey);
    }
}

