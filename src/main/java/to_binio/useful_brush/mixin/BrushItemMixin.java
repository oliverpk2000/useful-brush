package to_binio.useful_brush.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BrushItem;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import to_binio.useful_brush.UsefulBrush;
import to_binio.useful_brush.event.BrushBlockEvent;
import to_binio.useful_brush.event.BrushEntityEvent;

@Mixin (BrushItem.class)
public abstract class BrushItemMixin extends ItemMixin {

    @Shadow
    protected abstract HitResult getHitResult(LivingEntity user);

    @Inject (at = @At (value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;)V"), method = "usageTick")
    private void usageTickBlock(World world, LivingEntity user, ItemStack stack, int remainingUseTicks, CallbackInfo ci,
            @Local PlayerEntity playerEntity, @Local HitResult hitResult, @Local BlockHitResult blockHitResult,
            @Local BlockPos blockPos) {
        BlockState blockState = world.getBlockState(blockPos);

        var blockEntry = UsefulBrush.BRUSHABLE_BLOCKS.get(blockState.getBlock());

        if (blockEntry != null) {

            world.setBlockState(blockPos, blockEntry.block().getStateWithProperties(blockState));

            if (!world.isClient()) {
                EquipmentSlot equipmentSlot = stack.equals(playerEntity.getEquippedStack(EquipmentSlot.OFFHAND)) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
                stack.damage(1, user, (userx) -> {
                    userx.sendEquipmentBreakStatus(equipmentSlot);
                });

                if (blockEntry.lootTable() != null) {
                    var lootTable = world.getServer().getLootManager().getLootTable(blockEntry.lootTable());

                    if (lootTable != LootTable.EMPTY) {
                        LootContextParameterSet.Builder builder = (new LootContextParameterSet.Builder((ServerWorld) world)).add(LootContextParameters.ORIGIN, blockPos.toCenterPos()).add(LootContextParameters.TOOL, ItemStack.EMPTY).add(LootContextParameters.BLOCK_STATE, blockState);

                        LootContextParameterSet lootContextParameterSet = builder.build(LootContextTypes.BLOCK);
                        lootTable.generateLoot(lootContextParameterSet, 0L, itemStack -> {

                            UsefulBrush.LOGGER.info(itemStack.getItem().getName().toString());

                            Vec3d pos = hitResult.getPos();
                            Vec3d center = blockPos.toCenterPos();

                            Vec3d offset = pos.subtract(center);

                            Vec3d spawnPos = pos.add(offset.normalize().multiply(0.2));

                            ItemEntity itemEntity = new ItemEntity(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), itemStack);
                            itemEntity.setToDefaultPickupDelay();
                            world.spawnEntity(itemEntity);
                        });
                    } else {
                        UsefulBrush.LOGGER.error("Could not find loot_table '%s'".formatted(blockEntry.lootTable()));
                    }
                }
            }
        } else {
            ActionResult brushResult = BrushBlockEvent.getEvent(blockState.getBlock().getClass()).invoker().brush(playerEntity, blockPos);

            if (brushResult == ActionResult.SUCCESS && !world.isClient()) {
                EquipmentSlot equipmentSlot = stack.equals(playerEntity.getEquippedStack(EquipmentSlot.OFFHAND)) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
                stack.damage(1, user, ((userx) -> {
                    userx.sendEquipmentBreakStatus(equipmentSlot);
                }));
            }
        }
    }


    @Inject (at = @At (shift = At.Shift.AFTER, value = "INVOKE_ASSIGN", target = "Lnet/minecraft/item/BrushItem;getHitResult(Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/util/hit/HitResult;"), method = "usageTick", cancellable = true)
    private void usageTickEntity(World world, LivingEntity user, ItemStack stack, int remainingUseTicks,
            CallbackInfo ci,
            @Local (ordinal = 0) PlayerEntity playerEntity, @Local (ordinal = 0) HitResult hitResult) {

        BrushItem item = (BrushItem) (Object) this;

        if (hitResult instanceof EntityHitResult entityHitResult) {

            if (hitResult.getType() == HitResult.Type.ENTITY) {

                int i = item.getMaxUseTime(stack) - remainingUseTicks + 1;
                boolean bl = i % 10 == 5;
                if (bl) {

                    Entity entity = entityHitResult.getEntity();

                    ActionResult brushResult = BrushEntityEvent.getEvent(entity.getClass()).invoker().brush(entity, playerEntity, hitResult.getPos());

                    if (brushResult == ActionResult.SUCCESS && !world.isClient()) {
                        EquipmentSlot equipmentSlot = stack.equals(playerEntity.getEquippedStack(EquipmentSlot.OFFHAND)) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
                        stack.damage(1, user, ((userx) -> {
                            userx.sendEquipmentBreakStatus(equipmentSlot);
                        }));
                    }

                    if (brushResult == ActionResult.PASS) {
                        world.playSound(playerEntity, entity.getBlockPos(), SoundEvents.ITEM_BRUSH_BRUSHING_GENERIC, SoundCategory.BLOCKS);
                    }
                }

                ci.cancel();
            }
        }
    }

    @Override
    protected void useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand,
            CallbackInfoReturnable<ActionResult> cir) {

        if (user != null && this.getHitResult(user).getType() == HitResult.Type.ENTITY) {
            user.setCurrentHand(hand);
        }

        cir.setReturnValue(ActionResult.CONSUME);
    }

    //        BlockState blockStateToConvert = UsefulBrush.CLEAN_ABLE_BLOCK_STATES.get(block);
//
//        if (blockStateToConvert != null) {
//
//            world.setBlockState(blockPos, blockStateToConvert);
//
//            EquipmentSlot equipmentSlot = stack.equals(playerEntity.getEquippedStack(EquipmentSlot.OFFHAND)) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
//            stack.damage(1, user, (userx) -> {
//                userx.sendEquipmentBreakStatus(equipmentSlot);
//            });
//        }
//}
//
//    @Redirect (method = "getHitResult", at = @At (value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;getCollision(Lnet/minecraft/entity/Entity;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/HitResult;"))
//    public HitResult getHitResult(Entity entity, Predicate<Entity> predicate, double range) {
//        return hit(entity, range);
//    }
//
//    @Unique
//    private static HitResult hit(Entity user, double range) {
//
//        Vec3d velocity = user.getRotationVec(0.0F).multiply(range);
//        World world = user.getWorld();
//        Vec3d pos = user.getEyePos();
//
//        Vec3d vec3d = pos.add(velocity);
//
//        return world.raycast(new RaycastContext(pos, vec3d, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, user));
//    }
}
