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
package org.l2jmobius.gameserver.model.olympiad;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadUserInfo;

/**
 * 9v9 clan-based Olympiad game.
 */
public class OlympiadGameClan extends AbstractOlympiadGame
{
	private final Participant[] _teamOne;
	private final Participant[] _teamTwo;
	
	protected OlympiadGameClan(int id, Participant[] teamOne, Participant[] teamTwo)
	{
		super(id);
		_teamOne = teamOne;
		_teamTwo = teamTwo;
		for (Participant par : _teamOne)
		{
			if (par.getPlayer() != null)
			{
				par.getPlayer().setOlympiadGameId(id);
			}
		}
		for (Participant par : _teamTwo)
		{
			if (par.getPlayer() != null)
			{
				par.getPlayer().setOlympiadGameId(id);
			}
		}
	}
	
	/**
	 * Create a clan match from two pre-formed teams.
	 * @param id stadium id
	 * @param teamOneIds list of objectIds for team 1
	 * @param teamTwoIds list of objectIds for team 2
	 * @return game instance or null if players are unavailable
	 */
	public static OlympiadGameClan createGame(int id, List<Integer> teamOneIds, List<Integer> teamTwoIds)
	{
		final Participant[] t1 = buildTeam(teamOneIds, 1);
		final Participant[] t2 = buildTeam(teamTwoIds, 2);
		if ((t1 == null) || (t2 == null))
		{
			return null;
		}
		return new OlympiadGameClan(id, t1, t2);
	}
	
	private static Participant[] buildTeam(List<Integer> ids, int side)
	{
		final Participant[] team = new Participant[ids.size()];
		for (int i = 0; i < ids.size(); i++)
		{
			final Player player = World.getInstance().getPlayer(ids.get(i));
			if ((player == null) || !player.isOnline())
			{
				return null;
			}
			team[i] = new Participant(player, side);
		}
		return team;
	}
	
	@Override
	public CompetitionType getType()
	{
		return CompetitionType.CLAN;
	}
	
	@Override
	public String[] getPlayerNames()
	{
		final String[] names = new String[_teamOne.length + _teamTwo.length];
		int idx = 0;
		for (Participant p : _teamOne)
		{
			names[idx++] = p.getName();
		}
		for (Participant p : _teamTwo)
		{
			names[idx++] = p.getName();
		}
		return names;
	}
	
