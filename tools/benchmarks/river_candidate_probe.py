"""真实新旧最近河段查询对拍、候选计数、时间与调用线程分配；不换算游戏收益。"""
from pathlib import Path
import subprocess, json, hashlib

ROOT = Path(__file__).resolve().parents[2]
JAVA = Path(r'C:\Program Files\Java\jdk-21\bin')
CODE = r'''
package dev.streamsreflowing.core.river;
import java.util.*;
import java.lang.management.ManagementFactory;
public class CandidateProbe {
    static volatile long sink;
    static final com.sun.management.ThreadMXBean bean =
        (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
    static long run(Object index,double[][] q,int repeats) {
        long sum=0;
        for(int round=0;round<repeats;round++)for(var p:q){
            RiverSample s=index instanceof NearestRiverIndex a?a.nearestTiered(p[0],p[1],p[2])
                :((FrozenNearestRiverIndex)index).nearestTiered(p[0],p[1],p[2]);
            if(s!=null)sum+=Double.doubleToLongBits(s.gradeY())+s.segId();
        }
        sink=sum;return sum;
    }
    static long[] measure(Object index,double[][] queries) {
        long allocated=bean.getThreadAllocatedBytes(Thread.currentThread().threadId()), start=System.nanoTime();
        run(index,queries,8);
        return new long[]{System.nanoTime()-start,
            bean.getThreadAllocatedBytes(Thread.currentThread().threadId())-allocated};
    }
    static int candidates(Object index)throws Exception {
        var m=index.getClass().getDeclaredMethod("a",double.class,double.class,double.class);m.setAccessible(true);
        int[] ids=(int[])m.invoke(index,0.,0.,60.);
        int n=0;while(ids[n]>=0)n++;return n;
    }
    public static void main(String[] args)throws Exception {
        var random=new Random(77129);StringBuilder out=new StringBuilder("[");
        for(int type=0;type<4;type++){
            var lines=new ArrayList<RiverSegment>();int n=type==0?64:type==1?96:type==2?1:512;
            for(int i=0;i<n;i++){
                double x,z,x2,z2;
                if(type==1){x=-256;z=-256+i*5.;x2=256;z2=256-i*5.;}
                else if(type==2){x=-4;z=0;x2=4;z2=0;}
                else{x=(i%32)*16-256;z=(i/32)*16-128;x2=x+4;z2=z+3;}
                lines.add(new RiverSegment(x,z,76+i%7,2+i%9,x2,z2,74+i%7,2+i%9));
            }
            var old=new FrozenNearestRiverIndex(lines,24,16,1.4);
            var actual=new NearestRiverIndex(lines,24,16,1.4);
            int before=candidates(old),after=candidates(actual);
            double[][] queries=new double[512][3];
            for(var p:queries){p[0]=random.nextInt(513)-256+.25;p[1]=random.nextInt(321)-160+.75;p[2]=type==1?60:24;}
            int hits=0;
            for(var p:queries){var a=old.nearestTiered(p[0],p[1],p[2]);var b=actual.nearestTiered(p[0],p[1],p[2]);
                if(!Objects.equals(a,b))throw new AssertionError("完整采样字段不同");
                if(b!=null)hits++;
            }
            if(hits==0)throw new AssertionError("空夹具");
            for(int warm=0;warm<5;warm++){run(old,queries,8);run(actual,queries,8);}
            long[][] oldSamples=new long[7][],newSamples=new long[7][];
            for(int sample=0;sample<7;sample++){
                if(sample%2==0){oldSamples[sample]=measure(old,queries);newSamples[sample]=measure(actual,queries);}
                else{newSamples[sample]=measure(actual,queries);oldSamples[sample]=measure(old,queries);}
            }
            if(type>0)out.append(',');
            out.append("{\"fixture\":").append(type).append(",\"segments\":").append(n)
                .append(",\"nonempty_queries\":").append(hits).append(",\"queries_per_sample\":4096")
                .append(",\"center_cold_candidates_old\":").append(before).append(",\"center_cold_candidates_new\":").append(after)
                .append(",\"old_ns_allocated_bytes\":").append(Arrays.deepToString(oldSamples))
                .append(",\"new_ns_allocated_bytes\":").append(Arrays.deepToString(newSamples)).append('}');
        }
        System.out.println(out.append(']'));
    }
}
'''

def main():
    work=ROOT/'build/benchmarks/river-candidates'
    work.mkdir(parents=True,exist_ok=True)
    source=work/'CandidateProbe.java'
    source.write_text(CODE,encoding='utf-8')
    main=ROOT/'src/main/java/dev/streamsreflowing/core/river'
    files=[main/name for name in ['NearestRiverIndex.java','RiverCandidateCollector.java','RiverSegment.java','RiverSample.java']]
    frozen=ROOT/'src/test/java/dev/streamsreflowing/core/river/FrozenNearestRiverIndex.java'
    subprocess.run([str(JAVA/'javac.exe'),'-encoding','UTF-8','-d',str(work),*[str(p) for p in files],str(frozen),str(source)],check=True,capture_output=True)
    result=subprocess.run([str(JAVA/'java.exe'),'-Xmx512m','-cp',str(work),'dev.streamsreflowing.core.river.CandidateProbe'],check=True,capture_output=True)
    rows=json.loads(result.stdout)
    evidence=dict(scope='real index kernels; synthetic fixtures; not Minecraft or FPS',old_base='02dabc6',
                  original_source_sha256='b457856dabc1548368eefba8bc7e7884d44c3942d3154ba799503c5b7e5050c6',
                  source_sha256={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in files+[frozen]},
                  samples=rows)
    target=ROOT/'build/reviews/river-candidate-benchmark.json'
    target.parent.mkdir(parents=True,exist_ok=True)
    target.write_text(json.dumps(evidence,indent=2)+'\n',encoding='utf-8')
    for row in rows:
        for version in ['old','new']:
            samples=row[f'{version}_ns_allocated_bytes']
            row[version+'_p50_ms']=sorted(s[0] for s in samples)[3]/1e6
        print(row['fixture'],row['center_cold_candidates_old'],row['center_cold_candidates_new'],
              row['old_p50_ms'],row['new_p50_ms'])
if __name__=='__main__':main()
