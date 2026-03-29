package handlers.communityboard;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.custom.CommunityBoardConfig;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Custom Shop handler adapted from old WI configuration.
 */
public class ShopBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbsshop",
		"_bbsmultisell"
	};

	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}

	@Override
	public boolean onCommand(String command, Player player)
	{
		String html = "";

		// Multisell legacy bypass format: _bbsmultisell:1001;_bbshome
		if (command.startsWith("_bbsmultisell:"))
		{
			String[] parts = command.substring("_bbsmultisell:".length()).split(";");
			try
			{
				int listId = Integer.parseInt(parts[0]);
				MultisellData.getInstance().separateAndSend(listId, player, null, false);
				
				if (parts.length > 1)
				{
					CommunityBoardHandler.getInstance().handleParseCommand(parts[1], player);
				}
			}
			catch (Exception e)
			{
				// Ignore
			}
			return true;
		}

		if (command.startsWith("_bbsshop"))
		{
			if (!CommunityBoardConfig.COMMUNITYBOARD_ENABLE_MULTISELLS)
			{
				player.sendMessage("Community Board Shop is currently disabled.");
				return false;
			}

			String[] link = command.split(":");
			if (link.length > 1)
			{
				// e.g. _bbsshop:open:90000;_bbsshop:index
				if (link[1].equals("open") && link.length > 2)
				{
					String[] data = link[2].split(";");
					int listId = Integer.parseInt(data[0]);

					if (data.length > 1)
					{
						CommunityBoardHandler.getInstance().handleParseCommand(data[1], player);
					}

					MultisellData.getInstance().separateAndSend(listId, player, null, false);
					return true;
				}
				else
				{
					// e.g. _bbsshop:index -> data/html/community/high/shop/index.htm
					html = HtmCache.getInstance().getHtm(player, "data/html/community/high/shop/" + link[1] + ".htm");
					if (html == null)
					{
						// Fallback to legacy path if high doesn't exist
						html = HtmCache.getInstance().getHtm(player, "data/html/community/shop/" + link[1] + ".htm");
					}
				}
			}
			else
			{
				html = HtmCache.getInstance().getHtm(player, "data/html/community/high/shop/index.htm");
			}
		}

		if (html != null && !html.isEmpty())
		{
			CommunityBoardHandler.separateAndSend(html, player);
			return true;
		}
		
		return false;
	}
}
