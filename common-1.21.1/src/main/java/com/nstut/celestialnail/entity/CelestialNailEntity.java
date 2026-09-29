package com.nstut.celestialnail.entity;

import com.nstut.celestialnail.CelestialNailMath;
import com.nstut.celestialnail.CelestialNailVisuals;
import com.nstut.celestialnail.FluidPurgeCursor;
import com.nstut.celestialnail.ImpactWorkBudget;
import com.nstut.celestialnail.SphereBoundaryCursor;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
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
    private static final byte PHASE_IMPACT = 2, PHASE_EMBEDDED = 3, PHASE_CRUMBLING = 4;
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
    private static final EntityDataAccessor<Long> DATA_CRUMBLE_TIME = SynchedEntityData.defineId(CelestialNailEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> DATA_IMPACT_Y = SynchedEntityData.defineId(CelestialNailEntity.class, EntityDataSerializers.FLOAT);
    private boolean blastCleared;
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
    private boolean boundaryActive;
    private long boundaryIndex;
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
        builder.define(DATA_CRUMBLE_TIME, -1L);
        builder.define(DATA_IMPACT_Y, 0F);
    }

    public void configure(String id, float power) {
        this.entityData.set(DATA_NAIL_ID, id);
        this.entityData.set(DATA_POWER, power);
        this.setCustomName(net.minecraft.network.chat.Component.literal(id));
        this.setCustomNameVisible(true);
        this.setInvulnerable(true);
    }

    public String nailId() {
        return this.entityData.get(DATA_NAIL_ID);
    }

    public float power() {
        return this.entityData.get(DATA_POWER);
    }

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
    public boolean isImpacting() { return this.entityData.get(DATA_PHASE) == PHASE_IMPACT || isEmbedded(); }
    public boolean isEmbedded() { return this.entityData.get(DATA_PHASE) == PHASE_EMBEDDED; }
    public boolean isCrumbling() { return this.entityData.get(DATA_PHASE) == PHASE_CRUMBLING; }
    public Vec3 impactOrigin() { return new Vec3(getX(),this.entityData.get(DATA_IMPACT_Y),getZ()); }
    public float crumbleAge(float partial) {
        long started=this.entityData.get(DATA_CRUMBLE_TIME);
        return started<0 ? -1 : (float)(level().getGameTime()-started)+partial;
    }
    public boolean beginCrumbling() {
        if(isCrumbling())return false;
        this.entityData.set(DATA_CRUMBLE_TIME,level().getGameTime());
        this.entityData.set(DATA_PHASE,PHASE_CRUMBLING);
        this.setDeltaMovement(Vec3.ZERO);
        if(level() instanceof ServerLevel server)releaseImpactForcedChunks(server);
        return true;
    }
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
        if(isCrumbling())return new AABB(getX()-nailHeight(),getY()-nailHeight()*3,getZ()-nailHeight(),getX()+nailHeight(),Math.max(getY()+nailHeight(),portalY()),getZ()+nailHeight());
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

    public boolean isLaunched() {
        return this.entityData.get(DATA_PHASE) != PHASE_IDLE;
    }

    public int waveRadius() {
        return this.entityData.get(DATA_WAVE_RADIUS);
    }

    @Override
    public void tick() {
        if (isRemoved()) return;
        super.tick();
        if (isRemoved()) return;
        if (this.level().isClientSide) clientVisualTick.accept(this);
        if (this.level() instanceof ServerLevel serverLevel) forceOwnChunk(serverLevel);
        byte phase = this.entityData.get(DATA_PHASE);
        if(phase==PHASE_CRUMBLING) {
            // Cancel all terrain mutation and damage immediately; only a timed visual remains.
            if(level() instanceof ServerLevel && crumbleAge(0)>=80)discard();
            return;
        }
        if (phase == PHASE_IDLE) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (phase == PHASE_DESCENDING) {
            if (!this.level().isClientSide) tickDescending();
        } else if (phase == PHASE_IMPACT && !this.level().isClientSide) {
            tickImpactWave((ServerLevel) this.level());
        }
    }

    /** Deep embedding is intentional; only uncontrolled idle/descending nails use void removal. */
    @Override
    protected void onBelowWorld() {
        if (!isImpacting() && !isCrumbling()) super.onBelowWorld();
    }

    @Override
    public boolean isPushedByFluid() { return false; }

    /** Bypass both vanilla/loader full-model fluid scans; noPhysics alone does not do this. */
    @Override
    protected boolean updateInWaterStateAndDoFluidPushing() { return false; }

    private void tickDescending() {
        if (launchAge(0) < CelestialNailVisuals.CLOSE_TICKS) return;
        this.descentSpeed = Math.min(8.0, Math.max(1.25, this.descentSpeed + 0.22));
        Vec3 from = this.position();
        Vec3 to = from.add(0.0, -this.descentSpeed, 0.0);
        BlockHitResult hit = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (!this.level().isClientSide && hit.getType() != HitResult.Type.MISS) {
            beginImpact((ServerLevel) this.level(), hit.getBlockPos(), hit.getLocation());
            return;
        }
        this.setDeltaMovement(0.0, -this.descentSpeed, 0.0);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (!this.level().isClientSide) {
            ServerLevel server = (ServerLevel) this.level();
            server.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + nailHeight()*.2, this.getZ(), 8, nailHeight()*.025, nailHeight()*.12, nailHeight()*.025, 0.02);
            if (this.getY() <= server.getMinBuildHeight()) beginImpact(server, BlockPos.containing(this.getX(), server.getMinBuildHeight(), this.getZ()), new Vec3(this.getX(), server.getMinBuildHeight(), this.getZ()));
        }
    }

    private void beginImpact(ServerLevel level, BlockPos center, Vec3 tipPosition) {
        this.entityData.set(DATA_PHASE, PHASE_IMPACT);
        this.entityData.set(DATA_IMPACT_TIME, level.getGameTime());
        this.entityData.set(DATA_IMPACT_Y,(float)tipPosition.y);
        this.setDeltaMovement(Vec3.ZERO);
        this.impactCenter = center.immutable();
        this.setPos(tipPosition.x, tipPosition.y, tipPosition.z);
        this.shellRadius = 0;
        resetScanForShell();
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 6, 1.2, 1.2, 1.2, 0.0);
        damageEntities(level);
    }

    private void damageEntities(ServerLevel level) {
        float age=impactAge(0);
        double shock=com.nstut.celestialnail.CataclysmTimeline.shockRadius(Math.min(70,age));
        double radius=Math.max(power()*1.25,age<=70?shock:0);
        Vec3 origin=impactOrigin();
        for(Entity entity:level.getEntities(this,new AABB(origin,origin).inflate(radius),e->e instanceof LivingEntity)) {
            double distance=entity.position().distanceTo(origin);
            boolean inBlast=distance<=power()*1.25;
            boolean inWave=com.nstut.celestialnail.CataclysmTimeline.shockHits(distance,age,10);
            if(!inBlast&&!inWave)continue;
            float damage=inBlast ? (float)Math.max(4,(1-distance/(power()*1.25))*power()*4)
                : (float)Math.max(1,power()*.3*(1-distance/800));
            entity.hurt(level.damageSources().magic(), damage);
        }
    }

    private void tickImpactWave(ServerLevel level) {
        float age=impactAge(0);
        this.setPos(getX(),impactOrigin().y-com.nstut.celestialnail.CataclysmTimeline.pierceDepth(power(),nailHeight(),age),getZ());
        if((int)age%10==0)damageEntities(level);
        if(blastCleared) {
            if(age>=70)this.entityData.set(DATA_PHASE,PHASE_EMBEDDED);
            return;
        }
        if (this.boundaryActive) { tickBoundary(level); return; }
        if (this.fluidPurgeActive) { tickFluidPurge(level); return; }
        int targetRadius = CelestialNailMath.targetRadius(this.power());
        ImpactWorkBudget budget = ImpactWorkBudget.forTick(level, level.getGameTime());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        while (this.shellRadius <= targetRadius && !isRemoved() && !isCrumbling() && budget.tryScan()) {
            int dx = this.scanX;
            int dy = this.scanY;
            int dz = this.scanZ;
            cursor.set(this.impactCenter.getX() + dx, this.impactCenter.getY() + dy, this.impactCenter.getZ() + dz);
            if (!level.isOutsideBuildHeight(cursor)) {
                if (!ensureImpactChunkReady(level, cursor)) {
                    this.entityData.set(DATA_WAVE_RADIUS, displayedWaveRadius());
                    return;
                }
                var state = level.getBlockState(cursor);
                // UPDATE_KNOWN_SHAPE prevents neighbor shape updates from scheduling new fluid
                // cascades behind an inside-out wave. No drops, including waterlogged containers.
                if (!state.isAir() && NailWorldOperations.replaceWithoutDrops(level, cursor, Blocks.AIR.defaultBlockState())) budget.changed();
            }
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
        ImpactWorkBudget budget = ImpactWorkBudget.forTick(level, level.getGameTime());
        while (!scan.done() && !isRemoved() && !isCrumbling() && budget.tryScan()) {
            pos.set(impactCenter.getX()+scan.x(), impactCenter.getY()+scan.y(), impactCenter.getZ()+scan.z());
            if (scan.inside() && !level.isOutsideBuildHeight(pos)) {
                if (!ensureImpactChunkReady(level, pos)) { this.purgeIndex=scan.index(); return; }
                var state=level.getBlockState(pos);
                if (!state.getFluidState().isEmpty()) {
                    var dry=state.hasProperty(BlockStateProperties.WATERLOGGED)
                            ? state.setValue(BlockStateProperties.WATERLOGGED, false) : Blocks.AIR.defaultBlockState();
                    if (NailWorldOperations.replaceWithoutDrops(level, pos, dry)) budget.changed();
                }
            }
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
        this.fluidPurgeActive = false;
        this.boundaryActive = true;
        this.boundaryIndex = 0;
    }

    private void tickBoundary(ServerLevel level) {
        SphereBoundaryCursor scan = new SphereBoundaryCursor(CelestialNailMath.targetRadius(power()), boundaryIndex);
        ImpactWorkBudget budget = ImpactWorkBudget.forTick(level, level.getGameTime());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        while (!scan.done() && !isRemoved() && !isCrumbling() && budget.tryScan()) {
            pos.set(impactCenter.getX() + scan.x(), impactCenter.getY() + scan.y(), impactCenter.getZ() + scan.z());
            if (scan.valid() && !level.isOutsideBuildHeight(pos)) {
                if (!ensureBoundaryReady(level, pos)) { boundaryIndex = scan.index(); return; }
                var state = level.getBlockState(pos);
                if (!state.isAir()) {
                    // Reconcile one surviving boundary layer. Do not unleash recursive neighbor cascades.
                    var next = net.minecraft.world.level.block.Block.updateFromNeighbourShapes(state, level, pos);
                    if (next != state && NailWorldOperations.replaceWithoutDrops(level, pos, next)) budget.changed();
                }
            }
            scan.advance();
        }
        boundaryIndex = scan.index();
        if (scan.done() && !isRemoved() && !isCrumbling()) finishBlast(level);
    }

    private boolean ensureBoundaryReady(ServerLevel level, BlockPos pos) {
        if (!ensureImpactChunkReady(level, pos)) return false;
        // Shape calculation reads all six neighbors; none may trigger a synchronous chunk load.
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (!level.isOutsideBuildHeight(neighbor) && !ensureImpactChunkReady(level, neighbor)) return false;
        }
        return true;
    }

    private void finishBlast(ServerLevel level) {
        // Become ineligible BEFORE transfer; another finishing impact may run later this tick.
        this.blastCleared = true;
        this.boundaryActive = false;
        level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 160,
                this.power()*.35, this.power()*.18, this.power()*.35, .12);
        releaseImpactForcedChunks(level);
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
        if (level.getForcedChunks().contains(chunkKey)) return false;
        if (!ImpactWorkBudget.forTick(level, level.getGameTime()).tryRequestChunk()) return false;
        if (NailWorldOperations.forceChunk(level, new net.minecraft.world.level.ChunkPos(chunkX, chunkZ))) this.ownedImpactForcedChunks.add(chunkKey);
        return false;
    }

    private boolean needsForcedChunk(long chunkKey) {
        if (isRemoved()) return false;
        long ownChunk = CelestialNailMath.packChunk(this.chunkPosition().x, this.chunkPosition().z);
        if (chunkKey == ownChunk) return true;
        if (this.entityData.get(DATA_PHASE) != PHASE_IMPACT || this.blastCleared) return false;
        return CelestialNailMath.chunkIntersectsHorizontalRadius(
            CelestialNailMath.unpackChunkX(chunkKey), CelestialNailMath.unpackChunkZ(chunkKey),
            this.impactCenter.getX(), this.impactCenter.getZ(), CelestialNailMath.targetRadius(this.power()) + 2
        );
    }

    private void acceptForcedChunkOwnership(long chunkKey) {
        long ownChunk = CelestialNailMath.packChunk(this.chunkPosition().x, this.chunkPosition().z);
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
        if (isRemoved()) return;
        var chunk = this.chunkPosition();
        if (!level.getForcedChunks().contains(chunk.toLong())) {
            this.ownsForcedChunk = NailWorldOperations.forceChunk(level, chunk);
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
        level.setChunkForced(chunk.x, chunk.z, false);
        this.ownsForcedChunk = false;
    }
    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel server) {
            CelestialNailIndex.unregister(server.getServer(), this);
            if (reason.shouldDestroy() && !this.isRemoved()) {
                releaseImpactForcedChunks(server);
                releaseForcedChunk(server);
            }
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_CRUMBLE_TIME,tag.contains("CrumbleTime")?tag.getLong("CrumbleTime"):-1L);
        this.entityData.set(DATA_IMPACT_Y,tag.contains("ImpactYExact")?tag.getFloat("ImpactYExact"):(float)getY());
        this.blastCleared=tag.getBoolean("BlastCleared");
        this.entityData.set(DATA_IMPACT_TIME, tag.contains("ImpactTime") ? tag.getLong("ImpactTime") : -1L);
        this.configure(tag.getString("NailId"), tag.getFloat("Power"));
        this.entityData.set(DATA_PHASE, tag.getByte("Phase"));
        if(this.entityData.get(DATA_PHASE)==PHASE_IMPACT && this.entityData.get(DATA_IMPACT_TIME)<0)
            this.entityData.set(DATA_IMPACT_TIME,level().getGameTime()-70);
        this.entityData.set(DATA_SCALE, CelestialNailVisuals.safeScale(tag.contains("Scale") ? tag.getFloat("Scale") : 1));
        this.entityData.set(DATA_SUMMON_TIME, tag.contains("SummonTime") ? tag.getLong("SummonTime") : -10000L);
        this.entityData.set(DATA_LAUNCH_TIME, tag.contains("LaunchTime") ? tag.getLong("LaunchTime") : (this.isLaunched() ? this.level().getGameTime()-CelestialNailVisuals.CLOSE_TICKS : -1L));
        this.entityData.set(DATA_PORTAL_Y, tag.contains("PortalY") ? tag.getFloat("PortalY") : (float)(this.getY()+CelestialNailVisuals.portalHeight(nailHeight())));
        // Upgrade old floating anchors without moving a portal already closing during launch.
        if (tag.getInt("PortalLayout") < CelestialNailVisuals.PORTAL_LAYOUT_VERSION && !isLaunched())
            this.entityData.set(DATA_PORTAL_Y, (float)(this.getY()+CelestialNailVisuals.portalHeight(nailHeight())));
        this.purgeIndex=tag.getLong("PurgeIndex");
        this.purgePass=tag.getInt("PurgePass");
        this.purgeWait=tag.getInt("PurgeWait");
        this.refreshDimensions();
        this.descentSpeed = tag.getDouble("DescentSpeed");
        this.impactCenter = new BlockPos(tag.getInt("ImpactX"), tag.getInt("ImpactY"), tag.getInt("ImpactZ"));
        this.shellRadius = tag.getInt("ShellRadius");
        this.scanX = tag.getInt("ScanX");
        this.scanY = tag.getInt("ScanY");
        this.scanZ = tag.getInt("ScanZ");
        this.ownsForcedChunk = tag.getBoolean("OwnsForcedChunk");
        this.fluidPurgeActive = tag.getBoolean("FluidPurgeActive");
        this.boundaryActive = tag.getBoolean("BoundaryActive");
        this.boundaryIndex = tag.getLong("BoundaryIndex");
        this.ownedImpactForcedChunks.clear();
        for (long chunkKey : tag.getLongArray("OwnedImpactForcedChunks")) this.ownedImpactForcedChunks.add(chunkKey);
        this.entityData.set(DATA_WAVE_RADIUS, displayedWaveRadius());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("CrumbleTime",this.entityData.get(DATA_CRUMBLE_TIME));
        tag.putFloat("ImpactYExact",this.entityData.get(DATA_IMPACT_Y));
        tag.putBoolean("BlastCleared",blastCleared);
        tag.putLong("ImpactTime", this.entityData.get(DATA_IMPACT_TIME));
        tag.putString("NailId", this.nailId());
        tag.putFloat("Power", this.power());
        tag.putFloat("Scale", nailScale());
        tag.putLong("SummonTime", this.entityData.get(DATA_SUMMON_TIME));
        tag.putLong("LaunchTime", this.entityData.get(DATA_LAUNCH_TIME));
        tag.putFloat("PortalY", portalY());
        tag.putInt("PortalLayout", CelestialNailVisuals.PORTAL_LAYOUT_VERSION);
        tag.putLong("PurgeIndex", this.purgeIndex);
        tag.putInt("PurgePass", this.purgePass);
        tag.putInt("PurgeWait", this.purgeWait);
        tag.putByte("Phase", this.entityData.get(DATA_PHASE));
        tag.putDouble("DescentSpeed", this.descentSpeed);
        tag.putInt("ImpactX", this.impactCenter.getX());
        tag.putInt("ImpactY", this.impactCenter.getY());
        tag.putInt("ImpactZ", this.impactCenter.getZ());
        tag.putInt("ShellRadius", this.shellRadius);
        tag.putInt("ScanX", this.scanX);
        tag.putInt("ScanY", this.scanY);
        tag.putInt("ScanZ", this.scanZ);
        tag.putBoolean("OwnsForcedChunk", this.ownsForcedChunk);
        tag.putBoolean("FluidPurgeActive", this.fluidPurgeActive);
        tag.putBoolean("BoundaryActive", this.boundaryActive);
        tag.putLong("BoundaryIndex", this.boundaryIndex);
        tag.putLongArray("OwnedImpactForcedChunks", this.ownedImpactForcedChunks.stream().mapToLong(Long::longValue).toArray());
    }
}


