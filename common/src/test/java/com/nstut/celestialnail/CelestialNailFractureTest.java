package com.nstut.celestialnail;
import com.nstut.celestialnail.client.CelestialNailFracture;
import com.nstut.celestialnail.client.CelestialNailMesh;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CelestialNailFractureTest {
    private double distance(CelestialNailMesh.Point a,CelestialNailMesh.Point b) {
        return Math.sqrt(Math.pow(a.x()-b.x(),2)+Math.pow(a.y()-b.y(),2)+Math.pow(a.z()-b.z(),2));
    }
    @Test void fragmentsHoldTheirShapeAndHaveBoundedGeometry() {
        int count=0;
        for(var piece:CelestialNailFracture.BODY) {
            count+=piece.faces().size();var face=piece.faces().get(0);
            for(float age:new float[]{0,8,16,24,36,54}) {
                var motion=CelestialNailFracture.motion(piece,age);
                assertEquals(distance(face.a(),face.c()),distance(motion.apply(face.a()),motion.apply(face.c())),.00001);
                assertTrue(motion.alpha()>=0 && motion.alpha()<=1);
            }
            assertEquals(0,distance(face.a(),CelestialNailFracture.motion(piece,0).apply(face.a())),.00001);
            assertEquals(0,CelestialNailFracture.motion(piece,CelestialNailVisuals.CRUMBLE_TICKS).alpha());
        }
        assertTrue(count>CelestialNailMesh.BODY.size(),"Fractures require interior surfaces");
        assertTrue(count<35000,"Temporary geometry must remain bounded");
    }
    @Test void releasedFragmentsAccelerateDownInsteadOfDriftingAtConstantSpeed() {
        for(var piece:CelestialNailFracture.BODY) {
            float y20=CelestialNailFracture.motion(piece,20).dy();
            float y25=CelestialNailFracture.motion(piece,25).dy();
            float y30=CelestialNailFracture.motion(piece,30).dy();
            assertTrue(y30-y25<y25-y20);
        }
    }
}
