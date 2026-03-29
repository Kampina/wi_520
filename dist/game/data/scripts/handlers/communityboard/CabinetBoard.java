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

import org.l2jmobius.gameserver.config.custom.PasswordChangeConfig;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Community cabinet compatibility handler.
 */
public class CabinetBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbscabinet"
	};
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		CommunityBoardHandler.getInstance().addBypass(player, "Cabinet", command);
		final String section = extractSection(command);
		CommunityBoardHandler.separateAndSend(buildHtml(section), player);
		return true;
	}
	
	private String extractSection(String command)
	{
		if (command.startsWith("_bbscabinet:show:"))
		{
			return command.substring("_bbscabinet:show:".length());
		}
		if (command.startsWith("_bbscabinet:body:show:"))
		{
			return "visual";
		}
		if (command.startsWith("_bbscabinet:password"))
		{
			return "password";
		}
		if (command.startsWith("_bbscabinet:code"))
		{
			return "code";
		}
		return "index";
	}
	
	private String buildHtml(String section)
	{
		final StringBuilder html = new StringBuilder(2048);
		html.append("<html><body><br><center>");
		html.append("<font color=LEVEL>Cabinet</font><br><br>");
		html.append("<table width=320 border=0>");
		html.append("<tr><td><button value=\"Services\" action=\"bypass _bbscabinet:show:index\" width=140 height=22 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\"></td>");
		html.append("<td><button value=\"Security\" action=\"bypass _bbscabinet:show:security\" width=140 height=22 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\"></td></tr>");
		html.append("<tr><td><button value=\"Password\" action=\"bypass _bbscabinet:show:password\" width=140 height=22 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\"></td>");
		html.append("<td><button value=\"Visual\" action=\"bypass _bbscabinet:body:show:peace\" width=140 height=22 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\"></td></tr>");
		html.append("</table><br>");
		html.append(renderSection(section));
		html.append("<br><button value=\"Home\" action=\"bypass _bbshome\" width=100 height=22 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
		html.append("</center></body></html>");
		return html.toString();
	}
	
	private String renderSection(String section)
	{
		switch (section)
		{
			case "security":
			{
				return "RV does not contain the WI cabinet security runtime for account sharing, IP lock, HWID lock, or cabinet PIN actions yet.<br1>"
					+ "The cabinet security html in datapack is currently owner-missing without its original service layer.";
			}
			case "password":
			{
				return PasswordChangeConfig.ALLOW_CHANGE_PASSWORD
					? "RV has password change support, but it is exposed through the voiced command <font color=LEVEL>.changepassword</font>, not through the WI cabinet UI.<br1>"
						+ "This cabinet entry is a compatibility notice, not the original cabinet password form."
					: "Password change is disabled in current RV configuration.<br1>The WI cabinet password form is not active in this branch.";
			}
			case "visual":
			{
				return "RV supports appearance items and visual IDs in native systems, but the WI cabinet visual-equip flow and Body.Peace service bypasses are not present here.<br1>"
					+ "This branch remains owner-missing until a dedicated cabinet/runtime adaptation is implemented.";
			}
			case "code":
			{
				return "WI promo-code cabinet flow is not present in this RV branch.<br1>"
					+ "The datapack html exists, but no live promo-code cabinet owner was found.";
			}
			default:
			{
				return "WI cabinet is a multi-service subsystem that bundles account security, password change, promo codes, premium/account data, and visual-equip tools.<br1>"
					+ "RV does not ship that subsystem as a single Community Board owner.<br><br>"
					+ "Current compatibility status:<br1>"
					+ "- Cabinet button no longer falls into a missing parse handler.<br1>"
					+ "- Password change exists separately via .changepassword when enabled.<br1>"
					+ "- Security, promo-code, share-account, and cabinet visual flows remain unsupported in Community Board form.";
			}
		}
	}
}