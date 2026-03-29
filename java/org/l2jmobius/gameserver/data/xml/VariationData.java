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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.VariationInstance;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.EtcItem;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.options.OptionDataCategory;
import org.l2jmobius.gameserver.model.options.OptionDataGroup;
import org.l2jmobius.gameserver.model.options.Options;
import org.l2jmobius.gameserver.model.options.Variation;
import org.l2jmobius.gameserver.model.options.VariationFee;

/**
 * @author Pere, Mobius
 */
public class VariationData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(VariationData.class.getSimpleName());
	private static final int PHYSICAL_WEAPON_GROUP = 1;
	private static final int MAGIC_WEAPON_GROUP = 2;
	private static final int ARMOR_GROUP = 3;
	private static final int ACCESSORY_GROUP = 4;
	private static final int SHIELD_SIGIL_GROUP = 5;
	private static final int HEAD_ACCESSORY_GROUP = 6;
	
	private final Map<Integer, Set<Integer>> _itemGroups = new HashMap<>();
	private final Map<Integer, List<Variation>> _variations = new ConcurrentHashMap<>();
	private final Map<Integer, Map<Integer, VariationFee>> _fees = new ConcurrentHashMap<>();
	private final Map<Integer, Map<Integer, VariationFee>> _logicalGroupFees = new ConcurrentHashMap<>();
	private final Set<Integer> _missingMineralIds = new LinkedHashSet<>();
	private final Set<Integer> _missingItemIds = new LinkedHashSet<>();
	
	protected VariationData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_itemGroups.clear();
		_variations.clear();
		_fees.clear();
		_logicalGroupFees.clear();
		_missingMineralIds.clear();
		_missingItemIds.clear();
		parseDatapackFile("data/stats/augmentation/Variations.xml");
		if (!_missingMineralIds.isEmpty())
		{
			LOGGER.warning(getClass().getSimpleName() + ": Skipped missing augmentation mineral item ids: " + _missingMineralIds);
		}
		if (!_missingItemIds.isEmpty())
		{
			LOGGER.warning(getClass().getSimpleName() + ": Skipped missing augmentation item references: " + _missingItemIds);
		}
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _itemGroups.size() + " item groups.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _variations.size() + " variations.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _fees.size() + " direct fees and " + _logicalGroupFees.size() + " logical fee groups.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode ->
		{
			forEach(listNode, "variations", variationsNode -> forEach(variationsNode, "variation", variationNode ->
			{
				final int mineralId = parseInteger(variationNode.getAttributes(), "mineralId");
				final int itemGroup = parseInteger(variationNode.getAttributes(), "itemGroup", -1);
				if (ItemData.getInstance().getTemplate(mineralId) == null)
				{
					_missingMineralIds.add(mineralId);
				}
				
				final Variation variation = new Variation(mineralId, itemGroup);
				
				forEach(variationNode, "optionGroup", groupNode ->
				{
					final int order = parseInteger(groupNode.getAttributes(), "order");
					final List<OptionDataCategory> sets = new ArrayList<>();
					forEach(groupNode, "optionCategory", categoryNode ->
					{
						final double chance = parseDouble(categoryNode.getAttributes(), "chance");
						final Map<Options, Double> options = new HashMap<>();
						forEach(categoryNode, "option", optionNode ->
						{
							final double optionChance = parseDouble(optionNode.getAttributes(), "chance");
							final int optionId = parseInteger(optionNode.getAttributes(), "id");
							final Options opt = OptionData.getInstance().getOptions(optionId);
							if (opt == null)
							{
								LOGGER.warning(getClass().getSimpleName() + ": Null option for id " + optionId + " mineral " + mineralId);
								return;
							}
							
							options.put(opt, optionChance);
						});
						forEach(categoryNode, "optionRange", optionNode ->
						{
							final double optionChance = parseDouble(optionNode.getAttributes(), "chance");
							final int fromId = parseInteger(optionNode.getAttributes(), "from");
							final int toId = parseInteger(optionNode.getAttributes(), "to");
							for (int id = fromId; id <= toId; id++)
							{
								final Options op = OptionData.getInstance().getOptions(id);
								if (op == null)
								{
									LOGGER.warning(getClass().getSimpleName() + ": Null option for id " + id + " mineral " + mineralId);
									return;
								}
								
								options.put(op, optionChance);
							}
						});
						
						// Support for specific item ids.
						final Set<Integer> itemIds = new HashSet<>();
						forEach(categoryNode, "item", optionNode ->
						{
							final int itemId = parseInteger(optionNode.getAttributes(), "id");
							itemIds.add(itemId);
						});
						forEach(categoryNode, "items", optionNode ->
						{
							final int fromId = parseInteger(optionNode.getAttributes(), "from");
							final int toId = parseInteger(optionNode.getAttributes(), "to");
							for (int id = fromId; id <= toId; id++)
							{
								itemIds.add(id);
							}
						});
						
						sets.add(new OptionDataCategory(options, itemIds, chance));
					});
					
					variation.setEffectGroup(order, new OptionDataGroup(sets));
				});
				
				List<Variation> list = _variations.get(mineralId);
				if (list == null)
				{
					list = new ArrayList<>();
				}
				
				list.add(variation);
				
				_variations.put(mineralId, list);
				final ItemTemplate mineralItem = ItemData.getInstance().getTemplate(mineralId);
				if (mineralItem instanceof EtcItem)
				{
					((EtcItem) mineralItem).setMineral();
				}
			}));
			
			forEach(listNode, "itemGroups", variationsNode -> forEach(variationsNode, "itemGroup", variationNode ->
			{
				final int id = parseInteger(variationNode.getAttributes(), "id");
				final Set<Integer> items = new HashSet<>();
				forEach(variationNode, "item", itemNode ->
				{
					final int itemId = parseInteger(itemNode.getAttributes(), "id");
					if (ItemData.getInstance().getTemplate(itemId) == null)
					{
						_missingItemIds.add(itemId);
					}
					
					items.add(itemId);
				});
				
				if (_itemGroups.containsKey(id))
				{
					_itemGroups.get(id).addAll(items);
				}
				else
				{
					_itemGroups.put(id, items);
				}
			}));
			
			forEach(listNode, "fees", variationNode -> forEach(variationNode, "fee", feeNode ->
			{
				final int itemGroupId = parseInteger(feeNode.getAttributes(), "itemGroup");
				final Set<Integer> itemGroup = _itemGroups.get(itemGroupId);
				final int itemId = parseInteger(feeNode.getAttributes(), "itemId", 0);
				final long itemCount = parseLong(feeNode.getAttributes(), "itemCount", 0L);
				final long adenaFee = parseLong(feeNode.getAttributes(), "adenaFee", 0L);
				final long cancelFee = parseLong(feeNode.getAttributes(), "cancelFee", 0L);
				if ((itemId != 0) && (ItemData.getInstance().getTemplate(itemId) == null))
				{
					_missingItemIds.add(itemId);
				}
				
				final VariationFee fee = new VariationFee(itemId, itemCount, adenaFee, cancelFee);
				final Map<Integer, VariationFee> feeByMinerals = new HashMap<>();
				forEach(feeNode, "mineral", mineralNode ->
				{
					final int mId = parseInteger(mineralNode.getAttributes(), "id");
					feeByMinerals.put(mId, fee);
				});
				forEach(feeNode, "mineralRange", mineralNode ->
				{
					final int fromId = parseInteger(mineralNode.getAttributes(), "from");
					final int toId = parseInteger(mineralNode.getAttributes(), "to");
					for (int id = fromId; id <= toId; id++)
					{
						feeByMinerals.put(id, fee);
					}
				});
				
				if ((itemGroup == null) || itemGroup.isEmpty())
				{
					Map<Integer, VariationFee> fees = _logicalGroupFees.get(itemGroupId);
					if (fees == null)
					{
						fees = new HashMap<>();
					}
					
					fees.putAll(feeByMinerals);
					_logicalGroupFees.put(itemGroupId, fees);
					return;
				}
				
				for (int item : itemGroup)
				{
					Map<Integer, VariationFee> fees = _fees.get(item);
					if (fees == null)
					{
						fees = new HashMap<>();
					}
					
					fees.putAll(feeByMinerals);
					_fees.put(item, fees);
				}
			}));
		});
	}
	
	/**
	 * Retrieves the total count of variations available in the system.
	 * @return the number of variations
	 */
	public int getVariationCount()
	{
		return _variations.size();
	}
	
	/**
	 * Retrieves the total count of variation fees available in the system.
	 * @return the number of variation fees
	 */
	public int getFeeCount()
	{
		return _fees.size();
	}
	
	/**
	 * Generates a new random variation instance based on the specified variation template.
	 * <p>
	 * This method creates a {@link VariationInstance} by selecting random effects for the given variation and applying it to the specified target item.
	 * </p>
	 * @param variation the {@link Variation} template from which the instance will be generated
	 * @param targetItem the {@link Item} on which the variation will be applied
	 * @return a new {@link VariationInstance} with random effects
	 */
	public VariationInstance generateRandomVariation(Variation variation, Item targetItem)
	{
		return generateRandomVariation(variation, targetItem.getId());
	}
	
	/**
	 * Generates a new random variation instance based on the specified variation template and target item ID.
	 * <p>
	 * This private method is a helper that generates a {@link VariationInstance} by selecting random effects based on the target item ID.
	 * </p>
	 * @param variation the {@link Variation} template from which the instance will be generated
	 * @param targetItemId the ID of the item on which the variation will be applied
	 * @return a new {@link VariationInstance} with random effects
	 */
	private VariationInstance generateRandomVariation(Variation variation, int targetItemId)
	{
		final Options option1 = variation.getRandomEffect(0, targetItemId);
		final Options option2 = variation.getRandomEffect(1, targetItemId);
		final Options option3 = variation.getRandomEffect(2, targetItemId);
		return new VariationInstance(variation.getMineralId(), option1, option2, option3);
	}
	
	/**
	 * Retrieves a variation based on the mineral ID and target item.
	 * <p>
	 * This method searches for a matching {@link Variation} that applies to the specified mineral ID and item, checking the item group compatibility. If no exact match is found, the first available variation is returned.
	 * </p>
	 * @param mineralId the mineral ID associated with the desired variation
	 * @param item the {@link Item} for which the variation is being retrieved
	 * @return the {@link Variation} for the specified mineral ID and item, or {@code null} if not found
	 */
	public Variation getVariation(int mineralId, Item item)
	{
		final List<Variation> variations = _variations.get(mineralId);
		if ((variations == null) || variations.isEmpty())
		{
			return null;
		}
		
		for (Variation variation : variations)
		{
			final Set<Integer> group = _itemGroups.get(variation.getItemGroup());
			if ((group != null) && group.contains(item.getId()))
			{
				return variation;
			}
			if (matchesLogicalItemGroup(variation.getItemGroup(), item.getId()))
			{
				return variation;
			}
		}
		
		return variations.get(0);
	}
	
	/**
	 * Checks if there are any variations available for the specified mineral ID.
	 * @param mineralId the mineral ID to check for variations
	 * @return {@code true} if variations exist for the mineral ID, otherwise {@code false}
	 */
	public boolean hasVariation(int mineralId)
	{
		final List<Variation> variations = _variations.get(mineralId);
		return (variations != null) && !variations.isEmpty();
	}
	
	/**
	 * Retrieves the variation fee associated with the specified item and mineral IDs.
	 * @param itemId the ID of the item
	 * @param mineralId the ID of the mineral
	 * @return the {@link VariationFee} for the specified item and mineral IDs, or {@code null} if not found
	 */
	public VariationFee getFee(int itemId, int mineralId)
	{
		VariationFee fee = _fees.getOrDefault(itemId, Collections.emptyMap()).get(mineralId);
		if (fee != null)
		{
			return fee;
		}
		
		final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
		if (template == null)
		{
			return null;
		}
		
		for (Map.Entry<Integer, Map<Integer, VariationFee>> entry : _logicalGroupFees.entrySet())
		{
			if (matchesLogicalItemGroup(entry.getKey(), template))
			{
				fee = entry.getValue().get(mineralId);
				if (fee != null)
				{
					return fee;
				}
			}
		}
		return null;
	}
	
	/**
	 * Retrieves the cancellation fee for the specified item and mineral IDs.
	 * <p>
	 * If no specific fee is found for the given item and mineral combination, the method attempts to retrieve a default fee. If no fee is available, {@code -1} is returned.
	 * </p>
	 * @param itemId the ID of the item
	 * @param mineralId the ID of the mineral
	 * @return the cancellation fee as a {@code long}, or {@code -1} if not found
	 */
	public long getCancelFee(int itemId, int mineralId)
	{
		Map<Integer, VariationFee> fees = _fees.get(itemId);
		if ((fees == null) || fees.isEmpty())
		{
			final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
			if (template != null)
			{
				for (Map.Entry<Integer, Map<Integer, VariationFee>> entry : _logicalGroupFees.entrySet())
				{
					if (matchesLogicalItemGroup(entry.getKey(), template))
					{
						fees = entry.getValue();
						break;
					}
				}
			}
		}
		
		if (fees == null)
		{
			return -1;
		}
		
		VariationFee fee = fees.get(mineralId);
		if (fee == null)
		{
			// This will happen when the data is pre-rework or when augments were manually given, but still that's a cheap solution.
			LOGGER.warning(getClass().getSimpleName() + ": Cancellation fee not found for item [" + itemId + "] and mineral [" + mineralId + "]");
			fee = fees.values().iterator().next();
			if (fee == null)
			{
				return -1;
			}
		}
		
		return fee.getCancelFee();
	}
	
	/**
	 * Checks if there is fee data available for the specified item ID.
	 * @param itemId the ID of the item to check for fee data
	 * @return {@code true} if fee data exists for the item, otherwise {@code false}
	 */
	public boolean hasFeeData(int itemId)
	{
		final Map<Integer, VariationFee> itemFees = _fees.get(itemId);
		if ((itemFees != null) && !itemFees.isEmpty())
		{
			return true;
		}
		
		final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
		if (template == null)
		{
			return false;
		}
		
		for (Map.Entry<Integer, Map<Integer, VariationFee>> entry : _logicalGroupFees.entrySet())
		{
			if (matchesLogicalItemGroup(entry.getKey(), template) && !entry.getValue().isEmpty())
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean matchesLogicalItemGroup(int itemGroupId, int itemId)
	{
		final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
		return matchesLogicalItemGroup(itemGroupId, template);
	}
	
	private boolean matchesLogicalItemGroup(int itemGroupId, ItemTemplate template)
	{
		if (template == null)
		{
			return false;
		}
		
		switch (itemGroupId)
		{
			case PHYSICAL_WEAPON_GROUP:
			{
				return template.isWeapon() && !((Weapon) template).isMagicWeapon();
			}
			case MAGIC_WEAPON_GROUP:
			{
				return template.isWeapon() && ((Weapon) template).isMagicWeapon();
			}
			case ARMOR_GROUP:
			{
				return template.isArmor() && (template.getType2() == ItemTemplate.TYPE2_SHIELD_ARMOR) && (template.getBodyPart() != ItemTemplate.SLOT_L_HAND);
			}
			case ACCESSORY_GROUP:
			{
				return template.isArmor() && (template.getType2() == ItemTemplate.TYPE2_ACCESSORY);
			}
			case SHIELD_SIGIL_GROUP:
			{
				return template.isArmor() && (template.getType2() == ItemTemplate.TYPE2_SHIELD_ARMOR) && (template.getBodyPart() == ItemTemplate.SLOT_L_HAND);
			}
			case HEAD_ACCESSORY_GROUP:
			{
				return template.isArmor() && ((template.getBodyPart() == ItemTemplate.SLOT_HEAD) || (template.getBodyPart() == ItemTemplate.SLOT_HAIR) || (template.getBodyPart() == ItemTemplate.SLOT_HAIR2) || (template.getBodyPart() == ItemTemplate.SLOT_HAIRALL));
			}
		}
		return false;
	}
	
	public static VariationData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final VariationData INSTANCE = new VariationData();
	}
}
