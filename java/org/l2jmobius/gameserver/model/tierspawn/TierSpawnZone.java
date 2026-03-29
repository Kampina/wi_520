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
package org.l2jmobius.gameserver.model.tierspawn;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * Holds WI-compatible configuration for one Tier Spawn Zone.
 * <p>
 * The same tracked monster IDs are counted separately for normal, blue and red champion kills.
 * Each tier has its own kill threshold, raid boss and spawn location.
 */
public class TierSpawnZone
{
	public static final int TIER_COUNT = 3;
	public static final int NO_ACTIVE_RAID_BOSS_TIER = -1;

	private final int _id;
	private final String _name;
	private final int _locaId;
	private final Set<Integer> _monsterIds;
	private final int[] _killThresholds;
	private final int[] _raidBossIds;
	private final int[][] _spawnLocations;
	
	private final int[] _killCounts = new int[TIER_COUNT];
	private volatile int _activeRaidBossTier = NO_ACTIVE_RAID_BOSS_TIER;
	
	public TierSpawnZone(int id, String name, int locaId, Set<Integer> monsterIds, int[] killThresholds, int[] raidBossIds, int[][] spawnLocations)
	{
		_id = id;
		_name = name;
		_locaId = locaId;
		_monsterIds = Collections.unmodifiableSet(monsterIds);
		_killThresholds = Arrays.copyOf(killThresholds, TIER_COUNT);
		_raidBossIds = Arrays.copyOf(raidBossIds, TIER_COUNT);
		_spawnLocations = new int[TIER_COUNT][4];
		for (int tierIndex = 0; tierIndex < TIER_COUNT; tierIndex++)
		{
			_spawnLocations[tierIndex] = Arrays.copyOf(spawnLocations[tierIndex], 4);
		}
	}
	
	// ── Getters ──────────────────────────────────────────────────────────────
	
	public int getId()
	{
		return _id;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int getLocaId()
	{
		return _locaId;
	}
	
	public Set<Integer> getMonsterIds()
	{
		return _monsterIds;
	}
	
	public boolean isMonsterTracked(int npcId)
	{
		return _monsterIds.contains(npcId);
	}
	
	public int getKillThreshold(int tierIndex)
	{
		return _killThresholds[tierIndex];
	}
	
	public int getRaidBossId(int tierIndex)
	{
		return _raidBossIds[tierIndex];
	}
	
	public int getSpawnX(int tierIndex)
	{
		return _spawnLocations[tierIndex][0];
	}
	
	public int getSpawnY(int tierIndex)
	{
		return _spawnLocations[tierIndex][1];
	}
	
	public int getSpawnZ(int tierIndex)
	{
		return _spawnLocations[tierIndex][2];
	}
	
	public int getSpawnHeading(int tierIndex)
	{
		return _spawnLocations[tierIndex][3];
	}
	
	public int getKillCount(int tierIndex)
	{
		return _killCounts[tierIndex];
	}
	
	public synchronized void setKillCount(int tierIndex, int count)
	{
		_killCounts[tierIndex] = Math.max(0, count);
	}
	
	public synchronized int incrementAndGetKills(int tierIndex)
	{
		return ++_killCounts[tierIndex];
	}
	
	public int getActiveRaidBossTier()
	{
		return _activeRaidBossTier;
	}
	
	public void setActiveRaidBossTier(int tierIndex)
	{
		_activeRaidBossTier = tierIndex;
	}
	
	public synchronized void restoreState(int activeRaidBossTier, int[] killCounts)
	{
		_activeRaidBossTier = activeRaidBossTier;
		for (int tierIndex = 0; tierIndex < TIER_COUNT; tierIndex++)
		{
			_killCounts[tierIndex] = (tierIndex < killCounts.length) ? Math.max(0, killCounts[tierIndex]) : 0;
		}
	}
	
	public synchronized void reset()
	{
		Arrays.fill(_killCounts, 0);
		_activeRaidBossTier = NO_ACTIVE_RAID_BOSS_TIER;
	}
}
