package dev.streamsreflowing.compat;

import java.util.ArrayList;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import net.minecraft.server.level.ServerLevel;
import net.shiroha233.roadweaver.api.outgoing.WorldgenPreviewApi;

/** 远景展示河床和水面几何，真实河岸材质及流体仍由实际区块覆盖。 */
public final class RiverPreviewProvider {
    private RiverPreviewProvider() {}
    public static void register(){WorldgenPreviewApi.register("rivers",RiverPreviewProvider::peek);}
    private static WorldgenPreviewApi.Chunk peek(Object world,int cx,int cz) {
        if(!(world instanceof ServerLevel level))return WorldgenPreviewApi.Chunk.unavailable();
        var engine=StreamsWorldgen.engineIfPresent(level);
        if(engine==null)return WorldgenPreviewApi.Chunk.unavailable();
        var preview=engine.peekPreview(cx,cz);if(preview==null)return WorldgenPreviewApi.Chunk.unavailable();
        var runs=new ArrayList<WorldgenPreviewApi.Run>();
        for(int z=0;z<16;z++)for(int x=0;x<16;x++) {
            var column=preview.column(cx*16+x,cz*16+z);if(!column.present())continue;
            runs.add(new WorldgenPreviewApi.Run(x,z,column.bed(),column.bed()+1,"minecraft:gravel"));
            if(column.bed()+1<column.water()+1)runs.add(new WorldgenPreviewApi.Run(x,z,column.bed()+1,column.water()+1,"minecraft:water"));
            // 只有预览层清空上方；真实地形的河岸、巨树及保护由实际区块权威覆盖。
            if(column.water()+1<level.getMaxBuildHeight())runs.add(new WorldgenPreviewApi.Run(x,z,column.water()+1,level.getMaxBuildHeight(),"minecraft:air"));
        }
        if(StreamsWorldgen.engineIfPresent(level)!=engine)return WorldgenPreviewApi.Chunk.unavailable();
        return new WorldgenPreviewApi.Chunk(runs.isEmpty()?WorldgenPreviewApi.State.EMPTY:WorldgenPreviewApi.State.READY,
                WorldgenPreviewApi.Fidelity.GEOMETRY,"river-session-"+System.identityHashCode(engine),runs);
    }
}
