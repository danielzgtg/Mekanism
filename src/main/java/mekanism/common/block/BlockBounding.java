package mekanism.common.block;

import mekanism.common.block.states.BlockStateBounding;
import mekanism.common.tile.TileEntityAdvancedBoundingBlock;
import mekanism.common.tile.TileEntityBoundingBlock;
import mekanism.common.util.MekanismUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class BlockBounding extends Block
{
	public BlockBounding()
	{
		super(Material.IRON);
		setHardness(3.5F);
		setResistance(8F);
	}

	@Override
	public BlockStateContainer createBlockState()
	{
		return new BlockStateBounding(this);
	}

	@Override
	public IBlockState getStateFromMeta(int meta)
	{
		return getDefaultState().withProperty(BlockStateBounding.advancedProperty, meta > 0);
	}

	@Override
	public int getMetaFromState(IBlockState state)
	{
		boolean isAdvanced = state.getValue(BlockStateBounding.advancedProperty);
		return isAdvanced ? 1 : 0;
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		try {
			TileEntityBoundingBlock tileEntity = (TileEntityBoundingBlock)world.getTileEntity(pos);
			IBlockState state1 = world.getBlockState(tileEntity.mainPos);
			return state1.getBlock().onBlockActivated(world, tileEntity.mainPos, state1, player, hand, side, hitX, hitY, hitZ);
		} catch(Exception e) {
			return false;
		}
	}
	
	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state)
	{
		super.breakBlock(world, pos, state);
		
		world.removeTileEntity(pos);
	}

	/**
	 * {@inheritDoc}
	 * Delegate to main {@link Block#getPickBlock(IBlockState, RayTraceResult, World, BlockPos, EntityPlayer)}.
	 */
	@Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player)
	{
		try {
			TileEntityBoundingBlock tileEntity = (TileEntityBoundingBlock)world.getTileEntity(pos);
			IBlockState state1 = world.getBlockState(tileEntity.mainPos);
			return state1.getBlock().getPickBlock(state1, target, world, tileEntity.mainPos, player);
		} catch(Exception e) {
			e.printStackTrace();
			return ItemStack.EMPTY;
		}
	}

	/**
	 * {@inheritDoc}
	 * Delegate to main {@link Block#getDrops(NonNullList, IBlockAccess, BlockPos, IBlockState, int)}.
	 */
	@Override
	public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos,IBlockState state, int fortune)
	{
		try {
			TileEntityBoundingBlock tileEntity = (TileEntityBoundingBlock)world.getTileEntity(pos);
			BlockPos mainPos = tileEntity.mainPos;
			IBlockState state1 = world.getBlockState(mainPos);
			state1.getBlock().getDrops(drops, world, mainPos, state1, fortune);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * {@inheritDoc}
	 * Keep tile entity in world until after
	 * {@link Block#getDrops(NonNullList, IBlockAccess, BlockPos, IBlockState, int)}.
	 * Used together with {@link Block#harvestBlock(World, EntityPlayer, BlockPos, IBlockState, TileEntity, ItemStack)}.
	 *
	 * @author Forge
	 * @see BlockFlowerPot#removedByPlayer(IBlockState, World, BlockPos, EntityPlayer, boolean)
	 */
	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player,
	                               boolean willHarvest)
	{
		if (willHarvest) {
			return true;
		}

		removeMainBlock(world, pos);
		return super.removedByPlayer(state, world, pos, player, willHarvest);
	}

	private void removeMainBlock(World world, BlockPos pos) {
		TileEntityBoundingBlock tileEntity = (TileEntityBoundingBlock)world.getTileEntity(pos);
		world.setBlockToAir(tileEntity.mainPos);
	}

	/**
	 * {@inheritDoc}
	 * Used together with {@link Block#removedByPlayer(IBlockState, World, BlockPos, EntityPlayer, boolean)}.
	 *
	 * @author Forge
	 * @see BlockFlowerPot#harvestBlock(World, EntityPlayer, BlockPos, IBlockState, TileEntity, ItemStack)
	 */
	@Override
	public void harvestBlock(World world, EntityPlayer player, BlockPos pos,
	                         IBlockState state, TileEntity te, ItemStack stack)
	{
		super.harvestBlock(world, player, pos, state, te, stack);
		removeMainBlock(world, pos);
		world.setBlockToAir(pos);
	}

	/**
	 * Returns that this "cannot" be silk touched.
	 * This is so that {@link Block#getSilkTouchDrop(IBlockState)} is not called, because only
	 * {@link Block#getDrops(NonNullList, IBlockAccess, BlockPos, IBlockState, int)} supports tile entities.
	 * Our blocks keep their inventory and other behave like they are being silk touched by default anyway.
	 *
	 * @return false
	 */
	@Override
	protected boolean canSilkHarvest()
	{
		return false;
	}

	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighborBlock, BlockPos neighborPos)
	{
		try {
			TileEntityBoundingBlock tileEntity = (TileEntityBoundingBlock)world.getTileEntity(pos);
			tileEntity.onNeighborChange(state.getBlock());
			IBlockState state1 = world.getBlockState(tileEntity.mainPos);
			state1.getBlock().neighborChanged(state1, world, tileEntity.mainPos, neighborBlock, neighborPos);
		} catch(Exception e) {}
	}
	
	@Override
	public float getPlayerRelativeBlockHardness(IBlockState state, EntityPlayer player, World world, BlockPos pos)
	{
		try {
			TileEntityBoundingBlock tileEntity = (TileEntityBoundingBlock)world.getTileEntity(pos);
			IBlockState state1 = world.getBlockState(tileEntity.mainPos);
			return state1.getBlock().getPlayerRelativeBlockHardness(state1, player, world, tileEntity.mainPos);
		} catch(Exception e) {
			return super.getPlayerRelativeBlockHardness(state, player, world, pos);
		}
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state)
	{
		return EnumBlockRenderType.INVISIBLE;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state)
	{
		return false;
	}
	
	@Override
	public boolean isFullCube(IBlockState state)
	{
		return false;
	}

	@Override
	public boolean hasTileEntity(IBlockState state)
	{
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state)
	{
		if(state.getValue(BlockStateBounding.advancedProperty))
		{
			return new TileEntityAdvancedBoundingBlock();
		}
		else {
			return new TileEntityBoundingBlock();
		}
	}
}
