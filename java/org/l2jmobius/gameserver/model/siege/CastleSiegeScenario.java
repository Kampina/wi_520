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
package org.l2jmobius.gameserver.model.siege;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.CastleSiegeEventData.CastleSiegeAction;
import org.l2jmobius.gameserver.data.xml.CastleSiegeEventData.CastleSiegeActionType;
import org.l2jmobius.gameserver.data.xml.CastleSiegeEventData.CastleSiegeObjectGroup;
import org.l2jmobius.gameserver.data.xml.CastleSiegeEventData.CastleSiegeTemplate;
import org.l2jmobius.gameserver.data.xml.CastleSiegeEventData.SpawnNpcHolder;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Door;
import org.l2jmobius.gameserver.model.item.enums.SpecialItemType;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.model.zone.type.SiegeZone;

public class CastleSiegeScenario
{
	private static final Logger LOGGER = Logger.getLogger(CastleSiegeScenario.class.getName());

	private final Siege _siege;
	private final CastleSiegeTemplate _template;
	private final List<ScheduledFuture<?>> _preSiegeTasks = new CopyOnWriteArrayList<>();
	private final List<ScheduledFuture<?>> _runningTasks = new CopyOnWriteArrayList<>();
	private final Set<String> _activeSpawnTemplates = ConcurrentHashMap.newKeySet();
	private final Set<String> _spawnedRuntimeGroups = ConcurrentHashMap.newKeySet();
	private final ConcurrentHashMap<String, List<Npc>> _spawnedNpcGroups = new ConcurrentHashMap<>();

	public CastleSiegeScenario(Siege siege, CastleSiegeTemplate template)
	{
		_siege = siege;
		_template = template;
	}

	public void initialize()
	{
		if (!_siege.isInProgress())
		{
			executeActions(_template.getOnInit());
		}
	}

	public void reschedulePreSiegeActions(Calendar siegeDate)
	{
		cancelTasks(_preSiegeTasks);
		final long siegeStart = siegeDate.getTimeInMillis();
		final long now = System.currentTimeMillis();
		for (Entry<Integer, List<CastleSiegeAction>> entry : _template.getTimedActions().entrySet())
		{
			if (entry.getKey() >= 0)
			{
				continue;
			}

			final long executeAt = siegeStart + (entry.getKey() * 1000L);
			if (executeAt <= now)
			{
				if ((now < siegeStart) && !_siege.isInProgress())
				{
					executeActions(entry.getValue());
				}
				continue;
			}

			_preSiegeTasks.add(ThreadPool.schedule(() -> executeActions(entry.getValue()), executeAt - now));
		}
	}

	public void onSiegeStart()
	{
		cancelTasks(_runningTasks);
		executeActions(_template.getOnStart());
		executeActions(_template.getTimedActions(0));
		for (Entry<Integer, List<CastleSiegeAction>> entry : _template.getTimedActions().entrySet())
		{
			if (entry.getKey() <= 0)
			{
				continue;
			}
			_runningTasks.add(ThreadPool.schedule(() -> executeActions(entry.getValue()), entry.getKey() * 1000L));
		}
	}

	public void onSiegeStop()
	{
		cancelTasks(_runningTasks);
		executeActions(_template.getOnStop());
	}

	public void shutdown()
	{
		cancelTasks(_preSiegeTasks);
		cancelTasks(_runningTasks);
	}

	private void executeActions(List<CastleSiegeAction> actions)
	{
		for (CastleSiegeAction action : actions)
		{
			try
			{
				executeAction(action);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to execute castle siege scenario action " + action.getType() + " for castle " + _template.getName(), e);
			}
		}
	}

