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
package org.l2jmobius.gameserver.model.actor.instance;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.status.SiegeFlagStatus;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class SiegeFlag extends Npc
{
	public enum HeadquarterType
	{
		PARTY,
		PERSONAL
	}

	private final Player _owner;
	private final Party _party;
	private final boolean _isAdvanced;
	private final HeadquarterType _headquarterType;
	private boolean _canTalk;

	public SiegeFlag(Player player, NpcTemplate template, boolean advanced, HeadquarterType headquarterType)
	{
		super(template);
		setInstanceType(InstanceType.SiegeFlag);

		_headquarterType = headquarterType;
		_owner = _headquarterType == HeadquarterType.PERSONAL ? player : null;
		_party = _headquarterType == HeadquarterType.PARTY ? player.getParty() : null;
		_canTalk = true;

		if (_headquarterType == HeadquarterType.PARTY)
		{
			if (_party == null)
			{
				throw new NullPointerException(getClass().getSimpleName() + ": Initialization failed.");
			}
			_party.setHeadquarterFlag(this);
		}
		else
		{
			if (_owner == null)
			{
				throw new NullPointerException(getClass().getSimpleName() + ": Initialization failed.");
			}
			_owner.setHeadquarterFlag(this);
		}

		_isAdvanced = advanced;
		getStatus();
		setInvul(false);
	}
	
	@Override
	public boolean canBeAttacked()
	{
		return !isInvul();
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if (isInvul())
		{
			return false;
		}

		if (attacker == null)
		{
			return true;
		}

		final Player attackerPlayer = attacker.asPlayer();
		if ((_headquarterType == HeadquarterType.PARTY) && (attackerPlayer != null))
		{
			return attackerPlayer.getParty() != _party;
		}
		if ((_headquarterType == HeadquarterType.PERSONAL) && (attackerPlayer != null))
		{
			return attackerPlayer != _owner;
		}
		return true;
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		clearHeadquarterReference();
		
		return true;
	}

	@Override
	public boolean deleteMe()
	{
		clearHeadquarterReference();
		return super.deleteMe();
	}
	
	@Override
	public void onForcedAttack(Player player)
	{
		onAction(player);
	}
	
	@Override
	public void onAction(Player player, boolean interact)
	{
		if ((player == null) || !canTarget(player))
		{
			return;
		}
		
		// Check if the Player already target the Npc
		if (this != player.getTarget())
		{
			// Set the target of the Player player
			player.setTarget(this);
		}
		else if (interact)
		{
			if (isAutoAttackable(player) && (Math.abs(player.getZ() - getZ()) < 100))
			{
				player.getAI().setIntention(Intention.ATTACK, this);
			}
			else
			{
				// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
	}
	
	public boolean isAdvancedHeadquarter()
	{
		return _isAdvanced;
	}
	
	@Override
	public SiegeFlagStatus getStatus()
	{
		return (SiegeFlagStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new SiegeFlagStatus(this));
	}
	
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, Skill skill)
	{
		super.reduceCurrentHp(damage, attacker, skill);
		if (!canTalk())
		{
			return;
		}

		if ((_headquarterType == HeadquarterType.PARTY) && (_party != null))
		{
			_party.getMembers().forEach(member -> member.sendPacket(SystemMessageId.SIEGE_CAMP_IS_UNDER_ATTACK));
			setCanTalk(false);
			ThreadPool.schedule(new ScheduleTalkTask(), 20000);
		}
		else if ((_headquarterType == HeadquarterType.PERSONAL) && (_owner != null))
		{
			_owner.sendPacket(SystemMessageId.SIEGE_CAMP_IS_UNDER_ATTACK);
			setCanTalk(false);
			ThreadPool.schedule(new ScheduleTalkTask(), 20000);
		}
	}

	@Override
	public boolean isVisibleFor(Player player)
	{
		if ((player == null) || !super.isVisibleFor(player))
		{
			return false;
		}

		if (player.isGM())
		{
			return true;
		}

		if (_headquarterType == HeadquarterType.PERSONAL)
		{
			return player == _owner;
		}
		if (_headquarterType == HeadquarterType.PARTY)
		{
			return player.getParty() == _party;
		}

		return true;
	}

	private void clearHeadquarterReference()
	{
		if ((_headquarterType == HeadquarterType.PARTY) && (_party != null) && (_party.getHeadquarterFlag() == this))
		{
			_party.setHeadquarterFlag(null);
		}
		else if ((_headquarterType == HeadquarterType.PERSONAL) && (_owner != null) && (_owner.getHeadquarterFlag() == this))
		{
			_owner.setHeadquarterFlag(null);
		}
	}
	
	private class ScheduleTalkTask implements Runnable
	{
		public ScheduleTalkTask()
		{
		}
		
		@Override
		public void run()
		{
			setCanTalk(true);
		}
	}
	
	void setCanTalk(boolean value)
	{
		_canTalk = value;
	}
	
	private boolean canTalk()
	{
		return _canTalk;
	}
}
