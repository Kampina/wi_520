package org.l2jmobius.gameserver.model.zone.type;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.entity.SevenSigns;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;

public class SevenSignsZone extends ZoneType
{
private int _expPenalty = 100;

public SevenSignsZone(int id)
{
super(id);
}

@Override
public void setParameter(String name, String value)
{
if (name.equalsIgnoreCase("expPenalty"))
{
_expPenalty = Integer.parseInt(value);
}
else if (name.equalsIgnoreCase("itemDrop"))
{
// Ignore since we want items to drop normally
}
else
{
super.setParameter(name, value);
}
}

public int getExpPenalty()
{
return _expPenalty;
}

@Override
protected void onEnter(Creature creature)
{
if (creature.isPlayer())
{
Player player = creature.asPlayer();
if (!player.isGM())
{
int playerCabal = SevenSigns.getInstance().getPlayerCabal(player);
int compWinner = SevenSigns.getInstance().getCabalHighestScore();

if (playerCabal == SevenSigns.CABAL_NULL)
{
player.teleToLocation(TeleportWhereType.TOWN);
return;
}

if (SevenSigns.getInstance().isSealValidationPeriod())
{
if (compWinner != SevenSigns.CABAL_NULL && playerCabal != compWinner)
{
player.teleToLocation(TeleportWhereType.TOWN);
return;
}
}
}

creature.setInsideZone(ZoneId.SEVEN_SIGNS, true);
}
}

@Override
protected void onExit(Creature creature)
{
if (creature.isPlayer())
{
creature.setInsideZone(ZoneId.SEVEN_SIGNS, false);
}
}
}
