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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Creature;

/**
 * Parser domains and spawn points data for additional respawn filtering.
 */
public class ParserSpawnDomainData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ParserSpawnDomainData.class.getName());
	
	private final List<DomainPolygon> _domains = new ArrayList<>();
	private final Map<Integer, List<Location>> _domainSpawnPoints = new HashMap<>();
	
	protected ParserSpawnDomainData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_domains.clear();
		_domainSpawnPoints.clear();
		parseDatapackFile("data/mapregion/domains.xml");
		parseDatapackDirectory("data/spawns/Castles/VillageGuard", false);
		parseDatapackDirectory("data/spawns/fortress", true);
		int points = 0;
		for (List<Location> domainPoints : _domainSpawnPoints.values())
		{
			points += domainPoints.size();
		}
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _domains.size() + " domain polygons and " + points + " spawn points.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		if (file.getPath().replace('\\', '/').contains("/mapregion/domains.xml"))
		{
			parseDomains(document);
		}
		else
		{
			parseSpawnPoints(document);
		}
	}
	
	@Override
	public boolean isValidating()
	{
		return false;
	}
	
	public Location filterRespawn(Creature creature, Location suggestedRespawn)
	{
		if ((creature == null) || (suggestedRespawn == null) || _domains.isEmpty())
		{
			return suggestedRespawn;
		}
		
		final int creatureDomainId = getDomainId(creature.getX(), creature.getY(), creature.getZ());
		if (creatureDomainId <= 0)
		{
			return suggestedRespawn;
		}
		
		final int suggestedDomainId = getDomainId(suggestedRespawn.getX(), suggestedRespawn.getY(), suggestedRespawn.getZ());
		if ((suggestedDomainId <= 0) || (creatureDomainId == suggestedDomainId))
		{
			return suggestedRespawn;
		}
		
		final List<Location> domainSpawns = _domainSpawnPoints.get(creatureDomainId);
		if ((domainSpawns == null) || domainSpawns.isEmpty())
		{
			return suggestedRespawn;
		}
		
		Location bestLocation = null;
		double bestDistance = Double.MAX_VALUE;
		for (Location location : domainSpawns)
		{
			final double distance = creature.calculateDistance2D(location);
			if (distance < bestDistance)
			{
				bestDistance = distance;
				bestLocation = location;
			}
		}
		return bestLocation != null ? bestLocation : suggestedRespawn;
	}
	
	private void parseDomains(Document document)
	{
		forEach(document, "list", listNode -> forEach(listNode, "domain", domainNode ->
		{
			final int domainId = parseInteger(domainNode.getAttributes(), "id", 0);
			forEach(domainNode, "polygon", polygonNode ->
			{
				final DomainPolygon domainPolygon = new DomainPolygon(domainId);
				forEach(polygonNode, "coords", coordsNode ->
				{
					final int[] coords = parseCoords(parseString(coordsNode.getAttributes(), "loc"));
					if (coords.length >= 4)
					{
						domainPolygon.addPoint(coords[0], coords[1], coords[2], coords[3]);
					}
				});
				if (domainPolygon.isValid())
				{
					_domains.add(domainPolygon);
				}
			});
		}));
	}
	
	private void parseSpawnPoints(Document document)
	{
		forEach(document, "list", listNode -> forEach(listNode, "spawn", spawnNode -> forEach(spawnNode, "group", groupNode -> forEach(groupNode, "npc", npcNode ->
		{
			final int x = parseInteger(npcNode.getAttributes(), "x", 0);
			final int y = parseInteger(npcNode.getAttributes(), "y", 0);
			final int z = parseInteger(npcNode.getAttributes(), "z", 0);
			final int domainId = getDomainId(x, y, z);
			if (domainId > 0)
			{
				_domainSpawnPoints.computeIfAbsent(domainId, key -> new ArrayList<>()).add(new Location(x, y, z));
			}
		}))));
	}
	
	private int getDomainId(int x, int y, int z)
	{
		for (DomainPolygon polygon : _domains)
		{
			if (polygon.contains(x, y, z))
			{
				return polygon.getDomainId();
			}
		}
		return 0;
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
	
	private static class DomainPolygon
	{
		private final int _domainId;
		private final Polygon _polygon = new Polygon();
		private int _minZ = Integer.MAX_VALUE;
		private int _maxZ = Integer.MIN_VALUE;
		
		public DomainPolygon(int domainId)
		{
			_domainId = domainId;
		}
		
		public int getDomainId()
		{
			return _domainId;
		}
		
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
		
		public boolean contains(int x, int y, int z)
		{
			if ((_polygon.npoints < 3) || (z < _minZ) || (z > _maxZ))
			{
				return false;
			}
			return _polygon.contains(x, y);
		}
		
		public boolean isValid()
		{
			return (_domainId > 0) && (_polygon.npoints >= 3);
		}
	}
	
	public static ParserSpawnDomainData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ParserSpawnDomainData INSTANCE = new ParserSpawnDomainData();
	}
}