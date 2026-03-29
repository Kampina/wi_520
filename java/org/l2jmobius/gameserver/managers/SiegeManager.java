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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.data.sql.DominionData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.ParserSiegeEventData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.TowerSpawn;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemLocation;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.Siege;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.castlewar.MercenaryCastleWarCastleSiegeHudInfo;

public class SiegeManager
{
	private static final Logger LOGGER = Logger.getLogger(SiegeManager.class.getName());
	private static final int TERRITORY_WARD_MIN_ID = 13560;
	private static final int TERRITORY_WARD_MAX_ID = 13568;
	private static final long TERRITORY_WARD_RETURN_DELAY = 600000L;
	
	private static final String SIEGE_CONFIG_FILE = "./config/Siege.ini";
	
	private final Map<Integer, List<TowerSpawn>> _controlTowers = new HashMap<>();
	private final Map<Integer, List<TowerSpawn>> _flameTowers = new HashMap<>();
	private final Map<Integer, TowerSpawn> _relicTowers = new HashMap<>();
	private final Map<Integer, ScheduledFuture<?>> _territoryWardReturnTasks = new ConcurrentHashMap<>();
	
	private int _siegeCycle = 2; // 2 weeks by default
	private int _attackerMaxClans = 500; // Max number of clans
	private int _attackerRespawnDelay = 0; // Time in ms. Changeable in siege.config
	private int _defenderMaxClans = 500; // Max number of clans
	private int _flagMaxCount = 1; // Changeable in siege.config
	private int _siegeClanMinLevel = 5; // Changeable in siege.config
	private int _siegeLength = 120; // Time in minute. Changeable in siege.config
	private int _bloodAllianceReward = 0; // Number of Blood Alliance items reward for successful castle defending
	
	protected SiegeManager()
	{
		load();
	}
	
	public void addSiegeSkills(Player character)
	{
		for (Skill sk : SkillData.getInstance().getSiegeSkills(character.isNoble(), character.getClan().getCastleId() > 0))
		{
			character.addSkill(sk, false);
		}
	}
	
