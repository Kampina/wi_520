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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.holders.ElementalItemHolder;
import org.l2jmobius.gameserver.data.xml.ElementalAttributeData;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.model.actor.request.EnchantItemAttributeRequest;
import org.l2jmobius.gameserver.model.item.enchant.attribute.AttributeHolder;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExAttributeEnchantResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Mobius
 */
public class RequestExEnchantItemAttribute extends ClientPacket
{
	private int _objectId;
	private long _count;
	
	@Override
	protected void readImpl()
	{
		_objectId = readInt();
		_count = readLong();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final EnchantItemAttributeRequest request = player.getRequest(EnchantItemAttributeRequest.class);
		if (request == null)
		{
			return;
		}
		
		request.setProcessing(true);
		
		if (_objectId == 0xFFFFFFFF)
		{
			// Player canceled enchant
			player.removeRequest(request.getClass());
			player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			return;
		}
		
		if (!player.isOnline())
		{
			player.removeRequest(request.getClass());
			return;
		}
		
		if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_ADD_ELEMENTAL_POWER_WHILE_OPERATING_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			player.removeRequest(request.getClass());
			return;
		}
		
		if (player.isCombatFlagEquipped())
		{
			player.sendPacket(SystemMessageId.CANNOT_BE_USED_IF_YOU_ARE_HOLDING_A_FLAG);
			player.removeRequest(request.getClass());
			return;
		}
		
