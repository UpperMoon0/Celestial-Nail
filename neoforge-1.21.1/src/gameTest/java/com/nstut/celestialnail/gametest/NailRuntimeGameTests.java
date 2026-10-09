package com.nstut.celestialnail.gametest;

import com.nstut.celestialnail.entity.CelestialNailEntity;
import com.nstut.explosion.terrain.TerrainOperations;
import com.nstut.celestialnail.neoforge.CelestialNailNeoForge;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

@GameTestHolder("minecraft")
@PrefixGameTestTemplate(false)
public final class NailRuntimeGameTests {
    private static CelestialNailEntity nail(ServerLevel level, BlockPos pos, String id, float power) {
        var n = CelestialNailNeoForge.NAIL.get().create(level);
        if (n == null) throw new AssertionError("Entity creation failed");
        n.configure(id,power); n.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        return n;
    }
    private static CompoundTag save(CelestialNailEntity n) { return n.saveWithoutId(new CompoundTag()); }
    private static void set(CelestialNailEntity n,String field,Object value) {
        try { Field f=CelestialNailEntity.class.getDeclaredField(field); f.setAccessible(true); f.set(n,value); }
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static void finish(CelestialNailEntity n,ServerLevel level) {
        try { Method m=CelestialNailEntity.class.getDeclaredMethod("finishBlast",ServerLevel.class);m.setAccessible(true);m.invoke(n,level); }
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static void impact(CelestialNailEntity n,ServerLevel level,BlockPos pos) {
        try { Method m=CelestialNailEntity.class.getDeclaredMethod("beginImpact",ServerLevel.class,BlockPos.class,Vec3.class);
            m.setAccessible(true);m.invoke(n,level,pos,new Vec3(pos.getX()+.5,pos.getY(),pos.getZ()+.5)); }
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    @GameTest(template="empty",timeoutTicks=20,batch="nail_visibility")
    public static void buriedAnchorBoundsIncludeImpactOriginAcrossScales(GameTestHelper h) {
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(2,2,2));
        for(float scale:new float[]{.1F,1F,4F}) for(float power:new float[]{4F,32F,128F}) {
            var n=nail(level,base,"bounds",power);n.beginSummoning(scale);
            double originY=n.getY();
            n.setPos(n.getX(),originY-com.nstut.celestialnail.CataclysmTimeline.pierceDepth(power,n.nailHeight(),100),n.getZ());
            for(byte phase:new byte[]{2,3,4}) {
                var tag=save(n);tag.putByte("Phase",phase);tag.putFloat("ImpactYExact",(float)originY);
                tag.putLong("ImpactTime",level.getGameTime());n.load(tag);
                var bounds=n.visualBounds();
                h.assertTrue(bounds.maxY>=originY,"Buried anchor excluded the impact surface at scale "+scale+" power "+power);
                h.assertTrue(n.getBoundingBoxForCulling().equals(bounds),"Native culling omitted Nail effects in phase "+phase);
                h.assertTrue(n.shouldRender(n.getX(),originY+85,n.getZ()+40),"Vertical anchor cutoff rejected Nail");
                h.assertFalse(n.shouldRender(n.getX()+4000,originY,n.getZ()),"Own distance limit stopped applying");
            }
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20,batch="nail_lifecycle")
    public static void removedEntityCannotTickOrAcquireTickets(GameTestHelper h) {
        var level=h.getLevel(); var n=nail(level,h.absolutePos(new BlockPos(2,2,2)),"removed",4);
        var chunk=n.chunkPosition(); boolean previous=level.getForcedChunks().contains(chunk.toLong());
        try {
            n.discard(); n.forceOwnChunk(level); n.tick();
            h.assertTrue(level.getForcedChunks().contains(chunk.toLong())==previous,"Removed nail acquired a forced chunk");
        } finally { if(!previous)level.setChunkForced(chunk.x,chunk.z,false); }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100,batch="nail_void")
    public static void voidDescentCrumblesBeforeRemovalAndReleasesTickets(GameTestHelper h) {
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(2,2,2));
        var n=nail(level,new BlockPos(base.getX(),level.getMinBuildHeight()-1,base.getZ()),"void_descent",4);
        n.beginSummoning(1);
        var tag=save(n);tag.putByte("Phase",(byte)1);tag.putLong("LaunchTime",level.getGameTime()-30);n.load(tag);
        var chunk=n.chunkPosition();boolean previous=level.getForcedChunks().contains(chunk.toLong());
        n.tick();
        h.assertTrue(n.isCrumbling(),"Void descent must start the remove animation");
        h.assertFalse(n.isRemoved(),"Void descent discarded the Nail before its animation");
        h.assertFalse(n.isImpacting(),"Void descent scheduled a bottom-of-world impact");
        h.assertTrue(n.getDeltaMovement().equals(Vec3.ZERO),"Crumbling Nail kept falling");
        long started=level.getGameTime();
        h.runAfterDelay(com.nstut.celestialnail.CelestialNailVisuals.CRUMBLE_TICKS-1,()-> {
            n.tick();h.assertFalse(n.isRemoved(),"Nail disappeared before crumble completed");
            h.assertTrue(save(n).getLong("CrumbleTime")==started,"Crumble clock restarted");
        });
        h.runAfterDelay(com.nstut.celestialnail.CelestialNailVisuals.CRUMBLE_TICKS,()-> {
            n.tick();h.assertTrue(n.isRemoved(),"Nail survived completed crumble");
            h.assertTrue(level.getForcedChunks().contains(chunk.toLong())==previous,"Void removal leaked its chunk ticket");
            h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=20,batch="nail_void_idle")
    public static void belowWorldIdleStartsCrumbleAndReloadKeepsItsClock(GameTestHelper h) {
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(2,2,2));
        var n=nail(level,new BlockPos(base.getX(),level.getMinBuildHeight()-80,base.getZ()),"void_idle",4);
        n.beginSummoning(1);
        try {
            n.tick();h.assertTrue(n.isCrumbling(),"Below-world idle Nail skipped crumble");
            h.assertFalse(n.isRemoved(),"Below-world hook immediately discarded the Nail");
            var tag=save(n);long started=tag.getLong("CrumbleTime");n.load(tag);n.tick();
            h.assertTrue(n.isCrumbling() && save(n).getLong("CrumbleTime")==started,"Reload restarted or cancelled crumble");
        } finally {n.discard();}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20,batch="nail_deep")
    public static void deepImpactAndEmbeddedStateSurviveBaseTick(GameTestHelper h) {
        var level=h.getLevel(); BlockPos base=h.absolutePos(new BlockPos(2,2,2));
        var n=nail(level,new BlockPos(base.getX(),level.getMinBuildHeight()-80,base.getZ()),"deep",80);
        var chunk=n.chunkPosition(); boolean previous=level.getForcedChunks().contains(chunk.toLong());
        try {
            CompoundTag tag=save(n); tag.putByte("Phase",(byte)2);tag.putLong("ImpactTime",level.getGameTime()-35);
            tag.putFloat("ImpactYExact",level.getMinBuildHeight()+1); tag.putBoolean("BlastCleared",true);
            n.load(tag); n.tick();
            h.assertFalse(n.isRemoved(),"Intentional below-world impact was discarded by baseTick");
            n.setPos(n.getX(),level.getMinBuildHeight()-80,n.getZ());
            tag=save(n);tag.putByte("Phase",(byte)3);n.load(tag);n.tick();
            h.assertFalse(n.isRemoved(),"Reloaded embedded nail was discarded below the world");
        } finally {n.discard();if(!previous)level.setChunkForced(chunk.x,chunk.z,false);}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20,batch="nail_containers")
    public static void containerInventoriesDoNotBecomeItemEntities(GameTestHelper h) {
        var level=h.getLevel(); var origin=h.absolutePos(new BlockPos(2,2,2));
        var states=new net.minecraft.world.level.block.state.BlockState[]{
            Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true),
            Blocks.HOPPER.defaultBlockState(),Blocks.FURNACE.defaultBlockState()};
        for(int i=0;i<states.length;i++) {
            BlockPos pos=origin.offset(i*3,0,0);level.setBlock(pos,states[i],3);
            h.assertTrue(level.getBlockEntity(pos) instanceof Container,"Fixture container missing");
            ((Container)level.getBlockEntity(pos)).setItem(0,new ItemStack(Items.DIAMOND,64));
            h.assertTrue(TerrainOperations.replaceWithoutDrops(level,pos,Blocks.AIR.defaultBlockState()),"Removal failed");
            h.assertTrue(level.getBlockEntity(pos)==null,"Block entity was not removed");
        }
        h.runAtTickTime(2,()->{
            h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,new AABB(origin).inflate(12)).isEmpty(),"Inventory items escaped removal");
            h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=20,batch="nail_async")
    public static void coldChunkRequestIsPersistentButDoesNotLoadSynchronously(GameTestHelper h) {
        var level=h.getLevel();var local=new ChunkPos(h.absolutePos(new BlockPos(2,2,2)));
        var chunk=new ChunkPos(local.x+4096,local.z+4096);var pos=new BlockPos(chunk.x*16,80,chunk.z*16);
        h.assertFalse(level.isLoaded(pos),"Fixture chunk must start unloaded");
        h.assertFalse(level.getForcedChunks().contains(chunk.toLong()),"Fixture chunk must not be pre-forced");
        try {
            h.assertTrue(TerrainOperations.forceChunk(level,chunk),"Request was not recorded");
            h.assertTrue(level.getForcedChunks().contains(chunk.toLong()),"Request is absent from persistent forced set");
            h.assertFalse(level.isLoaded(pos),"Request synchronously loaded the cold chunk");
        } finally {level.setChunkForced(chunk.x,chunk.z,false);}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20,batch="nail_ownership")
    public static void finishingOverlappingImpactsDoNotTransferOwnershipBack(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(8,4,8));
        var a=nail(level,pos,"owner_a",32);var b=nail(level,pos.offset(1,0,0),"owner_b",32);
        ChunkPos shared = null;
        boolean acquired = false;
        try {
            h.assertTrue(level.addFreshEntity(a)&&level.addFreshEntity(b),"Fixture entities not added");
            impact(a,level,pos);impact(b,level,pos.offset(1,0,0));
            // GameTest's origin is random and its structure chunks are already forced.
            // Find a real temporary overlap, excluding both anchors and all pre-existing tickets.
            search: for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++) {
                var candidate = new ChunkPos(a.chunkPosition().x+dx,a.chunkPosition().z+dz);
                if(candidate.equals(a.chunkPosition()) || candidate.equals(b.chunkPosition())
                        || level.getForcedChunks().contains(candidate.toLong())) continue;
                boolean inA = com.nstut.celestialnail.CelestialNailMath.chunkIntersectsHorizontalRadius(
                        candidate.x,candidate.z,pos.getX(),pos.getZ(),32);
                boolean inB = com.nstut.celestialnail.CelestialNailMath.chunkIntersectsHorizontalRadius(
                        candidate.x,candidate.z,pos.getX()+1,pos.getZ(),32);
                if(inA && inB) { shared=candidate;break search; }
            }
            h.assertTrue(shared!=null,"No unowned temporary overlap available for fixture");
            long key=shared.toLong();
            acquired=TerrainOperations.forceChunk(level,shared);
            h.assertTrue(acquired,"Fixture did not acquire its temporary chunk");
            set(a,"ownedImpactForcedChunks",new java.util.HashSet<>(Set.of(key)));
            finish(a,level);
            h.assertTrue(save(b).getLongArray("OwnedImpactForcedChunks").length==1,"First owner did not transfer to active overlapping nail");
            finish(b,level);
            h.assertFalse(level.getForcedChunks().contains(key),"Completed nails exchanged temporary ownership back");
            h.assertTrue(save(a).getLongArray("OwnedImpactForcedChunks").length==0,"Completed first nail reaccepted ownership");
        } finally {
            a.discard();b.discard();
            if(acquired)level.setChunkForced(shared.x,shared.z,false);
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=400,batch="nail_boundary")
    public static void fullImpactReconcilesUnsupportedBoundaryTorch(GameTestHelper h) {
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(7,5,7));
        var support=center.above(4);var torch=support.above();
        level.setBlock(support,Blocks.STONE.defaultBlockState(),3);level.setBlock(torch,Blocks.TORCH.defaultBlockState(),3);
        var n=nail(level,center,"boundary",4);h.assertTrue(level.addFreshEntity(n),"Nail not added");
        n.forceOwnChunk(level);impact(n,level,center);
        h.succeedWhen(()->{
            h.assertTrue(save(n).getBoolean("BlastCleared"),"Blast/boundary pass is not finished");
            h.assertTrue(level.getBlockState(torch).isAir(),"Unsupported torch survived the boundary pass");
            h.assertTrue(level.getBlockState(support).isAir(),"Support inside sphere was not removed");
            n.discard();
        });
    }
    @GameTest(template="empty",timeoutTicks=20,batch="nail_persistence")
    public static void boundaryProgressRoundTripsWithoutRestartingBlast(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(2,2,2));
        var n=nail(level,pos,"save",4);var restored=nail(level,pos,"restore",4);
        set(n,"boundaryActive",true);set(n,"boundaryChanged",true);set(n,"boundaryIndex",123L);set(n,"purgePass",2);
        CompoundTag tag=save(n);restored.load(tag);CompoundTag after=save(restored);
        h.assertTrue(after.getBoolean("BoundaryActive")&&after.getLong("BoundaryIndex")==123,"Boundary cursor was lost on NBT round trip");
        h.assertTrue(after.getBoolean("BoundaryChanged"),"Changed pass lost its required revisit on NBT reload");
        tag.remove("BoundaryChanged");restored.load(tag);
        h.assertTrue(save(restored).getBoolean("BoundaryChanged"),"Legacy partial passes must be revisited conservatively");
        h.assertTrue(after.getInt("PurgePass")==2,"Completed purge phase was lost on NBT round trip");
        n.discard();restored.discard();h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100,batch="nail_callback_cold")
    public static void removingTripwireDoesNotLoadColdNeighbor(GameTestHelper h) {
        var level=h.getLevel();var local=new ChunkPos(h.absolutePos(new BlockPos(2,2,2)));
        var chunk=level.getChunk(local.x+2048,local.z+2048);
        var pos=new BlockPos(chunk.getPos().getMinBlockX(),100,chunk.getPos().getMaxBlockZ());
        // Install an existing wire without running placement callbacks as part of the fixture.
        chunk.getSection(chunk.getSectionIndex(pos.getY())).setBlockState(0,pos.getY()&15,15,Blocks.TRIPWIRE.defaultBlockState());
        h.assertFalse(level.isLoaded(pos.west()),"West neighbor must start cold");
        h.assertFalse(level.isLoaded(pos.south()),"South neighbor must start cold");
        h.assertTrue(TerrainOperations.replaceWithoutDrops(level,pos,Blocks.AIR.defaultBlockState()),"Wire removal failed");
        h.assertFalse(level.isLoaded(pos.west()),"Removal callback loaded west neighbor");
        h.assertFalse(level.isLoaded(pos.south()),"Removal callback loaded south neighbor");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=400,batch="nail_scaffolding")
    public static void fullImpactRemovesUnsupportedScaffoldingWithoutDeferredDrops(GameTestHelper h) {
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(7,5,7));
        var support=center.above(4);var scaffold=support.above();
        level.setBlock(support,Blocks.STONE.defaultBlockState(),3);
        level.setBlock(scaffold,Blocks.SCAFFOLDING.defaultBlockState().setValue(net.minecraft.world.level.block.ScaffoldingBlock.DISTANCE,0),3);
        var n=nail(level,center,"scaffolding_boundary",4);
        long[] finished={-1};
        h.runAtTickTime(5,()->{
            h.assertTrue(level.getBlockState(scaffold).is(Blocks.SCAFFOLDING),"Scaffolding fixture did not settle");
            h.assertTrue(level.addFreshEntity(n),"Nail not added");n.forceOwnChunk(level);impact(n,level,center);
        });
        h.succeedWhen(()->{
            h.assertTrue(save(n).getBoolean("BlastCleared"),"Blast/boundary pass is not finished");
            if(finished[0]<0)finished[0]=level.getGameTime();
            h.assertTrue(level.getGameTime()>=finished[0]+10,"Waiting for deferred survival ticks");
            h.assertTrue(level.getBlockState(support).isAir(),"Support inside sphere survived");
            h.assertTrue(level.getBlockState(scaffold).isAir(),"Unsupported boundary scaffolding survived");
            h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,new AABB(center).inflate(10)).isEmpty(),"Boundary survival tick created drops");
            h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class,new AABB(center).inflate(10)).isEmpty(),"Boundary spawned a falling block");
            n.discard();
        });
    }

    @GameTest(template="empty",timeoutTicks=400,batch="nail_adjacent_scaffolding")
    public static void fullImpactSettlesAdjacentUnsupportedScaffoldingWithoutDrops(GameTestHelper h) {
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(7,5,7));
        var support=center.offset(1,3,0);var scaffold=support.above();
        var otherSupport=center.offset(2,3,0);var otherScaffold=otherSupport.above();
        level.setBlock(otherSupport,Blocks.STONE.defaultBlockState(),3);
        level.setBlock(otherScaffold,Blocks.SCAFFOLDING.defaultBlockState().setValue(net.minecraft.world.level.block.ScaffoldingBlock.DISTANCE,0),3);
        level.setBlock(support,Blocks.STONE.defaultBlockState(),3);
        level.setBlock(scaffold,Blocks.SCAFFOLDING.defaultBlockState().setValue(net.minecraft.world.level.block.ScaffoldingBlock.DISTANCE,0),3);
        var n=nail(level,center,"adjacent_scaffolding_boundary",4);
        long[] finished={-1};
        h.runAtTickTime(5,()->{
            h.assertTrue(level.getBlockState(scaffold).is(Blocks.SCAFFOLDING),"Scaffolding fixture did not settle");
            h.assertTrue(level.getBlockState(otherScaffold).is(Blocks.SCAFFOLDING),"Adjacent fixture did not settle");
            h.assertTrue(level.addFreshEntity(n),"Nail not added");n.forceOwnChunk(level);impact(n,level,center);
        });
        h.succeedWhen(()->{
            h.assertTrue(save(n).getBoolean("BlastCleared"),"Blast/boundary pass is not finished");
            if(finished[0]<0)finished[0]=level.getGameTime();
            h.assertTrue(level.getGameTime()>=finished[0]+10,"Waiting for deferred survival ticks");
            h.assertTrue(level.getBlockState(support).isAir(),"Support inside sphere survived");
            h.assertTrue(level.getBlockState(otherSupport).isAir(),"Adjacent support survived");
            h.assertTrue(level.getBlockState(otherScaffold).isAir(),"Adjacent unsupported scaffolding survived");
            h.assertTrue(level.getBlockState(scaffold).isAir(),"Unsupported boundary scaffolding survived");
            h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,new AABB(center).inflate(10)).isEmpty(),"Boundary survival tick created drops");
            h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class,new AABB(center).inflate(10)).isEmpty(),"Boundary spawned a falling block");
            n.discard();
        });
    }

    @GameTest(template="empty",timeoutTicks=400,batch="nail_leaf_natural")
    public static void fullImpactRemovesUnsupportedNaturalBoundaryLeaf(GameTestHelper h) {
        boundaryLeaves(h,false,false,false);
    }
    @GameTest(template="empty",timeoutTicks=400,batch="nail_leaf_persistent")
    public static void fullImpactPreservesPersistentBoundaryLeaf(GameTestHelper h) {
        boundaryLeaves(h,true,false,false);
    }
    @GameTest(template="empty",timeoutTicks=400,batch="nail_leaf_supported")
    public static void fullImpactPreservesSupportedBoundaryLeaf(GameTestHelper h) {
        boundaryLeaves(h,false,true,false);
    }
    @GameTest(template="empty",timeoutTicks=400,batch="nail_leaf_adjacent")
    public static void fullImpactSettlesAdjacentUnsupportedBoundaryLeaves(GameTestHelper h) {
        boundaryLeaves(h,false,false,true);
    }
    private static void boundaryLeaves(GameTestHelper h,boolean persistent,boolean supported,boolean adjacent) {
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(7,5,7));
        var leaf=center.offset(1,4,0);var other=center.offset(2,4,0);
        var state=Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE,1)
                .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,persistent);
        level.setBlock(leaf.below(),Blocks.OAK_LOG.defaultBlockState(),3);
        if(supported)level.setBlock(leaf.above(),Blocks.OAK_LOG.defaultBlockState(),3);
        level.setBlock(leaf,state,3);
        if(adjacent) {
            level.setBlock(other.below(),Blocks.OAK_LOG.defaultBlockState(),3);
            level.setBlock(other,state,3);
        }
        var n=nail(level,center,"boundary_leaves",4);long[] finished={-1};
        h.runAtTickTime(5,()->{
            h.assertTrue(level.getBlockState(leaf).getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)==1,"Leaf fixture lost support before impact");
            h.assertTrue(level.addFreshEntity(n),"Nail not added");n.forceOwnChunk(level);impact(n,level,center);
        });
        h.succeedWhen(()->{
            h.assertTrue(save(n).getBoolean("BlastCleared"),"Boundary pass is unfinished");
            if(finished[0]<0)finished[0]=level.getGameTime();
            h.assertTrue(level.getGameTime()>=finished[0]+10,"Waiting for deferred ticks");
            h.assertTrue(level.getBlockState(leaf.below()).isAir(),"Supporting log inside sphere survived");
            if(persistent || supported) {
                var actual=level.getBlockState(leaf);
                h.assertTrue(actual.is(Blocks.OAK_LEAVES),"Legitimately supported or persistent leaf was removed");
                h.assertTrue(actual.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT)==persistent,"Persistence changed");
                h.assertTrue(actual.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)==(supported?1:7),"Leaf support distance stayed stale");
                if(supported)h.assertTrue(level.getBlockState(leaf.above()).is(Blocks.OAK_LOG),"External support was removed");
            } else h.assertTrue(level.getBlockState(leaf).isAir(),"Unsupported natural boundary leaf survived");
            if(adjacent) {
                h.assertTrue(level.getBlockState(other.below()).isAir(),"Second log survived");
                h.assertTrue(level.getBlockState(other).isAir(),"Adjacent unsupported natural leaf survived");
            }
            h.assertFalse(level.getBlockTicks().hasScheduledTick(leaf,Blocks.OAK_LEAVES),"Deferred support tick escaped reconciliation");
            h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,new AABB(center).inflate(10)).isEmpty(),"Leaf reconciliation created drops");
            n.discard();
        });
    }

    @GameTest(template="empty",timeoutTicks=400,batch="nail_suspicioussand")
    public static void fullImpactReconcilesSuspiciousSand(GameTestHelper h) {
        boundaryBrushable(h,Blocks.SUSPICIOUS_SAND,false);
    }
    @GameTest(template="empty",timeoutTicks=400,batch="nail_suspiciousgravel")
    public static void fullImpactReconcilesSuspiciousGravel(GameTestHelper h) {
        boundaryBrushable(h,Blocks.SUSPICIOUS_GRAVEL,false);
    }
    @GameTest(template="empty",timeoutTicks=400,batch="nail_supportedsuspicioussand")
    public static void fullImpactReconcilesSupportedSuspiciousSand(GameTestHelper h) {
        boundaryBrushable(h,Blocks.SUSPICIOUS_SAND,true);
    }
    @GameTest(template="empty",timeoutTicks=400,batch="nail_supportedsuspiciousgravel")
    public static void fullImpactReconcilesSupportedSuspiciousGravel(GameTestHelper h) {
        boundaryBrushable(h,Blocks.SUSPICIOUS_GRAVEL,true);
    }
    private static void boundaryBrushable(GameTestHelper h,net.minecraft.world.level.block.Block block,boolean supported) {
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(7,5,7));
        var pos=supported?center.offset(4,-1,0):center.above(5);
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(pos,block.defaultBlockState(),3);
        var original=level.getBlockEntity(pos);
        h.assertTrue(original!=null,"Brushable fixture has no block entity");
        var n=nail(level,center,"boundary_brushable",4);long[] finished={-1};
        h.runAtTickTime(5,()->{
            h.assertTrue(level.getBlockState(pos).is(block),"Brushable fell before impact");
            h.assertTrue(level.addFreshEntity(n),"Nail not added");n.forceOwnChunk(level);impact(n,level,center);
        });
        h.succeedWhen(()->{
            h.assertTrue(save(n).getBoolean("BlastCleared"),"Boundary unfinished");
            if(finished[0]<0)finished[0]=level.getGameTime();
            h.assertTrue(level.getGameTime()>=finished[0]+10,"Waiting for deferred gravity");
            if(supported) {
                h.assertTrue(level.getBlockState(pos).is(block),"Supported brushable removed");
                h.assertTrue(level.getBlockEntity(pos)==original && !original.isRemoved(),"Supported block entity replaced or removed");
            } else {
                h.assertTrue(level.getBlockState(pos.below()).isAir(),"Supporting stone survived");
                h.assertTrue(level.getBlockState(pos).isAir(),"Unsupported brushable survived");
                h.assertTrue(level.getBlockEntity(pos)==null && original.isRemoved(),"Obsolete block entity not cleaned up");
            }
            h.assertFalse(level.getBlockTicks().hasScheduledTick(pos,block),"Deferred brushable tick escaped");
            h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,new AABB(center).inflate(10)).isEmpty(),"Brushable dropped items");
            h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class,new AABB(center).inflate(10)).isEmpty(),"Brushable spawned a falling entity");
            n.discard();
        });
    }
    @GameTest(template="empty",timeoutTicks=400,batch="nail_coral_dry")
    public static void fullImpactKillsDehydratedBoundaryCoral(GameTestHelper h) { boundaryCoral(h,false); }
    @GameTest(template="empty",timeoutTicks=400,batch="nail_coral_wet")
    public static void fullImpactPreservesHydratedBoundaryCoral(GameTestHelper h) { boundaryCoral(h,true); }
    private static void boundaryCoral(GameTestHelper h,boolean hydrated) {
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(7,5,7));var coral=center.above(5);
        var water=hydrated?coral.above():coral.below();
        for(var direction:net.minecraft.core.Direction.values()) {
            var side=water.relative(direction);
            if(!side.equals(coral))level.setBlock(side,Blocks.STONE.defaultBlockState(),3);
        }
        level.setBlock(water,Blocks.WATER.defaultBlockState(),3);
        level.setBlock(coral,Blocks.TUBE_CORAL_BLOCK.defaultBlockState(),3);
        var n=nail(level,center,"boundary_coral",4);long[] finished={-1};
        h.runAtTickTime(5,()->{
            h.assertTrue(level.addFreshEntity(n),"Nail not added");n.forceOwnChunk(level);impact(n,level,center);
        });
        h.succeedWhen(()->{
            h.assertTrue(save(n).getBoolean("BlastCleared"),"Boundary unfinished");
            if(finished[0]<0)finished[0]=level.getGameTime();
            h.assertTrue(level.getGameTime()>=finished[0]+110,"Waiting beyond vanilla coral delay");
            if(!hydrated)for(var direction:net.minecraft.core.Direction.values())
                h.assertFalse(level.getFluidState(coral.relative(direction)).is(net.minecraft.tags.FluidTags.WATER),"Dry fixture still has water");
            h.assertTrue(level.getBlockState(coral).is(hydrated?Blocks.TUBE_CORAL_BLOCK:Blocks.DEAD_TUBE_CORAL_BLOCK),"Incorrect coral hydration settlement");
            h.assertFalse(level.getBlockTicks().hasScheduledTick(coral,Blocks.TUBE_CORAL_BLOCK),"Deferred coral death escaped");
            h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,new AABB(center).inflate(10)).isEmpty(),"Coral conversion dropped items");
            n.discard();
        });
    }

    @GameTest(template="empty",timeoutTicks=30,batch="nail_coral_variants")
    public static void coralVariantsPreserveHydrationAndWallOrientation(GameTestHelper h) throws Exception {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,5,3));
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(pos.west(),Blocks.STONE.defaultBlockState(),3);
        for(String color:new String[]{"TUBE","BRAIN","BUBBLE","FIRE","HORN"})
            for(String suffix:new String[]{"_CORAL_BLOCK","_CORAL","_CORAL_FAN","_CORAL_WALL_FAN"}) {
                var live=(net.minecraft.world.level.block.Block)Blocks.class.getField(color+suffix).get(null);
                var dead=(net.minecraft.world.level.block.Block)Blocks.class.getField("DEAD_"+color+suffix).get(null);
                var state=live.defaultBlockState();
                if(state.hasProperty(BlockStateProperties.WATERLOGGED))state=state.setValue(BlockStateProperties.WATERLOGGED,false);
                if(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))state=state.setValue(BlockStateProperties.HORIZONTAL_FACING,net.minecraft.core.Direction.EAST);
                TerrainOperations.replaceWithoutDrops(level,pos,state);
                var next=TerrainOperations.reconcileBoundary(level,pos,state);
                h.assertTrue(next.is(dead),"Dry coral variant not converted: "+color+suffix);
                if(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
                    h.assertTrue(next.getValue(BlockStateProperties.HORIZONTAL_FACING)==net.minecraft.core.Direction.EAST,"Wall fan orientation lost");
                level.setBlock(pos.above(),Blocks.WATER.defaultBlockState(),2);
                h.assertTrue(TerrainOperations.reconcileBoundary(level,pos,state)==state,"Externally hydrated coral converted");
                level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),2);
                if(state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    var wet=state.setValue(BlockStateProperties.WATERLOGGED,true);
                    TerrainOperations.replaceWithoutDrops(level,pos,wet);
                    h.assertTrue(TerrainOperations.reconcileBoundary(level,pos,wet)==wet,"Waterlogged coral converted");
                }
            }
        TerrainOperations.replaceWithoutDrops(level,pos,Blocks.AIR.defaultBlockState());
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=30,batch="nail_deferred")
    public static void boundarySuppressesDeferredBlockTicksButKeepsFluidsAndOrdinaryTicks(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,4,3));
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
        var scaffold=Blocks.SCAFFOLDING.defaultBlockState()
                .setValue(net.minecraft.world.level.block.ScaffoldingBlock.DISTANCE,0)
                .setValue(BlockStateProperties.WATERLOGGED,true);
        level.setBlock(pos,scaffold,3);
        var bounds=new net.minecraft.world.level.levelgen.structure.BoundingBox(pos.getX(),pos.getY(),pos.getZ(),pos.getX(),pos.getY(),pos.getZ());
        level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);
        var next=TerrainOperations.reconcileBoundary(level,pos,scaffold);
        h.assertTrue(next.is(Blocks.SCAFFOLDING),"Supported scaffolding was removed");
        h.assertFalse(level.getBlockTicks().hasScheduledTick(pos,Blocks.SCAFFOLDING),"Boundary queued destructive block work");
        h.assertTrue(level.getFluidTicks().hasScheduledTick(pos,net.minecraft.world.level.material.Fluids.WATER),"Boundary suppressed permitted water flow");
        // Exercise the generic scheduled-survival path, not only the scaffolding special case.
        var sandPos=pos.offset(3,0,0);level.setBlock(sandPos.below(),Blocks.STONE.defaultBlockState(),3);
        var sand=Blocks.SAND.defaultBlockState();TerrainOperations.replaceWithoutDrops(level,sandPos,sand);
        TerrainOperations.reconcileBoundary(level,sandPos,sand);
        h.assertFalse(level.getBlockTicks().hasScheduledTick(sandPos,Blocks.SAND),"Shape check leaked a sand survival tick");
        level.scheduleTick(pos,Blocks.SCAFFOLDING,1);
        h.assertTrue(level.getBlockTicks().hasScheduledTick(pos,Blocks.SCAFFOLDING),"Scoped suppression leaked into ordinary world ticks");
        h.assertFalse(com.nstut.explosion.terrain.TerrainMutationScope.active(),"Mutation scope leaked");
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=20,batch="nail_motion")
    public static void receivedMovementInterpolatesWithoutSnappingOrOvershoot(GameTestHelper h) {
        var level=h.getLevel();var n=nail(level,h.absolutePos(new BlockPos(2,20,2)),"motion",4);
        double start=n.getY();n.lerpTo(n.getX(),start-12,n.getZ(),0,0,3);
        h.assertTrue(n.getY()==start,"Position packet snapped immediately");
        try {
            Method step=CelestialNailEntity.class.getDeclaredMethod("interpolateMovement");step.setAccessible(true);
            step.invoke(n);h.assertTrue(Math.abs(n.getY()-(start-6))<.001,"First interpolation step missing");
            step.invoke(n);h.assertTrue(Math.abs(n.getY()-(start-12))<.001,"Interpolation missed target");
            step.invoke(n);h.assertTrue(Math.abs(n.getY()-(start-12))<.001,"Interpolation overshot settled target");
        } catch(ReflectiveOperationException e){throw new AssertionError(e);}
        n.discard();h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20,batch="nail_fast_descent")
    public static void acceleratedDescentStillHitsSingleBlockFloor(GameTestHelper h) {
        var level=h.getLevel();var floor=h.absolutePos(new BlockPos(3,40,3));
        level.setBlock(floor,Blocks.STONE.defaultBlockState(),3);
        var n=nail(level,floor.above(60),"fast",4);
        var tag=save(n);tag.putByte("Phase",(byte)1);tag.putLong("LaunchTime",level.getGameTime()-30);n.load(tag);
        try {
            Method fall=CelestialNailEntity.class.getDeclaredMethod("tickDescending");fall.setAccessible(true);
            for(int i=0;i<20&&!n.isImpacting();i++)fall.invoke(n);
            h.assertTrue(n.isImpacting(),"Fast descent missed the floor");
            h.assertTrue(Math.abs(n.impactOrigin().y-(floor.getY()+1))<.001,"Expected floor top "+(floor.getY()+1)+" but impact was "+n.impactOrigin().y);
        } catch(ReflectiveOperationException e){throw new AssertionError(e);}
        n.discard();h.succeed();
    }

}
