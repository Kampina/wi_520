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
package org.l2jmobius.gameserver.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.xml.TierSpawnData;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.tierspawn.TierSpawnZone;
import org.l2jmobius.gameserver.model.zone.ZoneType;

/**
 * WI-compatible raid boss spawn manager.
 * <p>
 * The same tracked monster IDs are counted separately for normal, blue and red champion kills.
 * Each tier has its own threshold and may spawn its own raid boss.
 */
public class TierSpawnManager
{
	private static final Logger LOGGER = Logger.getLogger(TierSpawnManager.class.getName());
	private static final int NORMAL_TIER_INDEX = 0;
	private static final int BLUE_TIER_INDEX = 1;
	private static final int RED_TIER_INDEX = 2;
	private static final String STATE_KEY_PREFIX = "TIER_SPAWN_STATE_";
	
	private final Map<Integer, List<Integer>> _trackedNpcToZones = new ConcurrentHashMap<>();
	private final Map<Integer, List<Integer>> _raidBossNpcToZones = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _activeRaidBossObjectIds = new ConcurrentHashMap<>();
	
	protected TierSpawnManager()
	{
		buildLookupMaps();
		restoreStates();
		LOGGER.info(getClass().getSimpleName() + ": Initialized with " + TierSpawnData.getInstance().getZones().size() + " zones.");
	}
	
	private void buildLookupMaps()
	{
		_trackedNpcToZones.clear();
		_raidBossNpcToZones.clear();
		_activeRaidBossObjectIds.clear();
		
		for (TierSpawnZone zone : TierSpawnData.getInstance().getZones().values())
		{
			for (int npcId : zone.getMonsterIds())
			{
				_trackedNpcToZones.computeIfAbsent(npcId, key -> new ArrayList<>()).add(zone.getId());
			}
			for (int tierIndex = 0; tierIndex < TierSpawnZone.TIER_COUNT; tierIndex++)
			{
				final int raidBossId = zone.getRaidBossId(tierIndex);
				if (raidBossId > 0)
				{
					_raidBossNpcToZones.computeIfAbsent(raidBossId, key -> new ArrayList<>()).add(zone.getId());
				}
			}
		}
	}
	
	public void onNpcKilled(Attackable npc)
	{
		if (handleRaidBossDeath(npc))
		{
			return;
		}
		
		final List<Integer> zoneIds = _trackedNpcToZones.get(npc.getId());
		if (zoneIds == null)
		{
			return;
		}
		
		final int tierIndex = getCounterTierIndex(npc);
		for (int zoneId : zoneIds)
		{
			final TierSpawnZone zone = TierSpawnData.getInstance().getZone(zoneId);
			if ((zone == null) || !isKillAllowed(zone, npc) || hasActiveRaidBoss(zoneId))
			{
				continue;
			}
			
			zone.incrementAndGetKills(tierIndex);
			persistZoneState(zone);
			trySpawnRaidBoss(zone, true);
		}
	}
	
	private boolean handleRaidBossDeath(Attackable npc)
	{
		final List<Integer> zoneIds = _raidBossNpcToZones.get(npc.getId());
		if (zoneIds == null)
		{
			return false;
		}
		
		boolean handled = false;
		for (int zoneId : zoneIds)
		{
			final Integer activeObjectId = _activeRaidBossObjectIds.get(zoneId);
			if ((activeObjectId == null) || (activeObjectId.intValue() != npc.getObjectId()))
			{
				continue;
			}
			
			final TierSpawnZone zone = TierSpawnData.getInstance().getZone(zoneId);
			if (zone == null)
			{
				continue;
			}
			
			_activeRaidBossObjectIds.remove(zoneId);
			zone.setActiveRaidBossTier(TierSpawnZone.NO_ACTIVE_RAID_BOSS_TIER);
			persistZoneState(zone);
			trySpawnRaidBoss(zone, true);
			handled = true;
		}
		return handled;
	}
	
	private void trySpawnRaidBoss(TierSpawnZone zone, boolean announce)
	{
		if (hasActiveRaidBoss(zone.getId()))
		{
			return;
		}
		
		for (int tierIndex = 0; tierIndex < TierSpawnZone.TIER_COUNT; tierIndex++)
		{
			final int killThreshold = zone.getKillThreshold(tierIndex);
			if ((killThreshold < 0) || (zone.getKillCount(tierIndex) < killThreshold))
			{
				continue;
			}
			
			zone.setKillCount(tierIndex, 0);
			final int raidBossId = zone.getRaidBossId(tierIndex);
			if (raidBossId <= 0)
			{
				persistZoneState(zone);
				continue;
			}
			
			final Npc raidBoss = spawnRaidBoss(zone, tierIndex, announce);
			if (raidBoss != null)
			{
				return;
			}
		}
		
		zone.setActiveRaidBossTier(TierSpawnZone.NO_ACTIVE_RAID_BOSS_TIER);
		persistZoneState(zone);
	}
	
