import com.nstut.celestialnail.client.CelestialNailMesh;
import com.nstut.celestialnail.client.CelestialNailFracture;
import java.io.*;
import java.util.stream.Stream;
/** Binary samples of the production breakup geometry, including fading and solid interior faces. */
public final class ExportNailFracture {
    public static void main(String[] args) throws Exception {
        var pieces=Stream.concat(CelestialNailFracture.BODY.stream(),CelestialNailFracture.SHARDS.stream()).toList();
        int faces=pieces.stream().mapToInt(p->p.faces().size()).sum();
        try(var out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(args[0])))) {
            out.writeInt(82);out.writeInt(faces);
            for(int frame=0;frame<82;frame++) {
                float age=frame*2/3F;out.writeFloat(age);
                for(var piece:pieces) {
                    var motion=CelestialNailFracture.motion(piece,age);
                    for(var f:piece.faces()) {
                        out.writeFloat(f.material());out.writeFloat(f.shade());out.writeFloat(motion.alpha());
                        for(var p:new CelestialNailMesh.Point[]{f.a(),f.b(),f.c(),f.d()}) {
                            var moved=motion.apply(p);out.writeFloat(moved.x());out.writeFloat(moved.y());out.writeFloat(moved.z());
                        }
                    }
                }
            }
        }
    }
}
