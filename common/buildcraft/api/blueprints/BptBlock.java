/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.blueprints;

import java.util.ArrayList;
import java.util.LinkedList;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import buildcraft.core.network.NetworkData;
import buildcraft.core.utils.Utils;

/**
 * This class allow to specify specific behavior for blocks stored in
 * blueprints:
 *
 * - what items needs to be used to create that block - how the block has to be
 * built on the world - how to rotate the block - what extra data to store /
 * load in the blueprint
 *
 * Default implementations of this can be seen in the package
 * buildcraft.api.bptblocks. The class BptBlockUtils provide some additional
 * utilities.
 *
 * Blueprints perform "id translation" in case the block ids between a blueprint
 * and the world installation are different. In order to translate block ids,
 * blocks needs to be uniquely identified. By default, this identification is
 * done by:
 *
 * - the block simple class name - the tile simple class name (if any) - the
 * block name
 *
 * In certain circumstances, the above is not enough (e.g. if several blocks
 * share the same class and the same name, with no tile). In this case,
 * additional data may be provided by children of this class:
 *
 * - mod name - custom signature
 *
 * At blueprint load time, BuildCraft will check that each block id of the
 * blueprint corresponds to the block id in the installation. If not, it will
 * perform a search through the block list, and upon matching signature, it will
 * translate all blocks ids of the blueprint to the installation ones. If no
 * such block id is found, BuildCraft will assume that the block is not
 * installed and will not load the blueprint.
 */
public class BptBlock {

	@NetworkData
	public Block block = null;

	@NetworkData
	public int x, y, z, meta = 0;

	/**
	 * This field contains requirements for a given block when stored in the
	 * blueprint. Modders can either rely on this list or compute their own int
	 * BptBlock.
	 */
	@NetworkData
	public ArrayList<ItemStack> storedRequirements = new ArrayList<ItemStack>();

	/**
	 * This tree contains additional data to be stored in the blueprint. By
	 * default, it will be initialized from BptBlock.initializeFromWorld with
	 * the standard readNBT function of the corresponding tile (if any) and will
	 * be loaded from BptBlock.buildBlock using the standard writeNBT function.
	 */
	@NetworkData
	public NBTTagCompound cpt = new NBTTagCompound();

	public enum Mode {
		ClearIfInvalid, Build
	};

	public Mode mode = Mode.Build;

	@SuppressWarnings("unchecked")
	@Override
	public BptBlock clone() {
		BptBlock obj = BlueprintManager.newSchematic(block);

		obj.x = x;
		obj.y = y;
		obj.z = z;
		obj.block = block;
		obj.meta = meta;
		obj.cpt = (NBTTagCompound) cpt.copy();
		obj.storedRequirements = (ArrayList<ItemStack>) storedRequirements.clone();
		obj.mode = mode;

		return obj;
	}

	public final LinkedList<ItemStack> getRequirements(IBptContext context) {
		LinkedList<ItemStack> res = new LinkedList<ItemStack>();

		addRequirements(context, res);

		return res;
	}


	/**
	 * Returns the requirements needed to build this block. When the
	 * requirements are met, they will be removed all at once from the builder,
	 * before calling buildBlock.
	 */
	public void addRequirements(IBptContext context, LinkedList<ItemStack> requirements) {
		if (block != null) {
			if (storedRequirements.size() != 0) {
				requirements.addAll(storedRequirements);
			} else {
				requirements.add(new ItemStack(block, 1, meta));
			}
		}
	}

	/**
	 * This is called each time an item matches a reqquirement, that is: (req id
	 * == stack id) for damageable items (req id == stack id && req dmg == stack
	 * dmg) for other items by default, it will increase damage of damageable
	 * items by the amount of damage of the requirement, and remove the intended
	 * amount of non damageable item.
	 *
	 * Client may override this behavior for default items. Note that this
	 * subprogram may be called twice with the same parameters, once with a copy
	 * of requirements and stack to check if the entire requirements can be
	 * fullfilled, and once with the real inventory. Implementer is responsible
	 * for updating req (with the remaining requirements if any) and stack
	 * (after usage)
	 *
	 * returns: what was used (similer to req, but created from stack, so that
	 * any NBT based differences are drawn from the correct source)
	 */
	public ItemStack useItem(IBptContext context, ItemStack req, ItemStack stack) {
		ItemStack result = stack.copy();
		if (stack.isItemStackDamageable()) {
			if (req.getItemDamage() + stack.getItemDamage() <= stack.getMaxDamage()) {
				stack.setItemDamage(req.getItemDamage() + stack.getItemDamage());
				result.setItemDamage(req.getItemDamage());
				req.stackSize = 0;
			}

			if (stack.getItemDamage() >= stack.getMaxDamage()) {
				stack.stackSize = 0;
			}
		} else {
			if (stack.stackSize >= req.stackSize) {
				result.stackSize = req.stackSize;
				stack.stackSize -= req.stackSize;
				req.stackSize = 0;
			} else {
				req.stackSize -= stack.stackSize;
				stack.stackSize = 0;
			}
		}

		if (stack.stackSize == 0 && stack.getItem().getContainerItem() != null) {
			Item container = stack.getItem().getContainerItem();

			//stack.itemID = container.itemID;
			stack.stackSize = 1;
			stack.setItemDamage(0);
		}
		return result;
	}

