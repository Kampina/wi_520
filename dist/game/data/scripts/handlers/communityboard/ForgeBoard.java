package handlers.communityboard;

import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Compatibility owner for legacy WI forge community bypasses.
 */
public class ForgeBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbsforge"
	};
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		CommunityBoardHandler.getInstance().addBypass(player, "Forge", command);
		final String html = "<html><body><br><center>"
			+ "<font color=LEVEL>Forge</font><br><br>"
			+ "RV ships legacy WI forge html and bypasses, but no confirmed Community Board owner for the _bbsforge contract was found.<br><br>"
			+ "Adjacent RV enhancement systems exist separately, however no safe board-level bridge for enchant, foundation, attribute, exchanger, or augment flows is confirmed in this branch.<br><br>"
			+ "This compatibility handler closes the dead legacy forge navigation safely instead of exposing partially wired actions.<br><br>"
			+ "<button value=\"Home\" action=\"bypass _bbshome\" width=100 height=22 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">"
			+ "</center></body></html>";
		CommunityBoardHandler.separateAndSend(html, player);
		return true;
	}
}