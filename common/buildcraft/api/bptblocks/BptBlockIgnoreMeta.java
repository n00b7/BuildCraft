/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.bptblocks;

import java.util.LinkedList;

import net.minecraft.item.ItemStack;
import buildcraft.api.blueprints.BptBlock;
import buildcraft.api.blueprints.IBptContext;

public class BptBlockIgnoreMeta extends BptBlock {

	@Override
	public void addRequirements(IBptContext context, LinkedList<ItemStack> requirements) {
		requirements.add(new ItemStack(block, 1, 0));
	}

	@Override
	public boolean isValid(IBptContext context) {
		return block == context.world().getBlock(x, y, z);
	}
}
