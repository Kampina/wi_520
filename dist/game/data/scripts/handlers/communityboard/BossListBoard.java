package handlers.communityboard;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.managers.DatabaseSpawnManager;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.holders.npc.DropGroupHolder;
import org.l2jmobius.gameserver.model.actor.holders.npc.DropHolder;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.npc.RaidBossStatus;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * Partial native owner for the legacy WI boss board contract.
 */
public class BossListBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbsbosslist",
		"_bbsboss"
	};
	private static final int BOSSES_PER_PAGE = 10;
	private static final int ACTION_NONE = 0;
	private static final int ACTION_SHOW_MAP = 1;
	private static final int ACTION_SHOW_DROPS = 2;
	private static final int ACTION_GO_TO_BOSS = 3;
	private static final int ACTION_CLEAR_MAP = 4;
	private static final int ACTION_SHOW_STATS = 5;
	private static final int ACTION_SHOW_SKILLS = 6;
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm");
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		CommunityBoardHandler.getInstance().addBypass(player, "Bosses", command);
		if (command.startsWith("_bbsbosslist"))
		{
			showBossList(player, command);
			return true;
		}
		if (command.startsWith("_bbsboss"))
		{
			showBossDetails(player, command);
			return true;
		}
		return false;
	}
	
	private void showBossList(Player player, String command)
	{
		final ListContext context = parseListContext(command);
		final List<BossEntry> bosses = getBossEntries(context.search);
		final SortMode sortMode = SortMode.fromId(context.sortId);
		bosses.sort(sortMode.comparator());
		final int maxPage = Math.max(0, ((bosses.size() - 1) / BOSSES_PER_PAGE));
		final int page = Math.min(Math.max(context.page, 0), maxPage);
		final String html = buildListHtml(bosses, sortMode, page, context.search);
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	private void showBossDetails(Player player, String command)
	{
		final DetailContext context = parseDetailContext(command);
		final BossEntry boss = getBossEntry(context.bossId);
		if (boss == null)
		{
			CommunityBoardHandler.separateAndSend(buildMissingBossHtml(context), player);
			return;
		}
		applyAction(player, boss, context);
		CommunityBoardHandler.separateAndSend(buildDetailsHtml(boss, context), player);
	}
	
	private void applyAction(Player player, BossEntry boss, DetailContext context)
	{
		switch (context.action)
		{
			case ACTION_SHOW_MAP:
			{
				if (boss.hasLocation())
				{
					player.getRadar().addMarker(boss.x, boss.y, boss.z);
					player.sendMessage("Boss marker updated.");
				}
				else
				{
					player.sendMessage("Boss location is not available.");
				}
				break;
			}
			case ACTION_CLEAR_MAP:
			{
				player.getRadar().removeAllMarkers();
				player.sendMessage("Boss markers cleared.");
				break;
			}
			case ACTION_GO_TO_BOSS:
			{
				if (player.isInOlympiadMode())
				{
					player.sendMessage("You cannot teleport to a boss during Olympiad.");
					break;
				}
				if (!player.isInsideZone(ZoneId.PEACE))
				{
					player.sendMessage("You must be in a peace zone to use this teleport.");
					break;
				}
				if (!boss.canTeleport())
				{
					player.sendMessage("Teleport is available only while the boss is alive and spawned.");
					break;
				}
				player.teleToLocation(boss.liveNpc, false);
				break;
			}
			case ACTION_SHOW_DROPS:
			{
				break;
			}
			case ACTION_SHOW_STATS:
			{
				break;
			}
			case ACTION_SHOW_SKILLS:
			{
				break;
			}
		}
	}
	
	private List<BossEntry> getBossEntries(String search)
	{
		final String normalizedSearch = search.toLowerCase(Locale.ENGLISH).trim();
		final List<BossEntry> result = new ArrayList<>();
		for (Map.Entry<Integer, StatSet> entry : GrandBossManager.getInstance().getStoredInfo().entrySet())
		{
			final BossEntry boss = createGrandBossEntry(entry.getKey(), entry.getValue());
			if ((boss != null) && matchesSearch(boss, normalizedSearch))
			{
				result.add(boss);
			}
		}
		for (Map.Entry<Integer, StatSet> entry : DatabaseSpawnManager.getInstance().getStoredInfo().entrySet())
		{
			final BossEntry boss = createRaidBossEntry(entry.getKey(), entry.getValue());
			if ((boss != null) && matchesSearch(boss, normalizedSearch))
			{
				result.add(boss);
			}
		}
		return result;
	}
	
	private BossEntry getBossEntry(int bossId)
	{
		final StatSet grandInfo = GrandBossManager.getInstance().getStatSet(bossId);
		if (grandInfo != null)
		{
			return createGrandBossEntry(bossId, grandInfo);
		}
		final StatSet raidInfo = DatabaseSpawnManager.getInstance().getStoredInfo().get(bossId);
		if (raidInfo != null)
		{
			return createRaidBossEntry(bossId, raidInfo);
		}
		return null;
	}
	
	private BossEntry createGrandBossEntry(int bossId, StatSet info)
	{
		final NpcTemplate template = NpcData.getInstance().getTemplate(bossId);
		if ((template == null) || !template.isType("GrandBoss"))
		{
			return null;
		}
		final GrandBoss boss = GrandBossManager.getInstance().getBoss(bossId);
		final long respawnTime = info.getLong("respawn_time", 0);
		final boolean alive = (boss != null) ? (!boss.isDead() && boss.isSpawned()) : (respawnTime <= System.currentTimeMillis());
		final boolean inCombat = alive && (boss != null) && boss.isInCombat();
		final int x = (boss != null) ? boss.getX() : info.getInt("loc_x", 0);
		final int y = (boss != null) ? boss.getY() : info.getInt("loc_y", 0);
		final int z = (boss != null) ? boss.getZ() : info.getInt("loc_z", 0);
		final double currentHp = (boss != null) ? boss.getCurrentHp() : info.getDouble("currentHP", 0);
		final double currentMp = (boss != null) ? boss.getCurrentMp() : info.getDouble("currentMP", 0);
		final double maxHp = (boss != null) ? boss.getMaxHp() : template.getBaseHpMax();
		final double maxMp = (boss != null) ? boss.getMaxMp() : template.getBaseMpMax();
		return new BossEntry(bossId, template.getName(), template.getLevel(), true, resolveStatus(alive, inCombat), respawnTime, x, y, z, currentHp, currentMp, maxHp, maxMp, boss);
	}
	
	private BossEntry createRaidBossEntry(int bossId, StatSet info)
	{
		final DatabaseSpawnManager spawnManager = DatabaseSpawnManager.getInstance();
		final NpcTemplate template = spawnManager.getValidTemplate(bossId);
		if ((template == null) || !template.isType("RaidBoss"))
		{
			return null;
		}
		final Npc npc = spawnManager.getNpc(bossId);
		final RaidBossStatus status = spawnManager.getStatus(bossId);
		final Spawn spawn = spawnManager.getSpawns().get(bossId);
		final boolean alive = (npc != null) && !npc.isDead();
		final boolean inCombat = alive && npc.isInCombat();
		final int x = (npc != null) ? npc.getX() : (spawn != null ? spawn.getX() : 0);
		final int y = (npc != null) ? npc.getY() : (spawn != null ? spawn.getY() : 0);
		final int z = (npc != null) ? npc.getZ() : (spawn != null ? spawn.getZ() : 0);
		final long respawnTime = info.getLong("respawnTime", 0);
		final double currentHp = (npc != null) ? npc.getCurrentHp() : info.getDouble("currentHP", 0);
		final double currentMp = (npc != null) ? npc.getCurrentMp() : info.getDouble("currentMP", 0);
		final double maxHp = (npc != null) ? npc.getMaxHp() : template.getBaseHpMax();
		final double maxMp = (npc != null) ? npc.getMaxMp() : template.getBaseMpMax();
		final BossStatus bossStatus = inCombat ? BossStatus.COMBAT : mapRaidStatus(status, alive, respawnTime);
		return new BossEntry(bossId, template.getName(), template.getLevel(), false, bossStatus, respawnTime, x, y, z, currentHp, currentMp, maxHp, maxMp, npc);
	}
	
	private boolean matchesSearch(BossEntry boss, String normalizedSearch)
	{
		return normalizedSearch.isBlank() || boss.name.toLowerCase(Locale.ENGLISH).contains(normalizedSearch) || String.valueOf(boss.id).contains(normalizedSearch);
	}
	
	private ListContext parseListContext(String command)
	{
		final String prefix = "_bbsbosslist";
		if (command.length() <= prefix.length())
		{
			return new ListContext(0, 0, "");
		}
		String raw = command.substring(prefix.length());
		if (raw.startsWith("_"))
		{
			raw = raw.substring(1);
		}
		final String[] parts = raw.split("_", 3);
		final int sortId = (parts.length > 0) ? parseInt(parts[0], 0) : 0;
		final int page = (parts.length > 1) ? parseInt(parts[1], 0) : 0;
		final String search = (parts.length > 2) ? normalizeSearch(parts[2]) : "";
		return new ListContext(sortId, page, search);
	}
	
	private DetailContext parseDetailContext(String command)
	{
		final String prefix = "_bbsboss_";
		String raw = command.substring(prefix.length());
		final String[] parts = raw.split("_", 5);
		final int bossId = (parts.length > 0) ? parseInt(parts[0], 0) : 0;
		final int action = (parts.length > 1) ? parseInt(parts[1], ACTION_NONE) : ACTION_NONE;
		final int sortId = (parts.length > 2) ? parseInt(parts[2], 0) : 0;
		final int page = (parts.length > 3) ? parseInt(parts[3], 0) : 0;
		final String search = (parts.length > 4) ? normalizeSearch(parts[4]) : "";
		return new DetailContext(bossId, action, sortId, page, search);
	}
	
	private String buildListHtml(List<BossEntry> bosses, SortMode sortMode, int page, String search)
	{
		final int pages = Math.max(1, (int) Math.ceil(bosses.size() / (double) BOSSES_PER_PAGE));
		final int start = page * BOSSES_PER_PAGE;
		final int end = Math.min(bosses.size(), start + BOSSES_PER_PAGE);
		final StringBuilder sb = new StringBuilder(8192);
		sb.append("<html><body><br><center>");
		sb.append("<font color=LEVEL>Boss Status</font><br1>");
		sb.append("<font color=808080>Partial native bridge: list, details, map marker, clear map and guarded live teleport.</font><br><br>");
		sb.append("<table width=300><tr>");
		sb.append(buttonCell("Name", "_bbsbosslist_0_0_$bossSearch", 90));
		sb.append(buttonCell("Level", "_bbsbosslist_1_0_$bossSearch", 90));
		sb.append(buttonCell("Status", "_bbsbosslist_2_0_$bossSearch", 90));
		sb.append("</tr></table><br>");
		sb.append("<table width=300><tr><td align=left><edit var=\"bossSearch\" width=120 height=15></td>");
		sb.append("<td align=left>").append(buttonTag("Find", "_bbsbosslist_" + sortMode.id + "_0_$bossSearch", 70)).append("</td>");
		sb.append("<td align=right><font color=B09878>Filter:</font> ").append(escape(search.isBlank() ? "all" : search)).append("</td></tr></table><br>");
		sb.append("<table width=300>");
		sb.append("<tr><td width=150><font color=B09878>Name</font></td><td width=40 align=center><font color=B09878>Lv</font></td><td width=70 align=center><font color=B09878>Status</font></td><td width=40 align=center><font color=B09878>Type</font></td></tr>");
		if (bosses.isEmpty())
		{
			sb.append("<tr><td colspan=4 align=center>No bosses matched the current filter.</td></tr>");
		}
		else
		{
			for (int index = start; index < end; index++)
			{
				final BossEntry boss = bosses.get(index);
				sb.append("<tr>");
				sb.append("<td><a action=\"bypass _bbsboss_").append(boss.id).append("_0_").append(sortMode.id).append("_").append(page).append("_").append(encodeBypass(search)).append("\">").append(escape(boss.name)).append("</a></td>");
				sb.append("<td align=center>").append(boss.level).append("</td>");
				sb.append("<td align=center><font color=").append(boss.status.color).append(">").append(boss.status.label).append("</font></td>");
				sb.append("<td align=center>").append(boss.grandBoss ? "GB" : "RB").append("</td>");
				sb.append("</tr>");
			}
		}
		sb.append("</table><br>");
		sb.append(buildPages(sortMode.id, page, pages, search));
		sb.append("<br>").append(buttonTag("Home", "_bbshome", 90));
		sb.append("</center></body></html>");
		return sb.toString();
	}
	
	private String buildDetailsHtml(BossEntry boss, DetailContext context)
	{
		final StringBuilder sb = new StringBuilder(6144);
		sb.append("<html><body><br><center>");
		sb.append("<font color=LEVEL>").append(escape(boss.name)).append("</font><br1>");
		sb.append("<font color=808080>").append(boss.grandBoss ? "Grand Boss" : "Raid Boss").append("</font><br><br>");
		sb.append("<table width=300>");
		sb.append(row("Npc Id", String.valueOf(boss.id)));
		sb.append(row("Level", String.valueOf(boss.level)));
		sb.append(row("Status", "<font color=" + boss.status.color + ">" + boss.status.label + "</font>"));
		sb.append(row("Location", boss.hasLocation() ? (boss.x + ", " + boss.y + ", " + boss.z) : "Unavailable"));
		sb.append(row("HP", formatGauge(boss.currentHp, boss.maxHp)));
		sb.append(row("MP", formatGauge(boss.currentMp, boss.maxMp)));
		sb.append(row("Respawn", formatRespawn(boss)));
		sb.append("</table><br>");
		sb.append("<table width=300>");
		sb.append("<tr>").append(buttonCell("Show Map", detailBypass(boss.id, ACTION_SHOW_MAP, context), 95)).append(buttonCell("Clear Map", detailBypass(boss.id, ACTION_CLEAR_MAP, context), 95)).append(buttonCell("Go To Boss", detailBypass(boss.id, ACTION_GO_TO_BOSS, context), 95)).append("</tr>");
		sb.append("<tr>").append(buttonCell("Drops", detailBypass(boss.id, ACTION_SHOW_DROPS, context), 95)).append(buttonCell("Stats", detailBypass(boss.id, ACTION_SHOW_STATS, context), 95)).append(buttonCell("Skills", detailBypass(boss.id, ACTION_SHOW_SKILLS, context), 95)).append("</tr>");
		sb.append("</table><br>");
		sb.append(buildActionHtml(boss, context.action));
		sb.append(buttonTag("Back", "_bbsbosslist_" + context.sortId + "_" + context.page + "_" + encodeBypass(context.search), 90));
		sb.append(" ").append(buttonTag("Home", "_bbshome", 90));
		sb.append("</center></body></html>");
		return sb.toString();
	}

	private String buildActionHtml(BossEntry boss, int action)
	{
		switch (action)
		{
			case ACTION_SHOW_DROPS:
			{
				return buildDropsHtml(boss);
			}
			case ACTION_SHOW_STATS:
			{
				return buildStatsHtml(boss);
			}
			case ACTION_SHOW_SKILLS:
			{
				return buildSkillsHtml(boss);
			}
			default:
			{
				return "<font color=808080>Native board view renders drops, base stats and skills directly from the boss template and live runtime when available.</font><br><br>";
			}
		}
	}

	private String buildDropsHtml(BossEntry boss)
	{
		final NpcTemplate template = NpcData.getInstance().getTemplate(boss.id);
		if (template == null)
		{
			return "<font color=808080>Drop list is unavailable for this boss template.</font><br><br>";
		}

		final List<DropViewEntry> drops = new ArrayList<>();
		appendDrops(drops, template.getDropList(), "Drop", 1);
		final List<DropGroupHolder> dropGroups = template.getDropGroups();
		if (dropGroups != null)
		{
			for (DropGroupHolder dropGroup : dropGroups)
			{
				appendDrops(drops, dropGroup.getDropList(), "Drop", dropGroup.getChance() / 100);
			}
		}
		appendDrops(drops, template.getSpoilList(), "Spoil", 1);
		if (drops.isEmpty())
		{
			return "<font color=808080>No drops are defined for this boss template.</font><br><br>";
		}

		drops.sort(Comparator.comparing((DropViewEntry entry) -> entry.category).thenComparing(entry -> entry.itemName.toLowerCase(Locale.ENGLISH)).thenComparingInt(entry -> entry.itemId));
		final int limit = Math.min(drops.size(), 30);
		final StringBuilder sb = new StringBuilder(4096);
		sb.append("<font color=LEVEL>Drop List</font><br1>");
		sb.append("<table width=300>");
		sb.append("<tr><td width=50><font color=B09878>Type</font></td><td width=150><font color=B09878>Item</font></td><td width=45 align=center><font color=B09878>Count</font></td><td width=55 align=center><font color=B09878>Chance</font></td></tr>");
		for (int index = 0; index < limit; index++)
		{
			final DropViewEntry drop = drops.get(index);
			sb.append("<tr>");
			sb.append("<td>").append(drop.category).append("</td>");
			sb.append("<td>").append(escape(drop.itemName)).append(" (" + drop.itemId + ")</td>");
			sb.append("<td align=center>").append(formatCountRange(drop.min, drop.max)).append("</td>");
			sb.append("<td align=center>").append(formatChance(drop.chance)).append("</td>");
			sb.append("</tr>");
		}
		sb.append("</table>");
		if (drops.size() > limit)
		{
			sb.append("<br1><font color=808080>Showing ").append(limit).append(" of ").append(drops.size()).append(" entries.</font>");
		}
		sb.append("<br><br>");
		return sb.toString();
	}

	private void appendDrops(List<DropViewEntry> drops, List<DropHolder> source, String category, double chanceMultiplier)
	{
		if (source == null)
		{
			return;
		}

		for (DropHolder drop : source)
		{
			final ItemTemplate item = ItemData.getInstance().getTemplate(drop.getItemId());
			final String itemName = (item != null) ? item.getName() : ("Item " + drop.getItemId());
			drops.add(new DropViewEntry(category, drop.getItemId(), itemName, drop.getMin(), drop.getMax(), drop.getChance() * chanceMultiplier));
		}
	}

	private String buildStatsHtml(BossEntry boss)
	{
		final NpcTemplate template = NpcData.getInstance().getTemplate(boss.id);
		if (template == null)
		{
			return "<font color=808080>Stats are unavailable for this boss template.</font><br><br>";
		}

		final StringBuilder sb = new StringBuilder(4096);
		sb.append("<font color=LEVEL>Base Stats</font><br1>");
		sb.append("<table width=300>");
		sb.append(row("STR / CON / DEX", template.getBaseSTR() + " / " + template.getBaseCON() + " / " + template.getBaseDEX()));
		sb.append(row("INT / WIT / MEN", template.getBaseINT() + " / " + template.getBaseWIT() + " / " + template.getBaseMEN()));
		sb.append(row("Base HP / MP", (long) template.getBaseHpMax() + " / " + (long) template.getBaseMpMax()));
		sb.append(row("Current HP / MP", (long) boss.currentHp + " / " + (long) boss.currentMp));
		sb.append(row("P. Atk / M. Atk", template.getBasePAtk() + " / " + template.getBaseMAtk()));
		sb.append(row("P. Def / M. Def", template.getBasePDef() + " / " + template.getBaseMDef()));
		sb.append(row("Atk Spd / Cast Spd", template.getBasePAtkSpd() + " / " + template.getBaseMAtkSpd()));
		sb.append(row("Crit / M. Crit", template.getBaseCritRate() + " / " + template.getBaseMCritRate()));
		sb.append(row("Atk Type / Range", template.getBaseAttackType() + " / " + template.getBaseAttackRange()));
		sb.append(row("Fire / Water", template.getBaseFire() + " / " + template.getBaseWater()));
		sb.append(row("Wind / Earth", template.getBaseWind() + " / " + template.getBaseEarth()));
		sb.append(row("Holy / Dark", template.getBaseHoly() + " / " + template.getBaseDark()));
		sb.append(row("Phys / Mag Resist", template.getBaseAbnormalResistPhysical() + " / " + template.getBaseAbnormalResistMagical()));
		sb.append("</table><br>");
		return sb.toString();
	}

	private String buildSkillsHtml(BossEntry boss)
	{
		final NpcTemplate template = NpcData.getInstance().getTemplate(boss.id);
		if (template == null)
		{
			return "<font color=808080>Skill list is unavailable for this boss template.</font><br><br>";
		}

		final List<Skill> skills = new ArrayList<>(template.getSkills().values());
		if (skills.isEmpty())
		{
			return "<font color=808080>No skills are defined for this boss template.</font><br><br>";
		}

		skills.sort(Comparator.comparingInt(Skill::getId).thenComparingInt(Skill::getLevel));
		final int limit = Math.min(skills.size(), 24);
		final StringBuilder sb = new StringBuilder(4096);
		sb.append("<font color=LEVEL>Skill List</font><br1>");
		sb.append("<table width=300>");
		sb.append("<tr><td width=55><font color=B09878>Id</font></td><td width=35 align=center><font color=B09878>Lv</font></td><td width=210><font color=B09878>Name</font></td></tr>");
		for (int index = 0; index < limit; index++)
		{
			final Skill skill = skills.get(index);
			sb.append("<tr>");
			sb.append("<td>").append(skill.getId()).append("</td>");
			sb.append("<td align=center>").append(skill.getLevel()).append("</td>");
			sb.append("<td>").append(escape(skill.getName())).append("</td>");
			sb.append("</tr>");
		}
		sb.append("</table>");
		if (skills.size() > limit)
		{
			sb.append("<br1><font color=808080>Showing ").append(limit).append(" of ").append(skills.size()).append(" skills.</font>");
		}
		sb.append("<br><br>");
		return sb.toString();
	}

	private String formatCountRange(long min, long max)
	{
		return min == max ? String.valueOf(min) : (min + "-" + max);
	}

	private String formatChance(double chance)
	{
		return String.format(Locale.ENGLISH, "%.4f%%", chance);
	}
	
	private String buildMissingBossHtml(DetailContext context)
	{
		return "<html><body><br><center><font color=LEVEL>Boss Status</font><br><br>"
			+ "Boss entry is not available in the current runtime state.<br><br>"
			+ buttonTag("Back", "_bbsbosslist_" + context.sortId + "_" + context.page + "_" + encodeBypass(context.search), 90)
			+ " " + buttonTag("Home", "_bbshome", 90)
			+ "</center></body></html>";
	}
	
	private String buildPages(int sortId, int page, int pages, String search)
	{
		final StringBuilder sb = new StringBuilder(512);
		sb.append("<table width=300><tr>");
		for (int current = 0; current < pages; current++)
		{
			if (current == page)
			{
				sb.append("<td align=center><font color=LEVEL>").append(current + 1).append("</font></td>");
			}
			else
			{
				sb.append("<td align=center><a action=\"bypass _bbsbosslist_").append(sortId).append("_").append(current).append("_").append(encodeBypass(search)).append("\">[").append(current + 1).append("]</a></td>");
			}
		}
		sb.append("</tr></table>");
		return sb.toString();
	}
	
	private String detailBypass(int bossId, int action, DetailContext context)
	{
		return "_bbsboss_" + bossId + "_" + action + "_" + context.sortId + "_" + context.page + "_" + encodeBypass(context.search);
	}
	
	private BossStatus resolveStatus(boolean alive, boolean inCombat)
	{
		return inCombat ? BossStatus.COMBAT : (alive ? BossStatus.ALIVE : BossStatus.DEAD);
	}
	
	private BossStatus mapRaidStatus(RaidBossStatus status, boolean alive, long respawnTime)
	{
		if (alive)
		{
			return BossStatus.ALIVE;
		}
		if ((status == RaidBossStatus.DEAD) || (respawnTime > System.currentTimeMillis()))
		{
			return BossStatus.DEAD;
		}
		if (status == RaidBossStatus.COMBAT)
		{
			return BossStatus.COMBAT;
		}
		return BossStatus.ALIVE;
	}
	
	private String formatRespawn(BossEntry boss)
	{
		if (boss.status != BossStatus.DEAD)
		{
			return "Alive / available";
		}
		if (boss.respawnTime <= 0)
		{
			return "Dead / unavailable";
		}
		return DATE_FORMAT.format(new Date(boss.respawnTime));
	}
	
	private String formatGauge(double current, double max)
	{
		if (max <= 0)
		{
			return "Unavailable";
		}
		final int percent = (int) Math.max(0, Math.min(100, Math.round((current / max) * 100)));
		return ((long) current) + " / " + ((long) max) + " (" + percent + "%)";
	}
	
	private String row(String name, String value)
	{
		return "<tr><td width=110><font color=B09878>" + name + "</font></td><td width=190>" + value + "</td></tr>";
	}
	
	private String buttonCell(String value, String bypass, int width)
	{
		return "<td align=center>" + buttonTag(value, bypass, width) + "</td>";
	}
	
	private String buttonTag(String value, String bypass, int width)
	{
		return "<button value=\"" + value + "\" action=\"bypass " + bypass + "\" width=" + width + " height=22 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">";
	}
	
	private String normalizeSearch(String search)
	{
		if ((search == null) || search.isBlank() || search.startsWith("$"))
		{
			return "";
		}
		return search.trim();
	}
	
	private String encodeBypass(String search)
	{
		return normalizeSearch(search);
	}
	
	private String escape(String value)
	{
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
	
	private int parseInt(String value, int defaultValue)
	{
		try
		{
			return Integer.parseInt(value);
		}
		catch (Exception e)
		{
			return defaultValue;
		}
	}
	
	private enum SortMode
	{
		NAME(0, Comparator.comparing((BossEntry boss) -> boss.name.toLowerCase(Locale.ENGLISH)).thenComparingInt(boss -> boss.level).thenComparingInt(boss -> boss.id)),
		LEVEL(1, Comparator.comparingInt((BossEntry boss) -> boss.level).reversed().thenComparing((BossEntry boss) -> boss.name.toLowerCase(Locale.ENGLISH)).thenComparingInt(boss -> boss.id)),
		STATUS(2, Comparator.comparingInt((BossEntry boss) -> boss.status.order).thenComparing((BossEntry boss) -> boss.name.toLowerCase(Locale.ENGLISH)).thenComparingInt(boss -> boss.id));
		
		private final int id;
		private final Comparator<BossEntry> comparator;
		
		SortMode(int id, Comparator<BossEntry> comparator)
		{
			this.id = id;
			this.comparator = comparator;
		}
		
		private Comparator<BossEntry> comparator()
		{
			return comparator;
		}
		
		private static SortMode fromId(int id)
		{
			for (SortMode mode : values())
			{
				if (mode.id == id)
				{
					return mode;
				}
			}
			return NAME;
		}
	}
	
	private enum BossStatus
	{
		ALIVE("Alive", "6C9E2E", 0),
		COMBAT("Combat", "C8782D", 1),
		DEAD("Dead", "B25B5B", 2);
		
		private final String label;
		private final String color;
		private final int order;
		
		BossStatus(String label, String color, int order)
		{
			this.label = label;
			this.color = color;
			this.order = order;
		}
	}
	
	private static class BossEntry
	{
		private final int id;
		private final String name;
		private final int level;
		private final boolean grandBoss;
		private final BossStatus status;
		private final long respawnTime;
		private final int x;
		private final int y;
		private final int z;
		private final double currentHp;
		private final double currentMp;
		private final double maxHp;
		private final double maxMp;
		private final Npc liveNpc;
		
		private BossEntry(int id, String name, int level, boolean grandBoss, BossStatus status, long respawnTime, int x, int y, int z, double currentHp, double currentMp, double maxHp, double maxMp, Npc liveNpc)
		{
			this.id = id;
			this.name = name;
			this.level = level;
			this.grandBoss = grandBoss;
			this.status = status;
			this.respawnTime = respawnTime;
			this.x = x;
			this.y = y;
			this.z = z;
			this.currentHp = currentHp;
			this.currentMp = currentMp;
			this.maxHp = maxHp;
			this.maxMp = maxMp;
			this.liveNpc = liveNpc;
		}
		
		private boolean hasLocation()
		{
			return (x != 0) || (y != 0) || (z != 0);
		}
		
		private boolean canTeleport()
		{
			return (liveNpc != null) && !liveNpc.isDead();
		}
	}
	
	private static class ListContext
	{
		public final int sortId;
		public final int page;
		public final String search;
		
		private ListContext(int sortId, int page, String search)
		{
			this.sortId = sortId;
			this.page = page;
			this.search = search;
		}
	}
	
	private static class DetailContext extends ListContext
	{
		public final int bossId;
		public final int action;
		
		private DetailContext(int bossId, int action, int sortId, int page, String search)
		{
			super(sortId, page, search);
			this.bossId = bossId;
			this.action = action;
		}
	}

	private static class DropViewEntry
	{
		private final String category;
		private final int itemId;
		private final String itemName;
		private final long min;
		private final long max;
		private final double chance;

		private DropViewEntry(String category, int itemId, String itemName, long min, long max, double chance)
		{
			this.category = category;
			this.itemId = itemId;
			this.itemName = itemName;
			this.min = min;
			this.max = max;
			this.chance = chance;
		}
	}
}