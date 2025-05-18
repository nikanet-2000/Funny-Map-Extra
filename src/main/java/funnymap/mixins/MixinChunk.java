package funnymap.mixins;

import funnymap.features.dungeon.MimicDetector;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Chunk.class)
public abstract class MixinChunk {
    @Shadow
    public abstract IBlockState getBlockState(BlockPos pos);

    @Inject(method = "setBlockState", at = @At("HEAD"))
    public void setBlockState(BlockPos pos, IBlockState state, CallbackInfoReturnable<IBlockState> cir) {
        IBlockState oldBlock = getBlockState(pos);
        MimicDetector.INSTANCE.onBlockChange(pos, oldBlock, state);
    }
}
