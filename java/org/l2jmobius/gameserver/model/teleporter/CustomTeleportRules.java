package org.l2jmobius.gameserver.model.teleporter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.MapRegionManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.MapRegion;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Fraction;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.siege.Castle;

public class CustomTeleportRules
{
    private static final Logger LOGGER = Logger.getLogger(CustomTeleportRules.class.getName());

    public enum Town
    {
        TALKING_ISLAND("Talking Island Town"),
        ELVEN("Elven Town"),
        DARK_ELVEN("Darkelven Town"),
        KAMAEL("Jin Kamael Village"),
        GLUDIN("Gludin Castle Town"),
        GLUDIO("Gludio Castle Town"),
        DION("Dion Castle Town"),
        GIRAN("Giran Castle Town"),
        HEINE("Heiness region"),
        HUNTERS("Hunters Village"),
        OREN("Oren Castle Town"),
        ADEN("Aden Castle Town"),
        GODDARD("Goddard Town"),
        RUNE("Rune Town"),
        SCHUTTGART("Town of Schuttgart"),
        ORC("Orc Town"),
        DWARVEN("Dwarven Town"),
        UNKNOWN("");

        private final String _regionName;

        Town(String regionName)
        {
            _regionName = regionName;
        }

        public String getRegionName()
        {
            return _regionName;
        }

        public static Town fromRegion(String regionName)
        {
            for (Town t : values())
            {
                if (t.getRegionName().equalsIgnoreCase(regionName))
                {
                    return t;
                }
            }
            return UNKNOWN;
        }
        
        public static Town getTownByPos(int x, int y)
        {
            if (x > 95000 && x < 125000 && y > 200000 && y < 240000)
            {
                return HEINE;
            }
            MapRegion region = MapRegionManager.getInstance().getMapRegion(x, y);
            if (region != null)
            {
                String t = region.getTown();
                return fromRegion(t);
            }
            return UNKNOWN;
        }
    }

    public static List<Town> getAllowedDestinations(Town from)
    {
        switch (from)
        {
            case TALKING_ISLAND:
                return Arrays.asList(Town.ELVEN);
            case KAMAEL:
                return Arrays.asList(Town.DARK_ELVEN);
            case DARK_ELVEN:
                return Arrays.asList(Town.KAMAEL, Town.GLUDIN);
            case ELVEN:
                return Arrays.asList(Town.TALKING_ISLAND, Town.GLUDIN);
            case GLUDIN:
                return Arrays.asList(Town.ELVEN, Town.DARK_ELVEN, Town.GLUDIO, Town.DION, Town.GIRAN, Town.HEINE, Town.HUNTERS, Town.OREN, Town.ADEN, Town.GODDARD, Town.RUNE, Town.SCHUTTGART, Town.ORC, Town.DWARVEN);
            case GLUDIO:
                return Arrays.asList(Town.GLUDIN, Town.DION, Town.GIRAN, Town.HEINE, Town.HUNTERS, Town.OREN, Town.ADEN, Town.GODDARD, Town.RUNE, Town.SCHUTTGART, Town.ORC, Town.DWARVEN);
            case DION:
                return Arrays.asList(Town.GLUDIN, Town.GLUDIO, Town.GIRAN, Town.HEINE, Town.HUNTERS, Town.OREN, Town.ADEN, Town.GODDARD, Town.RUNE, Town.SCHUTTGART, Town.ORC, Town.DWARVEN);
            case GIRAN:
                return Arrays.asList(Town.GLUDIN, Town.GLUDIO, Town.DION, Town.HEINE, Town.HUNTERS, Town.OREN, Town.ADEN, Town.GODDARD, Town.RUNE, Town.SCHUTTGART, Town.ORC, Town.DWARVEN);
            case HEINE:
                return Arrays.asList(Town.GLUDIN, Town.GLUDIO, Town.DION, Town.GIRAN, Town.HUNTERS, Town.OREN, Town.ADEN, Town.GODDARD, Town.RUNE, Town.SCHUTTGART, Town.ORC, Town.DWARVEN);
            case HUNTERS:
                return Arrays.asList(Town.GLUDIN, Town.GLUDIO, Town.DION, Town.GIRAN, Town.HEINE, Town.OREN, Town.ADEN, Town.GODDARD, Town.RUNE, Town.SCHUTTGART, Town.ORC, Town.DWARVEN);
            case OREN:
                return Arrays.asList(Town.GLUDIN, Town.GLUDIO, Town.DION, Town.GIRAN, Town.HEINE, Town.HUNTERS, Town.ADEN, Town.GODDARD, Town.RUNE, Town.SCHUTTGART, Town.ORC, Town.DWARVEN);
            case ADEN:
                return Arrays.asList(Town.GLUDIN, Town.GLUDIO, Town.DION, Town.GIRAN, Town.HEINE, Town.HUNTERS, Town.OREN, Town.GODDARD, Town.RUNE, Town.SCHUTTGART, Town.ORC, Town.DWARVEN);
            case GODDARD:
                return Arrays.asList(Town.GLUDIN, Town.GLUDIO, Town.DION, Town.GIRAN, Town.HEINE, Town.HUNTERS, Town.OREN, Town.ADEN, Town.RUNE, Town.SCHUTTGART, Town.ORC, Town.DWARVEN);
            case RUNE:
                return Arrays.asList(Town.GLUDIN, Town.GLUDIO, Town.DION, Town.GIRAN, Town.HEINE, Town.HUNTERS, Town.OREN, Town.ADEN, Town.GODDARD, Town.SCHUTTGART, Town.ORC, Town.DWARVEN);
            case SCHUTTGART:
                return Arrays.asList(Town.GLUDIN, Town.GLUDIO, Town.DION, Town.GIRAN, Town.HEINE, Town.HUNTERS, Town.OREN, Town.ADEN, Town.GODDARD, Town.RUNE, Town.ORC, Town.DWARVEN);
            case ORC:
                return Arrays.asList(Town.GLUDIN, Town.GLUDIO, Town.DION, Town.GIRAN, Town.HEINE, Town.HUNTERS, Town.OREN, Town.ADEN, Town.GODDARD, Town.RUNE, Town.SCHUTTGART, Town.DWARVEN);
            case DWARVEN:
                return Arrays.asList(Town.GLUDIN, Town.GLUDIO, Town.DION, Town.GIRAN, Town.HEINE, Town.HUNTERS, Town.OREN, Town.ADEN, Town.GODDARD, Town.RUNE, Town.SCHUTTGART, Town.ORC);
            default:
                return Arrays.asList();
        }
    }

