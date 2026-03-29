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
package org.l2jmobius.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanAccess;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExReplyRegisterDominion;

/**
 * @author Mobius
 */
public class RequestExRegisterDominion extends ClientPacket
{
	private static final Logger LOGGER = Logger.getLogger(RequestExRegisterDominion.class.getName());
	private static final String INSERT_PLAYER_REGISTRATION = "INSERT INTO siege_players (residence_id, object_id, clan_id) VALUES (?, ?, ?)";
	private static final String DELETE_PLAYER_REGISTRATION = "DELETE FROM siege_players WHERE residence_id=? AND object_id=?";
	private static final String SELECT_PLAYER_REGISTRATION = "SELECT 1 FROM siege_players WHERE residence_id=? AND object_id=?";
	private static final String SELECT_PLAYER_OTHER_REGISTRATION = "SELECT 1 FROM siege_players WHERE object_id=? AND residence_id<>?";

	private static final int MIN_DOMINION_ID = 81;
	private static final int MAX_DOMINION_ID = 89;
	private static final int TERRITORY_ID_OFFSET = 80;

	private int _dominionId;
	private boolean _isClanRegistration;
	private boolean _isRegistration;
	
	@Override
	protected void readImpl()
	{
		_dominionId = readInt();
		_isClanRegistration = readInt() == 1;
		_isRegistration = readInt() == 1;
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}

