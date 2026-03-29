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
package handlers.bypasshandlers;

import java.util.Locale;
import java.util.Map;

import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

import handlers.util.DominionHtmlHelper;

public class DominionTerritoryManager implements IBypassHandler
{
	private static final int ADENA_ID = 57;
	private static final long ADENA_PER_BADGE = 50000;
	private static final int DOMINION_MULTISELL_BASE_ID = 39000;

	private static final String[] COMMANDS =
	{
		"buyspecial",
		"rewardstatus",
		"receivelater",
		"receive"
	};
	
	private static final Map<Integer, String> TERRITORY_NAMES = Map.of(
		81, "Gludio",
		82, "Dion",
		83, "Giran",
		84, "Oren",
		85, "Aden",
		86, "Innadril",
		87, "Goddard",
		88, "Rune",
		89, "Schuttgart");
	
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		if ((command == null) || command.isBlank())
		{
			return false;
		}

		if (!target.isNpc())
		{
			return false;
		}
		
		final Npc npc = target.asNpc();
		final int dominionId = npc.getParameters().getInt("dominion_id", 0);
		if ((dominionId < 81) || (dominionId > 89))
		{
			return false;
		}
		if (DominionHtmlHelper.showTerritoryManagerRestriction(player, npc))
		{
			return true;
		}
		
		final String actualCommand = command.split(" ")[0].toLowerCase(Locale.ROOT);
		if ("buyspecial".equals(actualCommand))
		{
			final int badgeId = 13676 + dominionId;
			final long badgeCount = player.getInventory().getInventoryItemCount(badgeId, -1);
			if (badgeCount < 1)
			{
				showHtml(player, npc, "data/html/residence2/dominion/TerritoryManager-1.htm");
				return true;
			}
			
			final int npcId = npc.getId();
			final int[] multisellCandidates =
			{
				DOMINION_MULTISELL_BASE_ID + dominionId,
				(npcId * 10000) + 1,
				(npcId * 10000) + 2,
				(npcId * 10000) + 3,
				npcId
			};
			for (int multisellId : multisellCandidates)
			{
				if (MultisellData.getInstance().getMultisell(multisellId) != null)
				{
					MultisellData.getInstance().separateAndSend(multisellId, player, npc, false);
					return true;
				}
			}
			
			showHtml(player, npc, "data/html/residence2/dominion/TerritoryManager-4.htm");
			return true;
		}

		if ("rewardstatus".equals(actualCommand))
		{
			showRewardStatusHtml(player, npc, dominionId);
			return true;
		}
		
		if ("receivelater".equals(actualCommand) || "receive".equals(actualCommand))
		{
			final int badgeId = 13676 + dominionId;
			final long badgeCount = player.getInventory().getInventoryItemCount(badgeId, -1);
			final long rewardAdena = badgeCount * ADENA_PER_BADGE;
			final String territoryName = TERRITORY_NAMES.getOrDefault(dominionId, "Territory");
			final boolean isReceiveNow = "receive".equals(actualCommand);
			if (isReceiveNow && (badgeCount > 0))
			{
				if (player.destroyItemByItemId(ItemProcessType.SELL, badgeId, badgeCount, npc, false))
				{
					player.addItem(ItemProcessType.REWARD, ADENA_ID, rewardAdena, npc, true);
				}
			}
			
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile(player, isReceiveNow ? "data/html/residence2/dominion/TerritoryManager-7.htm" : "data/html/residence2/dominion/TerritoryManager-6.htm");
			html.replace("%territory%", territoryName);
			html.replace("%badges%", String.valueOf(badgeCount));
			html.replace("%adena%", String.valueOf(rewardAdena));
			player.sendPacket(html);
			return true;
		}
		
		return false;
	}
	
	private void showHtml(Player player, Npc npc, String path)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, path);
		player.sendPacket(html);
	}

	private void showRewardStatusHtml(Player player, Npc npc, int dominionId)
	{
		final int badgeId = 13676 + dominionId;
		final long badgeCount = player.getInventory().getInventoryItemCount(badgeId, -1);
		final long rewardAdena = badgeCount * ADENA_PER_BADGE;
		final String territoryName = TERRITORY_NAMES.getOrDefault(dominionId, "Territory");
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/html/residence2/dominion/TerritoryManager-5.htm");
		html.replace("%territory%", territoryName);
		html.replace("%badges%", String.valueOf(badgeCount));
		html.replace("%adena%", String.valueOf(rewardAdena));
		player.sendPacket(html);
	}
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}