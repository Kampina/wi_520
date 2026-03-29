package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.holders.SpawnHolder;

/**
 * Fort Data parser.
 */
public class FortData implements IXmlReader
{
private static final Map<Integer, List<SkillHolder>> _skills = new ConcurrentHashMap<>();
private static final Map<Integer, List<SpawnHolder>> _spawns = new ConcurrentHashMap<>();
private static final Map<Integer, List<Integer>> _doors = new ConcurrentHashMap<>();
	private static final Map<Integer, Map<Integer, String>> _doorGroups = new ConcurrentHashMap<>();

protected FortData()
{
load();
}

@Override
public void load()
{
_skills.clear();
_spawns.clear();
_doors.clear();
		_doorGroups.clear();
parseDatapackDirectory("data/residences/fortresses", true);
}

@Override
public void parseDocument(Document document, File file)
{
for (Node listNode = document.getFirstChild(); listNode != null; listNode = listNode.getNextSibling())
{
if ("list".equals(listNode.getNodeName()))
{
for (Node fortNode = listNode.getFirstChild(); fortNode != null; fortNode = fortNode.getNextSibling())
{
if ("fort".equals(fortNode.getNodeName()))
{
final int fortId = parseInteger(fortNode.getAttributes(), "id");
final List<SkillHolder> skillList = new ArrayList<>();
final List<SpawnHolder> spawnList = new ArrayList<>();
final List<Integer> doorList = new ArrayList<>();
						final Map<Integer, String> doorGroupsLocal = new java.util.HashMap<>();

for (Node tpNode = fortNode.getFirstChild(); tpNode != null; tpNode = tpNode.getNextSibling())
{
if ("skills".equalsIgnoreCase(tpNode.getNodeName()))
{
for (Node npcNode = tpNode.getFirstChild(); npcNode != null; npcNode = npcNode.getNextSibling())
{
if ("skill".equals(npcNode.getNodeName()))
{
final NamedNodeMap np = npcNode.getAttributes();
final int id = parseInteger(np, "id");
final int lvl = parseInteger(np, "lvl");
skillList.add(new SkillHolder(id, lvl));
}
}
}
else if ("doors".equalsIgnoreCase(tpNode.getNodeName()))
{
for (Node doorNode = tpNode.getFirstChild(); doorNode != null; doorNode = doorNode.getNextSibling())
{
if ("door".equals(doorNode.getNodeName()))
{
final NamedNodeMap np = doorNode.getAttributes();
int doorId = parseInteger(np, "id");
								doorList.add(doorId);
								String group = parseString(np, "group", null);
								if (group != null)
								{
									doorGroupsLocal.put(doorId, group);
								}
}
}
}
else if ("spawns".equalsIgnoreCase(tpNode.getNodeName()))
{
for (Node npcNode = tpNode.getFirstChild(); npcNode != null; npcNode = npcNode.getNextSibling())
{
if ("npc".equals(npcNode.getNodeName()))
{
final NamedNodeMap np = npcNode.getAttributes();
final int id = parseInteger(np, "id");
final int x = parseInteger(np, "x");
final int y = parseInteger(np, "y");
final int z = parseInteger(np, "z");
final int heading = parseInteger(np, "heading", 0);
spawnList.add(new SpawnHolder(id, x, y, z, heading));
}
}
}
}

if (!skillList.isEmpty())
{
_skills.put(fortId, skillList);
}
if (!spawnList.isEmpty())
{
_spawns.put(fortId, spawnList);
}
if (!doorList.isEmpty())
{
_doors.put(fortId, doorList);
}
}
}
}
}
}

public List<SkillHolder> getSkillsForFort(int fortId)
{
return _skills.getOrDefault(fortId, Collections.emptyList());
}

public List<SpawnHolder> getSpawnsForFort(int fortId)
{
return _spawns.getOrDefault(fortId, Collections.emptyList());
}
public List<Integer> getDoorsForFort(int fortId)
{
return _doors.getOrDefault(fortId, Collections.emptyList());
}

public String getDoorGroup(int fortId, int doorId)
{
Map<Integer, String> groups = _doorGroups.get(fortId);
return groups != null ? groups.get(doorId) : null;
}

public static FortData getInstance()
{
return SingletonHolder.INSTANCE;
}

private static class SingletonHolder
{
protected static final FortData INSTANCE = new FortData();
}
}
