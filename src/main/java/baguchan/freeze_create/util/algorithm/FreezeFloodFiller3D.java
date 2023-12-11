package baguchan.freeze_create.util.algorithm;


import baguchan.freeze_create.register.ModTags;
import com.mojang.datafixers.util.Pair;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
/*
 * Thanks Ad Astra mod!
 * https://github.com/terrarium-earth/Ad-Astra/blob/1.20.x/common/src/main/java/earth/terrarium/ad_astra/common/util/algorithm/FloodFiller3D.java
 */
public class FreezeFloodFiller3D {
    public static Set<BlockPos> run(Level level, BlockPos start, int limit) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        Set<Pair<BlockPos, Direction>> queue = new LinkedHashSet<>();
        queue.add(Pair.of(start, Direction.UP));

        while (!queue.isEmpty()) {
            if (positions.size() >= limit) {
                return Set.of();
            }

            var iterator = queue.iterator();
            var pair = iterator.next();
            BlockPos pos = pair.getFirst().relative(pair.getSecond());
            iterator.remove();

            BlockState state = level.getBlockState(pos);

            if (runAdditionalChecks(level, state, pos)) {
                continue;
            } else {
                if (level.getBlockState(pos).is(ModTags.HOT_BLOCK) || level.getFluidState(pos).is(ModTags.HOT_FLUID)) {
                    return Set.of();
                }
                VoxelShape collisionShape = state.getCollisionShape(level, pos);
                if (!state.isAir() && !state.is(ModTags.PASSES_FREEZE_FILL) && !(level.getBlockEntity(pos) instanceof Container) && !collisionShape.isEmpty() && isSideSolid(collisionShape, pair.getSecond(), state) && (isFaceSturdy(collisionShape, pair.getSecond(), state) || isFaceSturdy(collisionShape, pair.getSecond().getOpposite(), state))) {
                    continue;
                }
            }

            positions.add(pos);

            for (Direction dir : Direction.values()) {
                if (!positions.contains(pos.relative(dir))) {
                    queue.add(Pair.of(pos, dir));
                }
            }
        }

        return positions;
    }

    private static boolean runAdditionalChecks(Level level, BlockState state, BlockPos pos) {
        Block block = state.getBlock();
        if (block instanceof SlidingDoorBlock door) {
            BlockState blockState = level.getBlockState(pos);
            Optional<Boolean> open = blockState.getOptionalValue(SlidingDoorBlock.OPEN);
            Optional<Boolean> powered = blockState.getOptionalValue(SlidingDoorBlock.POWERED);
            return (open.isPresent() && !open.get()) && (powered.isPresent() && !powered.get());
        }
        return false;
    }

    private static boolean isSideSolid(VoxelShape collisionShape, Direction dir, BlockState state) {
        return checkBounds(collisionShape.bounds(), dir.getAxis());
    }

    private static boolean isFaceSturdy(VoxelShape collisionShape, Direction dir, BlockState state) {
        VoxelShape faceShape = collisionShape.getFaceShape(dir);
        if (faceShape.isEmpty()) return true;
        return checkBounds(faceShape.toAabbs().get(0), dir.getAxis());
    }

    private static boolean checkBounds(AABB bounds, Direction.Axis axis) {
        return switch (axis) {
            case X -> bounds.minY <= 0 && bounds.maxY >= 1 && bounds.minZ <= 0 && bounds.maxZ >= 1;
            case Y -> bounds.minX <= 0 && bounds.maxX >= 1 && bounds.minZ <= 0 && bounds.maxZ >= 1;
            case Z -> bounds.minX <= 0 && bounds.maxX >= 1 && bounds.minY <= 0 && bounds.maxY >= 1;
        };
    }
}
