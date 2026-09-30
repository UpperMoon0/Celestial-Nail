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
            model.floatingShard(p(Math.cos(a)*r, .28 + (i%5)*.30, Math.sin(a)*r),.073+(i%3)*.018,a);
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
        // Ordinary geometry survives replacement of Minecraft's shaders by Iris/Oculus.
        for(int i=0;i<192;i++) {
            double a=i*Math.PI*2/192,b=(i+1)*Math.PI*2/192;
            for(int band=0;band<4;band++) {
                double inner=.86+band*.02,outer=inner+.02;
                model.face(radial(a,inner,0),radial(b,inner,0),radial(b,outer,0),radial(a,outer,0),8);
            }
        }
        SHOCK_SURFACE=List.copyOf(model.faces);
        model.faces.clear(); model.ring(0,1,.035,.006,7); IMPACT_COLUMN=List.copyOf(model.faces);
    }
    private static Point p(double x, double y, double z) { return new Point((float)x,(float)y,(float)z); }
    private void face(Point a, Point b, Point c, Point d, int mat) {
        Point n = b.sub(a).cross(c.sub(a)).unit();
        float shade = (float)(.84 + .08*n.y + .10*n.x - .04*n.z);
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
    private void beam(Point a, Point b, double width, int mat) {beam(a,b,width,mat,true,true);}
    private void beam(Point a, Point b, double width, int mat,boolean capStart,boolean capEnd) {
        Point direction=b.sub(a).unit();
        Point u=direction.cross(Math.abs(direction.y)<.9?p(0,1,0):p(1,0,0)).unit().mul(width/2);
        Point v=direction.cross(u).unit().mul(width/2);
        Point[] lo={a.add(u).add(v),a.sub(u).add(v),a.sub(u).sub(v),a.add(u).sub(v)};
        Point[] hi={b.add(u).add(v),b.sub(u).add(v),b.sub(u).sub(v),b.add(u).sub(v)};
        for(int i=0;i<4;i++) { int j=(i+1)%4; face(lo[i],lo[j],hi[j],hi[i],mat); }
        if(capStart)face(lo[3],lo[2],lo[1],lo[0],mat);
        if(capEnd)face(hi[0],hi[1],hi[2],hi[3],mat);
    }
    private void path(double angle, double depth, double width, int mat, double... xy) {
        for(int i=0;i<xy.length-2;i+=2)
            beam(panel(angle,xy[i],xy[i+1],depth),panel(angle,xy[i+2],xy[i+3],depth),width,mat,i==0,i==xy.length-4);
    }
    private void jewel(double angle,double y,double width,double height,double depth) {
        if (height > .45) {
            // Broad shoulders and tapered feet give the large windows their lancet silhouette.
            double[][] outline={{0,.5},{-.26,.36},{-.44,.16},{-.47,-.03},{-.31,-.31},{0,-.5},{.31,-.31},{.47,-.03},{.44,.16},{.26,.36}};
            Point center=panel(angle,0,y,depth+.036);
            for(int i=0;i<outline.length;i++) {
                int j=(i+1)%outline.length;
                Point a=panel(angle,outline[i][0]*width,y+outline[i][1]*height,depth);
                Point b=panel(angle,outline[j][0]*width,y+outline[j][1]*height,depth);
                tri(a,b,center,i==6?CYAN:BLUE);
                beam(a,b,.037,GOLD);
                beam(a.add(panel(angle,0,0,.009)),b.add(panel(angle,0,0,.009)),.016,IVORY);
                if(i==2 || i==7) beam(a,center,.0035,CYAN);
            }
            // Animated glints are shaded on the glass; only structural facet veins use geometry.
            return;
        }
        diamond(angle,y,width,height,depth);
    }
    private void diamond(double angle,double y,double width,double height,double depth) {
        Point top=panel(angle,0,y+height/2,depth), right=panel(angle,width/2,y,depth);
        Point bottom=panel(angle,0,y-height/2,depth), left=panel(angle,-width/2,y,depth);
        Point center=panel(angle,0,y,depth+.06);
        tri(top,left,center,BLUE); tri(left,bottom,center,BLUE);
        tri(bottom,right,center,CYAN); tri(right,top,center,BLUE);
        beam(top,right,.024,GOLD); beam(right,bottom,.024,GOLD);
        beam(bottom,left,.024,GOLD); beam(left,top,.024,GOLD);
        beam(top,center,.006,CYAN); beam(bottom,center,.006,CYAN);
    }
    /** Tilted, illuminated cube fragments, matching the reference's floating crystal debris. */
    private void floatingShard(Point center,double size,double angle) {
        Point[] v=new Point[8];
        double ca=Math.cos(angle),sa=Math.sin(angle),cp=Math.cos(.48),sp=Math.sin(.48);
        for(int i=0;i<8;i++) {
            double x=((i&1)==0?-.5:.5)*size,y=((i&2)==0?-.5:.5)*size,z=((i&4)==0?-.5:.5)*size;
            double yy=y*cp-z*sp,zz=y*sp+z*cp;
            v[i]=center.add(p(x*ca-zz*sa,yy,x*sa+zz*ca));
        }
        int[][] panels={{0,1,3,2},{4,6,7,5},{0,4,5,1},{2,3,7,6},{0,2,6,4},{1,5,7,3}};
        for(int i=0;i<panels.length;i++){int[] q=panels[i];face(v[q[0]],v[q[1]],v[q[2]],v[q[3]],i==3?CYAN:BLUE);}
        for(int i=0;i<8;i++)for(int bit=1;bit<=4;bit*=2)if((i&bit)==0)beam(v[i],v[i|bit],.0035,6,false,false);
    }
    private void crystal(Point center,double radius,double height,int sides,double angle) {
        Point top=center.add(p(0,height*.42,0)), bottom=center.sub(p(0,height*.58,0));
        for(int i=0;i<sides;i++) {
            Point a=center.add(radial(angle+i*Math.PI*2/sides,radius,0));
            Point b=center.add(radial(angle+(i+1)*Math.PI*2/sides,radius,0));
            tri(a,b,top,i%5==0?CYAN:BLUE); tri(b,a,bottom,i%5==2?CYAN:BLUE);
            beam(a,b,.007,CYAN); beam(a,bottom,.006,CYAN);
        }
    }
    /** Broad beveled stone ribs with a narrow aged-metal border, rather than wire tracery. */
    private void molding(double angle,double depth,double width,double... xy) {
        path(angle,depth,width,GOLD,xy);
        path(angle,depth+.014,width*.55,IVORY,xy);
    }
    private static double shaftDepth(double y) {return (.38+.08*(y-1.45)/5.1)*Math.cos(Math.PI/8);}
    private void build() {
        // The intact limestone starts above the fractures; no pale core fills the blue cracks below.
        for(int band=0;band<8;band++) {
            double lo=2.87+3.68*band/8,hi=2.87+3.68*(band+1)/8;
            ring(lo,hi,.38+.08*(lo-1.45)/5.1,.38+.08*(hi-1.45)/5.1,IVORY);
        }
        crystalHeart();
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/4, broken=2.25+(i%4)*.14;
            double end=.53+(i%3)*.20;
            // Each long casing finger breaks at a different height and angle.
            int count=2+i%2;
            for(int section=0;section<count;section++) {
                double lo=end+(broken-end)*Math.pow(section/(double)count,.85);
                double hi=end+(broken-end)*Math.pow((section+1)/(double)count,.85)-.027;
                double depth=.355+section*.019;
                double tilt=((i+section)%3-1)*.055;
                Point[] outline={panel(a,-.145,lo+.07,depth),panel(a,-.154,hi-.015+tilt,depth),
                    panel(a,-.065,hi+.045+tilt,depth),panel(a,.018,hi-.025,depth),
                    panel(a,.150,hi+.04-tilt,depth),panel(a,.14,lo+.09,depth),panel(a,.04,lo-.035,depth+.007)};
                Point middle=panel(a,0,(lo+hi)/2,depth+.009);
                for(int j=0;j<outline.length;j++) {
                    Point x=outline[j],y=outline[(j+1)%outline.length];
                    tri(x,y,middle,STONE);
                    face(y,x,x.mul(.94).add(p(0,x.y()*.06,0)),y.mul(.94).add(p(0,y.y()*.06,0)),STONE);
                    if(j>0&&j<4)beam(x,y,.006,CYAN);
                }
            }
            // A chipped limestone transition sits above each dark finger, with a narrow blue break.
            double d=shaftDepth(2.65)+.008;
            Point[] edge={panel(a,-.155,broken+.07,d),panel(a,-.067,broken+.13,d),
                panel(a,.017,broken+.045,d),panel(a,.15,broken+.11,d)};
            for(int j=0;j<3;j++) {
                Point left=panel(a,-.155+j*.305/3,2.89,shaftDepth(2.89));
                Point right=panel(a,-.155+(j+1)*.305/3,2.89,shaftDepth(2.89));
                face(edge[j],edge[j+1],right,left,IVORY);
                beam(edge[j],edge[j+1],.006,CYAN);
            }
            // Recessed-looking blue seams and fine metal arrises continue almost to the tip.
            path(a,.408,.012,GOLD,-.146,broken+.09,-.146,4.95,-.10,5.22);
            path(a,.408,.012,GOLD,.146,broken+.09,.146,4.95,.10,5.22);
            path(a,.418,.007,CYAN,0,broken+.05,0,3.04);
            path(a,.423,.007,CYAN,0,3.48,0,4.92,.04,5.08);
            diamond(a,3.26,.135,.43,.432);
            // Ogee-shaped double arches surround the blue lancets and meet in a long central diamond.
            molding(a,.459,.047,-.18,6.46,-.145,6.37,-.085,6.23,0,6.02,.09,5.78,.14,5.55,.10,5.31,0,5.02);
            molding(a,.459,.047,.18,6.46,.145,6.37,.085,6.23,0,6.02,-.09,5.78,-.14,5.55,-.10,5.31,0,5.02);
            molding(a,.467,.031,-.17,6.44,0,6.27,.17,6.44);
            jewel(a,6.01,.275,.85,.477);
            diamond(a,5.36,.175,.56,.477);
            jewel(a,4.96,.095,.29,.451);
            // Sparse small weathering, not a noisy repeated wood-like grain.
            path(a,shaftDepth(4.3)+.004,.004,STONE,-.11,4.55,-.055,4.48,-.075,4.38,.015,4.26);
            path(a,shaftDepth(3.9)+.004,.003,STONE,.11,3.92,.07,3.86,.095,3.80);
        }
        // Broad stone collar with a fine metal reveal, matching the reference's pale horizontal lip.
        ring(6.44,6.52,.48,.58,GOLD);
        ring(6.52,6.59,.58,.71,IVORY);
        ring(6.59,6.68,.71,.71,IVORY);
        ring(6.68,6.70,.714,.69,GOLD);
        ring(6.70,6.76,.70,.65,IVORY);
        // The crown has the same broad drum silhouette as the concept, with a stone top and azure rim.
        ring(6.76,7.37,.575,.535,STONE);
        ring(7.37,7.42,.535,.52,GOLD);
        ring(7.42,7.445,.52,.50,BLUE);
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/4;
            jewel(a,7.11,.395,.61,.549);
            molding(a,.571,.043,-.218,7.44,-.212,7.27,-.18,7.06,-.12,6.91,0,6.79,.12,6.91,.18,7.06,.212,7.27,.218,7.44);
            path(a,.596,.018,GOLD,-.15,7.43,-.065,7.39,0,7.24,.065,7.39,.15,7.43);
            diamond(a,6.88,.082,.17,.583);
            // Short crown pins cap the ribs instead of a continuous pointed fence.
            beam(panel(a,0,7.40,.547),panel(a,0,7.49,.547),.056,STONE);
            beam(panel(a,0,7.465,.553),panel(a,0,7.48,.553),.061,GOLD);
        }
        stoneCrownCap();
        ring(7.44,7.56,.10,.10,STONE);
        ring(7.515,7.54,.12,.12,GOLD);
        ring(7.56,7.62,.12,.12,IVORY);
        // Asymmetric rear platform: lower than the crown, broader and swept farther backward.
        petal(-.22,.90,7.35,.80); petal(-.02,1.10,7.53,.57); petal(.20,1.03,7.46,1.05);
    }
    /** Uneven polygonal stone fragments; bisector-clipped cells avoid a radial pizza-slice cap. */
    private void stoneCrownCap() {
        double[][] sites={{-.29,-.19},{-.02,-.30},{.24,-.23},{-.31,.09},{-.08,-.02},{.21,.04},{-.12,.29},{.19,.29}};
        for(int cell=0;cell<sites.length;cell++) {
            List<double[]> polygon=new ArrayList<>();
            for(int i=0;i<8;i++){double a=(i-.5)*Math.PI/4;polygon.add(new double[]{Math.sin(a)*.496,Math.cos(a)*.496});}
            for(int j=0;j<sites.length;j++)if(j!=cell) {
                double nx=sites[j][0]-sites[cell][0],nz=sites[j][1]-sites[cell][1];
                double limit=(sites[j][0]*sites[j][0]+sites[j][1]*sites[j][1]-sites[cell][0]*sites[cell][0]-sites[cell][1]*sites[cell][1])/2;
                List<double[]> clipped=new ArrayList<>();
                for(int i=0;i<polygon.size();i++) {
                    double[] a=polygon.get(i),b=polygon.get((i+1)%polygon.size());
                    double da=a[0]*nx+a[1]*nz-limit,db=b[0]*nx+b[1]*nz-limit;
                    if(da<=0)clipped.add(a);
                    if((da<=0)!=(db<=0)){double t=da/(da-db);clipped.add(new double[]{a[0]+t*(b[0]-a[0]),a[1]+t*(b[1]-a[1])});}
                }
                polygon=clipped;
            }
            double y=7.449+(cell%3)*.005;
            Point middle=p(sites[cell][0],y,sites[cell][1]);
            for(int i=0;i<polygon.size();i++) {
                double[] a=polygon.get(i),b=polygon.get((i+1)%polygon.size());
                Point x=p(a[0]*.99+sites[cell][0]*.01,y,a[1]*.99+sites[cell][1]*.01);
                Point z=p(b[0]*.99+sites[cell][0]*.01,y,b[1]*.99+sites[cell][1]*.01);
                tri(x,z,middle,IVORY);
                Face f=faces.remove(faces.size()-1);
                faces.add(new Face(f.a,f.b,f.c,f.d,f.normal,f.material,.74F+(cell%4)*.04F));
                beam(x,z,.002,STONE,false,false);
            }
        }
    }
    private void crystalHeart() {
        double[] levels={.0,.27,.57,.96,1.35,1.78,2.24,2.82};
        double[] radii={0,.16,.29,.375,.36,.345,.333,.325};
        for(int layer=0;layer<levels.length-1;layer++) {
            for(int i=0;i<8;i++) {
                double a=(i-.5)*Math.PI/4,b=(i+.5)*Math.PI/4;
                Point lo=radial(a,radii[layer],levels[layer]);
                Point right=radial(b,radii[layer],levels[layer]);
                Point hi=radial(b,radii[layer+1],levels[layer+1]);
                Point left=radial(a,radii[layer+1],levels[layer+1]);
                if(layer==0) {
                    tri(lo,hi,left,i%5==0?CYAN:BLUE);
                    beam(lo,left,.005,CYAN);
                } else {
                    Point center=radial((a+b)/2,(radii[layer]+radii[layer+1])*.49,(levels[layer]+levels[layer+1])/2);
                    tri(lo,right,center,BLUE); tri(right,hi,center,i%5==0?CYAN:BLUE);
                    tri(hi,left,center,BLUE); tri(left,lo,center,i%5==2?CYAN:BLUE);
                    beam(lo,center,.004,CYAN); beam(hi,center,.004,CYAN);
                }
            }
        }
    }
    private void petal(double angle,double length,double peak,double width) {
        double[][] xy={{-.10,0},{-.29,.30},{-.35,.55},{-.33,.73},{-.24,.90},{-.10,1},
                {.04,.98},{.22,.86},{.33,.66},{.34,.44},{.25,.22},{.10,0}};
        Point[] rim=new Point[xy.length],inner=new Point[xy.length],under=new Point[xy.length];
        for(int i=0;i<xy.length;i++) {
            double t=xy[i][1], y=6.76+(peak-6.76)*t;
            rim[i]=panel(angle,xy[i][0]*width,y,.43+length*t);
            inner[i]=panel(angle,xy[i][0]*width*.79,6.81+(peak-6.86)*t,.43+length*t-.025);
            under[i]=rim[i].sub(p(0,.085,0));
        }
        Point middle=panel(angle,0,6.79+(peak-6.79)*.45,.43+length*.48);
        for(int i=0;i<rim.length;i++) {
            int j=(i+1)%rim.length;
            face(rim[i],rim[j],inner[j],inner[i],IVORY);
            tri(inner[i],inner[j],middle,IVORY);
            face(under[j],under[i],rim[i],rim[j],IVORY);
            tri(under[j],under[i],middle.sub(p(0,.12,0)),IVORY);
            beam(rim[i],rim[j],.012,GOLD);
            beam(inner[i],inner[j],.015,IVORY);
        }
        beam(panel(angle,0,6.71,.43),panel(angle,0,peak-.13,.40+length*.90),.017,STONE);
        beam(inner[2],middle,.003,STONE,false,false);
        beam(middle,inner[7],.003,STONE,false,false);
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
