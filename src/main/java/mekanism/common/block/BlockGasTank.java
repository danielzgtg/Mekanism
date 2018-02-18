package mekanism.common.block;

import mekanism.api.IMekWrench;
import mekanism.api.gas.IGasItem;
import mekanism.common.Mekanism;
import mekanism.common.MekanismBlocks;
import mekanism.common.base.ISustainedInventory;
import mekanism.common.base.ITierItem;
import mekanism.common.block.states.BlockStateFacing;
import mekanism.common.block.states.BlockStateGasTank;
import mekanism.common.integration.wrenches.Wrenches;
import mekanism.common.security.ISecurityItem;
import mekanism.common.tile.TileEntityGasTank;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.SecurityUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockGasTank extends BlockContainer
{
	private static final AxisAlignedBB TANK_BOUNDS = new AxisAlignedBB(0.1875F, 0.0F, 0.1875F, 0.8125F, 1.0F, 0.8125F);

	public BlockGasTank()
	{
		super(Material.IRON);
		setHardness(3.5F);
		setResistance(8F);
		setCreativeTab(Mekanism.tabMekanism);
	}

	@Override
	protected BlockStateContainer createBlockState()
	{
		return new BlockStateGasTank(this);
	}

	@Override
	public IBlockState getStateFromMeta(int meta)
	{
		return getDefaultState();
	}

	@Override
	public int getMetaFromState(IBlockState state)
	{
		return 0;
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos)
	{
		TileEntity tile = MekanismUtils.getTileEntitySafe(worldIn, pos);
		
		if(tile instanceof TileEntityGasTank)
		{
			TileEntityGasTank tank = (TileEntityGasTank)tile;
			
			if(tank.facing != null)
			{
				state = state.withProperty(BlockStateFacing.facingProperty, tank.facing);
			}

			if(tank.tier != null)
			{
				state = state.withProperty(BlockStateGasTank.typeProperty, tank.tier);
			}
		}
		
		return state;
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
	{
		TileEntityBasicBlock tileEntity = (TileEntityBasicBlock)world.getTileEntity(pos);

		int side = MathHelper.floor((double)(placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
		int change = 3;

		switch(side)
		{
			case 0: change = 2; break;
			case 1: change = 5; break;
			case 2: change = 3; break;
			case 3: change = 4; break;
		}

		tileEntity.setFacing((short)change);
		tileEntity.redstone = world.isBlockIndirectlyGettingPowered(pos) > 0;
	}

	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighborBlock, BlockPos neighborPos)
	{
		if(!world.isRemote)
		{
			TileEntity tileEntity = world.getTileEntity(pos);

			if(tileEntity instanceof TileEntityBasicBlock)
			{
				((TileEntityBasicBlock)tileEntity).onNeighborChange(neighborBlock);
			}
		}
	}
	
	@Override
	public float getPlayerRelativeBlockHardness(IBlockState state, EntityPlayer player, World world, BlockPos pos)
	{
		TileEntity tile = world.getTileEntity(pos);
		
		return SecurityUtils.canAccess(player, tile) ? super.getPlayerRelativeBlockHardness(state, player, world, pos) : 0.0F;
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer entityplayer, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		if(world.isRemote)
		{
			return true;
		}

		TileEntityGasTank tileEntity = (TileEntityGasTank)world.getTileEntity(pos);
		ItemStack stack = entityplayer.getHeldItem(hand);

		if(!stack.isEmpty())
		{
			IMekWrench wrenchHandler = Wrenches.getHandler(stack);
			if (wrenchHandler != null)
			{
				RayTraceResult raytrace = new RayTraceResult(new Vec3d(hitX, hitY, hitZ), side, pos);
				if (wrenchHandler.canUseWrench(entityplayer, hand, stack, raytrace))
				{
					if (SecurityUtils.canAccess(entityplayer, tileEntity))
					{
						wrenchHandler.wrenchUsed(entityplayer, hand, stack, raytrace);

						if(entityplayer.isSneaking())
						{
							dismantleBlock(state, world, pos);

							return true;
						}

						if(tileEntity != null)
						{
							int change = tileEntity.facing.rotateY().ordinal();

							tileEntity.setFacing((short)change);
							world.notifyNeighborsOfStateChange(pos, this, true);
						}
					}
					else {
						SecurityUtils.displayNoAccess(entityplayer);
					}

					return true;
				}
			}
		}

		if(tileEntity != null)
		{
			if(!entityplayer.isSneaking())
			{
				if(SecurityUtils.canAccess(entityplayer, tileEntity))
				{
					entityplayer.openGui(Mekanism.instance, 10, world, pos.getX(), pos.getY(), pos.getZ());
				}
				else {
					SecurityUtils.displayNoAccess(entityplayer);
				}
				
				return true;
			}
		}
		
		return false;
	}

	private void dismantleBlock(IBlockState state, World world, BlockPos pos)
	{
		dropBlockAsItem(world, pos, state, 0);
		world.setBlockToAir(pos);
	}

	@Override
	public boolean isOpaqueCube(IBlockState state)
	{
		return false;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state)
	{
		return EnumBlockRenderType.MODEL;
	}
	
	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
	{
		return TANK_BOUNDS;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta)
	{
		return new TileEntityGasTank();
	}

	private ItemStack getDropItem(IBlockAccess world, BlockPos pos) {
		TileEntityGasTank tileEntity = (TileEntityGasTank)world.getTileEntity(pos);

		ItemStack itemStack = new ItemStack(MekanismBlocks.GasTank);
		if (tileEntity == null)
		{
			throw new NullPointerException();
		}
		itemStack.setTagCompound(new NBTTagCompound());

		ISecurityItem securityItem = (ISecurityItem)itemStack.getItem();
		if(securityItem.hasSecurity(itemStack))
		{
			securityItem.setOwnerUUID(itemStack, tileEntity.getSecurity().getOwnerUUID());
			securityItem.setSecurity(itemStack, tileEntity.getSecurity().getMode());
		}

		tileEntity.getConfig().write(ItemDataUtils.getDataMap(itemStack));
		tileEntity.getEjector().write(ItemDataUtils.getDataMap(itemStack));

		ITierItem tierItem = (ITierItem)itemStack.getItem();
		tierItem.setBaseTier(itemStack, tileEntity.tier.getBaseTier());

		IGasItem storageTank = (IGasItem)itemStack.getItem();
		storageTank.setGas(itemStack, tileEntity.gasTank.getGas());

		ISustainedInventory inventory = (ISustainedInventory)itemStack.getItem();
		inventory.setInventory(tileEntity.getInventory(), itemStack);

		return itemStack;
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

	@Override
	public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te, ItemStack stack) {
		player.addStat(StatList.getBlockStats(this));
		player.addExhaustion(0.005F);
	}

	@Override
	public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
		if (!player.capabilities.isCreativeMode) {
			dropBlockAsItem(world, pos, state, 0);
		}
	}

	@Override
	public boolean hasComparatorInputOverride(IBlockState state)
	{
		return true;
	}

	@Override
	public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos)
	{
		TileEntityGasTank tileEntity = (TileEntityGasTank)world.getTileEntity(pos);
		return tileEntity.getRedstoneLevel();
	}
	
	@Override
	public EnumFacing[] getValidRotations(World world, BlockPos pos)
	{
		TileEntity tile = world.getTileEntity(pos);
		EnumFacing[] valid = new EnumFacing[6];
		
		if(tile instanceof TileEntityBasicBlock)
		{
			TileEntityBasicBlock basicTile = (TileEntityBasicBlock)tile;
			
			for(EnumFacing dir : EnumFacing.VALUES)
			{
				if(basicTile.canSetFacing(dir.ordinal()))
				{
					valid[dir.ordinal()] = dir;
				}
			}
		}
		
		return valid;
	}

	@Override
	public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis)
	{
		TileEntity tile = world.getTileEntity(pos);
		
		if(tile instanceof TileEntityBasicBlock)
		{
			TileEntityBasicBlock basicTile = (TileEntityBasicBlock)tile;
			
			if(basicTile.canSetFacing(axis.ordinal()))
			{
				basicTile.setFacing((short)axis.ordinal());
				return true;
			}
		}
		
		return false;
	}

	@Override
	public boolean isFullBlock(IBlockState state) 
	{
		return false;
	}

	@Override
	public boolean isFullCube(IBlockState state) 
	{
		return false;
	}

	/*
	@Override
	public boolean isBlockSolid(IBlockAccess worldIn, BlockPos pos, EnumFacing side)
	{
		return false;
	}
	*/
}