		// Restrict enchant during a trade (bug if enchant fails)
		if (player.getActiveRequester() != null)
		{
			// Cancel trade
			player.cancelActiveTrade();
			player.removeRequest(request.getClass());
			player.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_TRADING);
			return;
		}
		
		final Item item = player.getInventory().getItemByObjectId(_objectId);
		final Item stone = request.getEnchantingStone();
		if ((item == null) || (stone == null))
		{
			player.removeRequest(request.getClass());
			player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			return;
		}
		
		if (!item.isElementable())
		{
			player.sendPacket(SystemMessageId.ELEMENTAL_POWER_ENHANCER_USAGE_REQUIREMENT_IS_NOT_SUFFICIENT);
			player.removeRequest(request.getClass());
			return;
		}
		
		if (_count < 1)
		{
			player.removeRequest(request.getClass());
			return;
		}
		
		switch (item.getItemLocation())
		{
			case INVENTORY:
			case PAPERDOLL:
			{
				if (item.getOwnerId() != player.getObjectId())
				{
					player.removeRequest(request.getClass());
					return;
				}
				break;
			}
			default:
			{
				player.removeRequest(request.getClass());
				PunishmentManager.handleIllegalPlayerAction(player, player + " tried to use enchant Exploit!", GeneralConfig.DEFAULT_PUNISH);
				return;
			}
		}
		
		final int stoneId = stone.getId();
		final long count = Math.min(stone.getCount(), _count);
		AttributeType elementToAdd = ElementalAttributeData.getInstance().getItemElement(stoneId);

		final AttributeHolder oldElement = item.getAttribute(elementToAdd);
		final int elementValue = oldElement == null ? 0 : oldElement.getValue();
		
		final ElementalItemHolder elementItem = ElementalAttributeData.getInstance().getItemElemental(stoneId);
		if (elementItem == null)
		{
			player.removeRequest(request.getClass());
			return;
		}

		// Removed element check

		final int[] values = item.isWeapon() ? org.l2jmobius.gameserver.data.xml.ElementalAttributeData.WEAPON_VALUES : org.l2jmobius.gameserver.data.xml.ElementalAttributeData.ARMOR_VALUES;
		int currentLvl = 0;
		for (int i = 0; i < values.length; i++)
		{
			if (elementValue < values[i])
			{
				break;
			}
			currentLvl = i;
		}

		int maxLvl = (elementItem.getType() == org.l2jmobius.gameserver.model.item.enums.ElementalItemType.STONE || elementItem.getType() == org.l2jmobius.gameserver.model.item.enums.ElementalItemType.STONE_SUPER) ? 4 : 6;
		if (currentLvl >= maxLvl)
		{
			player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			player.removeRequest(request.getClass());
			return;
		}
		
		int usedStones = 0;
		int successfulAttempts = 0;
		int failedAttempts = 0;
		for (int i = 0; i < count; i++)
		{
			usedStones++;
			final int result = addElement(player, stone, item, elementToAdd);
			if (result == 1)
			{
				successfulAttempts++;
			}
			else if (result == 0)
			{
				failedAttempts++;
			}
			else
			{
				break;
			}
		}
		
		item.updateItemElementals();
		player.destroyItem(ItemProcessType.FEE, stone, usedStones, player, true);
		final AttributeHolder newElement = item.getAttribute(elementToAdd);
		final int newValue = newElement != null ? newElement.getValue() : 0;
		final AttributeType realElement = elementToAdd;
		final InventoryUpdate iu = new InventoryUpdate();
		if (successfulAttempts > 0)
		{
			SystemMessage sm;
			if (item.getEnchantLevel() == 0)
			{
				if (item.isArmor())
				{
					sm = new SystemMessage(SystemMessageId.S1_ARE_NOW_IMBUED_WITH_S2_ATTRIBUTE_AND_YOUR_S3_RESISTANCE_HAS_INCREASED);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S2_ATTRIBUTE_HAS_BEEN_ADDED_TO_S1);
				}
				
				sm.addItemName(item);
				sm.addAttribute(realElement.getClientId());
				if (item.isArmor())
				{
					sm.addAttribute(realElement.getClientId());
				}
			}
			else
			{
				if (item.isArmor())
				{
					sm = new SystemMessage(SystemMessageId.S3_POWER_HAS_BEEN_ADDED_TO_S1_S2_S4_RESISTANCE_IS_INCREASED);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_S2_S3_ATTRIBUTE_POWER_IS_ENABLED);
				}
				
				sm.addInt(item.getEnchantLevel());
				sm.addItemName(item);
				sm.addAttribute(realElement.getClientId());
				if (item.isArmor())
				{
					sm.addAttribute(realElement.getClientId());
				}
			}
			
			player.sendPacket(sm);
			
			// send packets
			iu.addModifiedItem(item);
		}
		else
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_FAILED_TO_ADD_ELEMENTAL_POWER);
		}
		
		int result = 0;
		if (successfulAttempts == 0)
		{
			// Failed
			result = 2;
		}
		
		// Stone must be removed
		if (stone.getCount() == 0)
		{
			iu.addRemovedItem(stone);
		}
		else
		{
			iu.addModifiedItem(stone);
		}
		
		player.removeRequest(request.getClass());
		player.sendPacket(new ExAttributeEnchantResult(result, item.isWeapon(), elementToAdd, elementValue, newValue, successfulAttempts, failedAttempts));
		player.updateUserInfo();
		player.sendInventoryUpdate(iu);
	}
	
	private int addElement(Player player, Item stone, Item item, AttributeType elementToAdd)
	{
		final AttributeHolder oldElement = item.getAttribute(elementToAdd);
		final int elementValue = oldElement == null ? 0 : oldElement.getValue();
		
		final ElementalItemHolder elementItem = ElementalAttributeData.getInstance().getItemElemental(stone.getId());
		if (elementItem == null) return -1;

		final int[] values = item.isWeapon() ? org.l2jmobius.gameserver.data.xml.ElementalAttributeData.WEAPON_VALUES : org.l2jmobius.gameserver.data.xml.ElementalAttributeData.ARMOR_VALUES;
		
		int currentLvl = 0;
		for (int i = 0; i < values.length; i++)
		{
			if (elementValue < values[i])
			{
				break;
			}
			currentLvl = i;
		}

		int maxLvl = (elementItem.getType() == org.l2jmobius.gameserver.model.item.enums.ElementalItemType.STONE || elementItem.getType() == org.l2jmobius.gameserver.model.item.enums.ElementalItemType.STONE_SUPER) ? 4 : 6;
		
		if (currentLvl >= maxLvl)
		{
			player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			player.removeRequest(EnchantItemAttributeRequest.class);
			return -1;
		}

		int chance = (elementItem.getType() == org.l2jmobius.gameserver.model.item.enums.ElementalItemType.STONE || elementItem.getType() == org.l2jmobius.gameserver.model.item.enums.ElementalItemType.STONE_SUPER) ? 50 : 60;
		boolean success = org.l2jmobius.commons.util.Rnd.get(100) < chance;

		if (success)
		{
			int nextValue = values[currentLvl + 1];
			item.setAttribute(new AttributeHolder(elementToAdd, nextValue), false);
		}
		else
		{
			item.clearAttribute(elementToAdd);
			player.sendMessage("Attribute insertion failed. The elemental power has been reset to 0.");
		}

		return success ? 1 : 0;
	}
	
	public int getLimit(Item item, int sotneId)
	{
		final ElementalItemHolder elementItem = ElementalAttributeData.getInstance().getItemElemental(sotneId);
		if (elementItem == null)
		{
			return 0;
		}
		
		if (item.isWeapon())
		{
			return ElementalAttributeData.WEAPON_VALUES[elementItem.getType().getMaxLevel()];
		}
		
		return ElementalAttributeData.ARMOR_VALUES[elementItem.getType().getMaxLevel()];
	}
	
	public int getPowerToAdd(int stoneId, int oldValue, Item item)
	{
		if (ElementalAttributeData.getInstance().getItemElement(stoneId) != AttributeType.NONE)
		{
			if (ElementalAttributeData.getInstance().getItemElemental(stoneId).getPower() > 0)
			{
				return ElementalAttributeData.getInstance().getItemElemental(stoneId).getPower();
			}
			
			if (item.isWeapon())
			{
				if (oldValue == 0)
				{
					return ElementalAttributeData.FIRST_WEAPON_BONUS;
				}
				
				return ElementalAttributeData.NEXT_WEAPON_BONUS;
			}
			else if (item.isArmor())
			{
				return ElementalAttributeData.ARMOR_BONUS;
			}
		}
		
		return 0;
	}
}
