package handlers.communityboard;

import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Custom generic services and fallback board mappings adapted from old WI configuration.
 */
public class ServicesBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbsbypass",
		"_bbsscripts",
		"_bbsnotdone",
		"_bbsmarket"
	};

	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}

	@Override
	public boolean onCommand(String command, Player player)
	{
		// Some legacy buttons route through _bbsbypass:service.Action;param
		if (command.startsWith("_bbsbypass") || command.startsWith("_bbsscripts"))
		{
			String[] parts = command.split(";", 2);
			
			// The redirect (like _bbshome) is usually passed after semicolon
			if (parts.length > 1)
			{
				CommunityBoardHandler.getInstance().handleParseCommand(parts[1], player);
			}
			else
			{
				CommunityBoardHandler.getInstance().handleParseCommand("_bbshome", player);
			}
			return true;
		}

		if (command.startsWith("_bbsmarket"))
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>Market is currently under construction for RoseVain.</center></body></html>", player);
			return true;
		}

		if (command.startsWith("_bbsnotdone"))
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>This feature is not implemented yet.</center></body></html>", player);
			return true;
		}

		return false;
	}
}
