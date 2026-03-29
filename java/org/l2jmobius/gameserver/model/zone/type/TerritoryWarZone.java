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
package org.l2jmobius.gameserver.model.zone.type;

import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Territory war combat zone.
 * @author Mobius
 */
public class TerritoryWarZone extends ZoneType
{
	private int _territoryId;

	public TerritoryWarZone(int id)
	{
		super(id);
	}

	@Override
	public void setParameter(String name, String value)
	{
		if ("territoryId".equals(name))
		{
			_territoryId = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}

	public int getTerritoryId()
	{
		return _territoryId;
	}

	private boolean isActive()
	{
		org.l2jmobius.gameserver.model.siege.Castle castle = org.l2jmobius.gameserver.managers.CastleManager.getInstance().getCastleById(_territoryId - 80);
		return (castle != null) && (castle.getSiege() != null) && castle.getSiege().isInProgress();
	}

	@Override
	protected void onEnter(Creature creature)
	{
		if (isActive())
		{
			if (creature.isPlayer() && !creature.isInsideZone(ZoneId.PVP))
			{
				creature.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_A_COMBAT_ZONE);
			}

			creature.setInsideZone(ZoneId.PVP, true);
			creature.setInsideZone(ZoneId.SIEGE, true);
		}
	}

	@Override
	protected void onExit(Creature creature)
	{
		if (isActive())
		{
			creature.setInsideZone(ZoneId.PVP, false);
			creature.setInsideZone(ZoneId.SIEGE, false);
			if (creature.isPlayer() && !creature.isInsideZone(ZoneId.PVP))
			{
				creature.sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
			}
		}

		if (!creature.isPlayer())
		{
			return;
		}

		final Player player = creature.asPlayer();
		final org.l2jmobius.gameserver.model.item.instance.Item ward = SiegeManager.getInstance().getTerritoryWard(player);
		if (ward != null)
		{
			SiegeManager.getInstance().dropTerritoryWard(player, true, false);
			
			org.l2jmobius.gameserver.model.MapRegion region = org.l2jmobius.gameserver.managers.MapRegionManager.getInstance().getMapRegion(player);
			org.l2jmobius.gameserver.model.Location returnLocation = null;
			if ((region != null) && (region.getCastleId() > 0))
			{
				int ownerTerritoryId = region.getCastleId() + 80;
				int wardTerritoryId = SiegeManager.getInstance().getTerritoryIdByWardId(ward.getId());
				returnLocation = org.l2jmobius.gameserver.data.xml.ParserSiegeEventData.getInstance().getWardLocation(ownerTerritoryId, wardTerritoryId);
			}
			SiegeManager.getInstance().scheduleTerritoryWardReturnLocal(ward, returnLocation);
		}
	}
}
