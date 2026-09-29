package com.nstut.celestialnail;

import com.nstut.celestialnail.client.CelestialNailMesh;
import org.junit.jupiter.api.Test;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class CelestialNailMeshTest {
    @Test
    void meshHasFiniteNormalsAndFitsItsRenderBounds() {
        assertTrue(CelestialNailMesh.BODY.size() + CelestialNailMesh.SHARDS.size() < 10000,
                "Keep the baked geometry within its per-entity vertex budget");
        Stream.concat(CelestialNailMesh.BODY.stream(), CelestialNailMesh.SHARDS.stream()).forEach(face -> {
            var n = face.normal();
            assertEquals(1, n.x()*n.x()+n.y()*n.y()+n.z()*n.z(), .001, "Degenerate face normal");
            assertTrue(face.material() >= 0 && face.material() < 8, "Atlas tile exists");
            for (var p : new CelestialNailMesh.Point[]{face.a(),face.b(),face.c(),face.d()}) {
                assertTrue(Float.isFinite(p.x()) && Float.isFinite(p.y()) && Float.isFinite(p.z()));
                assertTrue(Math.abs(p.x()) < 2 && Math.abs(p.z()) < 2 && p.y() >= -.01 && p.y() < 8,
                        "Mesh must fit expanded render bounds with its tip at the entity origin");
            }
        });
    }

    @Test
    void crystalShardsAreEmissiveAndStoneRemainsWorldLit() {
        assertFalse(CelestialNailMesh.SHARDS.isEmpty());
        assertTrue(CelestialNailMesh.SHARDS.stream().allMatch(CelestialNailMesh.Face::emissive));
        assertTrue(CelestialNailMesh.BODY.stream().anyMatch(CelestialNailMesh.Face::emissive));
        assertTrue(CelestialNailMesh.BODY.stream().anyMatch(f -> !f.emissive()));
    }
}
