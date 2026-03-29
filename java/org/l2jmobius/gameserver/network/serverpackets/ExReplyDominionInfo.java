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
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class ExReplyDominionInfo extends ServerPacket
{
	private static final int TERRITORY_ID_OFFSET = 80;
	
	private final List<TerritoryInfo> _territories = new ArrayList<>();
	
	public ExReplyDominionInfo()
	{
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if (castle.getSiegeDate().getTimeInMillis() == 0)
			{
				continue;
			}

			final Clan ownerClan = ClanTable.getInstance().getClan(castle.getOwnerId());
			_territories.add(new TerritoryInfo(castle.getResidenceId() + TERRITORY_ID_OFFSET, castle.getName(), ownerClan != null ? ownerClan.getName() : "", (int) (castle.getSiegeDate().getTimeInMillis() / 1000L)));
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_REPLY_DOMINION_INFO.writeId(this, buffer);
		buffer.writeInt(_territories.size());
		for (TerritoryInfo territory : _territories)
		{
			buffer.writeInt(territory._id);
			buffer.writeString(territory._name);
			buffer.writeString(territory._ownerClanName);
			buffer.writeInt(0);
			buffer.writeInt(territory._startTime);
		}
	}
	
	private static class TerritoryInfo
	{
		private final int _id;
		private final String _name;
		private final String _ownerClanName;
		private final int _startTime;
		
		private TerritoryInfo(int id, String name, String ownerClanName, int startTime)
		{
			_id = id;
			_name = name;
			_ownerClanName = ownerClanName;
			_startTime = startTime;
		}
	}
}
