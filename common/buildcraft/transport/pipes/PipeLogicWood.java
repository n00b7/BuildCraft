/**
 * Copyright (c) SpaceToad, 2011 http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License
 * 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.transport.pipes;

import buildcraft.api.tools.IToolWrench;
import buildcraft.api.transport.PipeManager;
import buildcraft.core.proxy.CoreProxy;
import buildcraft.core.utils.Utils;
import buildcraft.transport.TileGenericPipe;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.IFluidHandler;

public class PipeLogicWood extends PipeLogic {

	public void switchSource() {
		int meta = container.getBlockMetadata();
		int newMeta = 6;

		for (int i = meta + 1; i <= meta + 6; ++i) {
			ForgeDirection o = ForgeDirection.values()[i % 6];

			TileEntity tile = container.getTile(o);

			if (isInput(tile))
				if (PipeManager.canExtractItems(container.getPipe(), tile.worldObj, tile.xCoord, tile.yCoord, tile.zCoord)
						|| PipeManager.canExtractFluids(container.getPipe(), tile.worldObj, tile.xCoord, tile.yCoord, tile.zCoord)) {
					newMeta = o.ordinal();
					break;
				}
		}

		if (newMeta != meta) {
			container.worldObj.setBlockMetadataWithNotify(container.xCoord, container.yCoord, container.zCoord, newMeta, 0);
			container.scheduleRenderUpdate();
			// worldObj.markBlockNeedsUpdate(xCoord, yCoord, zCoord);
		}
	}

	public boolean isInput(TileEntity tile) {
		return !(tile instanceof TileGenericPipe) && (tile instanceof IInventory || tile instanceof IFluidHandler)
				&& Utils.checkPipesConnections(container, tile);
	}

	@Override
	public boolean blockActivated(EntityPlayer entityplayer) {
		Item equipped = entityplayer.getCurrentEquippedItem() != null ? entityplayer.getCurrentEquippedItem().getItem() : null;
		if (equipped instanceof IToolWrench && ((IToolWrench) equipped).canWrench(entityplayer, container.xCoord, container.yCoord, container.zCoord)) {
			switchSource();
			((IToolWrench) equipped).wrenchUsed(entityplayer, container.xCoord, container.yCoord, container.zCoord);
			return true;
		}

		return false;
	}

	@Override
	public void initialize() {
		super.initialize();

		if (!CoreProxy.proxy.isRenderWorld(container.worldObj)) {
			switchSourceIfNeeded();
		}
	}

	private void switchSourceIfNeeded() {
		int meta = container.getBlockMetadata();

		if (meta > 5) {
			switchSource();
		} else {
			TileEntity tile = container.getTile(ForgeDirection.values()[meta]);

			if (!isInput(tile)) {
				switchSource();
			}
		}
	}

	@Override
	public void onNeighborBlockChange(int blockId) {
		super.onNeighborBlockChange(blockId);

		if (!CoreProxy.proxy.isRenderWorld(container.worldObj)) {
			switchSourceIfNeeded();
		}
	}

	@Override
	public boolean outputOpen(ForgeDirection to) {
		if (this.container.pipe instanceof PipeFluidsWood) {
			int meta = container.getBlockMetadata();
			return meta != to.ordinal();
		}
		return super.outputOpen(to);
	}
}
