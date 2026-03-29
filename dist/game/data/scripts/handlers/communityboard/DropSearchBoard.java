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
package handlers.communityboard;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.npc.DropType;
import org.l2jmobius.gameserver.model.actor.holders.npc.DropGroupHolder;
import org.l2jmobius.gameserver.model.actor.holders.npc.DropHolder;
import org.l2jmobius.gameserver.model.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.spawns.NpcSpawnTemplate;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.serverpackets.RadarControl;
import org.l2jmobius.gameserver.network.serverpackets.ShowMiniMap;

/**
 * @author yksdtc
 */
public class DropSearchBoard implements IParseBoardHandler
{
	private static final String HTML_PATH = "data/html/CommunityBoard/Custom/dropsearch/main.html";
	private static final String NAVIGATION_PATH = "data/html/CommunityBoard/Custom/navigation.html";
	private static final String[] COMMAND =
	{
		"_bbsdropmain",
		"_bbsdropin",
		"_bbsdropmi",
		"_bbs_search_item",
		"_bbs_search_drop",
		"_bbs_npc_trace"
	};
	
	private static class SearchResultPage
	{
		final String resultHtml;
		final String pagesHtml;
		
		SearchResultPage(String resultHtml, String pagesHtml)
		{
			this.resultHtml = resultHtml;
			this.pagesHtml = pagesHtml;
		}
	}
	
	private class CBDropHolder
	{
		final int itemId;
		final int npcId;
		final int npcLevel;
		final long min;
		final long max;
		final double chance;
		final boolean isSpoil;
		final boolean isRaid;
		
		public CBDropHolder(NpcTemplate npcTemplate, DropHolder dropHolder)
		{
			isSpoil = dropHolder.getDropType() == DropType.SPOIL;
			itemId = dropHolder.getItemId();
			npcId = npcTemplate.getId();
			npcLevel = npcTemplate.getLevel();
			min = dropHolder.getMin();
			max = dropHolder.getMax();
			chance = dropHolder.getChance();
			isRaid = npcTemplate.getType().equals("RaidBoss") || npcTemplate.getType().equals("GrandBoss");
		}
		
		/**
		 * only for debug
		 */
		@Override
		public String toString()
		{
			return "DropHolder [itemId=" + itemId + ", npcId=" + npcId + ", npcLevel=" + npcLevel + ", min=" + min + ", max=" + max + ", chance=" + chance + ", isSpoil=" + isSpoil + "]";
		}
	}
	
	private final Map<Integer, List<CBDropHolder>> DROP_INDEX_CACHE = new HashMap<>();
	
	// nonsupport items
	private static final Set<Integer> BLOCK_ID = new HashSet<>();
	static
	{
		BLOCK_ID.add(Inventory.ADENA_ID);
	}
	
	public DropSearchBoard()
	{
		buildDropIndex();
	}
	
	private void buildDropIndex()
	{
		NpcData.getInstance().getTemplates(npc -> npc.getDropGroups() != null).forEach(npcTemplate ->
		{
			for (DropGroupHolder dropGroup : npcTemplate.getDropGroups())
			{
				final double chance = dropGroup.getChance() / 100;
				for (DropHolder dropHolder : dropGroup.getDropList())
				{
					addToDropList(npcTemplate, new DropHolder(dropHolder.getDropType(), dropHolder.getItemId(), dropHolder.getMin(), dropHolder.getMax(), dropHolder.getChance() * chance));
				}
			}
		});
		NpcData.getInstance().getTemplates(npc -> npc.getDropList() != null).forEach(npcTemplate ->
		{
			for (DropHolder dropHolder : npcTemplate.getDropList())
			{
				addToDropList(npcTemplate, dropHolder);
			}
		});
		NpcData.getInstance().getTemplates(npc -> npc.getSpoilList() != null).forEach(npcTemplate ->
		{
			for (DropHolder dropHolder : npcTemplate.getSpoilList())
			{
				addToDropList(npcTemplate, dropHolder);
			}
		});
		
		DROP_INDEX_CACHE.values().forEach(l -> l.sort((d1, d2) -> Integer.valueOf(d1.npcLevel).compareTo(Integer.valueOf(d2.npcLevel))));
	}
	