		player.sendMessage("Ручная регистрация на Битву за Земли отключена.");
		player.sendPacket(SystemMessageId.IT_IS_NOT_A_TERRITORY_WAR_REGISTRATION_PERIOD_SO_A_REQUEST_CANNOT_BE_MADE_AT_THIS_TIME);
		sendFailure(player);
	}

	private boolean registerClan(Player player, Clan clan, Castle castle)
	{
		if (castle.getSiege().checkIsAttacker(clan) || castle.getSiege().checkIsDefender(clan) || castle.getSiege().checkIsDefenderWaiting(clan))
		{
			return true;
		}

		castle.getSiege().registerDefender(player, true);
		return castle.getSiege().checkIsAttacker(clan) || castle.getSiege().checkIsDefender(clan) || castle.getSiege().checkIsDefenderWaiting(clan);
	}

	private boolean unregisterClan(Clan clan, Castle castle)
	{
		if (!castle.getSiege().checkIsAttacker(clan) && !castle.getSiege().checkIsDefender(clan) && !castle.getSiege().checkIsDefenderWaiting(clan))
		{
			return false;
		}

		castle.getSiege().removeSiegeClan(clan);
		return !castle.getSiege().checkIsAttacker(clan) && !castle.getSiege().checkIsDefender(clan) && !castle.getSiege().checkIsDefenderWaiting(clan);
	}

	private boolean registerPlayer(Player player, Castle castle)
	{
		if (player.getParty() != null)
		{
			player.sendPacket(SystemMessageId.A_CHARACTER_WHICH_IS_A_MEMBER_OF_A_PARTY_CANNOT_FILE_A_MERCENARY_REQUEST);
			return false;
		}

		if (player.isMercenary())
		{
			player.sendPacket(SystemMessageId.THE_CHARACTER_IS_PARTICIPATING_AS_A_MERCENARY);
			return false;
		}

		final Clan clan = player.getClan();
		if ((clan != null) && (clan.getCastleId() <= 0))
		{
			for (Castle entry : CastleManager.getInstance().getCastles())
			{
				if (entry.getSiege().checkIsAttacker(clan) || entry.getSiege().checkIsDefender(clan) || entry.getSiege().checkIsDefenderWaiting(clan))
				{
					player.sendPacket(SystemMessageId.ATTACKERS_AND_DEFENDERS_CANNOT_BE_RECRUITED_AS_MERCENARIES);
					return false;
				}
			}
		}

		if (isPlayerRegisteredInDifferentTerritory(player.getObjectId(), castle.getResidenceId()))
		{
			player.sendPacket(SystemMessageId.YOU_VE_ALREADY_REQUESTED_A_TERRITORY_WAR_IN_ANOTHER_TERRITORY_ELSEWHERE);
			return false;
		}

		if (isPlayerRegisteredInTerritory(player.getObjectId(), castle.getResidenceId()))
		{
			return true;
		}

		final int clanId = player.getClanId();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_PLAYER_REGISTRATION))
		{
			statement.setInt(1, castle.getResidenceId());
			statement.setInt(2, player.getObjectId());
			statement.setInt(3, clanId);
			return statement.executeUpdate() > 0;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not register territory player.", e);
			return false;
		}
	}

	private boolean unregisterPlayer(Player player, Castle castle)
	{
		if (!isPlayerRegisteredInTerritory(player.getObjectId(), castle.getResidenceId()))
		{
			player.sendPacket(SystemMessageId.NOT_IN_MERCENARY_MODE);
			return false;
		}

		if (!isPlayerCancellationAllowed(castle))
		{
			player.sendPacket(SystemMessageId.YOU_CAN_REVOKE_YOUR_PARTICIPATION_REQUEST_FOR_THE_TERRITORY_WAR_ONLY_DURING_THE_CANCELLATION_WINDOW_20_MIN_BEFORE_THE_WAR_STARTS);
			return false;
		}

		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_PLAYER_REGISTRATION))
		{
			statement.setInt(1, castle.getResidenceId());
			statement.setInt(2, player.getObjectId());
			final boolean success = statement.executeUpdate() > 0;
			if (success)
			{
				player.sendPacket(SystemMessageId.YOU_VE_REVOKED_YOUR_PARTICIPATION_REQUEST_FOR_THE_TERRITORY_WAR);
			}
			return success;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not unregister territory player.", e);
			return false;
		}
	}

	private boolean isPlayerRegisteredInTerritory(int objectId, int residenceId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_PLAYER_REGISTRATION))
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
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not check territory player registration.", e);
			return false;
		}
	}

	private boolean isPlayerRegisteredInDifferentTerritory(int objectId, int residenceId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_PLAYER_OTHER_REGISTRATION))
		{
			statement.setInt(1, objectId);
			statement.setInt(2, residenceId);
			try (ResultSet rs = statement.executeQuery())
			{
				return rs.next();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not check cross territory registration.", e);
			return false;
		}
	}

	private boolean isRegistrationAllowed(Castle castle)
	{
		if (castle.getSiege().isInProgress() || castle.getSiege().isRegistrationOver() || castle.isTimeRegistrationOver())
		{
			return false;
		}

		final long timeUntilSiege = castle.getSiegeDate().getTimeInMillis() - System.currentTimeMillis();
		return timeUntilSiege > FeatureConfig.DOMINION_REGISTRATION_CLOSE_WINDOW_MILLIS;
	}

	private boolean isPlayerCancellationAllowed(Castle castle)
	{
		if (castle.getSiege().isInProgress())
		{
			return false;
		}

		final long timeUntilSiege = castle.getSiegeDate().getTimeInMillis() - System.currentTimeMillis();
		return (timeUntilSiege > 0) && (timeUntilSiege <= FeatureConfig.DOMINION_BATTLEFIELD_START_MILLIS);
	}

	private void sendSuccess(Player player)
	{
		int clanCount = 0;
		int playerCount = 0;
		final Castle castle = CastleManager.getInstance().getCastleById(_dominionId - TERRITORY_ID_OFFSET);
		if ((castle != null) && (castle.getSiege() != null))
		{
			clanCount = Math.max(0, castle.getSiege().getDefenderClans().size() + castle.getSiege().getDefenderWaitingClans().size());
			playerCount = getPlayerRegistrationCount(castle.getResidenceId());
		}

		if (!_isClanRegistration && _isRegistration)
		{
			player.sendPacket(SystemMessageId.YOU_VE_REQUESTED_PARTICIPATION_A_TERRITORY_WAR_AS_A_MERCENARY);
		}

		player.sendPacket(new ExReplyRegisterDominion(_dominionId, _isClanRegistration, _isRegistration, true, clanCount, playerCount));
	}

	private void sendFailure(Player player)
	{
		int clanCount = 0;
		int playerCount = 0;
		final Castle castle = CastleManager.getInstance().getCastleById(_dominionId - TERRITORY_ID_OFFSET);
		if ((castle != null) && (castle.getSiege() != null))
		{
			clanCount = Math.max(0, castle.getSiege().getDefenderClans().size() + castle.getSiege().getDefenderWaitingClans().size());
			playerCount = getPlayerRegistrationCount(castle.getResidenceId());
		}

		player.sendPacket(new ExReplyRegisterDominion(_dominionId, _isClanRegistration, _isRegistration, false, clanCount, playerCount));
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
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not count territory player registrations.", e);
		}

		return 0;
	}
}
