package com.nstut.celestialnail;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CelestialNailVisualsTest {
    @Test void scaleDefinesHeightAndRejectsCorruptValues() {
        assertEquals(72, CelestialNailVisuals.height(1));
        assertEquals(144, CelestialNailVisuals.height(2));
        assertEquals(36, CelestialNailVisuals.height(.5F));
        assertEquals(72, CelestialNailVisuals.height(Float.NaN));
        assertEquals(72, CelestialNailVisuals.height(Float.POSITIVE_INFINITY));
    }
    @Test void portalOpensBeforeEmergenceAndClosesOnLaunch() {
        assertEquals(0, CelestialNailVisuals.opening(0,-1));
        assertEquals(1, CelestialNailVisuals.opening(30,-1));
        assertEquals(0, CelestialNailVisuals.emergence(30));
        assertEquals(1, CelestialNailVisuals.emergence(170));
        assertEquals(1, CelestialNailVisuals.opening(5000,-1));
        assertEquals(.5F, CelestialNailVisuals.opening(5000,15));
        assertEquals(0, CelestialNailVisuals.opening(5000,30));
    }
    @Test void animationRemainsBoundedAndMonotonic() {
        float last=0;
        for(int age=-20;age<220;age++) {
            float value=CelestialNailVisuals.emergence(age);
            assertTrue(value>=last && value<=1);
            last=value;
        }
    }
    @Test void nailTravelsFromPortalToCommandPositionWithScaledCrownClearance() {
        for(float scale:new float[]{.1F,.5F,1,2,4}) {
            float height=CelestialNailVisuals.height(scale);
            float portal=CelestialNailVisuals.portalHeight(height);
            assertEquals(height*.5F,portal-height,.0001F);
            assertEquals(portal,CelestialNailVisuals.emergenceOffset(portal,30));
            assertEquals(0,CelestialNailVisuals.emergenceOffset(portal,170));
            assertEquals(0,CelestialNailVisuals.emergenceOffset(portal,10000));
            // Once the crown clears the aperture, the nail must continue descending to rest.
            assertTrue(CelestialNailVisuals.emergenceOffset(portal,150)>0);
            assertTrue(CelestialNailVisuals.emergenceOffset(portal,150)+height<portal);
        }
        assertEquals(108,CelestialNailVisuals.portalHeight(72));
    }
    @Test void projectionContainsDeepEmbeddedTipAndCrownAcrossScalesAndCameraHeights() {
        for(float scale:new float[]{.1F,1,4}) for(double tip:new double[]{-220,-64,80,320})
            for(double cameraY:new double[]{-180,64,400}) for(double horizontal:new double[]{0,64,192,512}) {
                double height=CelestialNailVisuals.height(scale);
                double far=CelestialNailVisuals.bodyFarPlane(horizontal,cameraY,tip,height);
                for(double y:new double[]{tip,tip+height}) {
                    double farthestEdge=Math.hypot(horizontal+height*.65,y-cameraY);
                    assertTrue(far>farthestEdge,"Projection clips an endpoint of the tall body");
                }
            }
    }

}
