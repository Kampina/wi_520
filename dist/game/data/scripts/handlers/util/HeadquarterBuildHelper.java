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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.model.zone.type.HqZone;
import org.l2jmobius.gameserver.network.SystemMessageId;

public final class HeadquarterBuildHelper
{
	private HeadquarterBuildHelper()
	{
	}
	
	public static boolean canBuildHeadquarter(Player player, boolean partyRequired)
	{
		if ((player == null) || player.isAlikeDead() || player.isCursedWeaponEquipped())
		{
			return false;
		}
		
		if (partyRequired)
		{
			if (!player.isInParty() || !player.getParty().isLeader(player))
			{
				player.sendMessage("Only party leader can build advanced headquarters.");
				return false;
			}
		}
		
		if (player.isInsideZone(ZoneId.PEACE) || (getHeadquarterZone(player) == null))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BUILD_HEADQUARTERS_HERE);
			return false;
		}
		
		if (hasExistingHeadquarter(player))
		{
			player.sendPacket(SystemMessageId.AN_OUTPOST_OR_HEADQUARTERS_CANNOT_BE_BUILT_BECAUSE_ONE_ALREADY_EXISTS);
			return false;
		}
		
		return true;
	}
	
	public static boolean canBuildOutpost(Player player)
	{
		if ((player == null) || player.isAlikeDead() || player.isCursedWeaponEquipped())
		{
			return false;
		}
		
		final Clan clan = player.getClan();
		if ((clan == null) || (clan.getCastleId() <= 0))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BUILD_HEADQUARTERS_HERE);
			return false;
		}
		
		final Castle castle = CastleManager.getInstance().getCastleById(clan.getCastleId());
		if ((castle == null) || !castle.getSiege().isInProgress() || (player.getSiegeState() == 0))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BUILD_HEADQUARTERS_HERE);
			return false;
		}
		
		if (getOutpostZone(player) == null)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BUILD_HEADQUARTERS_HERE);
			return false;
		}
		
		if (hasExistingHeadquarter(player))
		{
			player.sendPacket(SystemMessageId.AN_OUTPOST_OR_HEADQUARTERS_CANNOT_BE_BUILT_BECAUSE_ONE_ALREADY_EXISTS);
			return false;
		}
		
		return true;
	}
	
	private static boolean hasClanCombatContext(Player player)
	{
		if ((player.getClan() == null) || (player.getSiegeState() != 1) || !player.isInsideZone(ZoneId.HQ))
		{
			return false;
		}
		return true;
	}
	
	private static boolean hasExistingHeadquarter(Player player)
	{
		final Party party = player.getParty();
		return (getHeadquarterFlag(player) != null) || ((party != null) && (getHeadquarterFlag(party) != null));
	}
	
	private static HqZone getHeadquarterZone(Player player)
	{
		for (ZoneType zone : ZoneManager.getInstance().getZones(player))
		{
			if (!(zone instanceof HqZone))
			{
				continue;
			}
			final HqZone hqZone = (HqZone) zone;
			
			final Clan clan = player.getClan();
			if ((clan == null) || (player.getSiegeState() != 1))
			{
				continue;
			}
			
			final int castleId = getZoneIdValue(hqZone, "getCastleId", "_castleId");
			if (castleId > 0)
			{
				final Castle castle = CastleManager.getInstance().getCastleById(castleId);
				if ((castle != null) && castle.getSiege().isInProgress() && (castle.getSiege().getAttackerClan(clan) != null))
				{
					return hqZone;
				}
			}
			else
			{
				final int fortId = getZoneIdValue(hqZone, "getFortId", "_fortId");
				if (fortId > 0)
				{
					final Fort fort = FortManager.getInstance().getFortById(fortId);
					if ((fort != null) && fort.getSiege().isInProgress() && (fort.getSiege().getAttackerClan(clan) != null))
					{
						return hqZone;
					}
				}
			}
		}
		return null;
	}
	
	private static HqZone getOutpostZone(Player player)
	{
		final Clan clan = player.getClan();
		if ((clan == null) || !player.isInsideZone(ZoneId.HQ))
		{
			return null;
		}
		
		for (ZoneType zone : ZoneManager.getInstance().getZones(player))
		{
			if (!(zone instanceof HqZone))
			{
				continue;
			}
			final HqZone hqZone = (HqZone) zone;
			
			final int territoryId = getZoneIdValue(hqZone, "getTerritoryId", "_territoryId");
			if ((territoryId > 0) && ((territoryId - 80) == clan.getCastleId()))
			{
				return hqZone;
			}
		}
		return null;
	}

	private static Object getHeadquarterFlag(Object owner)
	{
		final Object flag = invokeNoArg(owner, "getHeadquarterFlag");
		if (flag != null)
		{
			return flag;
		}
		return getFieldValue(owner, "_headquarterFlag");
	}

	private static int getZoneIdValue(HqZone zone, String methodName, String fieldName)
	{
		final Object value = invokeNoArg(zone, methodName);
		if (value instanceof Number)
		{
			return ((Number) value).intValue();
		}

		final Object fieldValue = getFieldValue(zone, fieldName);
		if (fieldValue instanceof Number)
		{
			return ((Number) fieldValue).intValue();
		}
		return 0;
	}

	private static Object invokeNoArg(Object target, String methodName)
	{
		if (target == null)
		{
			return null;
		}

		try
		{
			final Method method = target.getClass().getMethod(methodName);
			return method.invoke(target);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	private static Object getFieldValue(Object target, String fieldName)
	{
		if (target == null)
		{
			return null;
		}

		Class<?> type = target.getClass();
		while (type != null)
		{
			try
			{
				final Field field = type.getDeclaredField(fieldName);
				field.setAccessible(true);
				return field.get(target);
			}
			catch (Exception e)
			{
				type = type.getSuperclass();
			}
		}
		return null;
	}
}