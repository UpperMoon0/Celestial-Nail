package com.nstut.celestialnail.gametest;

import com.nstut.celestialnail.entity.CelestialNailEntity;
import com.nstut.celestialnail.entity.NailWorldOperations;
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
            h.assertTrue(NailWorldOperations.replaceWithoutDrops(level,pos,Blocks.AIR.defaultBlockState()),"Removal failed");
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
            h.assertTrue(NailWorldOperations.forceChunk(level,chunk),"Request was not recorded");
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
            acquired=NailWorldOperations.forceChunk(level,shared);
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
        set(n,"boundaryActive",true);set(n,"boundaryIndex",123L);set(n,"purgePass",2);
        CompoundTag tag=save(n);restored.load(tag);CompoundTag after=save(restored);
        h.assertTrue(after.getBoolean("BoundaryActive")&&after.getLong("BoundaryIndex")==123,"Boundary cursor was lost on NBT round trip");
        h.assertTrue(after.getInt("PurgePass")==2,"Completed purge phase was lost on NBT round trip");
        n.discard();restored.discard();h.succeed();
    }
}
