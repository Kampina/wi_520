package handlers.communityboard;

import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Compatibility owner for legacy and mixed buffer community bypasses.
 */
public class BufferBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbsbuffer",
		"_bbsbuffersave",
		"_bbsbufferuse",
		"_bbsbufferdelete",
		"_bbsbufferheal",
		"_bbsbufferremovebuffs",
		"_bbsbuffertype",
		"_bbsplayerbuffer",
		"_bbspetbuffer"
	};
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		CommunityBoardHandler.getInstance().addBypass(player, "Buffer", command);
		final String html = "<html><body><br><center>"
			+ "<font color=LEVEL>Buffer</font><br><br>"
			+ "RV ships a legacy community buffer html surface with mixed bypass families (_bbsbuffer* and _bbsplayerbuffer/_bbspetbuffer), but no confirmed Java owner for these board commands was found.<br><br>"
			+ "Because no safe runtime bridge is verified in the current checkout, this handler closes the dead branch explicitly instead of pretending the buffer service is live.<br><br>"
			+ "<button value=\"Home\" action=\"bypass _bbshome\" width=100 height=22 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">"
			+ "</center></body></html>";
		CommunityBoardHandler.separateAndSend(html, player);
		return true;
	}
}