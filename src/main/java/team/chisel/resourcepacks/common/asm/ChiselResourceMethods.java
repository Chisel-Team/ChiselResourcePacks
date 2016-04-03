package team.chisel.resourcepacks.common.asm;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.IExtendedBlockState;
import team.chisel.api.render.RenderContextList;
import team.chisel.client.render.ModelChisel;
import team.chisel.common.block.BlockCarvable;

public class ChiselResourceMethods {

    public static IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (ModelChisel.VALID_BLOCKS.containsKey(state.getBlock())) {
            IExtendedBlockState ext = (IExtendedBlockState) state;

            RenderContextList ctxList = new RenderContextList(ModelChisel.VALID_BLOCKS.get(state.getBlock()), world, pos);

            return ext.withProperty(BlockCarvable.CTX_LIST, ctxList);
        }
        return state.getBlock().getExtendedState(state, world, pos);
    }

    public static IBlockState cleanState(IBlockState state) {
        if (ModelChisel.VALID_BLOCKS.containsKey(state.getBlock())) {
            return ((IExtendedBlockState) state).withProperty(BlockCarvable.CTX_LIST, null);
        }
        return state;
    }
}