	private void addToDropList(NpcTemplate npcTemplate, DropHolder dropHolder)
	{
		if (BLOCK_ID.contains(dropHolder.getItemId()))
		{
			return;
		}
		
		List<CBDropHolder> dropList = DROP_INDEX_CACHE.get(dropHolder.getItemId());
		if (dropList == null)
		{
			dropList = new ArrayList<>();
			DROP_INDEX_CACHE.put(dropHolder.getItemId(), dropList);
		}
		
		dropList.add(new CBDropHolder(npcTemplate, dropHolder));
	}
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		final String navigation = HtmCache.getInstance().getHtm(player, NAVIGATION_PATH);
		final String[] params = command.split(" ");
		String html = HtmCache.getInstance().getHtm(player, HTML_PATH);
		switch (params[0])
		{
			case "_bbsdropmain":
			{
				html = html.replace("%searchResult%", buildEmptySearchResult());
				html = html.replace("%pages%", "");
				break;
			}
			case "_bbsdropin":
			{
				final LegacyItemSearch legacySearch = parseLegacyItemSearch(command);
				final SearchResultPage resultPage = buildItemSearchResult(legacySearch.itemName, legacySearch.page);
				html = html.replace("%searchResult%", resultPage.resultHtml);
				html = html.replace("%pages%", resultPage.pagesHtml);
				break;
			}
			case "_bbsdropmi":
			{
				final LegacyDropPage legacyDrop = parseLegacyDropPage(command);
				html = buildDropListPage(html, legacyDrop.itemId, legacyDrop.page, player);
				break;
			}
			case "_bbs_search_item":
			{
				final String itemName = buildItemName(params);
				final SearchResultPage resultPage = buildItemSearchResult(itemName, 1);
				html = html.replace("%searchResult%", resultPage.resultHtml);
				html = html.replace("%pages%", resultPage.pagesHtml);
				break;
			}
			case "_bbs_search_drop":
			{
				html = buildDropListPage(html, Integer.parseInt(params[1]), Integer.parseInt(params[2]), player);
				break;
			}
			case "_bbs_npc_trace":
			{
				final int npcId = Integer.parseInt(params[1]);
				final List<NpcSpawnTemplate> spawnList = SpawnData.getInstance().getNpcSpawns(npc -> npc.getId() == npcId);
				if (spawnList.isEmpty())
				{
					player.sendMessage("Cannot find any spawn. Maybe dropped by a boss or instance monster.");
				}
				else
				{
					player.sendPacket(new ShowMiniMap(-1));
					ThreadPool.schedule(() ->
					{
						final NpcSpawnTemplate spawn = spawnList.get(Rnd.get(spawnList.size()));
						player.getRadar().addMarker(spawn.getSpawnLocation().getX(), spawn.getSpawnLocation().getY(), spawn.getSpawnLocation().getZ());
						player.sendPacket(new RadarControl(0, 2, spawn.getSpawnLocation().getX(), spawn.getSpawnLocation().getY(), spawn.getSpawnLocation().getZ()));
					}, 500);
				}
				break;
			}
		}
		
		if (html != null)
		{
			html = html.replace("%navigation%", navigation);
			CommunityBoardHandler.separateAndSend(html, player);
		}
		
