package mekanism.common.block;

import java.util.Random;

import mekanism.api.Coord4D;
import mekanism.common.Mekanism;
import mekanism.common.MekanismBlocks;
import mekanism.common.block.property.PropertyColor;
import mekanism.common.block.states.BlockStateFacing;
import mekanism.common.block.states.BlockStateGlowPanel;
import mekanism.common.integration.multipart.MultipartMekanism;
import mekanism.common.tile.TileEntityGlowPanel;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MultipartUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockGlowPanel extends Block implements ITileEntityProvider
{
	private static Random rand = new Random();
	public static AxisAlignedBB[] bounds = new AxisAlignedBB[6];

	static
	{
		AxisAlignedBB cuboid = new AxisAlignedBB(0.25, 0, 0.25, 0.75, 0.125, 0.75);
		Vec3d fromOrigin = new Vec3d(-0.5, -0.5, -0.5);

		for(EnumFacing side : EnumFacing.VALUES)
		{
			bounds[side.ordinal()] = MultipartUtils.rotate(cuboid.offset(fromOrigin.x, fromOrigin.y, fromOrigin.z), side).offset(-fromOrigin.x, -fromOrigin.z, -fromOrigin.z);
		}
	}
	
	public BlockGlowPanel() 
	{
        super(Material.PISTON);
        setCreativeTab(Mekanism.tabMekanism);
        setHardness(1F);
        setResistance(10F);
    }
	
	@Override
	public int getMetaFromState(IBlockState state)
    {
		return 0;
    }
	
	@Override
	public BlockStateContainer createBlockState()
	{
		return new BlockStateGlowPanel(this);
	}
	
	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
	{
		TileEntityGlowPanel tileEntity = getTileEntityGlowPanel(world, pos);
		return tileEntity != null ? state.withProperty(BlockStateFacing.facingProperty, tileEntity.side) : state;
	}

	@SideOnly(Side.CLIENT)
    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) 
	{
		TileEntityGlowPanel tileEntity = getTileEntityGlowPanel(world, pos);
		
		if(tileEntity != null)
		{
			state = state.withProperty(BlockStateFacing.facingProperty, tileEntity.side);
			
			if(state instanceof IExtendedBlockState)
			{
				return ((IExtendedBlockState)state).withProperty(PropertyColor.INSTANCE, new PropertyColor(tileEntity.colour));
			}
		}
		
		return state;
	}

	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos neighbor)
	{
		TileEntityGlowPanel tileEntity = getTileEntityGlowPanel(world, pos);

		if(tileEntity != null && !world.isRemote && !canStay(world, pos))
		{
			dropBlockAsItem(world, pos, world.getBlockState(pos), 0);
			world.setBlockToAir(pos);
		}
	}
	
	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
	{
		TileEntityGlowPanel tileEntity = getTileEntityGlowPanel(world, pos);
		
		if(tileEntity != null)
		{
			return bounds[tileEntity.side.ordinal()];
		}
		
		return super.getBoundingBox(state, world, pos);
	}
	
	@Override
	public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side)
    {
		return world.isSideSolid(pos.offset(side.getOpposite()), side);
    }
	
	public static boolean canStay(IBlockAccess world, BlockPos pos)
	{
		boolean canStay = false;
		
		if(Mekanism.hooks.MCMPLoaded)
		{
			canStay = MultipartMekanism.hasCenterSlot(world, pos);
		}
		
		if(!canStay)
		{
			TileEntity tileEntity = world.getTileEntity(pos);
			if(tileEntity instanceof TileEntityGlowPanel)
			{
				TileEntityGlowPanel glowPanel = (TileEntityGlowPanel)tileEntity;
				Coord4D adj = new Coord4D(glowPanel.getPos().offset(glowPanel.side), glowPanel.getWorld());
				canStay = glowPanel.getWorld().isSideSolid(adj.getPos(), glowPanel.side.getOpposite());
			}
		}
		
		return canStay;
	}
	
	@Override
	public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos)
    {
		return 15;
    }

	private ItemStack getDropItem(IBlockAccess world, BlockPos pos) {
		TileEntityGlowPanel tileEntity = (TileEntityGlowPanel)world.getTileEntity(pos);
		return new ItemStack(MekanismBlocks.GlowPanel, 1, tileEntity.colour.getMetaValue());
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player)
	{
		return getDropItem(world, pos);
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune)
	{
		drops.add(getDropItem(world, pos));
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
		return willHarvest || super.removedByPlayer(state, world, pos, player, willHarvest);
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
	public TileEntity createNewTileEntity(World worldIn, int meta)
	{
		return new TileEntityGlowPanel();
	}
	
	@Override
	public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer)
	{
		return true;
	}
	
	@Override
    public EnumBlockRenderType getRenderType(IBlockState state) 
	{
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean isBlockNormalCube(IBlockState state) 
    {
        return false;
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
    public boolean isFullBlock(IBlockState state)
    {
        return false;
    }
    
    private static TileEntityGlowPanel getTileEntityGlowPanel(IBlockAccess world, BlockPos pos)
    {
    	TileEntity tileEntity = MekanismUtils.getTileEntitySafe(world, pos);
    	TileEntityGlowPanel glowPanel = null;
    	if(tileEntity instanceof TileEntityGlowPanel)
    	{
    		glowPanel = (TileEntityGlowPanel)tileEntity;
    	}
    	else if(Mekanism.hooks.MCMPLoaded)
    	{
    		TileEntity childEntity = MultipartMekanism.unwrapTileEntity(world);
    		if(childEntity instanceof TileEntityGlowPanel)
    		{
    			glowPanel = (TileEntityGlowPanel)childEntity;
    		}
    	}
    	
    	return glowPanel;
    }
}
