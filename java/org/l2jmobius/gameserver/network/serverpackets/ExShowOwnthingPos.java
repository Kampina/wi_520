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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.sql.DominionData;
import org.l2jmobius.gameserver.data.xml.ParserSiegeEventData;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * Displays ward positions during territory war.
 * @author Mobius
 */
public class ExShowOwnthingPos extends ServerPacket
{
	private static final int TERRITORY_ID_OFFSET = 80;

	private final List<WardInfo> _wardList;

	public ExShowOwnthingPos(Player activePlayer)
	{
		_wardList = new ArrayList<>();
		final Map<Integer, Location> carriedWardLocations = new HashMap<>();
		for (Player player : World.getInstance().getPlayers())
		{
			final Item ward = SiegeManager.getInstance().getTerritoryWard(player);
			if (ward == null)
			{
				continue;
			}

			final int dominionId = SiegeManager.getInstance().getTerritoryIdByWardId(ward.getId());
			if (dominionId <= 0)
			{
				continue;
			}

			carriedWardLocations.put(dominionId, new Location(player));
		}

		final Map<Integer, int[]> dominionFlags = DominionData.getInstance().loadDominionFlags();
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if (castle.getSiegeDate().getTimeInMillis() == 0)
			{
				continue;
			}

			final int territoryId = castle.getResidenceId() + TERRITORY_ID_OFFSET;
			for (int wardTerritoryId : dominionFlags.getOrDefault(territoryId, new int[]
			{
				territoryId
			}))
			{
				final Location location = carriedWardLocations.getOrDefault(wardTerritoryId, ParserSiegeEventData.getInstance().getWardLocation(territoryId, wardTerritoryId));
				if (location != null)
				{
					_wardList.add(new WardInfo(wardTerritoryId, location.getX(), location.getY(), location.getZ()));
				}
			}
		}
	}

	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_SHOW_OWNTHING_POS.writeId(this, buffer);
		buffer.writeInt(_wardList.size());
		for (WardInfo ward : _wardList)
		{
			buffer.writeInt(ward._dominionId);
			buffer.writeInt(ward._x);
			buffer.writeInt(ward._y);
			buffer.writeInt(ward._z);
		}
	}

	private static class WardInfo
	{
		private final int _dominionId;
		private final int _x;
		private final int _y;
		private final int _z;

		private WardInfo(int dominionId, int x, int y, int z)
		{
			_dominionId = dominionId;
			_x = x;
			_y = y;
			_z = z;
		}
	}
}