	private void executeAction(CastleSiegeAction action)
	{
		switch (action.getType())
		{
			case INIT:
			{
				initGroup(action.getStringValue());
				break;
			}
			case START:
			{
				startNamedAction(action.getStringValue());
				break;
			}
			case STOP:
			{
				stopNamedAction(action.getStringValue());
				break;
			}
			case SPAWN:
			{
				spawnGroup(action.getStringValue());
				break;
			}
			case DESPAWN:
			{
				despawnGroup(action.getStringValue());
				break;
			}
			case REFRESH:
			{
				refreshGroup(action.getStringValue());
				break;
			}
			case OPEN:
			{
				setGroupDoorState(action.getStringValue(), true);
				break;
			}
			case CLOSE:
			{
				setGroupDoorState(action.getStringValue(), false);
				break;
			}
			case ACTIVE:
			{
				setGroupActive(action.getStringValue(), true);
				break;
			}
			case DEACTIVE:
			{
				setGroupActive(action.getStringValue(), false);
				break;
			}
			case ANNOUNCE:
			{
				_siege.announceScenarioTime(action.getIntValue());
				break;
			}
			case PLAY_SOUND:
			{
				_siege.broadcastScenarioSound(action.getStringValue());
				break;
			}
			case GIVE_ITEM:
			{
				giveReward(action.getIntValue(), action.getLongValue());
				break;
			}
			case TELEPORT_PLAYERS:
			{
				teleportPlayers(action.getStringValue());
				break;
			}
		}
	}

	private void initGroup(String groupName)
	{
		if ("doors".equalsIgnoreCase(groupName))
		{
			_siege.refreshScenarioDoors();
			return;
		}
		setGroupActive(groupName, false);
	}

	private void spawnGroup(String groupName)
	{
		if ("control_towers".equalsIgnoreCase(groupName))
		{
			if (_spawnedRuntimeGroups.add(groupName))
			{
				_siege.spawnScenarioControlTowers();
			}
			return;
		}
		if ("flame_towers".equalsIgnoreCase(groupName))
		{
			if (_spawnedRuntimeGroups.add(groupName))
			{
				_siege.spawnScenarioFlameTowers();
			}
			return;
		}

		final CastleSiegeObjectGroup group = _template.getObjectGroup(groupName);
		if (group == null)
		{
			return;
		}

		spawnNpcs(groupName, group.getNpcSpawns());
		for (String spawnTemplateName : group.getSpawnTemplateNames())
		{
			spawnTemplate(spawnTemplateName);
		}
	}

	private void despawnGroup(String groupName)
	{
		if ("control_towers".equalsIgnoreCase(groupName) || "flame_towers".equalsIgnoreCase(groupName))
		{
			_spawnedRuntimeGroups.remove(groupName);
			return;
		}

		despawnNpcs(groupName);
		final CastleSiegeObjectGroup group = _template.getObjectGroup(groupName);
		if (group == null)
		{
			return;
		}

		for (String spawnTemplateName : group.getSpawnTemplateNames())
		{
			despawnTemplate(spawnTemplateName);
		}
	}

	private void refreshGroup(String groupName)
	{
		if ("doors".equalsIgnoreCase(groupName))
		{
			_siege.refreshScenarioDoors();
			return;
		}
		despawnGroup(groupName);
		spawnGroup(groupName);
	}

	private void setGroupActive(String groupName, boolean active)
	{
		if ("siege_zones".equalsIgnoreCase(groupName))
		{
			_siege.setScenarioSiegeZoneActive(active);
			return;
		}

		final CastleSiegeObjectGroup group = _template.getObjectGroup(groupName);
		if (group == null)
		{
			return;
		}

		for (String zoneName : group.getZoneNames())
		{
			setZoneActive(zoneName, active);
		}
	}

	private void setGroupDoorState(String groupName, boolean open)
	{
		final CastleSiegeObjectGroup group = _template.getObjectGroup(groupName);
		if (group == null)
		{
			return;
		}

		for (int doorId : group.getDoorIds())
		{
			final Door door = _siege.getCastle().getDoor(doorId);
			if (door == null)
			{
				continue;
			}

			if (open)
			{
				door.openMe();
			}
			else
			{
				door.closeMe();
			}
		}
	}

	private void startNamedAction(String name)
	{
		if ("event".equalsIgnoreCase(name) && !_siege.isInProgress())
		{
			_siege.startSiege();
		}
	}

	private void stopNamedAction(String name)
	{
		if ("registration".equalsIgnoreCase(name))
		{
			if (!_siege.getCastle().isTimeRegistrationOver())
			{
				_siege.endTimeRegistration(true);
			}
			return;
		}
		if ("event".equalsIgnoreCase(name) && _siege.isInProgress())
		{
			_siege.endSiege();
		}
	}

