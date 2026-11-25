package dev.streamsreflowing.core;

import java.util.List;
import dev.streamsreflowing.core.river.RiverNetwork;
import dev.streamsreflowing.core.river.RiverSample;
import dev.streamsreflowing.core.river.Lake;

/** 冻结已发布邻板引用，远景查询不会启动建板或等待future。 */
public record RiverPreview(List<RiverNetwork> rivers,List<RiverNetwork> lakes,double radius,int seaLevel,double depth,double inset) {
    public RiverPreview {rivers=List.copyOf(rivers);lakes=List.copyOf(lakes);}
    public record Column(boolean present,int bed,int water) {}
    public Column column(int x,int z) {
        RiverSample best=null;
        for(var network:rivers) {
            var sample=network.sample(x+.5,z+.5,radius);
            if(sample!=null&&sample.distance()<=sample.width()&&(best==null||sample.distance()<best.distance()))best=sample;
        }
        int localInset=best==null?0:(int)Math.round(inset*(.5+.5*Math.min(1,best.width()/3)));
        int water=best==null?Integer.MIN_VALUE:Math.max(seaLevel-1,(int)Math.floor(best.gradeY())-localInset);
        int bed=best==null?Integer.MIN_VALUE:water-Math.max(1,(int)Math.round(2*best.width()*depth));
        for(var network:lakes) {
            Lake lake=network.lakeAt(x,z);
            if(lake!=null&&(water==Integer.MIN_VALUE||lake.level()<water)) {
                water=lake.level();bed=water-Math.max(1,lake.digDepth());
            }
        }
        return new Column(water!=Integer.MIN_VALUE,bed,water);
    }
}
