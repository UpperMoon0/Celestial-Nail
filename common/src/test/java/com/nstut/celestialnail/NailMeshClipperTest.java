package com.nstut.celestialnail;

import com.nstut.celestialnail.client.CelestialNailMesh;
import com.nstut.celestialnail.client.NailMeshClipper;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class NailMeshClipperTest {
    @Test void emergedNailCannotBeEntirelyClippedAtAnySupportedScale() {
        for (float scale : new float[]{.1F, .5F, 1, 2, 4}) {
            float height = CelestialNailVisuals.height(scale);
            float portal = CelestialNailVisuals.portalHeight(height);
            float unit = height / CelestialNailMesh.HEIGHT;
            for (float age : new float[]{31, 60, 100, 170, 5000}) {
                float offset = CelestialNailVisuals.emergenceOffset(portal, age);
                float bob = (float) Math.sin(age * .045F) * height * .0025F * CelestialNailVisuals.emergence(age);
                float ceiling = (portal - offset - bob) / unit;
                var count = new AtomicInteger();
                NailMeshClipper.emit(CelestialNailMesh.BODY, ceiling, (face, p, u, v) -> count.incrementAndGet());
                assertTrue(count.get() > 0, "Emerging body disappeared at scale=" + scale + " age=" + age);
                assertEquals(0, count.get() % 4);
                if (age >= CelestialNailVisuals.READY_TICKS)
                    assertEquals(CelestialNailMesh.BODY.size() * 4, count.get(), "Ready body is still portal-clipped");
            }
        }
    }
    @Test void emergenceNeverDrawsGeometryAboveThePortalAndPreservesUvs() {
        for(float ceiling:new float[]{-1,0,.1F,1,3,6.8F,7.3F,8}) {
            var count=new AtomicInteger();
            NailMeshClipper.emit(CelestialNailMesh.BODY,ceiling,(face,p,u,v)->{
                assertTrue(p.y()<=ceiling+.00001F);
                assertTrue(Float.isFinite(p.x()) && Float.isFinite(p.z()));
                assertTrue(u>=0 && u<=1 && v>=0 && v<=1);
                count.incrementAndGet();
            });
            assertEquals(0,count.get()%4);
            if(ceiling<0) assertEquals(0,count.get());
            if(ceiling==8) assertEquals(CelestialNailMesh.BODY.size()*4,count.get());
        }
    }
    @Test void portalGeometryHasValidNormals() {
        for(var mesh:java.util.List.of(CelestialNailMesh.PORTAL_CORE,CelestialNailMesh.PORTAL_RIM,
                CelestialNailMesh.PORTAL_HALO,CelestialNailMesh.PORTAL_SPARKS,CelestialNailMesh.PORTAL_BEAM)) {
            assertFalse(mesh.isEmpty());
            for(var face:mesh) {
                var n=face.normal();
                assertEquals(1,n.x()*n.x()+n.y()*n.y()+n.z()*n.z(),.001);
                assertTrue(face.emissive());
            }
        }
    }
}
