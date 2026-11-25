package dev.streamsreflowing.core;
import java.util.*;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** 冻结水系正对照必须真命中，远景深度遵守河床与水面方向。 */
class RiverPreviewTest {
    @Test void frozenRiverHasBedBelowWaterAndOutsideIsEmpty() {
        var segments=List.of(new RiverSegment(-32,0,80,3,32,0,80,3));
        var network=new RiverNetwork(new PlateKey(0,0),segments,new NearestRiverIndex(segments,24,16,1.4),List.of());
        var preview=new RiverPreview(List.of(network),List.of(),32,63,.5,2);
        var center=preview.column(0,0);assertTrue(center.present());assertEquals(78,center.water());assertEquals(75,center.bed());
        assertFalse(preview.column(0,16).present());
        assertFalse(new RiverPreview(List.of(),List.of(),32,63,.5,2).column(0,0).present());
    }
    @Test void coveringWideChannelWinsOverCloserNonCoveringNarrowChannel(){
        var segments=List.of(new RiverSegment(-32,2,80,.1,32,2,80,.1),new RiverSegment(-32,5,90,6,32,5,90,6));
        var network=new RiverNetwork(new PlateKey(0,0),segments,new NearestRiverIndex(segments,24,16,1.4),List.of());
        assertEquals(.1,network.sample(.5,.5,32).width());
        assertEquals(6,network.sampleTiered(.5,.5,32).width());
        var column=new RiverPreview(List.of(network),List.of(),32,63,.5,2).column(0,0);
        assertTrue(column.present());assertEquals(88,column.water());assertEquals(82,column.bed());
    }
    @Test void frozenLakeOutletMatchesRealEngineWithoutSamplingOrSaving(){
        var config=dev.streamsreflowing.core.config.StreamsConfig.defaults();
        var segments=List.of(new RiverSegment(-32,0,80,3,32,0,80,3));
        var lake=new Lake(79,3,1,-3,0,1,1,new long[]{1L});
        var engine=new RiverEngine(41,config,(x,z)->fail("冻结河网不应重新采地形"));
        try{
            engine.setNetworkStore(new dev.streamsreflowing.core.cache.NetworkStore(){
                public RiverNetwork load(PlateKey key){return NetworkTracer.assemble(key,segments,List.of(lake),config);}
                public void save(PlateKey key,RiverNetwork network){fail("只读恢复不应写河网");}
            });
            for(var key:engine.platesNear(8,8,config.gridSpacing()+64))engine.networkFor(key);
            var actual=engine.plan(0,0);assertTrue(actual.inChannel());
            var frozen=engine.peekPreview(0,0);assertNotNull(frozen);
            var result=frozen.column(0,0);assertTrue(result.present());
            assertEquals(79,actual.waterSurfaceY());assertEquals(actual.waterSurfaceY(),result.water());assertEquals(actual.bedY(),result.bed());
        }finally{engine.close();}
    }
}
