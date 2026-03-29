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
 * Community recruitment compatibility handler.
 */
public class RecruitmentBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbsrecruitment"
	};
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		CommunityBoardHandler.getInstance().addBypass(player, "Recruitment", command);
		if (command.startsWith("_bbsrecruitment:list:clan"))
		{
			final String[] parts = command.split(":");
			if (parts.length >= 4)
			{
				CommunityBoardHandler.getInstance().handleParseCommand("_bbsclan_clanlist;" + parts[3], player);
			}
			else
			{
				CommunityBoardHandler.getInstance().handleParseCommand("_bbsclan_clanlist", player);
			}
			return true;
		}
		
		if (command.startsWith("_bbsrecruitment:clan:id:"))
		{
			final String[] parts = command.split(":");
			if (parts.length >= 4)
			{
				CommunityBoardHandler.getInstance().handleParseCommand("_bbsclan_clanhome;" + parts[3], player);
			}
			else
			{
				CommunityBoardHandler.getInstance().handleParseCommand("_bbsclan", player);
			}
			return true;
		}
		
		if (command.equals("_bbsrecruitment:invitelist"))
		{
			final String html = "<html><body><br><center>"
				+ "<font color=LEVEL>Recruitment</font><br><br>"
				+ "RV keeps clan community in the native clan board.<br1>"
				+ "WI recruitment invite/request runtime is not present in this branch yet.<br><br>"
				+ "<button value=\"Clan Community\" action=\"bypass _bbsclan\" width=140 height=22 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">"
				+ "</center></body></html>";
			CommunityBoardHandler.separateAndSend(html, player);
			return true;
		}
		
		final String html = "<html><body><br><center>"
			+ "<font color=LEVEL>Recruitment</font><br><br>"
			+ "WI recruitment uses a separate Community Board runtime and service layer.<br1>"
			+ "That runtime is not present in this RV branch yet.<br><br>"
			+ "Currently available compatibility paths:<br1>"
			+ "- clan list -> native clan community list<br1>"
			+ "- clan page -> native clan home page<br><br>"
			+ "Invite, request, academy, and RecruitmentPanel popup flows remain unsupported until a dedicated runtime owner is ported.<br><br>"
			+ "<button value=\"Clan Community\" action=\"bypass _bbsclan\" width=140 height=22 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">"
			+ "</center></body></html>";
		CommunityBoardHandler.separateAndSend(html, player);
		return true;
	}
}