	private void teleportPlayers(String teleportId)
	{
		if ("from_residence_to_town".equalsIgnoreCase(teleportId))
		{
			_siege.teleportScenarioPlayers(false, false);
		}
		else if ("from_residence_to_town_except_defenders".equalsIgnoreCase(teleportId))
		{
			_siege.teleportScenarioPlayers(true, false);
		}
		else if ("defenders".equalsIgnoreCase(teleportId))
		{
			_siege.teleportScenarioDefenders();
		}
	}

	private void giveReward(int itemId, long count)
	{
		for (Player player : getPlayersForRewards())
		{
			if ((itemId == SpecialItemType.FAME.getClientId()) && player.isDead() && !PlayerConfig.FAME_FOR_DEAD_PLAYERS)
			{
				continue;
			}

			if (itemId == SpecialItemType.FAME.getClientId())
			{
				player.setFame((int) (player.getFame() + count));
			}
			else
			{
				player.addItem(ItemProcessType.REWARD, itemId, count, null, true);
			}
		}
	}

	private List<Player> getPlayersForRewards()
	{
		final List<Player> players = new ArrayList<>();
		for (Player player : _siege.getPlayersInZone())
		{
			if ((player == null) || !player.isInSiege())
			{
				continue;
			}
			if ((_siege.getAttackerClan(player.getClanId()) != null) || (_siege.getDefenderClan(player.getClanId()) != null))
			{
				players.add(player);
			}
		}
		return players;
	}

	private void spawnTemplate(String name)
	{
		if (!_activeSpawnTemplates.add(name))
		{
			return;
		}

		final SpawnTemplate template = SpawnData.getInstance().getSpawnByName(name);
		if (template == null)
		{
			_activeSpawnTemplates.remove(name);
			LOGGER.warning(getClass().getSimpleName() + ": Missing spawn template " + name + " for castle scenario " + _template.getName());
			return;
		}

		template.spawnAll(null);
		template.notifyActivate();
	}

	private void despawnTemplate(String name)
	{
		if (!_activeSpawnTemplates.remove(name))
		{
			return;
		}

		final SpawnTemplate template = SpawnData.getInstance().getSpawnByName(name);
		if (template != null)
		{
			template.despawnAll();
		}
	}

	private void spawnNpcs(String groupName, List<SpawnNpcHolder> npcs)
	{
		if (npcs.isEmpty() || _spawnedNpcGroups.containsKey(groupName))
		{
			return;
		}

		final List<Npc> spawned = new ArrayList<>();
		for (SpawnNpcHolder holder : npcs)
		{
			try
			{
				final Spawn spawn = new Spawn(holder.getNpcId());
				spawn.setAmount(1);
				spawn.setXYZ(holder.getX(), holder.getY(), holder.getZ());
				spawn.stopRespawn();
				spawned.add(spawn.doSpawn(false));
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to spawn scenario NPC group " + groupName + " for castle " + _template.getName(), e);
			}
		}

		if (!spawned.isEmpty())
		{
			_spawnedNpcGroups.put(groupName, spawned);
		}
	}

	private void despawnNpcs(String groupName)
	{
		final List<Npc> npcs = _spawnedNpcGroups.remove(groupName);
		if (npcs == null)
		{
			return;
		}

		for (Npc npc : npcs)
		{
			if (npc != null)
			{
				npc.deleteMe();
			}
		}
	}

	private void setZoneActive(String zoneName, boolean active)
	{
		final ZoneType zone = ZoneManager.getInstance().getZoneByName(zoneName);
		if (zone == null)
		{
			return;
		}

		if (zone instanceof SiegeZone)
		{
			((SiegeZone) zone).setActive(active);
			((SiegeZone) zone).updateZoneStatusForCharactersInside();
		}
		else
		{
			zone.setEnabled(active);
		}
	}

	private void cancelTasks(List<ScheduledFuture<?>> tasks)
	{
		for (ScheduledFuture<?> task : tasks)
		{
			if (task != null)
			{
				task.cancel(false);
			}
		}
		tasks.clear();
	}
}