	@Override
	public boolean containsParticipant(int playerId)
	{
		for (Participant p : _teamOne)
		{
			if (p.getObjectId() == playerId)
			{
				return true;
			}
		}
		for (Participant p : _teamTwo)
		{
			if (p.getObjectId() == playerId)
			{
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void sendOlympiadInfo(Creature creature)
	{
		for (Participant p : _teamOne)
		{
			creature.sendPacket(new ExOlympiadUserInfo(p));
		}
		for (Participant p : _teamTwo)
		{
			creature.sendPacket(new ExOlympiadUserInfo(p));
		}
	}
	
	@Override
	public void broadcastOlympiadInfo(OlympiadStadium stadium)
	{
		for (Participant p : _teamOne)
		{
			stadium.broadcastPacket(new ExOlympiadUserInfo(p));
		}
		for (Participant p : _teamTwo)
		{
			stadium.broadcastPacket(new ExOlympiadUserInfo(p));
		}
	}
	
	@Override
	protected void broadcastPacket(ServerPacket packet)
	{
		for (Participant p : _teamOne)
		{
			if (p.updatePlayer())
			{
				p.getPlayer().sendPacket(packet);
			}
		}
		for (Participant p : _teamTwo)
		{
			if (p.updatePlayer())
			{
				p.getPlayer().sendPacket(packet);
			}
		}
	}
	
	@Override
	protected boolean portPlayersToArena(List<Location> spawns, Instance instance)
	{
		boolean result = true;
		try
		{
			final Location baseBlue = spawns.get(0);
			final Location baseRed = spawns.get(spawns.size() / 2);
			final Location[] blueSpawns = generateTeamSpawns(baseBlue, _teamOne.length);
			final Location[] redSpawns = generateTeamSpawns(baseRed, _teamTwo.length);
			
			for (int i = 0; i < _teamOne.length; i++)
			{
				result &= portPlayerToArena(_teamOne[i], blueSpawns[i], _stadiumId, instance, OlympiadMode.BLUE);
			}
			for (int i = 0; i < _teamTwo.length; i++)
			{
				result &= portPlayerToArena(_teamTwo[i], redSpawns[i], _stadiumId, instance, OlympiadMode.RED);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "", e);
			return false;
		}
		return result;
	}
	
	/**
	 * Generate spread spawn positions in a 3x3 grid around the base location.
	 */
	private static Location[] generateTeamSpawns(Location base, int count)
	{
		final int spacing = 100;
		final int cols = 3;
		final Location[] locs = new Location[count];
		for (int i = 0; i < count; i++)
		{
			final int row = i / cols;
			final int col = i % cols;
			final int dx = (col - 1) * spacing;
			final int dy = (row - 1) * spacing;
			locs[i] = new Location(base.getX() + dx, base.getY() + dy, base.getZ());
		}
		return locs;
	}
	
	@Override
	protected boolean needBuffers()
	{
		return false;
	}
	
	@Override
	protected void removals()
	{
		if (_aborted)
		{
			return;
		}
		// Keep party intact for team play (removeParty = false).
		for (Participant p : _teamOne)
		{
			if (p.getPlayer() != null)
			{
				removals(p.getPlayer(), false);
			}
		}
		for (Participant p : _teamTwo)
		{
			if (p.getPlayer() != null)
			{
				removals(p.getPlayer(), false);
			}
		}
	}
	
	@Override
	protected boolean makeCompetitionStart()
	{
		if (!super.makeCompetitionStart())
		{
			return false;
		}
		for (Participant p : _teamOne)
		{
			if (p.getPlayer() == null)
			{
				return false;
			}
			p.getPlayer().setOlympiadStart(true);
			p.getPlayer().updateEffectIcons();
		}
		for (Participant p : _teamTwo)
		{
			if (p.getPlayer() == null)
			{
				return false;
			}
			p.getPlayer().setOlympiadStart(true);
			p.getPlayer().updateEffectIcons();
		}
		return true;
	}
	
	@Override
	protected void cleanEffects()
	{
		for (Participant p : _teamOne)
		{
			if ((p.getPlayer() != null) && !p.isDefaulted() && !p.isDisconnected() && (p.getPlayer().getOlympiadGameId() == _stadiumId))
			{
				cleanEffects(p.getPlayer());
			}
		}
		for (Participant p : _teamTwo)
		{
			if ((p.getPlayer() != null) && !p.isDefaulted() && !p.isDisconnected() && (p.getPlayer().getOlympiadGameId() == _stadiumId))
			{
				cleanEffects(p.getPlayer());
			}
		}
	}
	
	@Override
	protected void portPlayersBack()
	{
		for (Participant p : _teamOne)
		{
			if ((p.getPlayer() != null) && !p.isDefaulted() && !p.isDisconnected())
			{
				portPlayerBack(p.getPlayer());
			}
		}
		for (Participant p : _teamTwo)
		{
			if ((p.getPlayer() != null) && !p.isDefaulted() && !p.isDisconnected())
			{
				portPlayerBack(p.getPlayer());
			}
		}
	}
	
	@Override
	protected void playersStatusBack()
	{
		for (Participant p : _teamOne)
		{
			if ((p.getPlayer() != null) && !p.isDefaulted() && !p.isDisconnected() && (p.getPlayer().getOlympiadGameId() == _stadiumId))
			{
				playerStatusBack(p.getPlayer());
			}
		}
		for (Participant p : _teamTwo)
		{
			if ((p.getPlayer() != null) && !p.isDefaulted() && !p.isDisconnected() && (p.getPlayer().getOlympiadGameId() == _stadiumId))
			{
				playerStatusBack(p.getPlayer());
			}
		}
	}
	
	@Override
	protected void clearPlayers()
	{
		for (int i = 0; i < _teamOne.length; i++)
		{
			_teamOne[i].setPlayer(null);
			_teamOne[i] = null;
		}
		for (int i = 0; i < _teamTwo.length; i++)
		{
			_teamTwo[i].setPlayer(null);
			_teamTwo[i] = null;
		}
	}
	
	@Override
	protected void handleDisconnect(Player player)
	{
		for (Participant p : _teamOne)
		{
			if ((p != null) && (p.getObjectId() == player.getObjectId()))
			{
				p.setDisconnected(true);
				return;
			}
		}
		for (Participant p : _teamTwo)
		{
			if ((p != null) && (p.getObjectId() == player.getObjectId()))
			{
				p.setDisconnected(true);
				return;
			}
		}
	}
	
	@Override
	protected boolean checkBattleStatus()
	{
		if (_aborted)
		{
			return false;
		}
		// If all members of either team are offline/disconnected, battle cannot continue.
		if (isTeamDead(_teamOne) || isTeamDead(_teamTwo))
		{
			return false;
		}
		return true;
	}
	
	@Override
	protected boolean haveWinner()
	{
		if (!checkBattleStatus())
		{
			return true;
		}
		return isTeamAllDead(_teamOne) || isTeamAllDead(_teamTwo);
	}
	
	private boolean isTeamDead(Participant[] team)
	{
		for (Participant p : team)
		{
			if ((p != null) && (p.getPlayer() != null) && !p.isDisconnected())
			{
				return false;
			}
		}
		return true;
	}
	
	private boolean isTeamAllDead(Participant[] team)
	{
		for (Participant p : team)
		{
			if (p == null)
			{
				continue;
			}
			try
			{
				if ((p.getPlayer() != null) && (p.getPlayer().getOlympiadGameId() == _stadiumId) && !p.getPlayer().isDead() && !p.isDisconnected())
				{
					return false;
				}
			}
			catch (Exception e)
			{
				// Player crashed.
			}
		}
		return true;
	}
	
	private int countAlive(Participant[] team)
	{
		int count = 0;
		for (Participant p : team)
		{
			if (p == null)
			{
				continue;
			}
			try
			{
				if ((p.getPlayer() != null) && !p.getPlayer().isDead() && !p.isDisconnected() && (p.getPlayer().getOlympiadGameId() == _stadiumId))
				{
					count++;
				}
			}
			catch (Exception e)
			{
				// Ignored.
			}
		}
		return count;
	}
	
	@Override
	protected void validateWinner(OlympiadStadium stadium)
	{
		if (_aborted)
		{
			return;
		}
		
		final boolean teamOneAllOut = isTeamAllDead(_teamOne) || isTeamDead(_teamOne);
		final boolean teamTwoAllOut = isTeamAllDead(_teamTwo) || isTeamDead(_teamTwo);
		
		final int aliveOne = countAlive(_teamOne);
		final int aliveTwo = countAlive(_teamTwo);
		
		final int winPoints = 1;
		SystemMessage sm;
		
		// Determine winner.
		int winSide = 0; // 0 = draw
		if (teamTwoAllOut && !teamOneAllOut)
		{
			winSide = 1;
		}
		else if (teamOneAllOut && !teamTwoAllOut)
		{
			winSide = 2;
		}
		else if (!teamOneAllOut && !teamTwoAllOut)
		{
			// Timer expired — team with more alive wins.
			if (aliveOne > aliveTwo)
			{
				winSide = 1;
			}
			else if (aliveTwo > aliveOne)
			{
				winSide = 2;
			}
			// Equal alive = draw.
		}
		// Both all-out = draw.
		
		if (winSide == 1)
		{
			sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH);
			sm.addString(_teamOne[0].getName() + " (клан)");
			stadium.broadcastPacket(sm);
			
			for (Participant p : _teamOne)
			{
				p.updateStat(COMP_WON, 1);
				addPointsToParticipant(p, winPoints);
				if (p.getPlayer() != null)
				{
					rewardParticipant(p.getPlayer(), OlympiadConfig.OLYMPIAD_WINNER_REWARD);
				}
			}
			for (Participant p : _teamTwo)
			{
				p.updateStat(COMP_LOST, 1);
			}
		}
		else if (winSide == 2)
		{
			sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH);
			sm.addString(_teamTwo[0].getName() + " (клан)");
			stadium.broadcastPacket(sm);
			
			for (Participant p : _teamTwo)
			{
				p.updateStat(COMP_WON, 1);
				addPointsToParticipant(p, winPoints);
				if (p.getPlayer() != null)
				{
					rewardParticipant(p.getPlayer(), OlympiadConfig.OLYMPIAD_WINNER_REWARD);
				}
			}
			for (Participant p : _teamOne)
			{
				p.updateStat(COMP_LOST, 1);
			}
		}
		else
		{
			// Draw.
			stadium.broadcastPacket(new SystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE));
			for (Participant p : _teamOne)
			{
				p.updateStat(COMP_DRAWN, 1);
			}
			for (Participant p : _teamTwo)
			{
				p.updateStat(COMP_DRAWN, 1);
			}
		}
		
		// Update competition stats and daily counters for all participants.
		for (Participant p : _teamOne)
		{
			p.updateStat(COMP_DONE, 1);
			p.updateStat(COMP_DONE_WEEK, 1);
			Olympiad.getInstance().addClanCompetitionDoneDay(p.getObjectId());
		}
		for (Participant p : _teamTwo)
		{
			p.updateStat(COMP_DONE, 1);
			p.updateStat(COMP_DONE_WEEK, 1);
			Olympiad.getInstance().addClanCompetitionDoneDay(p.getObjectId());
		}
		
		// Build result packet lists for spectators.
		final List<OlympiadInfo> list1 = new ArrayList<>();
		for (Participant p : _teamOne)
		{
			final int pts = p.getStats().getInt(POINTS);
			list1.add(new OlympiadInfo(p.getName(), p.getClanName(), p.getClanId(), p.getBaseClass(), 0, pts, (winSide == 1) ? winPoints : 0));
		}
		final List<OlympiadInfo> list2 = new ArrayList<>();
		for (Participant p : _teamTwo)
		{
			final int pts = p.getStats().getInt(POINTS);
			list2.add(new OlympiadInfo(p.getName(), p.getClanName(), p.getClanId(), p.getBaseClass(), 0, pts, (winSide == 2) ? winPoints : 0));
		}
		
		stadium.broadcastPacket(new org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadMatchResult(
			winSide == 0, winSide, list1, list2, 0, 0, 0,
			list1.isEmpty() ? 0 : list1.get(0).getCurrentPoints(),
			list1.isEmpty() ? 0 : list1.get(0).getDiffPoints()));
	}
	
