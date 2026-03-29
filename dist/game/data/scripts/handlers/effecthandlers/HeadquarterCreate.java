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
package handlers.effecthandlers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import handlers.util.HeadquarterBuildHelper;

import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.SiegeFlag;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * Headquarter Create effect implementation.
 * @author Adry_85
 */
public class HeadquarterCreate extends AbstractEffect
{
	private static final int HQ_NPC_ID = 35062;
	private final boolean _isAdvanced;
	private final String _headquarterType;
	
	public HeadquarterCreate(StatSet params)
	{
		_isAdvanced = params.getBoolean("isAdvanced", false);
		_headquarterType = params.getString("flagType", "PERSONAL").toUpperCase();
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player player = effector.asPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!HeadquarterBuildHelper.canBuildHeadquarter(player, isPartyHeadquarter()))
		{
			return;
		}

		if (isPartyHeadquarter())
		{
			final Party party = player.getParty();
			final SiegeFlag partyFlag = getHeadquarterFlag(party);
			if (partyFlag != null)
			{
				partyFlag.deleteMe();
			}
		}
		else
		{
			final SiegeFlag playerFlag = getHeadquarterFlag(player);
			if (playerFlag != null)
			{
				playerFlag.deleteMe();
			}
		}
		
		final SiegeFlag flag = createSiegeFlag(player);
		if (flag == null)
		{
			return;
		}
		flag.setTitle(player.getName());
		flag.setCurrentHpMp(flag.getMaxHp(), flag.getMaxMp());
		flag.setHeading(player.getHeading());
		flag.spawnMe(player.getX(), player.getY(), player.getZ() + 50);
	}

	private boolean isPartyHeadquarter()
	{
		return "PARTY".equals(_headquarterType);
	}

	private SiegeFlag createSiegeFlag(Player player)
	{
		final NpcTemplate template = NpcData.getInstance().getTemplate(HQ_NPC_ID);
		if (template == null)
		{
			return null;
		}

		try
		{
			final Class<?> enumClass = Class.forName("org.l2jmobius.gameserver.model.actor.instance.SiegeFlag$HeadquarterType");
			@SuppressWarnings("unchecked")
			final Object enumValue = Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), _headquarterType);
			final Constructor<SiegeFlag> constructor = SiegeFlag.class.getConstructor(Player.class, NpcTemplate.class, boolean.class, enumClass);
			return constructor.newInstance(player, template, _isAdvanced, enumValue);
		}
		catch (Exception e)
		{
			try
			{
				final Constructor<SiegeFlag> constructor = SiegeFlag.class.getConstructor(Player.class, NpcTemplate.class, boolean.class);
				return constructor.newInstance(player, template, _isAdvanced);
			}
			catch (Exception ignored)
			{
				return null;
			}
		}
	}

	private SiegeFlag getHeadquarterFlag(Object owner)
	{
		final Object flag = invokeNoArg(owner, "getHeadquarterFlag");
		if (flag instanceof SiegeFlag)
		{
			return (SiegeFlag) flag;
		}

		final Object fieldValue = getFieldValue(owner, "_headquarterFlag");
		if (fieldValue instanceof SiegeFlag)
		{
			return (SiegeFlag) fieldValue;
		}
		return null;
	}

	private Object invokeNoArg(Object target, String methodName)
	{
		if (target == null)
		{
			return null;
		}

		try
		{
			final Method method = target.getClass().getMethod(methodName);
			return method.invoke(target);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	private Object getFieldValue(Object target, String fieldName)
	{
		if (target == null)
		{
			return null;
		}

		Class<?> type = target.getClass();
		while (type != null)
		{
			try
			{
				final Field field = type.getDeclaredField(fieldName);
				field.setAccessible(true);
				return field.get(target);
			}
			catch (Exception e)
			{
				type = type.getSuperclass();
			}
		}
		return null;
	}
}
