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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.sql.DominionData;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class ExShowDominionRegistry extends ServerPacket
{
	private static final Logger LOGGER = Logger.getLogger(ExShowDominionRegistry.class.getName());
	private static final int TERRITORY_ID_OFFSET = 80;

	private final int _dominionId;
	private final String _ownerClanName;
	private final String _ownerLeaderName;
	private final String _ownerAllyName;
	private final int _clanReq;
	private final int _mercReq;
	private final int _warTime;
	private final int _currentTime;
	private final boolean _registeredAsPlayer;
	private final boolean _registeredAsClan;
	private final List<TerritoryFlagsInfo> _flags = new ArrayList<>();

	public ExShowDominionRegistry(Player player, int dominionId)
	{
		_dominionId = dominionId;
		_currentTime = (int) (System.currentTimeMillis() / 1000L);
		final Clan playerClan = player.getClan();

		final Castle castle = CastleManager.getInstance().getCastleById(dominionId - TERRITORY_ID_OFFSET);
		if (castle != null)
		{
			final Clan owner = ClanTable.getInstance().getClan(castle.getOwnerId());
			_ownerClanName = owner != null ? owner.getName() : "";
			_ownerLeaderName = owner != null ? owner.getLeaderName() : "";
			_ownerAllyName = owner != null ? owner.getDisplayAllyName() : "";
			_warTime = (int) (castle.getSiegeDate().getTimeInMillis() / 1000L);
			_mercReq = getPlayerRegistrationCount(castle.getResidenceId());
			_clanReq = Math.max(0, castle.getSiege().getDefenderClans().size() + castle.getSiege().getDefenderWaitingClans().size());
			_registeredAsClan = (playerClan != null) && (castle.getSiege().checkIsAttacker(playerClan) || castle.getSiege().checkIsDefender(playerClan) || castle.getSiege().checkIsDefenderWaiting(playerClan));
			_registeredAsPlayer = isPlayerRegistered(castle.getResidenceId(), player.getObjectId());
		}
		else
		{
			_ownerClanName = "";
			_ownerLeaderName = "";
			_ownerAllyName = "";
			_warTime = 0;
			_registeredAsPlayer = false;
			_registeredAsClan = false;
			_clanReq = 0;
			_mercReq = 0;
		}

		final Map<Integer, int[]> dominionFlags = DominionData.getInstance().loadDominionFlags();
		for (Castle entry : CastleManager.getInstance().getCastles())
		{
			final int territoryId = entry.getResidenceId() + TERRITORY_ID_OFFSET;
			_flags.add(new TerritoryFlagsInfo(territoryId, dominionFlags.getOrDefault(territoryId, new int[]
			{
				territoryId
			})));
		}
	}

	private int getPlayerRegistrationCount(int residenceId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT COUNT(*) FROM siege_players WHERE residence_id=?"))
		{
			statement.setInt(1, residenceId);
			try (ResultSet rs = statement.executeQuery())
			{
				if (rs.next())
				{
					return rs.getInt(1);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not load territory mercenary count.", e);
		}

		return 0;
	}

	private boolean isPlayerRegistered(int residenceId, int objectId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT 1 FROM siege_players WHERE residence_id=? AND object_id=?"))
		{
			statement.setInt(1, residenceId);
			statement.setInt(2, objectId);
			try (ResultSet rs = statement.executeQuery())
			{
				return rs.next();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not load territory mercenary status.", e);
		}

		return false;
	}

	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_SHOW_DOMINION_REGISTRY.writeId(this, buffer);
		buffer.writeInt(_dominionId);
		buffer.writeString(_ownerClanName);
		buffer.writeString(_ownerLeaderName);
		buffer.writeString(_ownerAllyName);
		buffer.writeInt(_clanReq);
		buffer.writeInt(_mercReq);
		buffer.writeInt(_warTime);
		buffer.writeInt(_currentTime);
		buffer.writeInt(_registeredAsClan ? 1 : 0);
		buffer.writeInt(_registeredAsPlayer ? 1 : 0);
		buffer.writeInt(1);
		buffer.writeInt(_flags.size());
		for (TerritoryFlagsInfo info : _flags)
		{
			buffer.writeInt(info._id);
			buffer.writeInt(info._flags.length);
			for (int flag : info._flags)
			{
				buffer.writeInt(flag);
			}
		}
	}

	private static class TerritoryFlagsInfo
	{
		private final int _id;
		private final int[] _flags;

		private TerritoryFlagsInfo(int id, int[] flags)
		{
			_id = id;
			_flags = flags;
		}
	}
}