	@Override
	public boolean checkDefaulted()
	{
		SystemMessage reason;
		boolean defaulted = false;
		for (Participant p : _teamOne)
		{
			p.updatePlayer();
			reason = checkDefaulted(p.getPlayer());
			if (reason != null)
			{
				p.setDefaulted(true);
				defaulted = true;
			}
		}
		for (Participant p : _teamTwo)
		{
			p.updatePlayer();
			reason = checkDefaulted(p.getPlayer());
			if (reason != null)
			{
				p.setDefaulted(true);
				defaulted = true;
			}
		}
		return defaulted;
	}
	
	@Override
	protected void resetDamage()
	{
		// No per-player damage tracking in clan mode.
	}
	
	@Override
	protected void addDamage(Player player, int damage)
	{
		// No individual damage tracking in clan mode.
	}
	
	@Override
	protected int getDivider()
	{
		return OlympiadConfig.OLYMPIAD_DIVIDER_NON_CLASSED;
	}
	
	@Override
	protected void healPlayers()
	{
		for (Participant p : _teamOne)
		{
			final Player pl = p.getPlayer();
			if (pl != null)
			{
				pl.setCurrentCp(pl.getMaxCp());
				pl.setCurrentHp(pl.getMaxHp());
				pl.setCurrentMp(pl.getMaxMp());
			}
		}
		for (Participant p : _teamTwo)
		{
			final Player pl = p.getPlayer();
			if (pl != null)
			{
				pl.setCurrentCp(pl.getMaxCp());
				pl.setCurrentHp(pl.getMaxHp());
				pl.setCurrentMp(pl.getMaxMp());
			}
		}
	}
	
	@Override
	protected void untransformPlayers()
	{
		for (Participant p : _teamOne)
		{
			if ((p.getPlayer() != null) && p.getPlayer().isTransformed())
			{
				p.getPlayer().stopTransformation(true);
			}
		}
		for (Participant p : _teamTwo)
		{
			if ((p.getPlayer() != null) && p.getPlayer().isTransformed())
			{
				p.getPlayer().stopTransformation(true);
			}
		}
	}
	
	@Override
	public void makePlayersInvul()
	{
		for (Participant p : _teamOne)
		{
			if (p.getPlayer() != null)
			{
				p.getPlayer().setInvul(true);
			}
		}
		for (Participant p : _teamTwo)
		{
			if (p.getPlayer() != null)
			{
				p.getPlayer().setInvul(true);
			}
		}
	}
	
	@Override
	public void removePlayersInvul()
	{
		for (Participant p : _teamOne)
		{
			if (p.getPlayer() != null)
			{
				p.getPlayer().setInvul(false);
			}
		}
		for (Participant p : _teamTwo)
		{
			if (p.getPlayer() != null)
			{
				p.getPlayer().setInvul(false);
			}
		}
	}
}
