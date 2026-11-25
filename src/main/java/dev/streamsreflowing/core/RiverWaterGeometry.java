package dev.streamsreflowing.core;

import java.util.function.BiFunction;
import dev.streamsreflowing.core.river.Lake;

/** 施工与冻结远景共用湖出口降水位规则，避免预览河水比实际河道低一截。 */
final class RiverWaterGeometry {
    static final int LAKE_LOOKBACK=6;
    private RiverWaterGeometry() {}
    static int inset(double width,double x,double z,double dirX,double dirZ,double grade,double ditchInset,
                     BiFunction<Integer,Integer,Lake> lakeAt) {
        int inset=(int)Math.round(ditchInset*(.5+.5*Math.min(1,width/3)));
        double length=Math.sqrt(dirX*dirX+dirZ*dirZ);
        if(inset<=0||length<1.0E-6)return inset;
        if(lakeAt.apply((int)Math.floor(x),(int)Math.floor(z))!=null)return inset;
        double dx=-dirX/length,dz=-dirZ/length;
        for(int i=1;i<=LAKE_LOOKBACK;i++){
            var lake=lakeAt.apply((int)Math.floor(x+dx*i),(int)Math.floor(z+dz*i));
            if(lake==null)continue;
            return Math.max((int)Math.round(inset*(i/7.0)),Math.min(inset,(int)Math.floor(grade)-lake.level()));
        }
        return inset;
    }
}
