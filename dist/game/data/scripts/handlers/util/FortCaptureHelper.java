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
package handlers.util;

import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.LocationUtil;

public final class FortCaptureHelper
{
	private FortCaptureHelper()
	{
	}
	
	public static Fort validateTakeFort(Creature caster, Skill skill, WorldObject target, boolean sendMessage)
	{
		if (!caster.isPlayer())
		{
			return null;
		}
		
		final Player player = caster.asPlayer();
		final WorldObject actualTarget = target != null ? target : player.getTarget();
		if (player.isAlikeDead() || (player.getClan() == null) || player.isMounted() || !player.isCombatFlagEquipped())
		{
			sendRequirementMessage(player, skill, sendMessage);
			return null;
		}
		
		final Fort fort = FortManager.getInstance().getFort(player);
		if ((fort == null) || (fort.getResidenceId() <= 0) || !fort.getSiege().isInProgress() || (fort.getSiege().getAttackerClan(player.getClan()) == null))
		{
			sendRequirementMessage(player, skill, sendMessage);
			return null;
		}
		if (actualTarget != fort.getFlagPole())
		{
			if (sendMessage)
			{
				player.sendPacket(SystemMessageId.THE_TARGET_IS_NOT_A_FLAGPOLE_SO_A_FLAG_CANNOT_BE_DISPLAYED);
			}
			return null;
		}
		if (!LocationUtil.checkIfInRange(skill.getCastRange(), player, actualTarget, true))
		{
			if (sendMessage)
			{
				player.sendPacket(SystemMessageId.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_CANCELLED);
			}
			return null;
		}
		return fort;
	}
	
	private static void sendRequirementMessage(Player player, Skill skill, boolean sendMessage)
	{
		if (!sendMessage)
		{
			return;
		}
		final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_THE_REQUIREMENTS_ARE_NOT_MET);
		sm.addSkillName(skill);
		player.sendPacket(sm);
	}
}