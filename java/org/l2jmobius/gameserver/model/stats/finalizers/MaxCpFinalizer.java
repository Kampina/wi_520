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
package org.l2jmobius.gameserver.model.stats.finalizers;

import java.util.OptionalDouble;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.stats.IStatFunction;
import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * Fixed max CP by class archetype. Cannot be modified by buffs, items, or CON.
 * @author UnAfraid, RoseVain
 */
public class MaxCpFinalizer implements IStatFunction
{
	private static final int MAX_CP_MAGE_PRIEST = 120;
	private static final int MAX_CP_ROGUE_ARCHER = 80;
	private static final int MAX_CP_WARRIOR_KNIGHT = 100;
	
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		final Player player = creature.asPlayer();
		if (player != null)
		{
			return getFixedMaxCp(player.getPlayerClass());
		}
		
		return creature.getTemplate().getBaseValue(stat, 0);
	}
	
	private static int getFixedMaxCp(PlayerClass playerClass)
	{
		if (playerClass.isMage() || isForcedMage(playerClass))
		{
			return MAX_CP_MAGE_PRIEST;
		}
		if (isRogueOrArcher(playerClass))
		{
			return MAX_CP_ROGUE_ARCHER;
		}
		return MAX_CP_WARRIOR_KNIGHT;
	}
	
	private static boolean isForcedMage(PlayerClass pc)
	{
		return pc == PlayerClass.SOUL_FINDER || pc.childOf(PlayerClass.SOUL_FINDER);
	}
	
	private static boolean isRogueOrArcher(PlayerClass pc)
	{
		return pc == PlayerClass.ROGUE || pc.childOf(PlayerClass.ROGUE)
			|| pc == PlayerClass.ELVEN_SCOUT || pc.childOf(PlayerClass.ELVEN_SCOUT)
			|| pc == PlayerClass.ASSASSIN || pc.childOf(PlayerClass.ASSASSIN)
			|| pc == PlayerClass.SCAVENGER || pc.childOf(PlayerClass.SCAVENGER)
			|| pc == PlayerClass.WARDER || pc.childOf(PlayerClass.WARDER)
			|| pc == PlayerClass.SYLPH_GUNNER || pc.childOf(PlayerClass.SYLPH_GUNNER)
			|| pc == PlayerClass.ASSASSIN_MALE_0 || pc.childOf(PlayerClass.ASSASSIN_MALE_0)
			|| pc == PlayerClass.ASSASSIN_FEMALE_0 || pc.childOf(PlayerClass.ASSASSIN_FEMALE_0)
			|| pc == PlayerClass.WARG_0 || pc.childOf(PlayerClass.WARG_0)
			|| pc == PlayerClass.BLOOD_ROSE_0 || pc.childOf(PlayerClass.BLOOD_ROSE_0)
			|| pc == PlayerClass.ORC_MONK || pc.childOf(PlayerClass.ORC_MONK);
	}
}
