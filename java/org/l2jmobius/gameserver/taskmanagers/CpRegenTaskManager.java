/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.taskmanagers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Manages CP regeneration/drain based on player movement state.
 * <ul>
 * <li>Running: -1 CP/sec</li>
 * <li>Walking (moving): +1 CP/sec</li>
 * <li>Standing idle (after 5 sec): +2 CP/sec</li>
 * <li>Sitting: +3 CP/sec</li>
 * </ul>
 * @author RoseVain
 */
public class CpRegenTaskManager implements Runnable
{
	private static final int CP_DRAIN_RUNNING = -1;
	private static final int CP_REGEN_WALKING = 1;
	private static final int CP_REGEN_STANDING = 2;
	private static final int CP_REGEN_SITTING = 3;
	
	private static final Map<Player, Long> PLAYERS = new ConcurrentHashMap<>();
	private static boolean _working = false;
	
	protected CpRegenTaskManager()
	{
		ThreadPool.schedulePriorityTaskAtFixedRate(this, 1000, 1000);
	}
	
	@Override
	public void run()
	{
		if (_working)
		{
			return;
		}
		_working = true;
		
		final long now = System.currentTimeMillis();
		for (Map.Entry<Player, Long> entry : PLAYERS.entrySet())
		{
			final Player player = entry.getKey();
			if ((player == null) || !player.isOnline() || player.isAlikeDead())
			{
				PLAYERS.remove(player);
				continue;
			}
			
			final double currentCp = player.getCurrentCp();
			final int maxCp = player.getMaxCp();
			final int cpChange = getCpChange(player, now, entry.getValue());
			
			if (cpChange < 0)
			{
				// Drain: only if CP > 0
				if (currentCp > 0)
				{
					player.getStatus().setCurrentCp(Math.max(0, currentCp + cpChange));
				}
			}
			else if (cpChange > 0)
			{
				// Regen: only if CP < maxCp
				if (currentCp < maxCp)
				{
					player.getStatus().setCurrentCp(Math.min(maxCp, currentCp + cpChange));
				}
			}
		}
		
		_working = false;
	}
	
	private static int getCpChange(Player player, long now, long stopMovingTime)
	{
		if (player.isSitting())
		{
			return CP_REGEN_SITTING;
		}
		if (player.isMoving())
		{
			if (player.isRunning())
			{
				return CP_DRAIN_RUNNING;
			}
			return CP_REGEN_WALKING;
		}
		// Standing still - regen starts immediately
		return CP_REGEN_STANDING;
	}
	
	/**
	 * Registers a player for CP management. Call on login/respawn.
	 * @param player the player to register
	 */
	public static void add(Player player)
	{
		PLAYERS.put(player, System.currentTimeMillis());
	}
	
	/**
	 * Unregisters a player from CP management. Call on logout/death.
	 * @param player the player to remove
	 */
	public static void remove(Player player)
	{
		PLAYERS.remove(player);
	}
	
	/**
	 * Notifies that the player has stopped moving. Resets idle timer.
	 * @param player the player who stopped
	 */
	public static void notifyStopMoving(Player player)
	{
		if (PLAYERS.containsKey(player))
		{
			PLAYERS.put(player, System.currentTimeMillis());
		}
	}
	
	public static CpRegenTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CpRegenTaskManager INSTANCE = new CpRegenTaskManager();
	}
}