	/**
	 * @param clan The Clan of the player
	 * @param castleid
	 * @return true if the clan is registered or owner of a castle
	 */
	public boolean checkIsRegistered(Clan clan, int castleid)
	{
		if (clan == null)
		{
			return false;
		}
		
		if (clan.getCastleId() > 0)
		{
			return true;
		}
		
		boolean register = false;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT clan_id FROM siege_clans where clan_id=? and castle_id=?"))
		{
			statement.setInt(1, clan.getId());
			statement.setInt(2, castleid);
			try (ResultSet rs = statement.executeQuery())
			{
				if (rs.next())
				{
					register = true;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: checkIsRegistered(): " + e.getMessage(), e);
		}
		
		return register;
	}
	
	public void removeSiegeSkills(Player character)
	{
		for (Skill sk : SkillData.getInstance().getSiegeSkills(character.isNoble(), character.getClan().getCastleId() > 0))
		{
			character.removeSkill(sk);
		}
	}
	
	private void load()
	{
		final ConfigReader siegeConfig = new ConfigReader(SIEGE_CONFIG_FILE);
		
		// Siege configurations.
		_siegeCycle = siegeConfig.getInt("SiegeCycle", 2);
		_attackerMaxClans = siegeConfig.getInt("AttackerMaxClans", 500);
		_attackerRespawnDelay = siegeConfig.getInt("AttackerRespawn", 0);
		_defenderMaxClans = siegeConfig.getInt("DefenderMaxClans", 500);
		_flagMaxCount = siegeConfig.getInt("MaxFlags", 1);
		_siegeClanMinLevel = siegeConfig.getInt("SiegeClanMinLevel", 5);
		_siegeLength = siegeConfig.getInt("SiegeLength", 120);
		_bloodAllianceReward = siegeConfig.getInt("BloodAllianceReward", 1);
		
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			final List<TowerSpawn> controlTowers = new ArrayList<>();
			for (int i = 1; i < 0xFF; i++)
			{
				final String configKey = castle.getName() + "ControlTower" + i;
				if (!siegeConfig.containsKey(configKey))
				{
					break;
				}
				
				final StringTokenizer st = new StringTokenizer(siegeConfig.getString(configKey, ""), ",");
				try
				{
					final int x = Integer.parseInt(st.nextToken());
					final int y = Integer.parseInt(st.nextToken());
					final int z = Integer.parseInt(st.nextToken());
					final int npcId = Integer.parseInt(st.nextToken());
					
					controlTowers.add(new TowerSpawn(npcId, new Location(x, y, z)));
				}
				catch (Exception e)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Error while loading control tower(s) for " + castle.getName() + " castle.");
				}
			}
			
			final List<TowerSpawn> flameTowers = new ArrayList<>();
			for (int i = 1; i < 0xFF; i++)
			{
				final String configKey = castle.getName() + "FlameTower" + i;
				if (!siegeConfig.containsKey(configKey))
				{
					break;
				}
				
				final StringTokenizer st = new StringTokenizer(siegeConfig.getString(configKey, ""), ",");
				try
				{
					final int x = Integer.parseInt(st.nextToken());
					final int y = Integer.parseInt(st.nextToken());
					final int z = Integer.parseInt(st.nextToken());
					final int npcId = Integer.parseInt(st.nextToken());
					final List<Integer> zoneList = new ArrayList<>();
					
					while (st.hasMoreTokens())
					{
						zoneList.add(Integer.parseInt(st.nextToken()));
					}
					
					flameTowers.add(new TowerSpawn(npcId, new Location(x, y, z), zoneList));
				}
				catch (Exception e)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Error while loading flame tower(s) for " + castle.getName() + " castle.");
				}
			}
			
			_controlTowers.put(castle.getResidenceId(), controlTowers);
			_flameTowers.put(castle.getResidenceId(), flameTowers);
			
			if (castle.getOwnerId() != 0)
			{
				loadTrapUpgrade(castle.getResidenceId());
			}
		}
		
