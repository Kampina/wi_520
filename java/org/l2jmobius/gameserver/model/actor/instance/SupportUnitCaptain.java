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
package org.l2jmobius.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExShowFortressSiegeInfo;

/**
 * Support Unit Captain implementation for fortress support NPCs.
 */
public class SupportUnitCaptain extends Merchant
{
	private static final String REGISTER_SIEGE = "register_siege";
	private static final String CANCEL_SIEGE = "cancel_siege";
	private static final String SHOW_SIEGE_INFO = "siege_info";

	private static final int COND_ALL_FALSE = 0;
	private static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	private static final int COND_OWNER = 2;

	public SupportUnitCaptain(NpcTemplate template)
	{
		super(template);
	}

	@Override
	public void showChatWindow(Player player)
	{
		showMessageWindow(player);
	}

	private void showMessageWindow(Player player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);

		String filename;
		switch (validateCondition(player))
		{
			case COND_BUSY_BECAUSE_OF_SIEGE:
			{
				filename = "data/html/fortress/SupportUnitCaptain-busy.htm";
				break;
			}
			case COND_OWNER:
			{
				filename = "data/html/fortress/SupportUnitCaptain.htm";
				break;
			}
			default:
			{
				filename = "data/html/fortress/SupportUnitCaptain-no.htm";
				break;
			}
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getId()));
		player.sendPacket(html);
	}

	private int validateCondition(Player player)
	{
		if ((player == null) || (getFort() == null))
		{
			return COND_ALL_FALSE;
		}

		if (getFort().getSiege().isInProgress())
		{
			return COND_BUSY_BECAUSE_OF_SIEGE;
		}
		return COND_OWNER;
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if ((player == null) || (player.getLastFolkNPC() == null) || (player.getLastFolkNPC().getObjectId() != getObjectId()))
		{
			return;
		}

		final int condition = validateCondition(player);
		if (condition <= COND_ALL_FALSE)
		{
			return;
		}
		else if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
		{
			showMessageWindow(player);
			return;
		}

		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();

		if (actualCommand.equalsIgnoreCase(REGISTER_SIEGE))
		{
			handleRegisterSiege(player);
			showMessageWindow(player);
			return;
		}
		else if (actualCommand.equalsIgnoreCase(CANCEL_SIEGE))
		{
			handleCancelSiege(player);
			showMessageWindow(player);
			return;
		}
		else if (actualCommand.equalsIgnoreCase(SHOW_SIEGE_INFO))
		{
			final Fort fort = getFort();
			if (fort != null)
			{
				player.sendPacket(new ExShowFortressSiegeInfo(fort));
			}
			return;
		}

		super.onBypassFeedback(player, command);
	}

	private void handleRegisterSiege(Player player)
	{
		final Fort fort = getFort();
		if (fort == null)
		{
			return;
		}

		switch (fort.getSiege().addAttacker(player, true))
		{
			case 0:
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
				break;
			}
			case 1:
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA);
				break;
			}
			case 2:
			{
				player.sendPacket(new SystemMessage(SystemMessageId.THE_DEADLINE_TO_REGISTER_FOR_THE_SIEGE_OF_S1_HAS_PASSED).addCastleId(fort.getResidenceId()));
				break;
			}
			case 3:
			{
				player.sendMessage("Your clan is already registered for this fortress battle.");
				break;
			}
			case 4:
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOUR_CLAN_HAS_BEEN_REGISTERED_TO_S1_S_FORTRESS_BATTLE).addCastleId(fort.getResidenceId()));
				break;
			}
		}
	}

	private void handleCancelSiege(Player player)
	{
		final Fort fort = getFort();
		if (fort == null)
		{
			return;
		}

		final Clan clan = player.getClan();
		if ((clan == null) || !player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}

		if (fort.getOwnerClan() == clan)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_CANCEL_YOUR_FORTRESS_DEFENSE_REGISTRATION);
			return;
		}

		if (!FortSiegeManager.getInstance().checkIsRegistered(clan, fort.getResidenceId()))
		{
			player.sendMessage("Your clan is not registered for this fortress battle.");
			return;
		}

		fort.getSiege().removeAttacker(clan);
		player.sendMessage("Your clan has been removed from the fortress battle registration.");
	}

	@Override
	public String getHtmlPath(int npcId, int value, Player player)
	{
		if (value == 0)
		{
			return "data/html/fortress/SupportUnitCaptain.htm";
		}
		return "data/html/fortress/SupportUnitCaptain-" + value + ".htm";
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
}