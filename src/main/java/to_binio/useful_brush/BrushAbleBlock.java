package to_binio.useful_brush;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public interface BrushAbleBlock {

    boolean brush(PlayerEntity playerEntity, BlockPos blockPos);
}