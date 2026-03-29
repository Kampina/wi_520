package handlers.itemhandlers;

import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.item.instance.Item;

/**
 * @author l3x
 */
public class Harvester implements IItemHandler
{
    @Override
    public boolean onItemUse(Playable playable, Item item, boolean forceUse)
    {
        return false;
    }
}
