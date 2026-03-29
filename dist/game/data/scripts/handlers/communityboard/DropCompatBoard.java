/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.communityboard;

import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Compatibility handler for legacy WI drop database bypasses.
 */
public class DropCompatBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbsdropmn",
		"_bbsdropiinfo",
		"_bbsdropninfo"
	};
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		CommunityBoardHandler.getInstance().addBypass(player, "Drop", command);
		final String html = "<html><body><br><center>"
			+ "<font color=LEVEL>Drop Database</font><br><br>"
			+ "RV drop search is now restored for main and item-search flows.<br><br>"
			+ "The remaining WI deep links handled by this compatibility owner are still unsupported in the current RV runtime:<br1>"
			+ "monster-name search and legacy detail pages (_bbsdropmn / _bbsdropiinfo / _bbsdropninfo).<br><br>"
			+ "This notice keeps those residual deep links explicit instead of redirecting them into unrelated or incomplete pages.<br><br>"
			+ "<button value=\"Home\" action=\"bypass _bbshome\" width=100 height=22 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">"
			+ "</center></body></html>";
		CommunityBoardHandler.separateAndSend(html, player);
		return true;
	}
}