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
package ai.others.DominionNpcHtml;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

import handlers.util.DominionHtmlHelper;

public class DominionNpcHtml extends Script
{
	private static final int[] MERCENARY_CAPTAINS =
	{
		36481,
		36482,
		36483,
		36484,
		36485,
		36486,
		36487,
		36488,
		36489,
	};

	private static final int[] TERRITORY_MANAGERS =
	{
		36490,
		36491,
		36492,
		36493,
		36494,
		36495,
		36496,
		36497,
		36498,
	};

	private DominionNpcHtml()
	{
		addFirstTalkId(MERCENARY_CAPTAINS);
		addFirstTalkId(TERRITORY_MANAGERS);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if ((npc.getId() >= 36481) && (npc.getId() <= 36489))
		{
			DominionHtmlHelper.showMercenaryCaptainHtml(player, npc);
		}
		else
		{
			DominionHtmlHelper.showTerritoryManagerHtml(player, npc);
		}
		return null;
	}

	public static void main(String[] args)
	{
		new DominionNpcHtml();
	}
}