import com.nstut.celestialnail.client.CelestialNailMesh;
import java.util.stream.Stream;

/** Export the exact game mesh for offline visual inspection, without a Minecraft client. */
public final class ExportNailMesh {
    public static void main(String[] args) {
        Stream.concat(CelestialNailMesh.BODY.stream(), CelestialNailMesh.SHARDS.stream()).forEach(f -> {
            System.out.print(f.material() + " " + f.shade());
            for (var p : new CelestialNailMesh.Point[]{f.a(), f.b(), f.c(), f.d()})
                System.out.print(" " + p.x() + " " + p.y() + " " + p.z());
            System.out.println();
        });
    }
}