    public static boolean canTeleport(Player player, Npc npc, Location loc, boolean isNoblesse)
    {
        if (npc != null && npc.getClanHall() != null)
        {
            player.sendMessage("Услуги Gatekeeper в Клан Холле отключены.");
            return false;
        }

        Town targetTown = Town.getTownByPos(loc.getX(), loc.getY());
        Town currentTown = Town.getTownByPos(player.getX(), player.getY());
        
        boolean isSourceCastleOrFort = npc != null && (npc.getCastle() != null || npc.getFort() != null);
        
        if (targetTown == Town.UNKNOWN)
        {
            if (!isSourceCastleOrFort && !isNoblesse) 
            {
                player.sendMessage("Телепорт в зоны охоты доступен только из Замков или Фортов.");
                return false;
            }
            
            if (isSourceCastleOrFort)
            {
                Clan clan = player.getClan();
                Castle castle = npc.getCastle();
                org.l2jmobius.gameserver.model.siege.Fort fort = npc.getFort();
                
                boolean isOwner = false;
                if (clan != null)
                {
                    if (castle != null && castle.getOwnerId() == clan.getId()) isOwner = true;
                    if (fort != null && fort.getOwnerClan() == clan) isOwner = true;
                }
                
                if (!isOwner)
                {
                    player.sendMessage("Этот телепорт доступен только владельцам.");
                    return false;
                }
            }
            return true; 
        }
        else
        {
            if (isNoblesse && isSourceCastleOrFort)
            {
                player.sendMessage("Дворянские телепорты отключены в замках и фортах.");
                return false;
            }

            if (currentTown != Town.UNKNOWN && currentTown != targetTown)
            {
                List<Town> allowed = getAllowedDestinations(currentTown);
                if (!allowed.contains(targetTown))
                {
                    player.sendMessage("Прямой телепорт в этот город невозможен.");
                    return false;
                }
            }

            

            

            
        }
        
        return true;
    }

        public static Location getFinalLocation(Player player, Location originalLoc)
    {
        Town targetTown = Town.getTownByPos(originalLoc.getX(), originalLoc.getY());
        if (targetTown != Town.UNKNOWN)
        {
            MapRegion region = MapRegionManager.getInstance().getMapRegion(originalLoc.getX(), originalLoc.getY());
            if (region != null)
            {
                boolean isChaotic = false;
                int targetCastleId = region.getCastleId();
                if (targetCastleId > 0)
                {
                    org.l2jmobius.gameserver.model.siege.Castle destCastle = org.l2jmobius.gameserver.managers.CastleManager.getInstance().getCastleById(targetCastleId);
                    if (destCastle != null && destCastle.getOwnerId() > 0)
                    {
                        org.l2jmobius.gameserver.model.clan.Clan ownerClan = org.l2jmobius.gameserver.data.sql.ClanTable.getInstance().getClan(destCastle.getOwnerId());
                        if (ownerClan != null)
                        {
                            Fraction cFraction = ownerClan.getFraction();
                            if (cFraction != Fraction.NONE && player.getFraction() != Fraction.NONE && player.getFraction() != cFraction)
                            {
                                isChaotic = true;
                            }
                        }
                    }
                }
                
                                Location finalSpawn = isChaotic ? region.getChaoticSpawnLoc() : region.getSpawnLoc();
                if (finalSpawn != null)
                {
                    return finalSpawn;
                }
            }
        }
        return originalLoc;
    }
}
