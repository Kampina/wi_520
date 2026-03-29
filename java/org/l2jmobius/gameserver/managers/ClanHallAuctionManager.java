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
package org.l2jmobius.gameserver.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.data.xml.ClanHallData;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.residences.Bidder;
import org.l2jmobius.gameserver.model.residences.ClanHall;
import org.l2jmobius.gameserver.model.residences.ClanHallAuction;

/**
 * @author Sdw
 */
public class ClanHallAuctionManager
{
	private static final Logger LOGGER = Logger.getLogger(ClanHallAuctionManager.class.getName());
	private static final long MINIMUM_AUCTION_DURATION = TimeUnit.MINUTES.toMillis(1);
	private static final long AUCTION_CHECK_INTERVAL = TimeUnit.MINUTES.toMillis(1);
	private static final Map<Integer, ClanHallAuction> AUCTIONS = new ConcurrentHashMap<>();
	
	private final long _auctionDuration;
	
	protected ClanHallAuctionManager()
	{
		_auctionDuration = Math.max(MINIMUM_AUCTION_DURATION, FeatureConfig.CLAN_HALL_AUCTION_TIME);
		restoreAuctions();
		startFreeHallAuctions();
		ThreadPool.scheduleAtFixedRate(this::processAuctions, AUCTION_CHECK_INTERVAL, AUCTION_CHECK_INTERVAL);
	}

	private void restoreAuctions()
	{
		final long currentTime = System.currentTimeMillis();
		final List<Integer> restoredClanHalls = new ArrayList<>();
		final List<Integer> finalizedClanHalls = new ArrayList<>();
		for (ClanHall clanHall : ClanHallData.getInstance().getClanHalls())
		{
			if (clanHall == null)
			{
				continue;
			}

			final long auctionEndTime = clanHall.getAuctionEndTime();
			if (auctionEndTime <= 0)
			{
				continue;
			}

			final ClanHallAuction auction = new ClanHallAuction(clanHall.getResidenceId(), auctionEndTime);
			if (auctionEndTime <= currentTime)
			{
				auction.finalizeAuctions();
				finalizedClanHalls.add(clanHall.getResidenceId());
				continue;
			}

			if (AUCTIONS.putIfAbsent(clanHall.getResidenceId(), auction) == null)
			{
				restoredClanHalls.add(clanHall.getResidenceId());
			}
		}
		if (!restoredClanHalls.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Restored active clan hall auctions for clan halls " + restoredClanHalls + ".");
		}
		if (!finalizedClanHalls.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Finalized expired clan hall auctions for clan halls " + finalizedClanHalls + " during startup restore.");
		}
	}
	
	private void processAuctions()
	{
		final long currentTime = System.currentTimeMillis();
		for (Entry<Integer, ClanHallAuction> entry : new ArrayList<>(AUCTIONS.entrySet()))
		{
			final ClanHallAuction auction = entry.getValue();
			if ((auction != null) && (auction.getAuctionEndTime() <= currentTime) && AUCTIONS.remove(entry.getKey(), auction))
			{
				auction.finalizeAuctions();
				LOGGER.info(getClass().getSimpleName() + ": Clan Hall Auction has ended for clan hall " + entry.getKey() + ".");
			}
		}
		startFreeHallAuctions();
	}
	
	private void startFreeHallAuctions()
	{
		final List<Integer> startedClanHalls = new ArrayList<>();
		for (ClanHall clanHall : ClanHallData.getInstance().getFreeAuctionableHall())
		{
			if (clanHall != null)
			{
				final long auctionEndTime = System.currentTimeMillis() + _auctionDuration;
				if (AUCTIONS.putIfAbsent(clanHall.getResidenceId(), new ClanHallAuction(clanHall.getResidenceId(), auctionEndTime)) == null)
				{
					clanHall.setAuctionEndTime(auctionEndTime);
					startedClanHalls.add(clanHall.getResidenceId());
				}
			}
		}
		if (!startedClanHalls.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Clan Hall Auction has started for clan halls " + startedClanHalls + ".");
		}
	}
	
	public ClanHallAuction getClanHallAuctionById(int clanHallId)
	{
		return AUCTIONS.get(clanHallId);
	}
	
	public ClanHallAuction getClanHallAuctionByClan(Clan clan)
	{
		for (ClanHallAuction auction : AUCTIONS.values())
		{
			if (auction.getBids().containsKey(clan.getId()))
			{
				return auction;
			}
		}
		return null;
	}
	
	public boolean checkForClanBid(int clanHallId, Clan clan)
	{
		for (Entry<Integer, ClanHallAuction> auction : AUCTIONS.entrySet())
		{
			if ((auction.getKey() != clanHallId) && auction.getValue().getBids().containsKey(clan.getId()))
			{
				return true;
			}
		}
		return false;
	}
	
	public long getRemainingTime()
	{
		return AUCTIONS.values().stream().mapToLong(ClanHallAuction::getRemainingTime).filter(time -> time > 0).min().orElse(0);
	}
	
	public long getCurrentAuctionEndTime()
	{
		return System.currentTimeMillis() + getRemainingTime();
	}
	
	public boolean registerOwnedClanHallAuction(ClanHall clanHall)
	{
		if ((clanHall == null) || AUCTIONS.containsKey(clanHall.getResidenceId()))
		{
			return false;
		}
		final long auctionEndTime = System.currentTimeMillis() + _auctionDuration;
		AUCTIONS.put(clanHall.getResidenceId(), new ClanHallAuction(clanHall.getResidenceId(), auctionEndTime));
		clanHall.setAuctionEndTime(auctionEndTime);
		LOGGER.info(getClass().getSimpleName() + ": Clan Hall Auction has started for owned clan hall " + clanHall.getResidenceId() + ".");
		return true;
	}
	
	public boolean cancelOwnedClanHallAuction(ClanHall clanHall)
	{
		if (clanHall == null)
		{
			return false;
		}
		final ClanHallAuction auction = AUCTIONS.remove(clanHall.getResidenceId());
		if (auction == null)
		{
			return false;
		}
		auction.clearBidsAndRefund();
		return true;
	}
	
	public List<ClanHall> getAuctionableClanHalls()
	{
		final List<ClanHall> result = new ArrayList<>();
		for (Integer clanHallId : AUCTIONS.keySet())
		{
			final ClanHall clanHall = ClanHallData.getInstance().getClanHallById(clanHallId);
			if (clanHall != null)
			{
				result.add(clanHall);
			}
		}
		Collections.sort(result, (a, b) -> Integer.compare(a.getResidenceId(), b.getResidenceId()));
		return result;
	}
	
	public boolean selectAuctionWinner(int clanHallId, int clanId)
	{
		final ClanHallAuction auction = AUCTIONS.get(clanHallId);
		if ((auction == null) || (auction.getRemainingTime() <= 0))
		{
			return false;
		}
		final Bidder winner = auction.getBids().get(clanId);
		if (winner == null)
		{
			return false;
		}
		auction.finalizeAuctionWithWinner(winner);
		AUCTIONS.remove(clanHallId);
		return true;
	}
	
	public static ClanHallAuctionManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ClanHallAuctionManager INSTANCE = new ClanHallAuctionManager();
	}
}
