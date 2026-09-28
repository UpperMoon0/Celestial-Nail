package com.nstut.celestialnail.entity;

import com.nstut.celestialnail.CelestialNailMath;

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
    private static final byte PHASE_IMPACT = 2;
    private static final int MAX_BLOCK_CHANGES_PER_TICK = 3000;
    private static final int MAX_SCAN_STEPS_PER_TICK = 45000;
    private static final double TIP_OFFSET = 2.4;

    private static final EntityDataAccessor<String> DATA_NAIL_ID = SynchedEntityData.defineId(CelestialNailEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_POWER = SynchedEntityData.defineId(CelestialNailEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> DATA_PHASE = SynchedEntityData.defineId(CelestialNailEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_WAVE_RADIUS = SynchedEntityData.defineId(CelestialNailEntity.class, EntityDataSerializers.INT);

    private double descentSpeed;
    private BlockPos impactCenter = BlockPos.ZERO;
    private int shellRadius;
    private int scanX;
    private int scanY;
    private int scanZ;
    private boolean ownsForcedChunk;

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

    public boolean launch() {
        if (this.entityData.get(DATA_PHASE) != PHASE_IDLE) return false;
        this.entityData.set(DATA_PHASE, PHASE_DESCENDING);
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
        super.tick();
        byte phase = this.entityData.get(DATA_PHASE);
        if (phase == PHASE_IDLE) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (phase == PHASE_DESCENDING) {
            tickDescending();
        } else if (phase == PHASE_IMPACT && !this.level().isClientSide) {
            tickImpactWave((ServerLevel) this.level());
        }
    }

    private void tickDescending() {
        this.descentSpeed = Math.min(8.0, Math.max(1.25, this.descentSpeed + 0.22));
        Vec3 from = this.position().add(0.0, -TIP_OFFSET, 0.0);
        Vec3 to = from.add(0.0, -this.descentSpeed, 0.0);
        BlockHitResult hit = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (!this.level().isClientSide && hit.getType() != HitResult.Type.MISS) {
            beginImpact((ServerLevel) this.level(), hit.getBlockPos());
            return;
        }
        this.setDeltaMovement(0.0, -this.descentSpeed, 0.0);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (!this.level().isClientSide) {
            ServerLevel server = (ServerLevel) this.level();
            server.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.0, this.getZ(), 8, 0.35, 1.5, 0.35, 0.02);
            if (this.getY() - TIP_OFFSET <= server.getMinBuildHeight()) beginImpact(server, BlockPos.containing(this.getX(), server.getMinBuildHeight(), this.getZ()));
        }
    }

    private void beginImpact(ServerLevel level, BlockPos center) {
        this.entityData.set(DATA_PHASE, PHASE_IMPACT);
        this.setDeltaMovement(Vec3.ZERO);
        this.impactCenter = center.immutable();
        this.setPos(center.getX() + 0.5, center.getY() + 0.5 + TIP_OFFSET, center.getZ() + 0.5);
        this.shellRadius = 0;
        resetScanForShell();
        level.playSound(null, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 8.0F, 0.55F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 6, 1.2, 1.2, 1.2, 0.0);
        damageEntities(level);
    }

    private void damageEntities(ServerLevel level) {
        double radius = this.power() * 1.25;
        AABB box = new AABB(this.impactCenter).inflate(radius);
        for (Entity entity : level.getEntities(this, box, e -> e instanceof LivingEntity)) {
            double distance = Math.sqrt(entity.distanceToSqr(Vec3.atCenterOf(this.impactCenter)));
            if (distance > radius) continue;
            float damage = (float) Math.max(8.0, (1.0 - distance / radius) * this.power() * 8.0);
            entity.hurt(level.damageSources().magic(), damage);
        }
    }

    private void tickImpactWave(ServerLevel level) {
        int targetRadius = CelestialNailMath.targetRadius(this.power());
        int changed = 0;
        int scanned = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        while (this.shellRadius <= targetRadius && changed < MAX_BLOCK_CHANGES_PER_TICK && scanned < MAX_SCAN_STEPS_PER_TICK) {
            int dx = this.scanX;
            int dy = this.scanY;
            int dz = this.scanZ;
            cursor.set(this.impactCenter.getX() + dx, this.impactCenter.getY() + dy, this.impactCenter.getZ() + dz);
            if (cursor.getY() >= level.getMinBuildHeight() && cursor.getY() < level.getMaxBuildHeight() && !level.getBlockState(cursor).isAir()) {
                if (level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2)) changed++;
            }
            scanned++;
            advanceScan();
        }
        this.entityData.set(DATA_WAVE_RADIUS, this.shellRadius);
        if (this.shellRadius > targetRadius) {
            level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 160, this.power() * 0.35, this.power() * 0.18, this.power() * 0.35, 0.12);
            releaseForcedChunk(level);
            this.discard();
        }
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
    public void forceOwnChunk(ServerLevel level) {
        var chunk = this.chunkPosition();
        if (!level.getForcedChunks().contains(chunk.toLong())) {
            this.ownsForcedChunk = level.setChunkForced(chunk.x, chunk.z, true);
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
            if (reason.shouldDestroy() && !this.isRemoved()) releaseForcedChunk(server);
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.configure(tag.getString("NailId"), tag.getFloat("Power"));
        this.entityData.set(DATA_PHASE, tag.getByte("Phase"));
        this.descentSpeed = tag.getDouble("DescentSpeed");
        this.impactCenter = new BlockPos(tag.getInt("ImpactX"), tag.getInt("ImpactY"), tag.getInt("ImpactZ"));
        this.shellRadius = tag.getInt("ShellRadius");
        this.scanX = tag.getInt("ScanX");
        this.scanY = tag.getInt("ScanY");
        this.scanZ = tag.getInt("ScanZ");
        this.ownsForcedChunk = tag.getBoolean("OwnsForcedChunk");
        this.entityData.set(DATA_WAVE_RADIUS, this.shellRadius);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("NailId", this.nailId());
        tag.putFloat("Power", this.power());
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
    }
}


