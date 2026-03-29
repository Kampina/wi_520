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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;

public class CastleSiegeEventData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(CastleSiegeEventData.class.getName());
	private static final String EVENTS_DIRECTORY = "data/events/siege";
	private static final String LEGACY_EVENTS_DIRECTORY = "data/parser/events/siege";

	private final Map<Integer, CastleSiegeTemplate> _templates = new ConcurrentHashMap<>();

	protected CastleSiegeEventData()
	{
		load();
	}

	@Override
	public void load()
	{
		_templates.clear();
		parseDatapackDirectory(EVENTS_DIRECTORY, false);
		if (_templates.isEmpty())
		{
			parseDatapackDirectory(LEGACY_EVENTS_DIRECTORY, false);
		}
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _templates.size() + " castle siege scenario templates.");
	}

	@Override
	public boolean isValidating()
	{
		return false;
	}

	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node node = document.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if (!"list".equalsIgnoreCase(node.getNodeName()))
			{
				continue;
			}

			for (Node eventNode = node.getFirstChild(); eventNode != null; eventNode = eventNode.getNextSibling())
			{
				if (!"event".equalsIgnoreCase(eventNode.getNodeName()))
				{
					continue;
				}

				final NamedNodeMap attributes = eventNode.getAttributes();
				if (!"CastleSiege".equalsIgnoreCase(parseString(attributes, "impl")))
				{
					continue;
				}

				final int castleId = parseInteger(attributes, "id");
				final CastleSiegeTemplate template = new CastleSiegeTemplate(castleId, parseString(attributes, "name"));
				for (Node child = eventNode.getFirstChild(); child != null; child = child.getNextSibling())
				{
					final String nodeName = child.getNodeName();
					if ("on_init".equalsIgnoreCase(nodeName))
					{
						parseActions(child, template.getOnInit(), Integer.MIN_VALUE);
					}
					else if ("on_start".equalsIgnoreCase(nodeName))
					{
						parseActions(child, template.getOnStart(), Integer.MIN_VALUE);
					}
					else if ("on_stop".equalsIgnoreCase(nodeName))
					{
						parseActions(child, template.getOnStop(), Integer.MIN_VALUE);
					}
					else if ("on_time".equalsIgnoreCase(nodeName))
					{
						parseTimedActions(child, template);
					}
					else if ("objects".equalsIgnoreCase(nodeName))
					{
						final CastleSiegeObjectGroup group = parseObjectGroup(child);
						template.getObjectGroups().put(group.getName(), group);
					}
				}
				_templates.put(castleId, template);
			}
		}
	}

	private void parseTimedActions(Node parentNode, CastleSiegeTemplate template)
	{
		for (Node child = parentNode.getFirstChild(); child != null; child = child.getNextSibling())
		{
			if (!"on".equalsIgnoreCase(child.getNodeName()))
			{
				continue;
			}

			final int time = parseInteger(child.getAttributes(), "time");
			final List<CastleSiegeAction> actions = template.getTimedActions().computeIfAbsent(time, key -> new ArrayList<>());
			parseActions(child, actions, time);
		}
	}

	private void parseActions(Node parentNode, List<CastleSiegeAction> actions, int time)
	{
		for (Node child = parentNode.getFirstChild(); child != null; child = child.getNextSibling())
		{
			final NamedNodeMap attributes = child.getAttributes();
			final String nodeName = child.getNodeName();
			if ("init".equalsIgnoreCase(nodeName))
			{
				actions.add(CastleSiegeAction.named(CastleSiegeActionType.INIT, parseString(attributes, "name")));
			}
			else if ("start".equalsIgnoreCase(nodeName))
			{
				actions.add(CastleSiegeAction.named(CastleSiegeActionType.START, parseString(attributes, "name")));
			}
			else if ("stop".equalsIgnoreCase(nodeName))
			{
				actions.add(CastleSiegeAction.named(CastleSiegeActionType.STOP, parseString(attributes, "name")));
			}
			else if ("spawn".equalsIgnoreCase(nodeName))
			{
				actions.add(CastleSiegeAction.named(CastleSiegeActionType.SPAWN, parseString(attributes, "name")));
			}
			else if ("despawn".equalsIgnoreCase(nodeName))
			{
				actions.add(CastleSiegeAction.named(CastleSiegeActionType.DESPAWN, parseString(attributes, "name")));
			}
			else if ("refresh".equalsIgnoreCase(nodeName))
			{
				actions.add(CastleSiegeAction.named(CastleSiegeActionType.REFRESH, parseString(attributes, "name")));
			}
			else if ("open".equalsIgnoreCase(nodeName))
			{
				actions.add(CastleSiegeAction.named(CastleSiegeActionType.OPEN, parseString(attributes, "name")));
			}
			else if ("close".equalsIgnoreCase(nodeName))
			{
				actions.add(CastleSiegeAction.named(CastleSiegeActionType.CLOSE, parseString(attributes, "name")));
			}
			else if ("active".equalsIgnoreCase(nodeName))
			{
				actions.add(CastleSiegeAction.named(CastleSiegeActionType.ACTIVE, parseString(attributes, "name")));
			}
			else if ("deactive".equalsIgnoreCase(nodeName))
			{
				actions.add(CastleSiegeAction.named(CastleSiegeActionType.DEACTIVE, parseString(attributes, "name")));
			}
			else if ("announce".equalsIgnoreCase(nodeName))
			{
				final int value = hasAttribute(attributes, "val") ? parseInteger(attributes, "val") : time;
				actions.add(CastleSiegeAction.integer(CastleSiegeActionType.ANNOUNCE, value));
			}
			else if ("play_sound".equalsIgnoreCase(nodeName))
			{
				actions.add(CastleSiegeAction.named(CastleSiegeActionType.PLAY_SOUND, parseString(attributes, "sound")));
			}
			else if ("give_item".equalsIgnoreCase(nodeName))
			{
				actions.add(CastleSiegeAction.integerLong(CastleSiegeActionType.GIVE_ITEM, parseInteger(attributes, "id"), parseLongValue(attributes, "count")));
			}
			else if ("teleport_players".equalsIgnoreCase(nodeName))
			{
				actions.add(CastleSiegeAction.named(CastleSiegeActionType.TELEPORT_PLAYERS, parseString(attributes, "id")));
			}
		}
	}

	private CastleSiegeObjectGroup parseObjectGroup(Node objectsNode)
	{
		final CastleSiegeObjectGroup group = new CastleSiegeObjectGroup(parseString(objectsNode.getAttributes(), "name"));
		for (Node child = objectsNode.getFirstChild(); child != null; child = child.getNextSibling())
		{
			final NamedNodeMap attributes = child.getAttributes();
			final String nodeName = child.getNodeName();
			if ("door".equalsIgnoreCase(nodeName))
			{
				group.getDoorIds().add(parseInteger(attributes, "id"));
			}
			else if ("spawn_ex".equalsIgnoreCase(nodeName))
			{
				group.getSpawnTemplateNames().add(parseString(attributes, "name"));
			}
			else if ("spawn_npc".equalsIgnoreCase(nodeName))
			{
				group.getNpcSpawns().add(new SpawnNpcHolder(parseInteger(attributes, "id"), parseInteger(attributes, "x"), parseInteger(attributes, "y"), parseInteger(attributes, "z")));
			}
			else if ("zone".equalsIgnoreCase(nodeName) || "castle_zone".equalsIgnoreCase(nodeName))
			{
				group.getZoneNames().add(parseString(attributes, "name"));
			}
		}
		return group;
	}

	private boolean hasAttribute(NamedNodeMap attributes, String name)
	{
		return (attributes != null) && (attributes.getNamedItem(name) != null);
	}

	private long parseLongValue(NamedNodeMap attributes, String name)
	{
		return Long.parseLong(parseString(attributes, name));
	}

	public CastleSiegeTemplate getTemplate(int castleId)
	{
		return _templates.get(castleId);
	}

	public static CastleSiegeEventData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final CastleSiegeEventData INSTANCE = new CastleSiegeEventData();
	}

	public enum CastleSiegeActionType
	{
		INIT,
		START,
		STOP,
		SPAWN,
		DESPAWN,
		REFRESH,
		OPEN,
		CLOSE,
		ACTIVE,
		DEACTIVE,
		ANNOUNCE,
		PLAY_SOUND,
		GIVE_ITEM,
		TELEPORT_PLAYERS
	}

	public static class CastleSiegeAction
	{
		private final CastleSiegeActionType _type;
		private final String _stringValue;
		private final int _intValue;
		private final long _longValue;

		private CastleSiegeAction(CastleSiegeActionType type, String stringValue, int intValue, long longValue)
		{
			_type = type;
			_stringValue = stringValue;
			_intValue = intValue;
			_longValue = longValue;
		}

		public static CastleSiegeAction named(CastleSiegeActionType type, String value)
		{
			return new CastleSiegeAction(type, value, 0, 0);
		}

		public static CastleSiegeAction integer(CastleSiegeActionType type, int value)
		{
			return new CastleSiegeAction(type, null, value, 0);
		}

		public static CastleSiegeAction integerLong(CastleSiegeActionType type, int intValue, long longValue)
		{
			return new CastleSiegeAction(type, null, intValue, longValue);
		}

		public CastleSiegeActionType getType()
		{
			return _type;
		}

		public String getStringValue()
		{
			return _stringValue;
		}

		public int getIntValue()
		{
			return _intValue;
		}

		public long getLongValue()
		{
			return _longValue;
		}
	}

	public static class SpawnNpcHolder
	{
		private final int _npcId;
		private final int _x;
		private final int _y;
		private final int _z;

		public SpawnNpcHolder(int npcId, int x, int y, int z)
		{
			_npcId = npcId;
			_x = x;
			_y = y;
			_z = z;
		}

		public int getNpcId()
		{
			return _npcId;
		}

		public int getX()
		{
			return _x;
		}

		public int getY()
		{
			return _y;
		}

		public int getZ()
		{
			return _z;
		}
	}

	public static class CastleSiegeObjectGroup
	{
		private final String _name;
		private final List<Integer> _doorIds = new ArrayList<>();
		private final List<String> _spawnTemplateNames = new ArrayList<>();
		private final List<String> _zoneNames = new ArrayList<>();
		private final List<SpawnNpcHolder> _npcSpawns = new ArrayList<>();

		public CastleSiegeObjectGroup(String name)
		{
			_name = name;
		}

		public String getName()
		{
			return _name;
		}

		public List<Integer> getDoorIds()
		{
			return _doorIds;
		}

		public List<String> getSpawnTemplateNames()
		{
			return _spawnTemplateNames;
		}

		public List<String> getZoneNames()
		{
			return _zoneNames;
		}

		public List<SpawnNpcHolder> getNpcSpawns()
		{
			return _npcSpawns;
		}
	}

	public static class CastleSiegeTemplate
	{
		private final int _castleId;
		private final String _name;
		private final List<CastleSiegeAction> _onInit = new ArrayList<>();
		private final List<CastleSiegeAction> _onStart = new ArrayList<>();
		private final List<CastleSiegeAction> _onStop = new ArrayList<>();
		private final Map<Integer, List<CastleSiegeAction>> _timedActions = new TreeMap<>();
		private final Map<String, CastleSiegeObjectGroup> _objectGroups = new ConcurrentHashMap<>();

		public CastleSiegeTemplate(int castleId, String name)
		{
			_castleId = castleId;
			_name = name;
		}

		public int getCastleId()
		{
			return _castleId;
		}

		public String getName()
		{
			return _name;
		}

		public List<CastleSiegeAction> getOnInit()
		{
			return _onInit;
		}

		public List<CastleSiegeAction> getOnStart()
		{
			return _onStart;
		}

		public List<CastleSiegeAction> getOnStop()
		{
			return _onStop;
		}

		public Map<Integer, List<CastleSiegeAction>> getTimedActions()
		{
			return _timedActions;
		}

		public Map<String, CastleSiegeObjectGroup> getObjectGroups()
		{
			return _objectGroups;
		}

		public CastleSiegeObjectGroup getObjectGroup(String name)
		{
			return _objectGroups.get(name);
		}

		public List<CastleSiegeAction> getTimedActions(int time)
		{
			return _timedActions.getOrDefault(time, Collections.emptyList());
		}
	}
}