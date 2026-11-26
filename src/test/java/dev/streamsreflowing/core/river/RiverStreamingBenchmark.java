package dev.streamsreflowing.core.river;

import java.util.*;

/** 冻结实现逐项对拍后再交替计时；场景分别报告，不能用密集收益掩盖首命中回退。 */
final class RiverStreamingBenchmark {
    private static final int QUERIES=4096,WARMUP=12,BATCHES=21;
    @FunctionalInterface private interface Query {long sample(double x,double z,double radius,int excluded);}
    private record Batch(long nanos,long checksum) {}
    static void run() {
        var dense=NearestRiverIndexEquivalenceTest.fixture(0,new Random(8116103));
        var shortLines=NearestRiverIndexEquivalenceTest.fixture(2,new Random(8116103));
        for(String fixture:List.of("long-miss","dense-hit","first-hit","single-bucket","foreign-excluded","outside")) {
            var chain=List.of(new RiverSegment(-200,0,70,8,0,0,69,8),new RiverSegment(0,0,69,8,200,0,68,8));
            var source=fixture.equals("dense-hit")?dense:fixture.equals("single-bucket")?shortLines:fixture.equals("foreign-excluded")?chain:RiverStreamingQueryTest.lines();
            var current=new NearestRiverIndex(source,24,16,1.4);var old=new FrozenNearestRiverIndex(source,24,16,1.4);
            var x=new double[QUERIES];var z=new double[QUERIES];var radius=new double[QUERIES];var excluded=new int[QUERIES];
            var random=new Random(251126);
            for(int i=0;i<QUERIES;i++) {
                x[i]=switch(fixture){case "outside"->10000+i%17;case "dense-hit"->random.nextDouble(-128,160);default->i%7*.01;};
                z[i]=switch(fixture){case "outside"->-10000;case "long-miss"->60+i%7*.01;case "dense-hit"->random.nextDouble(-128,160);default->i%5*.01;};
                radius[i]=fixture.equals("single-bucket")?0:60;
                excluded[i]=fixture.equals("foreign-excluded")?0:-1;
                if(!Objects.equals(old.nearestCoveringForeign(x[i],z[i],radius[i],excluded[i]),current.nearestCoveringForeign(x[i],z[i],radius[i],excluded[i]))
                        ||old.inAnyChannel(x[i],z[i],radius[i])!=current.inAnyChannel(x[i],z[i],radius[i]))throw new AssertionError("计时前独立查询对拍不一致");
            }
            measure(fixture,"channel",(a,b,r,e)->old.inAnyChannel(a,b,r)?1:0,(a,b,r,e)->current.inAnyChannel(a,b,r)?1:0,x,z,radius,excluded);
            measure(fixture,"foreign",(a,b,r,e)->fingerprint(old.nearestCoveringForeign(a,b,r,e)),(a,b,r,e)->fingerprint(current.nearestCoveringForeign(a,b,r,e)),x,z,radius,excluded);
        }
    }
    private static long fingerprint(RiverSample value){return value==null?0:value.hashCode();}
    private static void measure(String fixture,String method,Query old,Query current,double[] x,double[] z,double[] radius,int[] excluded) {
        long[] a=new long[BATCHES],b=new long[BATCHES];
        for(int batch=-WARMUP;batch<BATCHES;batch++) {
            Batch before,after;
            if((batch&1)==0){before=time(old,x,z,radius,excluded);after=time(current,x,z,radius,excluded);}
            else {after=time(current,x,z,radius,excluded);before=time(old,x,z,radius,excluded);}
            if(before.checksum()!=after.checksum())throw new AssertionError("计时输出不同");
            if(batch>=0){a[batch]=before.nanos();b[batch]=after.nanos();}
        }
        Arrays.sort(a);Arrays.sort(b);
        System.out.printf(Locale.ROOT,"RIVER_STREAM_BENCH fixture=%s method=%s queries=%d batches=%d oldP50Ns=%d newP50Ns=%d oldP90Ns=%d newP90Ns=%d%n",
                fixture,method,QUERIES,BATCHES,a[BATCHES/2]/QUERIES,b[BATCHES/2]/QUERIES,a[18]/QUERIES,b[18]/QUERIES);
    }
    private static Batch time(Query query,double[] x,double[] z,double[] radius,int[] excluded) {
        long checksum=1,started=System.nanoTime();
        for(int i=0;i<QUERIES;i++)checksum=31*checksum+query.sample(x[i],z[i],radius[i],excluded[i]);
        return new Batch(System.nanoTime()-started,checksum);
    }
}
