package dev.streamsreflowing.core.river;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 外河查询跨桶去重保持每次查询独立；首命中布尔查询经基准否决优化，保留原路径。 */
class RiverStreamingQueryTest {
    @Test void unchangedChannelQueryStillStopsAtFirstHit() {
        var source=new CountingList(lines());
        var index=new NearestRiverIndex(source,24,16,1.4);
        source.reset();
        assertTrue(index.inAnyChannel(0,0,60));
        assertArrayEquals(new int[]{1,0,0},source.reads);
        assertFalse(index.inAnyChannel(0,60,60));
    }
    @Test void firstHitAndSingleBucketNeverInitializeStreamingCollector() throws Exception {
        var field=NearestRiverIndex.class.getDeclaredField("D");field.setAccessible(true);
        var local=(ThreadLocal<?>)field.get(null);local.remove();
        var index=new NearestRiverIndex(lines(),24,16,1.4);
        assertTrue(index.inAnyChannel(0,0,60));
        var collector=(RiverCandidateCollector)local.get();
        var epoch=RiverCandidateCollector.class.getDeclaredField("epoch");epoch.setAccessible(true);
        assertEquals(0,epoch.getInt(collector),"首命中不能为了去重先启动收集器");
        assertNotNull(index.nearestCoveringForeign(0,0,0,-1));
        assertEquals(0,epoch.getInt(collector),"单桶没有重复，不应初始化去重器");
    }
    @Test void successiveIndexesWrapAndMixedQueriesMatchFrozenWithoutLosingEqualDistanceWinner() throws Exception {
        var field=NearestRiverIndex.class.getDeclaredField("D");field.setAccessible(true);
        var collector=(RiverCandidateCollector)((ThreadLocal<?>)field.get(null)).get();
        var epoch=RiverCandidateCollector.class.getDeclaredField("epoch");epoch.setAccessible(true);
        int hits=0;
        for(int pass=0;pass<8;pass++) {
            var source=lines();var index=new NearestRiverIndex(source,24,16,1.4);
            var frozen=new FrozenNearestRiverIndex(source,24,16,1.4);
            if(pass==3)epoch.setInt(collector,-1);
            for(double z:new double[]{0,5,30,60,-20}) {
                assertEquals(frozen.inAnyChannel(0,z,60),index.inAnyChannel(0,z,60));
                var sample=index.nearestCoveringForeign(0,z,60,-1);
                assertEquals(frozen.nearestCoveringForeign(0,z,60,-1),sample);
                if(sample!=null)hits++;
            }
        }
        assertTrue(hits>10);
        var tie=List.of(new RiverSegment(-200,0,70,8,200,0,68,8),new RiverSegment(-200,0,80,8,200,0,78,8));
        var index=new NearestRiverIndex(tie,24,16,1.4);
        assertEquals(0,Objects.requireNonNull(index.nearestCoveringForeign(0,3,60,-1)).segId());
        // 河链按三维端点连接，不能用XZ相同而Y不同的夹具冒充同一条链。
        var chain=new NearestRiverIndex(List.of(new RiverSegment(-200,0,70,8,0,0,69,8),
                new RiverSegment(0,0,69,8,200,0,68,8)),24,16,1.4);
        assertNotNull(chain.nearestCoveringForeign(0,3,60,-1));
        assertNull(chain.nearestCoveringForeign(0,3,60,0));
        assertFalse(index.inAnyChannel(10000,-10000,60));
        assertNull(index.nearestCoveringForeign(10000,-10000,60,-1));
    }
    @Test void firstVisitDoesNotAllocateCandidateArrayOrLoseLargeWordAndWrap() throws Exception {
        var collector=new RiverCandidateCollector();collector.begin(4096);
        assertTrue(collector.firstVisit(4095));assertFalse(collector.firstVisit(4095));
        assertTrue(collector.firstVisit(0));assertEquals(0,collector.size());assertEquals(0,collector.inputs());
        var epoch=RiverCandidateCollector.class.getDeclaredField("epoch");epoch.setAccessible(true);epoch.setInt(collector,-1);
        collector.begin(4096);assertTrue(collector.firstVisit(4095));assertFalse(collector.firstVisit(4095));
    }
    static List<RiverSegment> lines() {
        return List.of(new RiverSegment(-200,-200,70,4,200,200,68,4),
                new RiverSegment(-190,-200,72,4,210,200,70,4),new RiverSegment(-180,-200,74,4,220,200,72,4));
    }
    private static final class CountingList extends AbstractList<RiverSegment> {
        final List<RiverSegment> source;final int[] reads;
        CountingList(List<RiverSegment> source){this.source=source;reads=new int[source.size()];}
        void reset(){Arrays.fill(reads,0);}
        public RiverSegment get(int i){reads[i]++;return source.get(i);}
        public int size(){return source.size();}
    }
}