	/**
	 * Return true if the block on the world correspond to the block stored in
	 * the blueprint at the location given by the slot. By default, this
	 * subprogram is permissive and doesn't take into account metadata.
	 */
	public boolean isValid(IBptContext context) {
		return block == context.world().getBlock(x, y, z) && meta == context.world().getBlockMetadata(x, y, z);
	}

	/**
	 * Perform a 90 degree rotation to the slot.
	 */
	public void rotateLeft(IBptContext context) {

	}

	/**
	 * Places the block in the world, at the location specified in the slot.
	 */
	public void buildBlock(IBptContext context) {
		// Meta needs to be specified twice, depending on the block behavior
		context.world().setBlock(x, y, z, block, meta, 3);
		//context.world().setBlockMetadataWithNotify(slot.x, slot.y, slot.z, slot.meta, 3);

		if (block instanceof BlockContainer) {
			TileEntity tile = context.world().getTileEntity(x, y, z);

			cpt.setInteger("x", x);
			cpt.setInteger("y", y);
			cpt.setInteger("z", z);

			if (tile != null) {
				tile.readFromNBT(cpt);
			}

			// By default, clear the inventory to avoid possible dupe bugs
			if (tile instanceof IInventory) {
				IInventory inv = (IInventory) tile;

				for (int i = 0; i < inv.getSizeInventory(); ++i) {
					inv.setInventorySlotContents(i, null);
				}
			}
		}
	}

	/**
	 * Return true if the block should not be placed to the world. Requirements
	 * will not be asked on such a block, and building will not be called.
	 */
	public boolean ignoreBuilding() {
		return false;
	}

	/**
	 * Initializes a slot from the blueprint according to an objet placed on {x,
	 * y, z} on the world. This typically means adding entries in slot.cpt. Note
	 * that "id" and "meta" will be set automatically, corresponding to the
	 * block id and meta.
	 *
	 * By default, if the block is a BlockContainer, tile information will be to
	 * save / load the block.
	 */
	public void initializeFromWorld(IBptContext context, int x, int y, int z) {
		if (block instanceof BlockContainer) {
			TileEntity tile = context.world().getTileEntity(x, y, z);

			if (tile != null) {
				tile.writeToNBT(cpt);
			}
		}

		if (block != null) {
			ArrayList<ItemStack> req = block.getDrops(context.world(), x,
					y, z, context.world().getBlockMetadata(x, y, z), 0);

			if (req != null) {
				storedRequirements.addAll(req);
			}
		}
	}

	/**
	 * Called on a block when the blueprint has finished to place all the
	 * blocks. This may be useful to adjust variable depending on surrounding
	 * blocks that may not be there already at initial building.
	 */
	public void postProcessing(IBptContext context) {

	}

	private boolean starMatch(String s1, String s2) {
		return s1.equals("*") || s2.equals("*") || s1.equals(s2);
	}

	public void writeToNBT(NBTTagCompound nbt, MappingRegistry registry) {
		nbt.setInteger("blockId", registry.getIdForBlock(block));
		nbt.setInteger("blockMeta", meta);
		nbt.setTag("blockCpt", cpt);

		NBTTagList rq = new NBTTagList();

		for (ItemStack stack : storedRequirements) {
			NBTTagCompound sub = new NBTTagCompound();
			stack.writeToNBT(stack.writeToNBT(sub));
			sub.setInteger("id", Item.itemRegistry.getIDForObject(registry
					.getItemForId(sub.getInteger("id"))));
			rq.appendTag(sub);
		}

		nbt.setTag("rq", rq);
	}

	public void readFromNBT(NBTTagCompound nbt,	MappingRegistry registry) {
		block = registry.getBlockForId(nbt.getInteger("blockId"));
		meta = nbt.getInteger("blockMeta");
		cpt = nbt.getCompoundTag("blockCpt");

		NBTTagList rq = nbt.getTagList("rq", Utils.NBTTag_Types.NBTTagList.ordinal());

		for (int i = 0; i < rq.tagCount(); ++i) {
			NBTTagCompound sub = rq.getCompoundTagAt(i);

			// Maps the id in the blueprint to the id in the world
			sub.setInteger("id", Item.itemRegistry.getIDForObject(registry
					.getItemForId(sub.getInteger("id"))));

			storedRequirements.add(ItemStack.loadItemStackFromNBT(sub));
		}
	}
}