		// Mercenary Siege.
		final String[] relics = siegeConfig.getString("Relic", null).split(";");
		for (String elem : relics)
		{
			final String[] s = elem.split(",");
			final int castleId = Integer.parseInt(s[0]);
			final int npcId = Integer.parseInt(s[1]);
			final Location loc = new Location(Integer.parseInt(s[2]), Integer.parseInt(s[3]), Integer.parseInt(s[4]));
			final TowerSpawn towerSpawn = new TowerSpawn(npcId, loc);
			_relicTowers.put(castleId, towerSpawn);
		}
	}
	
	public TowerSpawn getRelicTowers(int castleId)
	{
		return _relicTowers.get(castleId);
	}
	
	public List<TowerSpawn> getControlTowers(int castleId)
	{
		return _controlTowers.get(castleId);
	}
	
	public List<TowerSpawn> getFlameTowers(int castleId)
	{
		return _flameTowers.get(castleId);
	}
	
	public int getSiegeCycle()
	{
		return _siegeCycle;
	}
	
	public int getAttackerMaxClans()
	{
		return _attackerMaxClans;
	}
	
	public int getAttackerRespawnDelay()
	{
		return _attackerRespawnDelay;
	}
	
	public int getDefenderMaxClans()
	{
		return _defenderMaxClans;
	}
	
	public int getFlagMaxCount()
	{
		return _flagMaxCount;
	}

	public boolean isTerritoryWard(int itemId)
	{
		return (itemId >= TERRITORY_WARD_MIN_ID) && (itemId <= TERRITORY_WARD_MAX_ID);
	}

	public Item getTerritoryWard(Player player)
	{
		if (player == null)
		{
			return null;
		}

		for (int itemId = TERRITORY_WARD_MIN_ID; itemId <= TERRITORY_WARD_MAX_ID; itemId++)
		{
			final Item item = player.getInventory().getItemByItemId(itemId);
			if (item != null)
			{
				return item;
			}
		}

		return null;
	}

	public int getTerritoryIdByWardId(int itemId)
	{
		return isTerritoryWard(itemId) ? (itemId - 13479) : 0;
	}

	public Location getTerritoryWardReturnLocation(int wardItemId)
	{
		final int wardTerritoryId = getTerritoryIdByWardId(wardItemId);
		if (wardTerritoryId <= 0)
		{
			return null;
		}

		int ownerTerritoryId = wardTerritoryId;
		for (Map.Entry<Integer, int[]> entry : DominionData.getInstance().loadDominionFlags().entrySet())
		{
			for (int ownedWardTerritoryId : entry.getValue())
			{
				if (ownedWardTerritoryId == wardTerritoryId)
				{
					ownerTerritoryId = entry.getKey();
					break;
				}
			}
		}

		return ParserSiegeEventData.getInstance().getWardLocation(ownerTerritoryId, wardTerritoryId);
	}

	public boolean returnTerritoryWardToBase(Player player, boolean sendMessage)
	{
		if (player == null)
		{
			return false;
		}

		final Item ward = getTerritoryWard(player);
		if (ward == null)
		{
			return false;
		}

		final Location returnLocation = getTerritoryWardReturnLocation(ward.getId());
		if (returnLocation == null)
		{
			return dropTerritoryWard(player, sendMessage);
		}

		final long slot = player.getInventory().getSlotFromItem(ward);
		if (slot > 0)
		{
			player.getInventory().unEquipItemInBodySlot(slot);
		}

		if (player.dropItem(ItemProcessType.DROP, ward.getObjectId(), ward.getCount(), returnLocation.getX(), returnLocation.getY(), returnLocation.getZ(), null, false, true) == null)
		{
			return false;
		}

		player.setCombatFlagEquipped(false);
		player.broadcastUserInfo();
		if (sendMessage)
		{
			player.sendPacket(SystemMessageId.THE_EFFECT_OF_TERRITORY_WARD_IS_DISAPPEARING);
		}
		return true;
	}

	public boolean dropTerritoryWard(Player player, boolean sendMessage)
	{
		return dropTerritoryWard(player, sendMessage, false);
	}

	public boolean dropTerritoryWard(Player player, boolean sendMessage, boolean scheduleReturn)
	{
		if (player == null)
		{
			return false;
		}

		final Item ward = getTerritoryWard(player);
		if (ward == null)
		{
			return false;
		}

		final long slot = player.getInventory().getSlotFromItem(ward);
		if (slot > 0)
		{
			player.getInventory().unEquipItemInBodySlot(slot);
		}

		if (!player.dropItem(ItemProcessType.DROP, ward, null, false, true))
		{
			player.destroyItem(ItemProcessType.DESTROY, ward, null, true);
		}
		else if (scheduleReturn)
		{
			scheduleTerritoryWardReturn(ward);
		}

		player.setCombatFlagEquipped(false);
		player.broadcastUserInfo();
		if (sendMessage)
		{
			player.sendPacket(SystemMessageId.THE_EFFECT_OF_TERRITORY_WARD_IS_DISAPPEARING);
		}
		return true;
	}

	public void cancelTerritoryWardReturn(int objectId)
	{
		final ScheduledFuture<?> task = _territoryWardReturnTasks.remove(objectId);
		if (task != null)
		{
			task.cancel(false);
		}
	}

	private void scheduleTerritoryWardReturn(Item ward)
	{
		cancelTerritoryWardReturn(ward.getObjectId());
		final ScheduledFuture<?> task = ThreadPool.schedule(() -> returnDroppedTerritoryWard(ward.getObjectId()), TERRITORY_WARD_RETURN_DELAY);
		if (task != null)
		{
			_territoryWardReturnTasks.put(ward.getObjectId(), task);
		}
	}

	private void returnDroppedTerritoryWard(int objectId)
	{
		_territoryWardReturnTasks.remove(objectId);

		final WorldObject object = World.getInstance().findObject(objectId);
		if (!(object instanceof Item ward) || !isTerritoryWard(ward.getId()) || !ward.isSpawned() || (ward.getOwnerId() != 0) || (ward.getItemLocation() != ItemLocation.VOID))
		{
			return;
		}

		final Location returnLocation = getTerritoryWardReturnLocation(ward.getId());
		if (returnLocation == null)
		{
			return;
		}

		ItemsOnGroundManager.getInstance().removeObject(ward);
		ward.decayMe();
		ward.dropMe(null, returnLocation.getX(), returnLocation.getY(), returnLocation.getZ());
	}
	
	public void scheduleTerritoryWardReturnLocal(Item ward, Location location)
	{
		cancelTerritoryWardReturn(ward.getObjectId());
		final java.util.concurrent.ScheduledFuture<?> task = org.l2jmobius.commons.threads.ThreadPool.schedule(() -> returnDroppedTerritoryWardLocal(ward.getObjectId(), location), 60000L); // 1 min
		if (task != null)
		{
			_territoryWardReturnTasks.put(ward.getObjectId(), task);
		}
	}

	private void returnDroppedTerritoryWardLocal(int objectId, Location returnLocation)
	{
		_territoryWardReturnTasks.remove(objectId);

		final WorldObject object = World.getInstance().findObject(objectId);
		if (!(object instanceof Item ward) || !isTerritoryWard(ward.getId()) || !ward.isSpawned() || (ward.getOwnerId() != 0) || (ward.getItemLocation() != org.l2jmobius.gameserver.model.item.enums.ItemLocation.VOID))
		{
			return;
		}

		if (returnLocation == null)
		{
			returnLocation = getTerritoryWardReturnLocation(ward.getId());
		}
		if (returnLocation == null) return;

		org.l2jmobius.gameserver.managers.ItemsOnGroundManager.getInstance().removeObject(ward);
		ward.decayMe();
		ward.dropMe(null, returnLocation.getX(), returnLocation.getY(), returnLocation.getZ());
	}
	
	public Siege getSiege(ILocational loc)
	{
		return getSiege(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public Siege getSiege(WorldObject activeObject)
	{
		return getSiege(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	public Siege getSiege(int x, int y, int z)
	{
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if (castle.getSiege().checkIfInZone(x, y, z))
			{
				return castle.getSiege();
			}
		}
		
		return null;
	}
	
	public int getSiegeClanMinLevel()
	{
		return _siegeClanMinLevel;
	}
	
	public int getSiegeLength()
	{
		return _siegeLength;
	}
	
	public int getBloodAllianceReward()
	{
		return _bloodAllianceReward;
	}
	
	public Collection<Siege> getSieges()
	{
		final List<Siege> sieges = new LinkedList<>();
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			sieges.add(castle.getSiege());
		}
		
		return sieges;
	}
	
	public Siege getSiege(int castleId)
	{
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if (castle.getResidenceId() == castleId)
			{
				return castle.getSiege();
			}
		}
		
		return null;
	}
	
	private void loadTrapUpgrade(int castleId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM castle_trapupgrade WHERE castleId=?"))
		{
			ps.setInt(1, castleId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					_flameTowers.get(castleId).get(rs.getInt("towerIndex")).setUpgradeLevel(rs.getInt("level"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: loadTrapUpgrade(): " + e.getMessage(), e);
		}
	}
	
	public void sendSiegeInfo(Player player)
	{
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			final int diff = (int) (castle.getSiegeDate().getTimeInMillis() - System.currentTimeMillis());
			if (((diff > 0) && (diff < 86400000)) || castle.getSiege().isInProgress())
			{
				player.sendPacket(new MercenaryCastleWarCastleSiegeHudInfo(castle.getResidenceId()));
			}
		}
	}
	
	public void sendSiegeInfo(Player player, int castleId)
	{
		player.sendPacket(new MercenaryCastleWarCastleSiegeHudInfo(castleId));
	}
	
	public static SiegeManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SiegeManager INSTANCE = new SiegeManager();
	}
}
