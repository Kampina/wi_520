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
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExShowDominionRegistry;

import handlers.util.DominionHtmlHelper;

public class DominionMercenaryCaptain implements IBypassHandler
{
	private static final int DOMINION_MULTISELL_BASE_ID = 39000;
	private static final String HTML_ROOT = "data/html/residence2/dominion/";
	private static final String[] COMMANDS =
	{
		"buytw",
		"mercenary_info",
		"territory_register",
		"certificate_multisell"
	};

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

		final DominionHtmlHelper.CaptainState state = DominionHtmlHelper.getCaptainState(player, dominionId);

		final String commandKey = command.split(" ")[0].toLowerCase(Locale.ROOT);
		if ("buytw".equals(commandKey))
		{
			if ((state == DominionHtmlHelper.CaptainState.LOW_LEVEL) || (state == DominionHtmlHelper.CaptainState.OWNER_SIEGE) || (state == DominionHtmlHelper.CaptainState.NON_OWNER_SIEGE))
			{
				showStateHtml(player, npc, state);
				return true;
			}
			return showCaptainMultisell(player, npc, dominionId);
		}

		if ("mercenary_info".equals(commandKey))
		{
			showHtml(player, npc, HTML_ROOT + "gludio_merc_captain004.htm");
			return true;
		}

		if ("certificate_multisell".equals(commandKey))
		{
			try
			{
				final StringTokenizer tokenizer = new StringTokenizer(command);
				tokenizer.nextToken();
				if (tokenizer.countTokens() < 2)
				{
					return true;
				}

				final int certification = Integer.parseInt(tokenizer.nextToken());
				final int multisellId = Integer.parseInt(tokenizer.nextToken());
				if (player.getInventory().getInventoryItemCount(certification, -1) > 0)
				{
					MultisellData.getInstance().separateAndSend(multisellId, player, npc, false);
				}
				else
				{
					showHtml(player, npc, HTML_ROOT + "gludio_merc_captain-no-certificate.htm");
				}
				return true;
			}
			catch (Exception e)
			{
				return true;
			}
		}

		if ("territory_register".equals(commandKey))
		{
			if (state != DominionHtmlHelper.CaptainState.DEFAULT)
			{
				showStateHtml(player, npc, state);
				return true;
			}
			player.sendPacket(new ExShowDominionRegistry(player, dominionId));
			return true;
		}

		return false;
	}

	private boolean showCaptainMultisell(Player player, Npc npc, int dominionId)
	{
		final int badgeId = 13676 + dominionId;
		if (player.getInventory().getInventoryItemCount(badgeId, -1) < 1)
		{
			showHtml(player, npc, HTML_ROOT + "gludio_merc_captain-no-badge.htm");
			return true;
		}

		final int multisellId = DOMINION_MULTISELL_BASE_ID + dominionId;
		if (MultisellData.getInstance().getMultisell(multisellId) != null)
		{
			MultisellData.getInstance().separateAndSend(multisellId, player, npc, false);
		}
		else
		{
			showHtml(player, npc, HTML_ROOT + "gludio_merc_captain-no-supplies.htm");
		}
		return true;
	}

	private void showHtml(Player player, Npc npc, String path)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, path);
		player.sendPacket(html);
	}

	private void showStateHtml(Player player, Npc npc, DominionHtmlHelper.CaptainState state)
	{
		switch (state)
		{
			case LOW_LEVEL:
			{
				showHtml(player, npc, HTML_ROOT + "gludio_merc_captain026.htm");
				break;
			}
			case OWNER_SIEGE:
			{
				showHtml(player, npc, HTML_ROOT + "mercenary_captain-siege.htm");
				break;
			}
			case NON_OWNER_SIEGE:
			{
				showHtml(player, npc, HTML_ROOT + "mercenary_captain-siege.htm");
				break;
			}
			case OWNER_AVAILABLE:
			{
				showHtml(player, npc, HTML_ROOT + "gludio_merc_captain007.htm");
				break;
			}
			default:
			{
				showHtml(player, npc, HTML_ROOT + "gludio_merc_captain001.htm");
				break;
			}
		}
	}

	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}