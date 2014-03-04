package buildcraft.transport.blueprints;

import java.util.HashMap;

import net.minecraft.item.Item;
import buildcraft.api.blueprints.BptBlock;
import buildcraft.api.blueprints.IBptContext;

public class BptPipeExtension {

	private static final HashMap <Item, BptPipeExtension> bptPipeExtensionRegistry = new HashMap<Item, BptPipeExtension>();

	public BptPipeExtension (Item i) {
		bptPipeExtensionRegistry.put(i, this);
	}

	public void postProcessing(BptBlock slot, IBptContext context) {

	}

	public void rotateLeft(BptBlock slot, IBptContext context) {

	}

	public static boolean contains (Item i) {
		return bptPipeExtensionRegistry.containsKey(i);
	}

	public static BptPipeExtension get (Item i) {
		return bptPipeExtensionRegistry.get(i);
	}
}
