package com.nstut.celestialnail;

import com.nstut.celestialnail.client.CinematicSettings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CinematicTimelineTest {
    @Test void shakeSurvivesCatchUpButOldEventsStayQuiet() {
        var clock=new ImpactPresentationClock();
        clock.observe(-1); clock.observe(12);
        assertEquals(1,CinematicTimeline.cameraShake(clock.age(1_000_000_000L,20),90));
        assertTrue(CinematicTimeline.cameraShake(clock.age(1_250_000_000L,25),90)>.5);
        assertEquals(0,CinematicTimeline.cameraShake(-1,90));
        assertEquals(0,CinematicTimeline.cameraShake(100,90));
        assertEquals(0,CinematicTimeline.cameraShake(2000,90));
    }
    @Test void dustFallsBelowHighImpactsWithBoundedVolumeGeometry() {
        var fresh=CinematicDustShape.at(0,32,72);
        var falling=CinematicDustShape.at(90,32,72);
        assertEquals(0,fresh.drop());
        assertTrue(falling.drop()>100);
        assertTrue(falling.span()>=falling.front()+falling.width()*2);
        assertTrue(falling.top()>falling.curtainHeight());
        assertTrue(CinematicDustShape.at(240,10000,10000).drop()<=384);
    }
    @Test void delayedDeliveryGetsOnePulseButJoiningOrDuplicateUpdatesNeverReplayIt() {
        var clock = new ImpactPresentationClock();
        clock.observe(-1);
        clock.observe(5);
        // Several game ticks may elapse during impact terrain work before rendering.
        assertEquals(0, clock.age(1_000_000_000L, 12));
        clock.observe(13);
        assertEquals(1.5F, clock.age(1_075_000_000L, 13));
        clock.observe(17);
        assertEquals(0, CinematicTimeline.impactFrame(clock.age(1_800_000_000L, 28)));
        var joining = new ImpactPresentationClock();
        joining.observe(900);
        assertEquals(900, joining.age(1_000_000_000L, 900));
        var stale = new ImpactPresentationClock();
        stale.observe(-1);
        stale.observe(100);
        assertEquals(100, stale.age(1_000_000_000L, 100));
        var hidden = new ImpactPresentationClock();
        hidden.observe(-1);
        hidden.observe(0);
        assertEquals(120, hidden.age(1_000_000_000L, 120));
    }
    @Test void authoredSequenceHasDistinctBeatsAndBoundedRecovery() {
        float[] ages={0,1.5F,3.2F,4.5F,6,7.5F,8.7F,10,12};
        int[] stages={0,1,5,2,5,3,5,4,5};
        for(int i=0;i<ages.length;i++) assertEquals(stages[i],ImpactSequence.stage(ages[i],ImpactSequence.FULL));
        assertTrue(ImpactSequence.strength(4.5F,2)>ImpactSequence.strength(3.2F,2));
        assertTrue(ImpactSequence.strength(10,2)>ImpactSequence.strength(8.7F,2));
        float previous=1;
        for(float age=0;age<15;age+=.1F) {
            float strength=ImpactSequence.strength(age,ImpactSequence.REDUCED);
            assertTrue(strength<=previous); previous=strength;
            assertEquals(6,ImpactSequence.stage(age,ImpactSequence.REDUCED));
            assertEquals(0,ImpactSequence.strength(age,ImpactSequence.OFF));
        }
        for(float age:new float[]{-1,15,20,1200,100000}) assertEquals(0,CinematicTimeline.impactFrame(age));
        assertEquals(0,ImpactSequence.mode("off"));
        assertEquals(1,ImpactSequence.mode("Reduced"));
        assertEquals(2,ImpactSequence.mode("invalid"));
    }
    @Test void dustBuildsThenRevealsTheNailAndOnlyLocalPresenceRemains() {
        assertEquals(0, CinematicTimeline.dust(-1));
        assertEquals(0, CinematicTimeline.dust(0));
        assertEquals(1, CinematicTimeline.dust(6));
        assertEquals(1, CinematicTimeline.dust(90));
        assertTrue(CinematicTimeline.dust(180) < CinematicTimeline.dust(120));
        assertEquals(0, CinematicTimeline.dust(240));
        assertEquals(0, CinematicTimeline.lingering(0, 1199));
        assertEquals(.12F, CinematicTimeline.lingering(0, 1200));
        assertEquals(0, CinematicTimeline.lingering(96, 1200));
        assertEquals(CinematicTimeline.lingering(20, 1200), CinematicTimeline.lingering(20, 100000));
    }
    @Test void rangeAndAccessibilityValuesHaveFiniteBounds() {
        assertEquals(1, CinematicTimeline.proximity(0, 640));
        assertEquals(0, CinematicTimeline.proximity(640, 640));
        assertEquals(0, CinematicTimeline.proximity(Double.NaN, 640));
        assertEquals(0, CinematicTimeline.proximity(10, 0));
        assertEquals(0, CinematicSettings.samples("0"));
        assertEquals(8, CinematicSettings.samples("1"));
        assertEquals(24, CinematicSettings.samples("100"));
        assertEquals(16, CinematicSettings.samples("oops"));
        assertEquals(0, CinematicSettings.intensity("-1", .8F));
        assertEquals(1, CinematicSettings.intensity("2", .8F));
        for (String value : new String[]{"NaN", "Infinity", "oops", null})
            assertEquals(.8F, CinematicSettings.intensity(value, .8F));
    }
}
