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

import java.awt.Polygon;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;

/**
 * Parser mapregion restart points data.
 */
public class ParserMapRegionData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ParserMapRegionData.class.getName());
	
	private final List<RestartArea> _areas = new ArrayList<>();
	private final Map<String, RestartLocationSet> _locations = new HashMap<>();
	
	protected ParserMapRegionData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_areas.clear();
		_locations.clear();
		parseDatapackFile("data/parser/mapregion/restart_points.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _areas.size() + " restart areas and " + _locations.size() + " restart locations.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (!"list".equalsIgnoreCase(n.getNodeName()))
			{
				continue;
			}
			
			for (Node c = n.getFirstChild(); c != null; c = c.getNextSibling())
			{
				if ("restart_area".equalsIgnoreCase(c.getNodeName()))
				{
					parseRestartArea(c);
				}
				else if ("restart_loc".equalsIgnoreCase(c.getNodeName()))
				{
					parseRestartLocation(c);
				}
			}
		}
	}
	
	public Location getNearestTownRestart(Creature creature)
	{
		return getRestartLocation(creature, false);
	}
	
	public Location getNearestChaoticRestart(Creature creature)
	{
		return getRestartLocation(creature, true);
	}
	
	private Location getRestartLocation(Creature creature, boolean chaotic)
	{
		for (RestartArea area : _areas)
		{
			if (!area.isInside(creature.getX(), creature.getY(), creature.getZ()))
			{
				continue;
			}
			
			final String locationName = area.getLocationName(creature.getRace());
			if (locationName == null)
			{
				continue;
			}
			
			final RestartLocationSet restartLocation = _locations.get(normalizeName(locationName));
			if (restartLocation == null)
			{
				continue;
			}
			
			final Location location = restartLocation.getLocation(chaotic);
			if (location != null)
			{
				return location;
			}
		}
		
		return null;
	}
	
	private void parseRestartArea(Node areaNode)
	{
		final RestartArea area = new RestartArea();
		for (Node c = areaNode.getFirstChild(); c != null; c = c.getNextSibling())
		{
			if ("polygon".equalsIgnoreCase(c.getNodeName()))
			{
				for (Node p = c.getFirstChild(); p != null; p = p.getNextSibling())
				{
					if (!"coords".equalsIgnoreCase(p.getNodeName()))
					{
						continue;
					}
					
					final int[] coords = parseCoords(parseString(p.getAttributes(), "loc"));
					if (coords.length >= 4)
					{
						area.addPoint(coords[0], coords[1], coords[2], coords[3]);
					}
				}
			}
			else if ("region".equalsIgnoreCase(c.getNodeName()))
			{
				final String map = parseString(c.getAttributes(), "map");
				if (map != null)
				{
					area.addRegion(map);
				}
			}
			else if ("restart".equalsIgnoreCase(c.getNodeName()))
			{
				final NamedNodeMap attrs = c.getAttributes();
				final String race = parseString(attrs, "race");
				final String locationName = parseString(attrs, "loc");
				if ((race != null) && (locationName != null))
				{
					area.addRestart(race, locationName);
				}
			}
		}
		
		if (area.isValid())
		{
			_areas.add(area);
		}
	}
	
	private void parseRestartLocation(Node restartLocNode)
	{
		final String name = parseString(restartLocNode.getAttributes(), "name");
		if (name == null)
		{
			return;
		}
		
		final RestartLocationSet locationSet = _locations.computeIfAbsent(normalizeName(name), key -> new RestartLocationSet());
		for (Node c = restartLocNode.getFirstChild(); c != null; c = c.getNextSibling())
		{
			final boolean normalPoint = "restart_point".equalsIgnoreCase(c.getNodeName());
			final boolean pkPoint = "PKrestart_point".equalsIgnoreCase(c.getNodeName());
			if (!normalPoint && !pkPoint)
			{
				continue;
			}
			
			for (Node p = c.getFirstChild(); p != null; p = p.getNextSibling())
			{
				if (!"coords".equalsIgnoreCase(p.getNodeName()))
				{
					continue;
				}
				
				final int[] coords = parseCoords(parseString(p.getAttributes(), "loc"));
				if (coords.length >= 3)
				{
					locationSet.addLocation(normalPoint, new Location(coords[0], coords[1], coords[2]));
				}
			}
		}
	}
	
	private String normalizeName(String value)
	{
		if (value == null)
		{
			return "";
		}
		
		String normalized = value.trim();
		if (normalized.startsWith("[") && normalized.endsWith("]") && (normalized.length() > 1))
		{
			normalized = normalized.substring(1, normalized.length() - 1);
		}
		return normalized.toLowerCase(Locale.ENGLISH);
	}
	
	private int[] parseCoords(String locValue)
	{
		if ((locValue == null) || locValue.isBlank())
		{
			return new int[0];
		}
		
		final String[] split = locValue.trim().split("\\s+");
		final int[] result = new int[split.length];
		for (int i = 0; i < split.length; i++)
		{
			result[i] = Integer.parseInt(split[i]);
		}
		return result;
	}
	
	private static class RestartLocationSet
	{
		private final List<Location> _normalLocations = new ArrayList<>();
		private final List<Location> _pkLocations = new ArrayList<>();
		
		public void addLocation(boolean normalPoint, Location location)
		{
			if (normalPoint)
			{
				_normalLocations.add(location);
			}
			else
			{
				_pkLocations.add(location);
			}
		}
		
		public Location getLocation(boolean chaotic)
		{
			if (chaotic && !_pkLocations.isEmpty())
			{
				return getRandomOrFirst(_pkLocations);
			}
			
			if (!_normalLocations.isEmpty())
			{
				return getRandomOrFirst(_normalLocations);
			}
			
			if (!_pkLocations.isEmpty())
			{
				return getRandomOrFirst(_pkLocations);
			}
			
			return null;
		}
		
		private Location getRandomOrFirst(List<Location> locations)
		{
			if (PlayerConfig.RANDOM_RESPAWN_IN_TOWN_ENABLED)
			{
				return locations.get(Rnd.get(locations.size()));
			}
			
			return locations.get(0);
		}
	}
	
	private class RestartArea
	{
		private final Polygon _polygon = new Polygon();
		private int _minZ = Integer.MAX_VALUE;
		private int _maxZ = Integer.MIN_VALUE;
		private final Set<Long> _regions = new HashSet<>();
		private final Map<String, String> _raceRestart = new HashMap<>();
		
		public void addPoint(int x, int y, int z1, int z2)
		{
			_polygon.addPoint(x, y);
			final int low = Math.min(z1, z2);
			final int high = Math.max(z1, z2);
			if (low < _minZ)
			{
				_minZ = low;
			}
			if (high > _maxZ)
			{
				_maxZ = high;
			}
		}
		
		public void addRegion(String map)
		{
			final String[] split = map.split("_");
			if (split.length != 2)
			{
				return;
			}
			
			_regions.add(toRegionKey(Integer.parseInt(split[0]), Integer.parseInt(split[1])));
		}
		
		public void addRestart(String race, String locationName)
		{
			_raceRestart.put(normalizeName(race), locationName);
		}
		
		public String getLocationName(Race race)
		{
			String locationName = _raceRestart.get(normalizeName(race.name()));
			if (locationName == null)
			{
				switch (race)
				{
					case DARK_ELF:
					{
						locationName = _raceRestart.get("darkelf");
						break;
					}
					case HIGH_ELF:
					{
						locationName = _raceRestart.get("elf");
						break;
					}
					default:
					{
						break;
					}
				}
			}
			if (locationName == null)
			{
				locationName = _raceRestart.get("human");
			}
			return locationName;
		}
		
		public boolean isInside(int x, int y, int z)
		{
			if (_regions.contains(toRegionKey((x >> 15) + 20, (y >> 15) + 18)))
			{
				return true;
			}
			
			if ((_polygon.npoints < 3) || (z < _minZ) || (z > _maxZ))
			{
				return false;
			}
			
			return _polygon.contains(x, y);
		}
		
		public boolean isValid()
		{
			return ((_polygon.npoints >= 3) || !_regions.isEmpty()) && !_raceRestart.isEmpty();
		}
		
		private long toRegionKey(int regionX, int regionY)
		{
			return (((long) regionX) << 32) | (regionY & 0xffffffffL);
		}
	}
	
	public static ParserMapRegionData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ParserMapRegionData INSTANCE = new ParserMapRegionData();
	}
}