		return false;
	}
	
	private static class LegacyItemSearch
	{
		final String itemName;
		final int page;
		
		LegacyItemSearch(String itemName, int page)
		{
			this.itemName = itemName;
			this.page = page;
		}
	}
	
	private static class LegacyDropPage
	{
		final int itemId;
		final int page;
		
		LegacyDropPage(int itemId, int page)
		{
			this.itemId = itemId;
			this.page = page;
		}
	}
	
	private LegacyItemSearch parseLegacyItemSearch(String command)
	{
		String body = command.substring("_bbsdropin".length()).replaceFirst("^[_ ]+", "").trim();
		int page = 1;
		final int lastUnderscore = body.lastIndexOf('_');
		if ((lastUnderscore > -1) && (lastUnderscore < (body.length() - 1)))
		{
			try
			{
				page = Integer.parseInt(body.substring(lastUnderscore + 1));
				body = body.substring(0, lastUnderscore).trim();
			}
			catch (NumberFormatException e)
			{
				// Legacy search without explicit page suffix.
			}
		}
		return new LegacyItemSearch(body, Math.max(1, page));
	}
	
	private LegacyDropPage parseLegacyDropPage(String command)
	{
		String body = command.substring("_bbsdropmi".length()).replaceFirst("^[_ ]+", "").trim();
		final String[] split = body.split("_");
		final int itemId = Integer.parseInt(split[0].trim());
		final int page = (split.length > 1) ? Integer.parseInt(split[1].trim()) : 1;
		return new LegacyDropPage(itemId, Math.max(1, page));
	}
	
	private String buildDropListPage(String html, int itemId, int page, Player player)
	{
		final DecimalFormat chanceFormat = new DecimalFormat("0.00##");
		final List<CBDropHolder> list = DROP_INDEX_CACHE.get(itemId);
		if ((list == null) || list.isEmpty())
		{
			html = html.replace("%searchResult%", "<tr><td width=100 align=CENTER>No Match</td></tr>");
			html = html.replace("%pages%", "");
			return html;
		}
		int pages = list.size() / 14;
		if ((list.size() % 14) != 0)
		{
			pages++;
		}
		page = Math.min(Math.max(page, 1), pages);
		final int start = (page - 1) * 14;
		final int end = Math.min(list.size() - 1, start + 13);
		final StringBuilder builder = new StringBuilder();
		final PlayerStat stat = player.getStat();
		final double dropAmountAdenaEffectBonus = stat.getMul(Stat.BONUS_DROP_ADENA, 1);
		final double dropAmountEffectBonus = stat.getMul(Stat.BONUS_DROP_AMOUNT, 1);
		final double dropRateEffectBonus = stat.getMul(Stat.BONUS_DROP_RATE, 1);
		final double spoilRateEffectBonus = stat.getMul(Stat.BONUS_SPOIL_RATE, 1);
		for (int index = start; index <= end; index++)
		{
			final CBDropHolder cbDropHolder = list.get(index);
			double rateChance = 1;
			double rateAmount = 1;
			if (cbDropHolder.isSpoil)
			{
				rateChance = RatesConfig.RATE_SPOIL_DROP_CHANCE_MULTIPLIER;
				rateAmount = RatesConfig.RATE_SPOIL_DROP_AMOUNT_MULTIPLIER;
				if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
				{
					rateChance *= PremiumSystemConfig.PREMIUM_RATE_SPOIL_CHANCE;
					rateAmount *= PremiumSystemConfig.PREMIUM_RATE_SPOIL_AMOUNT;
				}
				rateChance *= spoilRateEffectBonus;
			}
			else
			{
				final ItemTemplate item = ItemData.getInstance().getTemplate(cbDropHolder.itemId);
				if (RatesConfig.RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId) != null)
				{
					rateChance *= RatesConfig.RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId);
					if ((cbDropHolder.itemId == Inventory.ADENA_ID) && (rateChance > 100))
					{
						rateChance = 100;
					}
				}
				else if (item.hasExImmediateEffect())
				{
					rateChance *= RatesConfig.RATE_HERB_DROP_CHANCE_MULTIPLIER;
				}
				else if (cbDropHolder.isRaid)
				{
					rateChance *= RatesConfig.RATE_RAID_DROP_CHANCE_MULTIPLIER;
				}
				else
				{
					rateChance *= RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER;
				}
				if (RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId) != null)
				{
					rateAmount *= RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId);
				}
				else if (item.hasExImmediateEffect())
				{
					rateAmount *= RatesConfig.RATE_HERB_DROP_AMOUNT_MULTIPLIER;
				}
				else if (cbDropHolder.isRaid)
				{
					rateAmount *= RatesConfig.RATE_RAID_DROP_AMOUNT_MULTIPLIER;
				}
				else
				{
					rateAmount *= RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER;
				}
				if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
				{
					if (PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId) != null)
					{
						rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId);
					}
					else if (!item.hasExImmediateEffect() && !cbDropHolder.isRaid)
					{
						rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE;
					}
					if (PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId) != null)
					{
						rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId);
					}
					else if (!item.hasExImmediateEffect() && !cbDropHolder.isRaid)
					{
						rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT;
					}
				}
				rateAmount *= dropAmountEffectBonus;
				if (item.getId() == Inventory.ADENA_ID)
				{
					rateAmount *= dropAmountAdenaEffectBonus;
				}
				rateChance *= dropRateEffectBonus;
				if (item.getId() == Inventory.LCOIN_ID)
				{
					rateChance *= stat.getMul(Stat.BONUS_DROP_RATE_LCOIN, 1);
				}
			}
			builder.append("<tr>");
			builder.append("<td width=30>").append(cbDropHolder.npcLevel).append("</td>");
			builder.append("<td width=170>").append("<a action=\"bypass _bbs_npc_trace ").append(cbDropHolder.npcId).append("\">").append("&@").append(cbDropHolder.npcId).append(";").append("</a>").append("</td>");
			builder.append("<td width=80 align=CENTER>").append(cbDropHolder.min * rateAmount).append("-").append(cbDropHolder.max * rateAmount).append("</td>");
			builder.append("<td width=50 align=CENTER>").append(chanceFormat.format(cbDropHolder.chance * rateChance)).append("%").append("</td>");
			builder.append("<td width=50 align=CENTER>").append(cbDropHolder.isSpoil ? "Spoil" : "Drop").append("</td>");
			builder.append("</tr>");
		}
		html = html.replace("%searchResult%", builder.toString());
		builder.setLength(0);
		builder.append("<tr>");
		for (int currentPage = 1; currentPage <= pages; currentPage++)
		{
			builder.append("<td>").append("<a action=\"bypass -h _bbs_search_drop ").append(itemId).append(" ").append(currentPage).append(" $order $level\">").append(currentPage).append("</a></td>");
		}
		builder.append("</tr>");
		html = html.replace("%pages%", builder.toString());
		return html;
	}
	
	private String buildEmptySearchResult()
	{
		final StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 7; i++)
		{
			builder.append("<tr><td height=36></td></tr>");
		}
		return builder.toString();
	}
	
	/**
	 * @param itemName
	 * @return
	 */
	private SearchResultPage buildItemSearchResult(String itemName, int page)
	{
		final Set<Integer> existInDropData = DROP_INDEX_CACHE.keySet();
		final List<ItemTemplate> items = new ArrayList<>();
		for (ItemTemplate item : ItemData.getInstance().getAllItems())
		{
			if (item == null)
			{
				continue;
			}
			
			if (!existInDropData.contains(item.getId()))
			{
				continue;
			}
			
			if (item.getName().toLowerCase().contains(itemName.toLowerCase()))
			{
				items.add(item);
			}
		}
		
		if (items.isEmpty())
		{
			return new SearchResultPage("<tr><td width=100 align=CENTER>No Match</td></tr>", "");
		}
		final int pages = (int) Math.ceil(items.size() / 14.0);
		page = Math.min(Math.max(page, 1), pages);
		final int start = (page - 1) * 14;
		final int end = Math.min(items.size(), start + 14);
		
		int line = 0;
		
		final StringBuilder builder = new StringBuilder((end - start) * 28);
		int i = 0;
		for (int index = start; index < end; index++)
		{
			final ItemTemplate item = items.get(index);
			i++;
			if (i == 1)
			{
				line++;
				builder.append("<tr>");
			}
			
			String icon = item.getIcon();
			if (icon == null)
			{
				icon = "icon.etc_question_mark_i00";
			}
			
			builder.append("<td>");
			builder.append("<button value=\".\" action=\"bypass _bbs_search_drop " + item.getId() + " 1 $order $level\" width=32 height=32 back=\"" + icon + "\" fore=\"" + icon + "\">");
			builder.append("</td>");
			builder.append("<td width=200>");
			builder.append("&#").append(item.getId()).append(";");
			builder.append("</td>");
			
			if (i == 2)
			{
				builder.append("</tr>");
				i = 0;
			}
		}
		
		if ((i % 2) == 1)
		{
			builder.append("</tr>");
		}
		
		if (line < 7)
		{
			for (i = 0; i < (7 - line); i++)
			{
				builder.append("<tr><td height=36></td></tr>");
			}
		}
		
		final String resultHtml = builder.toString();
		final StringBuilder pageBuilder = new StringBuilder();
		if (pages > 1)
		{
			pageBuilder.append("<tr>");
			for (int currentPage = 1; currentPage <= pages; currentPage++)
			{
				pageBuilder.append("<td><a action=\"bypass _bbsdropin_").append(itemName).append("_").append(currentPage).append("\">").append(currentPage).append("</a></td>");
			}
			pageBuilder.append("</tr>");
		}
		return new SearchResultPage(resultHtml, pageBuilder.toString());
	}
	
	/**
	 * @param params
	 * @return
	 */
	private String buildItemName(String[] params)
	{
		final StringJoiner joiner = new StringJoiner(" ");
		for (int i = 1; i < params.length; i++)
		{
			joiner.add(params[i]);
		}
		
		return joiner.toString();
	}
	
	@Override
	public String[] getCommandList()
	{
		return COMMAND;
	}
}
