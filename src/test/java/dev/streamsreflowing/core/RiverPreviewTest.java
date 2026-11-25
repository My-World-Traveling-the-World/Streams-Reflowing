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
}
