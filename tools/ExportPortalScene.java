import com.nstut.celestialnail.CelestialNailVisuals;
import com.nstut.celestialnail.client.CelestialNailMesh;
import com.nstut.celestialnail.client.NailMeshClipper;
import java.util.List;

/** Contact-sheet poses from the same timeline and clipped geometry used by the renderer. */
public final class ExportPortalScene {
    private static int frame;
    public static void main(String[] args) {
        float h=CelestialNailMesh.HEIGHT;
        float[] ages={15,100,220,300};
        for(frame=0;frame<ages.length;frame++) {
            float age=ages[frame], launch=frame==3?15:-1;
            float open=CelestialNailVisuals.opening(age,launch);
            float emergence=CelestialNailVisuals.emergence(age), offset=CelestialNailVisuals.portalHeight(h)*(1-emergence);
            if(age>=30) {
                emit(CelestialNailMesh.BODY,CelestialNailVisuals.portalHeight(h)-offset,1,offset,age*.12F,1);
                emit(CelestialNailMesh.SHARDS,CelestialNailVisuals.portalHeight(h)-offset,1,offset,age*.92F,1);
            }
            float radius=h*.33F*open;
            emit(CelestialNailMesh.PORTAL_CORE,Float.POSITIVE_INFINITY,radius,CelestialNailVisuals.portalHeight(h),age*.15F,1);
            emit(CelestialNailMesh.PORTAL_RIM,Float.POSITIVE_INFINITY,radius,CelestialNailVisuals.portalHeight(h),age*.15F,open);
            emit(CelestialNailMesh.PORTAL_HALO,Float.POSITIVE_INFINITY,radius,CelestialNailVisuals.portalHeight(h),age*.15F,.25F*open);
            emit(CelestialNailMesh.PORTAL_SPARKS,Float.POSITIVE_INFINITY,radius,CelestialNailVisuals.portalHeight(h),-age*.4F,open);
            emit(CelestialNailMesh.PORTAL_BEAM,Float.POSITIVE_INFINITY,radius,CelestialNailVisuals.portalHeight(h),age*.15F,.6F*open);
        }
    }
    private static void emit(List<CelestialNailMesh.Face> mesh,float clip,float scale,float y,float yaw,float alpha) {
        double angle=Math.toRadians(yaw),s=Math.sin(angle),c=Math.cos(angle);
        int[] index={0}; StringBuilder line=new StringBuilder();
        NailMeshClipper.emit(mesh,clip,(f,p,u,v)->{
            if(index[0]%4==0) line.append(frame).append(' ').append(f.material()).append(' ').append(f.shade());
            line.append(' ').append((p.x()*c+p.z()*s)*scale).append(' ').append(p.y()*scale+y)
                    .append(' ').append((-p.x()*s+p.z()*c)*scale);
            if(++index[0]%4==0) {
                System.out.println(line.append(' ').append(alpha)); line.setLength(0);
            }
        });
    }
}