	private Npc spawnRaidBoss(TierSpawnZone zone, int tierIndex, boolean announce)
	{
		final Npc raidBoss = spawnNpc(zone.getRaidBossId(tierIndex), zone.getSpawnX(tierIndex), zone.getSpawnY(tierIndex), zone.getSpawnZ(tierIndex), zone.getSpawnHeading(tierIndex));
		if (raidBoss == null)
		{
			return null;
		}
		
		_activeRaidBossObjectIds.put(zone.getId(), raidBoss.getObjectId());
		zone.setActiveRaidBossTier(tierIndex);
		persistZoneState(zone);
		LOGGER.info(getClass().getSimpleName() + ": Zone " + zone.getName() + " [" + zone.getId() + "] spawned tier " + (tierIndex + 1) + " raid boss id=" + zone.getRaidBossId(tierIndex) + ".");
		if (announce)
		{
			LOGGER.info(getClass().getSimpleName() + ": Raid boss spawned for zone " + zone.getName() + ".");
		}
		return raidBoss;
	}
	
	private Npc spawnNpc(int npcId, int x, int y, int z, int heading)
	{
		try
		{
			final Spawn spawn = new Spawn(npcId);
			spawn.setXYZ(x, y, z);
			spawn.setHeading(heading);
			spawn.setAmount(1);
			spawn.stopRespawn();
			return spawn.doSpawn(true);
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to spawn NPC id=" + npcId + ": " + e.getMessage());
			return null;
		}
	}
	
	private boolean isKillAllowed(TierSpawnZone zone, Attackable npc)
	{
		if (!zone.isMonsterTracked(npc.getId()))
		{
			return false;
		}
		
		if (zone.getLocaId() <= 0)
		{
			return true;
		}
		
		final ZoneType locationZone = ZoneManager.getInstance().getZoneById(zone.getLocaId());
		if (locationZone == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Zone " + zone.getName() + " [" + zone.getId() + "] refers to missing locaId=" + zone.getLocaId() + ".");
			return false;
		}
		return locationZone.isInsideZone(npc);
	}
	
	private int getCounterTierIndex(Attackable npc)
	{
		return switch (npc.getChampionTier())
		{
			case 2 -> RED_TIER_INDEX;
			case 1 -> BLUE_TIER_INDEX;
			default -> NORMAL_TIER_INDEX;
		};
	}
	
	private boolean hasActiveRaidBoss(int zoneId)
	{
		return _activeRaidBossObjectIds.containsKey(zoneId);
	}
	
	private void restoreStates()
	{
		for (TierSpawnZone zone : TierSpawnData.getInstance().getZones().values())
		{
			final String storedState = GlobalVariablesManager.getInstance().getString(STATE_KEY_PREFIX + zone.getId(), null);
			if ((storedState == null) || storedState.isBlank())
			{
				persistZoneState(zone);
				continue;
			}
			
			try
			{
				final String[] split = storedState.split(",");
				if (split.length != 5)
				{
					zone.reset();
					persistZoneState(zone);
					continue;
				}
				
				final int activeRaidBossTier = Integer.parseInt(split[0]);
				final int[] killCounts =
				{
					Integer.parseInt(split[1]),
					Integer.parseInt(split[2]),
					Integer.parseInt(split[3])
				};
				final boolean raidBossDead = Boolean.parseBoolean(split[4]);
				zone.restoreState(activeRaidBossTier, killCounts);
				if ((activeRaidBossTier != TierSpawnZone.NO_ACTIVE_RAID_BOSS_TIER) && !raidBossDead)
				{
					spawnRaidBoss(zone, activeRaidBossTier, false);
				}
			}
			catch (Exception e)
			{
				zone.reset();
				persistZoneState(zone);
			}
		}
	}
	
	private void persistZoneState(TierSpawnZone zone)
	{
		final String value = zone.getActiveRaidBossTier() + "," + zone.getKillCount(NORMAL_TIER_INDEX) + "," + zone.getKillCount(BLUE_TIER_INDEX) + "," + zone.getKillCount(RED_TIER_INDEX) + "," + !hasActiveRaidBoss(zone.getId());
		GlobalVariablesManager.getInstance().set(STATE_KEY_PREFIX + zone.getId(), value);
	}
	
	public boolean isTracked(int npcId)
	{
		return _trackedNpcToZones.containsKey(npcId) || _raidBossNpcToZones.containsKey(npcId);
	}
	
	public static TierSpawnManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TierSpawnManager INSTANCE = new TierSpawnManager();
	}
}