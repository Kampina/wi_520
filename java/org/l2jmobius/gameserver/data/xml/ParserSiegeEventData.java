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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.Location;

public class ParserSiegeEventData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ParserSiegeEventData.class.getName());
	private static final Pattern FILE_ID_PATTERN = Pattern.compile("^\\[(\\d+)\\].*\\.xml$");
	private static final int WARD_TERRITORY_OFFSET = 13479;
	private static final String EVENTS_DIRECTORY = "data/events/siege";
	private static final String LEGACY_EVENTS_DIRECTORY = "data/parser/events/siege";

	private final Map<Integer, Map<Integer, Location>> _dominionWardLocations = new HashMap<>();
	private final Map<Integer, List<Location>> _fortCombatFlags = new HashMap<>();
	private final Map<Integer, Integer> _fortFlagPoleIds = new HashMap<>();

	protected ParserSiegeEventData()
	{
		load();
	}

	@Override
	public synchronized void load()
	{
		_dominionWardLocations.clear();
		_fortCombatFlags.clear();
		_fortFlagPoleIds.clear();
		if (!parseDatapackDirectory(EVENTS_DIRECTORY, false))
		{
			LOGGER.warning(getClass().getSimpleName() + ": Directory not found, falling back to legacy path " + LEGACY_EVENTS_DIRECTORY + ".");
			parseDatapackDirectory(LEGACY_EVENTS_DIRECTORY, false);
		}

		int wardCount = 0;
		for (Map<Integer, Location> locations : _dominionWardLocations.values())
		{
			wardCount += locations.size();
		}
		int combatFlagCount = 0;
		for (List<Location> locations : _fortCombatFlags.values())
		{
			combatFlagCount += locations.size();
		}
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + wardCount + " territory ward locations, " + combatFlagCount + " combat flag locations and " + _fortFlagPoleIds.size() + " fortress flag poles.");
	}

	@Override
	public void parseDocument(Document document, File file)
	{
		final Integer residenceId = getResidenceId(file);
		if (residenceId == null)
		{
			return;
		}

		forEach(document, "list", listNode -> forEach(listNode, "event", eventNode -> forEach(eventNode, "objects", objectsNode ->
		{
			for (Node child = objectsNode.getFirstChild(); child != null; child = child.getNextSibling())
			{
				switch (child.getNodeName())
				{
					case "territory_ward":
					{
						final int itemId = parseInteger(child.getAttributes(), "item_id", 0);
						final int territoryId = itemId - WARD_TERRITORY_OFFSET;
						if (territoryId > 0)
						{
							_dominionWardLocations.computeIfAbsent(residenceId, key -> new HashMap<>()).put(territoryId, new Location(parseInteger(child.getAttributes(), "x", 0), parseInteger(child.getAttributes(), "y", 0), parseInteger(child.getAttributes(), "z", 0)));
						}
						break;
					}
					case "combat_flag":
					{
						_fortCombatFlags.computeIfAbsent(residenceId, key -> new ArrayList<>()).add(new Location(parseInteger(child.getAttributes(), "x", 0), parseInteger(child.getAttributes(), "y", 0), parseInteger(child.getAttributes(), "z", 0)));
						break;
					}
					case "static_object":
					{
						_fortFlagPoleIds.put(residenceId, parseInteger(child.getAttributes(), "id", 0));
						break;
					}
				}
			}
		})));
	}

	@Override
	public boolean isValidating()
	{
		return false;
	}

	public Location getWardLocation(int territoryId, int wardTerritoryId)
	{
		final Map<Integer, Location> territoryWardLocations = _dominionWardLocations.get(territoryId);
		return territoryWardLocations != null ? territoryWardLocations.get(wardTerritoryId) : null;
	}

	public List<Location> getCombatFlagLocations(int fortId)
	{
		return _fortCombatFlags.containsKey(fortId) ? Collections.unmodifiableList(_fortCombatFlags.get(fortId)) : Collections.emptyList();
	}

	public int getFlagPoleId(int fortId)
	{
		return _fortFlagPoleIds.getOrDefault(fortId, 0);
	}

	private Integer getResidenceId(File file)
	{
		final Matcher matcher = FILE_ID_PATTERN.matcher(file.getName());
		if (!matcher.matches())
		{
			return null;
		}
		return Integer.parseInt(matcher.group(1));
	}

	public static ParserSiegeEventData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final ParserSiegeEventData INSTANCE = new ParserSiegeEventData();
	}
}