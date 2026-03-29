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
package org.l2jmobius.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class FortDoorman extends Doorman
{
	public FortDoorman(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.FortDoorman);
	}

	private void sendHtmlMessage(Player player, NpcHtmlMessage html)
	{
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getId()));
		player.sendPacket(html);
	}

	private String buildDoorCommand(String action, Fort fort)
	{
		final StringBuilder command = new StringBuilder(action).append(' ').append(action);
		for (Door door : fort.getDoors())
		{
			command.append(", ").append(door.getId());
		}
		return command.toString();
	}

	private boolean canManageDoorsDuringSiege()
	{
		final Fort fort = getFort();
		return (fort != null) && (fort.getSiege().getFlagCount() == 0);
	}

	private void showDoorControlWindow(Player player, String notice)
	{
		final Fort fort = getFort();
		if (fort == null)
		{
			return;
		}

		final String openAllCommand = buildDoorCommand("open_doors", fort);
		final String closeAllCommand = buildDoorCommand("close_doors", fort);
		final StringBuilder html = new StringBuilder();
		html.append("<html><body><font color=\"LEVEL\">Fortress Doorkeeper</font><br>");
		if ((notice != null) && !notice.isEmpty())
		{
			html.append(notice).append("<br><br>");
		}

		if (fort.getDoors().isEmpty())
		{
			html.append("No fortress doors are registered for this NPC.");
		}
		else
		{
			html.append("[npc_%objectId%_")
				.append(openAllCommand)
				.append("|Open all fortress doors.]<br1>")
				.append("[npc_%objectId%_")
				.append(closeAllCommand)
				.append("|Close all fortress doors.]<br><br>");

			for (Door door : fort.getDoors())
			{
				html.append("Door ")
					.append(door.getId())
					.append("<br1>[npc_%objectId%_open_doors open_doors, ")
					.append(door.getId())
					.append("|Open] [npc_%objectId%_close_doors close_doors, ")
					.append(door.getId())
					.append("|Close]<br><br>");
			}
		}

		html.append("</body></html>");

		final NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
		msg.setHtml(html.toString());
		sendHtmlMessage(player, msg);
	}

	private void showDoorControlWindow(Player player)
	{
		showDoorControlWindow(player, null);
	}

	private void showSimpleWindow(Player player, String text)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setHtml("<html><body>" + text + "</body></html>");
		sendHtmlMessage(player, html);
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		if (!isOwnerClan(player))
		{
			showSimpleWindow(player, "You cannot enter if you are not qualified!");
		}
		else if (isUnderSiege())
		{
			showDoorControlWindow(player, canManageDoorsDuringSiege()
				? "The fortress is under siege. Emergency door control is currently available."
				: "The fortress doors cannot be controlled while combat flags are active.");
		}
		else
		{
			showDoorControlWindow(player);
		}
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("Chat"))
		{
			showChatWindow(player);
			return;
		}
		else if (command.startsWith("open_doors"))
		{
			if (isOwnerClan(player))
			{
				if (!isUnderSiege() || canManageDoorsDuringSiege())
				{
					openDoors(player, command);
				}
				else
				{
					cannotManageDoors(player);
				}
			}
			return;
		}
		else if (command.startsWith("close_doors"))
		{
			if (isOwnerClan(player))
			{
				if (!isUnderSiege() || canManageDoorsDuringSiege())
				{
					closeDoors(player, command);
				}
				else
				{
					cannotManageDoors(player);
				}
			}
			return;
		}

		super.onBypassFeedback(player, command);
	}

	@Override
	protected void cannotManageDoors(Player player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		showDoorControlWindow(player, "The fortress doors cannot be controlled while combat flags are active.");
	}
	
	@Override
	protected void openDoors(Player player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
		st.nextToken();
		
		while (st.hasMoreTokens())
		{
			getFort().openDoor(player, Integer.parseInt(st.nextToken()));
		}
	}
	
	@Override
	protected void closeDoors(Player player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command.substring(11), ", ");
		st.nextToken();
		
		while (st.hasMoreTokens())
		{
			getFort().closeDoor(player, Integer.parseInt(st.nextToken()));
		}
	}
	
	@Override
	protected final boolean isOwnerClan(Player player)
	{
		return (player.getClan() != null) && (getFort() != null) && (getFort().getOwnerClan() != null) && (player.getClanId() == getFort().getOwnerClan().getId());
	}
	
	@Override
	protected final boolean isUnderSiege()
	{
		return getFort().getZone().isActive();
	}
}
