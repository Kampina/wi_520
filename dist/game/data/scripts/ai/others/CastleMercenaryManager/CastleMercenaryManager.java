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
package ai.others.CastleMercenaryManager;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.data.xml.BuyListData;
import org.l2jmobius.gameserver.model.buylist.ProductList;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Merchant;
import org.l2jmobius.gameserver.model.clan.ClanAccess;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Castle Mercenary Manager AI.
 * @author malyelfik
 */
public class CastleMercenaryManager extends Script
{
	// NPCs
	private static final int[] NPCS =
	{
		35102, // Greenspan
		35144, // Sanford
		35186, // Arvid
		35228, // Morrison
		35276, // Eldon
		35318, // Solinus
		35365, // Rowell
		35511, // Gompus
		35557, // Kendrew
	};
	
	private CastleMercenaryManager()
	{
		addStartNpc(NPCS);
		addTalkId(NPCS);
		addFirstTalkId(NPCS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		final StringTokenizer st = new StringTokenizer(event, " ");
		switch (st.nextToken())
		{
			case "limit":
			{
				final Castle castle = npc.getCastle();
				final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
				if (castle.getName().equalsIgnoreCase("Aden"))
				{
					html.setHtml(getHtm(player, "mercmanager-aden-limit.html"));
				}
				else if (castle.getName().equalsIgnoreCase("Rune"))
				{
					html.setHtml(getHtm(player, "mercmanager-rune-limit.html"));
				}
				else
				{
					html.setHtml(getHtm(player, "mercmanager-limit.html"));
				}
				
				html.replace("%feud_name%", String.valueOf(1001000 + castle.getResidenceId()));
				player.sendPacket(html);
				break;
			}
			case "buy":
			{
				if (!canManageMercenaries(player, npc))
				{
					return "mercmanager-no.html";
				}
				if (npc.getCastle().getSiege().isInProgress())
				{
					return "mercmanager-siege.html";
				}
				final int listSuffix = Integer.parseInt(st.nextToken());
				if (!hasMercenaryBuyList(npc, listSuffix))
				{
					showMainHtml(player, npc, "Mercenary buylist is not available for this castle.");
					return null;
				}

				final int listId = getMercenaryBuyListId(npc, listSuffix);
				((Merchant) npc).showBuyWindow(player, listId, false); // NOTE: Not affected by Castle Taxes, baseTax is 20% (done in merchant buylists)
				break;
			}
			case "main":
			{
				return onFirstTalk(npc, player);
			}
			case "mercmanager-01.html":
			{
				htmltext = event;
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (canManageMercenaries(player, npc))
		{
			if (npc.getCastle().getSiege().isInProgress())
			{
				return "mercmanager-siege.html";
			}

			showMainHtml(player, npc, "");
			return null;
		}

		return "mercmanager-no.html";
	}

	private boolean canManageMercenaries(Player player, Npc npc)
	{
		return player.isGM() || ((player.getClanId() == npc.getCastle().getOwnerId()) && player.hasAccess(ClanAccess.CASTLE_MERCENARIES));
	}

	private int getMercenaryBuyListId(Npc npc, int listSuffix)
	{
		return Integer.parseInt(npc.getId() + String.valueOf(listSuffix));
	}

	private boolean hasMercenaryBuyList(Npc npc, int listSuffix)
	{
		final ProductList buyList = BuyListData.getInstance().getBuyList(getMercenaryBuyListId(npc, listSuffix));
		return (buyList != null) && buyList.isNpcAllowed(npc.getId());
	}

	private void showMainHtml(Player player, Npc npc, String notice)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(getHtm(player, "mercmanager.html"));
		html.replace("%mercenaryButton1%", hasMercenaryBuyList(npc, 1) ? "<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h Quest CastleMercenaryManager buy 1\">\"I want to hire a mercenary.\"</Button>" : "");
		html.replace("%mercenaryButton2%", hasMercenaryBuyList(npc, 2) ? "<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h Quest CastleMercenaryManager buy 2\">\"I'd like to hire an Elite Mercenary.\"</Button>" : "");
		html.replace("%mercenaryButton3%", hasMercenaryBuyList(npc, 3) ? "<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h Quest CastleMercenaryManager buy 3\">\"I want to hire a teleporter.\"</Button>" : "");
		html.replace("%mercenaryManagerNotice%", notice.isBlank() ? "" : ("<br><font color=\"LEVEL\">" + notice + "</font>"));
		player.sendPacket(html);
	}
	
	public static void main(String[] args)
	{
		new CastleMercenaryManager();
	}
}
