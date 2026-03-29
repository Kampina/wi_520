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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.tierspawn.TierSpawnZone;

/**
 * Loads WI-compatible raid boss spawn counters from data/custom/TierSpawnZones.xml.
 */
public class TierSpawnData implements IXmlReader
{
	private final Map<Integer, TierSpawnZone> _zones = new HashMap<>();
	
	protected TierSpawnData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_zones.clear();
		parseDatapackFile("data/custom/TierSpawnZones.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _zones.size() + " tier spawn zones.");
	}

	@Override
	public boolean isValidating()
	{
		return false;
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode ->
			forEach(listNode, "zone", zoneNode ->
			{
				final int zoneId = parseInteger(zoneNode.getAttributes(), "id");
				final String zoneName = parseString(zoneNode.getAttributes(), "name", "Zone_" + zoneId);
				final int locaId = parseInteger(zoneNode.getAttributes(), "locaId", parseInteger(zoneNode.getAttributes(), "locationId", 0));
				final Set<Integer> monsterIds = new HashSet<>();
				final int[] killThresholds =
				{
					-1,
					-1,
					-1
				};
				final int[] raidBossIds = new int[TierSpawnZone.TIER_COUNT];
				final int[][] spawnLocations = new int[TierSpawnZone.TIER_COUNT][4];
				
				for (Node childNode = zoneNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling())
				{
					if ("monster".equals(childNode.getNodeName()))
					{
						monsterIds.add(parseInteger(childNode.getAttributes(), "id"));
						continue;
					}
					
					if (!"tier".equals(childNode.getNodeName()))
					{
						continue;
					}
					
					final int tierIndex = parseInteger(childNode.getAttributes(), "level", 1) - 1;
					if ((tierIndex < 0) || (tierIndex >= TierSpawnZone.TIER_COUNT))
					{
						LOGGER.warning(getClass().getSimpleName() + ": Zone " + zoneId + " (" + zoneName + ") has unsupported tier level, skipping tier node.");
						continue;
					}
					
					killThresholds[tierIndex] = parseInteger(childNode.getAttributes(), "killThreshold", killThresholds[tierIndex]);
					for (Node tierChild = childNode.getFirstChild(); tierChild != null; tierChild = tierChild.getNextSibling())
					{
						if ("raidBoss".equals(tierChild.getNodeName()))
						{
							raidBossIds[tierIndex] = parseInteger(tierChild.getAttributes(), "id", 0);
						}
						else if ("spawn".equals(tierChild.getNodeName()) || "spawnLocation".equals(tierChild.getNodeName()) || "nextSpawn".equals(tierChild.getNodeName()))
						{
							spawnLocations[tierIndex][0] = parseInteger(tierChild.getAttributes(), "x", 0);
							spawnLocations[tierIndex][1] = parseInteger(tierChild.getAttributes(), "y", 0);
							spawnLocations[tierIndex][2] = parseInteger(tierChild.getAttributes(), "z", 0);
							spawnLocations[tierIndex][3] = parseInteger(tierChild.getAttributes(), "heading", 0);
						}
					}
				}
				
				if (monsterIds.isEmpty())
				{
					LOGGER.warning(getClass().getSimpleName() + ": Zone " + zoneId + " (" + zoneName + ") has no tracked monster ids, skipping.");
					return;
				}
				
				_zones.put(zoneId, new TierSpawnZone(zoneId, zoneName, locaId, monsterIds, killThresholds, raidBossIds, spawnLocations));
			})
		);
	}
	
	public Map<Integer, TierSpawnZone> getZones()
	{
		return _zones;
	}
	
	public TierSpawnZone getZone(int id)
	{
		return _zones.get(id);
	}
	
	public static TierSpawnData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TierSpawnData INSTANCE = new TierSpawnData();
	}
}
