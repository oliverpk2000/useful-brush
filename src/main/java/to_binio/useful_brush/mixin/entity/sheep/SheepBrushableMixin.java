package to_binio.useful_brush.mixin.entity.sheep;

import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import to_binio.useful_brush.BrushableEntity;
import to_binio.useful_brush.BrushCount;
import to_binio.useful_brush.UsefulBrush;

@Mixin (SheepEntity.class)
public class SheepBrushableMixin implements BrushableEntity {

    @Override
    public boolean brush(PlayerEntity playerEntity) {
        SheepEntity sheep = (SheepEntity) (Object) this;
        BrushCount brushCount = (BrushCount) sheep;

        if (brushCount.getBrushCount() >= UsefulBrush.SHEEP_MAX_BRUSH_COUNT) {
            return false;
        }

        if (brushCount.getBrushCount() >= UsefulBrush.SHEEP_MAX_BRUSH_COUNT || Random.create().nextBetween(0, 5) == 0) {
            return false;
        }

        sheep.dropItem(Items.STRING.asItem(), 1);
        sheep.getWorld().playSound(playerEntity, sheep.getBlockPos(), SoundEvents.ITEM_BRUSH_BRUSHING_GENERIC, SoundCategory.BLOCKS);

        brushCount.setBrushCount(brushCount.getBrushCount() + 1);

        return true;
    }
}