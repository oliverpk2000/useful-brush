package to_binio.useful_brush.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class BrushBlockEvent {

    private static final Map<Class<? extends Block>, Event<BrushBlock>> EVENTS = new HashMap<>();

    public static Event<BrushBlock> getEvent(Class<? extends Block> entityClass) {
        Event<BrushBlock> brushBlockEvent = EVENTS.get(entityClass);

        if (brushBlockEvent == null) {
            Event<BrushBlock> event = EventFactory.createArrayBacked(BrushBlock.class, brushEntities -> (playerEntity, blockPos) -> {
                for (BrushBlock brushEntity : brushEntities) {

                    ActionResult result = brushEntity.brush(playerEntity, blockPos);

                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }

                return ActionResult.PASS;
            });

            EVENTS.put(entityClass, event);
            brushBlockEvent = event;
        }

        return brushBlockEvent;
    }


    @FunctionalInterface
    public interface BrushBlock {
        ActionResult brush(PlayerEntity playerEntity, BlockPos blockPos);
    }
}
