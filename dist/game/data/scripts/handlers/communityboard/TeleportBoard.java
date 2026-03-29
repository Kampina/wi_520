package handlers.communityboard;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Custom Teleport handler adapted from old WI configuration.
 */
public class TeleportBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbsteleport"
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

		if (command.equals("_bbsteleport"))
		{
			html = HtmCache.getInstance().getHtm(player, "data/html/community/high/teleport/index.htm");
		}
		else if (command.startsWith("_bbsteleport:page"))
		{
			String[] path = command.split(":");
			if (path.length > 3)
			{
				html = HtmCache.getInstance().getHtm(player, "data/html/community/high/teleport/" + path[2] + "/" + path[3] + ".htm");
			}
			else if (path.length > 2)
			{
				html = HtmCache.getInstance().getHtm(player, "data/html/community/high/teleport/" + path[2] + ".htm");
			}
		}
		else if (command.startsWith("_bbsteleport:go"))
		{
			html = HtmCache.getInstance().getHtm(player, "data/html/community/high/teleport/index.htm");
			String[] cord = command.split(":");
			try 
			{
				int x = Integer.parseInt(cord[2]);
				int y = Integer.parseInt(cord[3]);
				int z = Integer.parseInt(cord[4]);
				player.teleToLocation(x, y, z);
			}
			catch (Exception e)
			{
				// Ignore missing coordinates
			}
		}

		// If no custom teleport HTML was found, fall back
		if (html == null)
		{
			html = "<html><body><br><br><center>404 - Teleport page not found.</center></body></html>";
		}

		CommunityBoardHandler.separateAndSend(html, player);
		return true;
	}
}
