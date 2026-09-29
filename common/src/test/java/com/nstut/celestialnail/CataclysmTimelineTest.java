package com.nstut.celestialnail;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CataclysmTimelineTest {
 @Test void horizontalVisibilityKeepsNormalChunkBoundary() {
  assertEquals(1,CataclysmTimeline.horizontalFade(95,128));
  assertEquals(.5F,CataclysmTimeline.horizontalFade(112,128),.0001F);
  assertEquals(0,CataclysmTimeline.horizontalFade(128,128));
  assertEquals(0,CataclysmTimeline.horizontalFade(10000,128));
 }
 @Test void SoundAndWaveReachListenerTogether() {
  for(int distance:new int[]{0,24,120,384,720})
   assertEquals(distance,CataclysmTimeline.shockRadius(CataclysmTimeline.arrival(distance)),.001);
 }
 @Test void WaveDamageFollowsMovingFrontRatherThanWholeDisc() {
  assertTrue(CataclysmTimeline.shockHits(120,18,10));
  assertFalse(CataclysmTimeline.shockHits(150,18,10));
  assertFalse(CataclysmTimeline.shockHits(24,40,10));
  assertFalse(CataclysmTimeline.shockHits(720,80,10));
 }
 @Test void NailSettlesBelowCraterFloorAndStopsMoving() {
  assertEquals(0,CataclysmTimeline.pierceDepth(32,72,0));
  assertEquals(44.96F,CataclysmTimeline.pierceDepth(32,72,35),.001);
  assertEquals(CataclysmTimeline.pierceDepth(32,72,35),CataclysmTimeline.pierceDepth(32,72,10000));
 }
 @Test void AtmosphereEndsAndNeverShakesBeforeArrival() {
  assertEquals(0,CataclysmTimeline.shake(-1));
  assertEquals(1,CataclysmTimeline.aftermath(0));
  assertEquals(0,CataclysmTimeline.aftermath(1200));
  assertEquals(0,CataclysmTimeline.aftermath(10000));
  assertEquals(0,CataclysmTimeline.charge(-1));
  assertEquals(1,CataclysmTimeline.charge(24));
 }
}
