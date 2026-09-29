package com.nstut.celestialnail.client;

import java.util.ArrayList;
import java.util.List;

/** Loader-independent, baked geometry in blocks. The crystal point is the entity origin. */
public final class CelestialNailMesh {
    public record Point(float x, float y, float z) {
        Point add(Point b) { return p(x + b.x, y + b.y, z + b.z); }
        Point sub(Point b) { return p(x - b.x, y - b.y, z - b.z); }
        Point mul(double s) { return p(x * s, y * s, z * s); }
        Point cross(Point b) { return p(y*b.z-z*b.y, z*b.x-x*b.z, x*b.y-y*b.x); }
        Point unit() { return mul(1.0 / Math.sqrt(x*x+y*y+z*z)); }
    }
    public record Face(Point a, Point b, Point c, Point d, Point normal, int material, float shade) {
        public boolean emissive() { return material >= 3; }
    }
    private static final int IVORY = 0, GOLD = 1, STONE = 2, BLUE = 3, CYAN = 4;
    public static final List<Face> BODY;
    public static final List<Face> SHARDS;
    public static final List<Face> PORTAL_CORE, PORTAL_RIM, PORTAL_HALO, PORTAL_SPARKS, PORTAL_BEAM;
    public static final List<Face> DEBRIS, SHOCK_SURFACE, IMPACT_COLUMN;
    public static final float HEIGHT;
    private final List<Face> faces = new ArrayList<>();
    static {
        CelestialNailMesh model = new CelestialNailMesh();
        model.build();
        BODY = List.copyOf(model.faces);
        HEIGHT = (float) BODY.stream().flatMap(f -> java.util.stream.Stream.of(f.a(),f.b(),f.c(),f.d()))
                .mapToDouble(Point::y).max().orElseThrow();
        model.faces.clear();
        for (int i = 0; i < 15; i++) {
            double a = i * 2.399963;
            double r = .61 + (i % 3) * .12;
            model.crystal(p(Math.cos(a)*r, .40 + (i%5)*.27, Math.sin(a)*r),
                    .035 + (i%3)*.014, .12 + (i%4)*.025, 4, a);
        }
        SHARDS = List.copyOf(model.faces);
        model.faces.clear(); model.portalDisc(); PORTAL_CORE=List.copyOf(model.faces);
        model.faces.clear();
        model.portalBand(.965,1.005,.008,7); model.portalBand(1.006,1.035,.009,6);
        PORTAL_RIM=List.copyOf(model.faces);
        model.faces.clear();
        model.portalBand(1.035,1.08,.006,6); model.portalBand(1.08,1.17,.004,3);
        PORTAL_HALO=List.copyOf(model.faces);
        model.faces.clear();
        for(int i=0;i<48;i++) {
            double angle=i*2.399963, r=1.04+(i%7)*.065;
            model.crystal(radial(angle,r,(i%5)*.045),.009+(i%3)*.004,.03+(i%4)*.009,4,angle);
        }
        PORTAL_SPARKS=List.copyOf(model.faces);
        model.faces.clear();
        // Crossed tapered light planes: visible from every azimuth, with no camera billboard.
        for(int i=0;i<4;i++) {
            double a=i*Math.PI/4;
            model.tri(panel(a,-.012,0,0),panel(a,.012,0,0),p(0,3.8,0),7);
            model.tri(panel(a,.009,0,0),panel(a,-.009,0,0),p(0,-1.55,0),6);
            model.tri(panel(a,-.095,0,0),panel(a,.095,0,0),p(0,.65,0),6);
            model.tri(panel(a,.07,0,0),panel(a,-.07,0,0),p(0,-.55,0),6);
        }
        PORTAL_BEAM=List.copyOf(model.faces);
        model.faces.clear();
        for(int i=0;i<24;i++) {
            double a=i*2.399963,r=.23+(i%4)*.035,y=.05+(i%8)*.04;
            model.beam(radial(a,r,y),radial(a+.025,r,y+.017),.012+(i%3)*.004,2);
        }
        DEBRIS=List.copyOf(model.faces);
        model.faces.clear();
        // A carrier quad only: its transparent radial wave is entirely procedural in the fragment shader.
        model.face(p(-1,0,-1),p(-1,0,1),p(1,0,1),p(1,0,-1),8);
        SHOCK_SURFACE=List.copyOf(model.faces);
        model.faces.clear(); model.ring(0,1,.035,.006,7); IMPACT_COLUMN=List.copyOf(model.faces);
    }
    private static Point p(double x, double y, double z) { return new Point((float)x,(float)y,(float)z); }
    private void face(Point a, Point b, Point c, Point d, int mat) {
        Point n = b.sub(a).cross(c.sub(a)).unit();
        float shade = (float)(.80 + .14*n.y + .06*n.x);
        if (mat >= BLUE) shade = (float)(.83 + .17*Math.abs(n.x));
        if (mat >= 5) shade = 1;
        faces.add(new Face(a,b,c,d,n,mat,shade));
    }
    private void tri(Point a, Point b, Point c, int mat) { face(a,b,c,c,mat); }
    private static Point radial(double angle, double r, double y) {
        return p(Math.sin(angle)*r,y,Math.cos(angle)*r);
    }
    /** Local coordinates on one of the eight shaft faces. */
    private static Point panel(double angle, double x, double y, double depth) {
        return p(Math.cos(angle)*x+Math.sin(angle)*depth,y,-Math.sin(angle)*x+Math.cos(angle)*depth);
    }
    private void ring(double bottom, double top, double r0, double r1, int mat) {
        for (int i=0;i<8;i++) {
            double a=(i-.5)*Math.PI/4, b=(i+.5)*Math.PI/4;
            Point x=radial(a,r0,bottom), y=radial(b,r0,bottom);
            Point z=radial(b,r1,top), w=radial(a,r1,top);
            face(x,y,z,w,mat);
            tri(p(0,top,0),w,z,mat);
            tri(p(0,bottom,0),y,x,mat);
        }
    }
    /** Square-section raised molding; no smooth tubes or round surfaces. */
    private void beam(Point a, Point b, double width, int mat) {
        Point direction=b.sub(a).unit();
        Point u=direction.cross(Math.abs(direction.y)<.9?p(0,1,0):p(1,0,0)).unit().mul(width/2);
        Point v=direction.cross(u).unit().mul(width/2);
        Point[] lo={a.add(u).add(v),a.sub(u).add(v),a.sub(u).sub(v),a.add(u).sub(v)};
        Point[] hi={b.add(u).add(v),b.sub(u).add(v),b.sub(u).sub(v),b.add(u).sub(v)};
        for(int i=0;i<4;i++) { int j=(i+1)%4; face(lo[i],lo[j],hi[j],hi[i],mat); }
        face(lo[3],lo[2],lo[1],lo[0],mat);
        face(hi[0],hi[1],hi[2],hi[3],mat);
    }
    private void path(double angle, double depth, double width, int mat, double... xy) {
        for(int i=0;i<xy.length-2;i+=2)
            beam(panel(angle,xy[i],xy[i+1],depth),panel(angle,xy[i+2],xy[i+3],depth),width,mat);
    }
    private void jewel(double angle,double y,double width,double height,double depth) {
        if (height > .45) {
            // Broad shoulders and tapered feet give the large windows their lancet silhouette.
            double[][] outline={{0,.5},{-.5,.20},{-.40,-.20},{0,-.5},{.40,-.20},{.5,.20}};
            Point center=panel(angle,0,y,depth+.065);
            for(int i=0;i<outline.length;i++) {
                int j=(i+1)%outline.length;
                Point a=panel(angle,outline[i][0]*width,y+outline[i][1]*height,depth);
                Point b=panel(angle,outline[j][0]*width,y+outline[j][1]*height,depth);
                tri(a,b,center,i==3?CYAN:BLUE);
                beam(a,b,.025,GOLD);
                if(i%2==0) beam(a,center,.005,CYAN);
            }
            return;
        }
        Point top=panel(angle,0,y+height/2,depth), right=panel(angle,width/2,y,depth);
        Point bottom=panel(angle,0,y-height/2,depth), left=panel(angle,-width/2,y,depth);
        Point center=panel(angle,0,y,depth+.06);
        tri(top,left,center,BLUE); tri(left,bottom,center,BLUE);
        tri(bottom,right,center,CYAN); tri(right,top,center,BLUE);
        beam(top,right,.024,GOLD); beam(right,bottom,.024,GOLD);
        beam(bottom,left,.024,GOLD); beam(left,top,.024,GOLD);
        beam(top,center,.006,CYAN); beam(bottom,center,.006,CYAN);
    }
    private void crystal(Point center,double radius,double height,int sides,double angle) {
        Point top=center.add(p(0,height*.42,0)), bottom=center.sub(p(0,height*.58,0));
        for(int i=0;i<sides;i++) {
            Point a=center.add(radial(angle+i*Math.PI*2/sides,radius,0));
            Point b=center.add(radial(angle+(i+1)*Math.PI*2/sides,radius,0));
            tri(a,b,top,i%3==0?CYAN:BLUE); tri(b,a,bottom,i%3==1?CYAN:BLUE);
            beam(a,b,.007,CYAN); beam(a,bottom,.006,CYAN);
        }
    }
    private void build() {
        // Long octagonal fluted shaft, with an exposed blue heart below its broken casing.
        for(int band=0;band<12;band++) {
            double t=band/12.0, next=(band+1)/12.0;
            ring(1.45+5.1*t,1.45+5.1*next,.38+.08*t,.38+.08*next,IVORY);
        }
        crystalHeart();
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/4;
            double end=.72+(i%3)*.19, broken=1.86+(i%4)*.17;
            // Separate chipped stone fingers leave cyan cracks between the lower plates.
            for(int section=0;section<3;section++) {
                double lo=end+(broken-end)*section/3, hi=end+(broken-end)*(section+1)/3-.045;
                double depth=.365+section*.016;
                Point[] outline={panel(a,-.135,lo+.10,depth),panel(a,-.14,hi-.02,depth),
                        panel(a,-.055,hi+.06,depth),panel(a,.025,hi-.01,depth),
                        panel(a,.14,hi+.08,depth),panel(a,.125,lo+.05,depth),panel(a,.015,lo-.025,depth-.014)};
                Point mid=panel(a,0,(lo+hi)/2,depth+.018);
                for(int j=0;j<outline.length;j++) {
                    Point x=outline[j],y=outline[(j+1)%outline.length];
                    tri(x,y,mid,STONE);
                    face(y,x,x.mul(.91).add(p(0,x.y()*.09,0)),y.mul(.91).add(p(0,y.y()*.09,0)),STONE);
                    if(j>0&&j<4) beam(x,y,.011,CYAN);
                }
            }
            // Fine gold arrises, channels and small diamond clasps along the unbroken shaft.
            path(a,.414,.014,GOLD,-.155,2.25,-.155,5.35,-.12,5.62);
            path(a,.414,.014,GOLD,.155,2.2,.155,5.35,.12,5.62);
            path(a,.423,.011,CYAN,0,broken+.05,0,3.05);
            path(a,.434,.011,CYAN,0,3.48,0,5.02,.04,5.22);
            jewel(a,3.25,.13,.40,.434);
            jewel(a,5.08,.10,.25,.447);
            // Interlocking lancet arches, following the concept's diamond-and-leaf rhythm.
            path(a,.453,.033,GOLD,-.178,6.47,-.11,6.29,0,6.04,.135,5.73,0,5.38,-.135,5.73,0,6.04,.11,6.29,.178,6.47);
            path(a,.466,.025,IVORY,-.16,6.37,-.08,6.18,0,5.97,.16,5.65);
            path(a,.466,.025,IVORY,.16,6.37,.08,6.18,0,5.97,-.16,5.65);
            jewel(a,6.01,i%2==0?.27:.19,i%2==0?.76:.61,.473);
            jewel(a,5.45,.13,.35,.465);
            // Hairline, angular weathering, kept sparse and pixel-scaled.
            path(a,.427,.006,STONE,-.11,4.55,-.055,4.48,-.08,4.37,.015,4.26);
            path(a,.415,.006,STONE,.12,2.78,.07,2.68,.095,2.58);
        }
        // Stepped collar, broad pale overhang and a gold reveal underneath.
        ring(6.43,6.53,.48,.57,GOLD);
        ring(6.53,6.60,.57,.69,IVORY);
        ring(6.60,6.68,.69,.69,IVORY);
        ring(6.68,6.72,.70,.66,GOLD);
        ring(6.72,6.79,.68,.63,IVORY);
        // Low crown drum with eight blue glass lancets and raised forked prongs.
        ring(6.79,7.37,.49,.43,STONE);
        ring(7.37,7.43,.43,.39,GOLD);
        ring(7.43,7.48,.39,.33,BLUE);
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/4;
            jewel(a,7.10,.32,.54,.466);
            path(a,.489,.033,GOLD,-.185,7.44,-.16,7.10,-.10,6.91,0,6.81,.10,6.91,.16,7.10,.185,7.44);
            path(a,.502,.025,IVORY,-.19,7.40,-.19,7.08,-.12,6.89,0,6.80,.12,6.89,.19,7.08,.19,7.40);
            path(a,.496,.025,GOLD,-.12,7.42,0,7.32,.12,7.42);
            jewel(a,6.87,.075,.15,.50);
        }
        ring(7.44,7.56,.10,.10,STONE);
        ring(7.51,7.55,.117,.117,GOLD);
        ring(7.56,7.61,.12,.12,IVORY);
        // Three asymmetric swept stone petals, with inset panels and underside ribs.
        petal(-.40,.76,7.50); petal(.03,1.02,7.78); petal(.46,.92,7.61);
    }
    private void crystalHeart() {
        double[] levels={.0,.43,.87,1.28,1.79,2.30};
        double[] radii={0,.23,.37,.355,.34,.325};
        for(int layer=0;layer<levels.length-1;layer++) {
            for(int i=0;i<8;i++) {
                double a=(i-.5)*Math.PI/4,b=(i+.5)*Math.PI/4;
                Point lo=radial(a,radii[layer],levels[layer]);
                Point right=radial(b,radii[layer],levels[layer]);
                Point hi=radial(b,radii[layer+1],levels[layer+1]);
                Point left=radial(a,radii[layer+1],levels[layer+1]);
                if(layer==0) {
                    tri(lo,hi,left,i%3==0?CYAN:BLUE);
                    beam(lo,left,.005,CYAN);
                } else {
                    Point center=radial((a+b)/2,(radii[layer]+radii[layer+1])*.49,(levels[layer]+levels[layer+1])/2);
                    tri(lo,right,center,BLUE); tri(right,hi,center,i%3==0?CYAN:BLUE);
                    tri(hi,left,center,BLUE); tri(left,lo,center,i%3==1?CYAN:BLUE);
                    beam(lo,center,.004,CYAN); beam(hi,center,.004,CYAN);
                }
            }
        }
    }
    private void petal(double angle,double length,double peak) {
        double[][] xy={{-.10,0},{-.29,.30},{-.35,.55},{-.33,.73},{-.24,.90},{-.10,1},
                {.04,.98},{.22,.86},{.33,.66},{.34,.44},{.25,.22},{.10,0}};
        Point[] rim=new Point[xy.length],inner=new Point[xy.length],under=new Point[xy.length];
        for(int i=0;i<xy.length;i++) {
            double t=xy[i][1], y=6.76+(peak-6.76)*t;
            rim[i]=panel(angle,xy[i][0],y,.40+length*t);
            inner[i]=panel(angle,xy[i][0]*.79,6.81+(peak-6.86)*t,.40+length*t-.025);
            under[i]=rim[i].sub(p(0,.085,0));
        }
        Point middle=panel(angle,0,7.06,.40+length*.48);
        for(int i=0;i<rim.length;i++) {
            int j=(i+1)%rim.length;
            face(rim[i],rim[j],inner[j],inner[i],IVORY);
            tri(inner[i],inner[j],middle,IVORY);
            face(under[j],under[i],rim[i],rim[j],IVORY);
            tri(under[j],under[i],middle.sub(p(0,.12,0)),IVORY);
            beam(rim[i],rim[j],.023,GOLD);
            beam(inner[i],inner[j],.015,IVORY);
        }
        beam(panel(angle,0,6.71,.43),panel(angle,0,peak-.13,.40+length*.90),.025,GOLD);
        for(int i=1;i<4;i++) {
            beam(middle.sub(p(0,.1,0)),under[i],.018,IVORY);
            beam(middle.sub(p(0,.1,0)),under[rim.length-1-i],.018,IVORY);
        }
    }
    private static double starRadius(double angle) {
        // Astroid aperture: four sharp cusps with concave shoulders, like a slit in the sky.
        double sum=Math.pow(Math.abs(Math.cos(angle)),2.0/3)+Math.pow(Math.abs(Math.sin(angle)),2.0/3);
        return 1/Math.pow(sum,1.5);
    }
    private void portalDisc() {
        for(int i=0;i<96;i++) {
            double a=i*Math.PI/48,b=(i+1)*Math.PI/48;
            tri(p(0,0,0),radial(b,starRadius(b)*.966,0),radial(a,starRadius(a)*.966,0),5);
            // Deep blue inner refraction band along the black aperture.
        }
        portalBand(.88,.966,.002,3);
    }
    private void portalBand(double inner,double outer,double y,int mat) {
        for(int i=0;i<96;i++) {
            double a=i*Math.PI/48,b=(i+1)*Math.PI/48;
            face(radial(a,starRadius(a)*inner,y),radial(b,starRadius(b)*inner,y),
                    radial(b,starRadius(b)*outer,y),radial(a,starRadius(a)*outer,y),mat);
        }
    }
    private CelestialNailMesh() {}
}
