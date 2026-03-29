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
package handlers.util;

import java.util.Map;

import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.Siege;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public final class DominionHtmlHelper
{
	private static final String HTML_ROOT = "data/html/residence2/dominion/";
	private static final int TERRITORY_ID_OFFSET = 80;
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

	public enum CaptainState
	{
		DEFAULT,
		LOW_LEVEL,
		OWNER_AVAILABLE,
		OWNER_SIEGE,
		NON_OWNER_SIEGE
	}

	private DominionHtmlHelper()
	{
	}

	public static CaptainState getCaptainState(Player player, int dominionId)
	{
		if (isLowLevel(player))
		{
			return CaptainState.LOW_LEVEL;
		}

		final Castle castle = getDominionCastle(dominionId);
		if (castle == null)
		{
			return CaptainState.DEFAULT;
		}

		final Clan clan = player.getClan();
		final boolean isOwner = (clan != null) && (clan.getCastleId() == castle.getResidenceId());
		final boolean battlefieldActive = isBattlefieldActive(castle);
		if (isOwner)
		{
			return battlefieldActive ? CaptainState.OWNER_SIEGE : CaptainState.OWNER_AVAILABLE;
		}
		if (battlefieldActive)
		{
			return CaptainState.NON_OWNER_SIEGE;
		}
		return CaptainState.DEFAULT;
	}

	public static void showMercenaryCaptainHtml(Player player, Npc npc)
	{
		switch (getCaptainState(player, getDominionId(npc)))
		{
			case LOW_LEVEL:
			{
				showHtml(player, npc, HTML_ROOT + "gludio_merc_captain026.htm");
				break;
			}
			case OWNER_AVAILABLE:
			{
				showHtml(player, npc, HTML_ROOT + "gludio_merc_captain007.htm");
				break;
			}
			case OWNER_SIEGE:
			case NON_OWNER_SIEGE:
			{
				showHtml(player, npc, HTML_ROOT + "mercenary_captain-siege.htm");
				break;
			}
			default:
			{
				showHtml(player, npc, HTML_ROOT + "gludio_merc_captain001.htm");
				break;
			}
		}
	}

	public static void showTerritoryManagerHtml(Player player, Npc npc)
	{
		if (showTerritoryManagerRestriction(player, npc))
		{
			return;
		}
		showHtml(player, npc, HTML_ROOT + "TerritoryManager.htm");
	}

	public static boolean showTerritoryManagerRestriction(Player player, Npc npc)
	{
		boolean canParticipate = false;
                if (player.getClan() != null) {
                        if (player.getClan().getCastleId() > 0) {
                                canParticipate = true;
                        } else if (player.getClan().getFortId() > 0) {
                                org.l2jmobius.gameserver.model.siege.Fort fort = org.l2jmobius.gameserver.managers.FortManager.getInstance().getFortById(player.getClan().getFortId());
                                if (fort != null && fort.getContractedCastleId() > 0) {
                                        canParticipate = true;
                                }
                        }
                }
                if (!canParticipate) { showHtml(player, npc, HTML_ROOT + "TerritoryManager-8.htm"); return true; }
		final int dominionId = getDominionId(npc);
		if (isLowLevel(player))
		{
			showHtml(player, npc, HTML_ROOT + "TerritoryManager-8.htm");
			return true;
		}

		final Castle castle = getDominionCastle(dominionId);
		if ((castle != null) && isBattlefieldActive(castle))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			if (isBoundToOtherTerritoryWar(player, castle))
			{
				html.setFile(player, HTML_ROOT + "gludio_feud_manager_q0717_02.htm");
			}
			else
			{
				html.setFile(player, HTML_ROOT + "gludio_feud_manager_q0717_01.htm");
				html.replace("%territory%", TERRITORY_NAMES.getOrDefault(dominionId, "Territory"));
			}
			player.sendPacket(html);
			return true;
		}
		return false;
	}

	private static boolean isBoundToOtherTerritoryWar(Player player, Castle castle)
	{
		if ((player.getSiegeState() == 0) || (castle == null))
		{
			return false;
		}

		final int siegeSide = player.getSiegeSide();
		return (siegeSide != castle.getResidenceId()) && (CastleManager.getInstance().getCastleById(siegeSide) != null);
	}

	private static boolean isLowLevel(Player player)
	{
		return (player.getLevel() < 40) || (player.getPlayerClass().level() <= 2);
	}

	private static int getDominionId(Npc npc)
	{
		return npc.getParameters().getInt("dominion_id", 0);
	}

	private static Castle getDominionCastle(int dominionId)
	{
		return CastleManager.getInstance().getCastleById(dominionId - TERRITORY_ID_OFFSET);
	}

	private static boolean isBattlefieldActive(Castle castle)
	{
		return Siege.isDominionBattlefieldActive(castle);
	}

	private static void showHtml(Player player, Npc npc, String path)
	{
		npc.showChatWindow(player, path);
	}
}