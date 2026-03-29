/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.admincommandhandlers;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.StringTokenizer;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class AdminTerritoryWar implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_territory_war",
		"admin_territory_war_time",
		"admin_territory_war_start",
		"admin_territory_war_end",
		"admin_territory_wards_list",
		"admin_set_dominion_time",
		"admin_start_dominion_war",
		"admin_stop_dominion_war",
	};

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	@Override
	public boolean onCommand(String commandValue, Player activeChar)
	{
		StringTokenizer st = new StringTokenizer(commandValue, " ");
		final String command = st.nextToken();

		switch (command)
		{
			case "admin_territory_war":
			{
				showMainPage(activeChar);
				break;
			}
			case "admin_territory_war_time":
				case "admin_set_dominion_time":
			{
				handleTimeCommand(activeChar, st);
				break;
			}
			case "admin_territory_war_start":
				case "admin_start_dominion_war":
			{
					handleWarStateCommand(activeChar, st, true);
				showMainPage(activeChar);
				break;
			}
			case "admin_territory_war_end":
				case "admin_stop_dominion_war":
			{
					handleWarStateCommand(activeChar, st, false);
				showMainPage(activeChar);
				break;
			}
			case "admin_territory_wards_list":
			{
				showWardsList(activeChar);
				break;
			}
		}

		return true;
	}

	private void handleTimeCommand(Player activeChar, StringTokenizer st)
	{
		if (st.countTokens() >= 6)
		{
			try
			{
				if (st.countTokens() >= 7)
				{
					st.nextToken();
				}

				final int hour = Integer.parseInt(st.nextToken());
				final int minute = Integer.parseInt(st.nextToken());
				final int day = Integer.parseInt(st.nextToken());
				final int month = Integer.parseInt(st.nextToken());
				final int year = Integer.parseInt(st.nextToken());
				final Calendar newDate = getCurrentTerritoryWarDate();
				newDate.set(Calendar.HOUR_OF_DAY, Math.max(0, Math.min(23, hour)));
				newDate.set(Calendar.MINUTE, Math.max(0, Math.min(59, minute)));
				newDate.set(Calendar.DAY_OF_MONTH, Math.max(1, Math.min(31, day)));
				newDate.set(Calendar.MONTH, Math.max(1, Math.min(12, month)) - 1);
				newDate.set(Calendar.YEAR, year);
				newDate.set(Calendar.SECOND, 0);
				newDate.set(Calendar.MILLISECOND, 0);

				for (Castle castle : CastleManager.getInstance().getCastles())
				{
					castle.getSiegeDate().setTimeInMillis(newDate.getTimeInMillis());
					castle.getSiege().startAutoTask();
				}

				showTimePage(activeChar);
				return;
			}
			catch (NumberFormatException e)
			{
				activeChar.sendSysMessage("Invalid Territory War date values.");
				showTimePage(activeChar);
				return;
			}
		}

		if (st.countTokens() >= 2)
		{
			final String field = st.nextToken();
			final int value;
			try
			{
				value = Integer.parseInt(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				activeChar.sendSysMessage("Invalid Territory War time value.");
				showTimePage(activeChar);
				return;
			}
			final Calendar newDate = getCurrentTerritoryWarDate();
			switch (field)
			{
				case "month":
					newDate.add(Calendar.MONTH, value);
					break;
				case "day":
					newDate.set(Calendar.DAY_OF_MONTH, Math.max(1, Math.min(31, value)));
					break;
				case "hour":
					newDate.set(Calendar.HOUR_OF_DAY, Math.max(0, Math.min(23, value)));
					break;
				case "min":
					newDate.set(Calendar.MINUTE, Math.max(0, Math.min(59, value)));
					break;
				default:
					activeChar.sendSysMessage("Invalid Territory War time field.");
					showTimePage(activeChar);
					return;
			}
			newDate.set(Calendar.SECOND, 0);
			newDate.set(Calendar.MILLISECOND, 0);

			for (Castle castle : CastleManager.getInstance().getCastles())
			{
				castle.getSiegeDate().setTimeInMillis(newDate.getTimeInMillis());
				castle.getSiege().startAutoTask();
			}
		}

		showTimePage(activeChar);
	}

	private void handleWarStateCommand(Player activeChar, StringTokenizer st, boolean start)
	{
		int delaySeconds = 0;
		if (st.hasMoreTokens())
		{
			try
			{
				delaySeconds = Math.max(0, Integer.parseInt(st.nextToken()));
			}
			catch (NumberFormatException e)
			{
				activeChar.sendSysMessage("Invalid Territory War delay value.");
				return;
			}
		}

		final Runnable task = () ->
		{
			for (Castle castle : CastleManager.getInstance().getCastles())
			{
				if (start)
				{
					if (!castle.getSiege().isInProgress())
					{
						castle.getSiege().startSiege();
					}
				}
				else if (castle.getSiege().isInProgress())
				{
					castle.getSiege().endSiege(true);
				}
			}
		};

		if (delaySeconds > 0)
		{
			ThreadPool.schedule(task, delaySeconds * 1000L);
			activeChar.sendSysMessage("Territory War " + (start ? "start" : "end") + " scheduled in " + delaySeconds + " seconds.");
		}
		else
		{
			task.run();
			activeChar.sendSysMessage("Territory War " + (start ? "start" : "end") + " command executed.");
		}
	}

	private Calendar getCurrentTerritoryWarDate()
	{
		final Castle castle = CastleManager.getInstance().getCastleById(1);
		if ((castle != null) && (castle.getSiegeDate() != null))
		{
			final Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(castle.getSiegeDate().getTimeInMillis());
			return calendar;
		}
		return Calendar.getInstance();
	}

	private void showMainPage(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(activeChar, "data/html/admin/territorywar.htm");
		activeChar.sendPacket(html);
	}

	private void showTimePage(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(activeChar, "data/html/admin/territorywartime.htm");
		html.replace("%time%", DATE_FORMAT.format(getCurrentTerritoryWarDate().getTime()));
		activeChar.sendPacket(html);
	}

	private void showWardsList(Player activeChar)
	{
		final StringBuilder sb = new StringBuilder(4096);
		sb.append("<html><body>");
		sb.append("<table width=270><tr>");
		sb.append("<td width=45><button value=\"Main\" action=\"bypass -h admin_admin\" width=45 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=180><center>Territory Wards</center></td>");
		sb.append("<td width=45><button value=\"Back\" action=\"bypass -h admin_territory_war\" width=45 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("</tr></table><br>");

		int count = 0;
		for (Player player : World.getInstance().getPlayers())
		{
			final Item ward = SiegeManager.getInstance().getTerritoryWard(player);
			if (ward == null)
			{
				continue;
			}

			count++;
			sb.append("<table width=280>");
			sb.append("<tr><td width=190>").append(player.getName()).append(" - Item ").append(ward.getId()).append("</td>");
			sb.append("<td width=90><a action=\"bypass -h admin_move_to ").append(player.getX()).append(" ").append(player.getY()).append(" ").append(player.getZ()).append("\">Teleport</a></td></tr>");
			sb.append("</table>");
		}

		if (count == 0)
		{
			sb.append("No active territory ward carriers.");
		}

		sb.append("</body></html>");
		activeChar.sendPacket(new NpcHtmlMessage(0, sb.toString()));
	}

	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
