package handlers.communityboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;

/**
 * Partial native owner for the legacy WI statistics branch.
 */
public class StatisticsBoard implements IParseBoardHandler
{
	private static final Logger LOGGER = Logger.getLogger(StatisticsBoard.class.getName());
	private static final String[] COMMANDS =
	{
		"_bbsstatistic"
	};
	private static final String MENU_PATH = "data/html/community/high/statistic/menu.htm";
	private static final int RESULT_LIMIT = 15;
	private static final int ADENA_ITEM_ID = 57;
	private static final String PK_QUERY = "SELECT charId, char_name, sex, base_class, clanid, online, onlinetime, pkkills, pvpkills, pkkills AS metricValue FROM characters WHERE accesslevel = 0 AND deletetime = 0 ORDER BY pkkills DESC, pvpkills DESC, onlinetime DESC LIMIT ?";
	private static final String PVP_QUERY = "SELECT charId, char_name, sex, base_class, clanid, online, onlinetime, pkkills, pvpkills, pvpkills AS metricValue FROM characters WHERE accesslevel = 0 AND deletetime = 0 ORDER BY pvpkills DESC, pkkills DESC, onlinetime DESC LIMIT ?";
	private static final String ONLINE_QUERY = "SELECT charId, char_name, sex, base_class, clanid, online, onlinetime, pkkills, pvpkills, onlinetime AS metricValue FROM characters WHERE accesslevel = 0 AND deletetime = 0 ORDER BY onlinetime DESC, pvpkills DESC, pkkills DESC LIMIT ?";
	private static final String RICH_QUERY = "SELECT c.charId, c.char_name, c.sex, c.base_class, c.clanid, c.online, c.onlinetime, c.pkkills, c.pvpkills, COALESCE(SUM(i.count), 0) AS metricValue FROM characters c LEFT JOIN items i ON i.owner_id = c.charId AND i.item_id = ? WHERE c.accesslevel = 0 AND c.deletetime = 0 GROUP BY c.charId, c.char_name, c.sex, c.base_class, c.clanid, c.online, c.onlinetime, c.pkkills, c.pvpkills ORDER BY metricValue DESC, c.onlinetime DESC LIMIT ?";
	private static final String RAID_QUERY = "SELECT charId, char_name, sex, base_class, clanid, online, onlinetime, pkkills, pvpkills, raidbossPoints AS metricValue FROM characters WHERE accesslevel = 0 AND deletetime = 0 ORDER BY raidbossPoints DESC, onlinetime DESC, pvpkills DESC LIMIT ?";

	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}

	@Override
	public boolean onCommand(String command, Player player)
	{
		CommunityBoardHandler.getInstance().addBypass(player, "Statistics", command);
		final String section = extractSection(command);
		switch (section)
		{
			case "pk":
			{
				CommunityBoardHandler.separateAndSend(buildPkPage(), player);
				return true;
			}
			case "pvp":
			{
				CommunityBoardHandler.separateAndSend(buildPvpPage(), player);
				return true;
			}
			case "online":
			{
				CommunityBoardHandler.separateAndSend(buildOnlinePage(), player);
				return true;
			}
			case "rich":
			{
				CommunityBoardHandler.separateAndSend(buildMetricPage("RICH", "Adena", loadRichRankings(), "FFCC66", false), player);
				return true;
			}
			case "rk":
			{
				CommunityBoardHandler.separateAndSend(buildMetricPage("RK", "Raid Boss Points", loadRankings(RAID_QUERY), "FFCC66", false), player);
				return true;
			}
			case "cis":
			case "cip":
			{
				CommunityBoardHandler.separateAndSend(buildUnsupportedPage(section), player);
				return true;
			}
			default:
			{
				CommunityBoardHandler.separateAndSend(buildMenuPage(), player);
				return true;
			}
		}
	}

	private String extractSection(String command)
	{
		if (command.startsWith("_bbsstatistic:"))
		{
			return command.substring("_bbsstatistic:".length());
		}
		return "menu";
	}

	private String buildMenuPage()
	{
		return wrapLegacyLayout("Statistics", getMenuHtml() + "<br><center><font color=LEVEL>Statistics branch is now partially owned by RV Community Board.</font><br1>Supported native sections: PK, PvP, Online, Rich, RK.<br1>Unsupported WI-only residuals remain explicit: CIS, CIP.</center>");
	}

	private String buildPkPage()
	{
		return buildDualStatPage("PK", loadRankings(PK_QUERY), true);
	}

	private String buildPvpPage()
	{
		return buildDualStatPage("PvP", loadRankings(PVP_QUERY), false);
	}

	private String buildOnlinePage()
	{
		return buildDualStatPage("Online Time", loadRankings(ONLINE_QUERY), null);
	}

	private String buildDualStatPage(String title, List<RankingEntry> entries, Boolean highlightPk)
	{
		final StringBuilder html = new StringBuilder(8192);
		html.append(getMenuHtml());
		html.append("<br><table width=740 border=0 cellspacing=1 cellpadding=3 background=\"l2ui_ct1.ComboBox_DF_Dropmenu_Bg\">");
		html.append("<tr><td height=24 align=center bgcolor=222222 colspan=9><font color=CCCC66>").append(title).append("</font></td></tr>");
		html.append("<tr>");
		html.append("<td width=30 align=center bgcolor=0e0d0d><font color=bbbbbb>#</font></td>");
		html.append("<td width=125 align=center bgcolor=0e0d0d><font color=bbbbbb>Name</font></td>");
		html.append("<td width=110 align=center bgcolor=0e0d0d><font color=bbbbbb>Clan</font></td>");
		html.append("<td width=70 align=center bgcolor=0e0d0d><font color=bbbbbb>Sex</font></td>");
		html.append("<td width=145 align=center bgcolor=0e0d0d><font color=bbbbbb>Profession</font></td>");
		html.append("<td width=70 align=center bgcolor=0e0d0d><font color=bbbbbb>Online</font></td>");
		html.append("<td width=95 align=center bgcolor=0e0d0d><font color=bbbbbb>Time</font></td>");
		if (Boolean.TRUE.equals(highlightPk))
		{
			html.append("<td width=45 align=center bgcolor=0e0d0d><font color=FF3333>PK</font></td>");
			html.append("<td width=50 align=center bgcolor=0e0d0d><font color=bbbbbb>PvP</font></td>");
		}
		else if (Boolean.FALSE.equals(highlightPk))
		{
			html.append("<td width=45 align=center bgcolor=0e0d0d><font color=bbbbbb>PK</font></td>");
			html.append("<td width=50 align=center bgcolor=0e0d0d><font color=FF6600>PvP</font></td>");
		}
		else
		{
			html.append("<td width=45 align=center bgcolor=0e0d0d><font color=bbbbbb>PK</font></td>");
			html.append("<td width=50 align=center bgcolor=0e0d0d><font color=bbbbbb>PvP</font></td>");
		}
		html.append("</tr>");

		if (entries.isEmpty())
		{
			html.append("<tr><td colspan=9 align=center bgcolor=151515><br>No ranking data found.<br></td></tr>");
		}
		else
		{
			for (int index = 0; index < entries.size(); index++)
			{
				final RankingEntry entry = entries.get(index);
				final String rowColor = ((index % 2) == 0) ? "151515" : "212121";
				html.append("<tr>");
				html.append("<td align=center bgcolor=").append(rowColor).append(">" + getRankMarkup(index + 1) + "</td>");
				html.append("<td align=center bgcolor=").append(rowColor).append("><font color=").append(getNameColor(index + 1)).append(">")
					.append(entry.name).append("</font></td>");
				html.append("<td align=center bgcolor=").append(rowColor).append("><font color=bbbbbb>")
					.append(entry.clanName).append("</font></td>");
				html.append("<td align=center bgcolor=").append(rowColor).append("><font color=bbbbbb>")
					.append(entry.male ? "Male" : "Female").append("</font></td>");
				html.append("<td align=center bgcolor=").append(rowColor).append("><font color=bbbbbb>")
					.append(entry.className).append("</font></td>");
				html.append("<td align=center bgcolor=").append(rowColor).append(">")
					.append(entry.online ? "<font color=66FF66>Yes</font>" : "<font color=FF6666>No</font>").append("</td>");
				html.append("<td align=center bgcolor=").append(rowColor).append("><font color=669900>")
					.append(formatOnlineTime(entry.onlineTimeSeconds)).append("</font></td>");
				html.append("<td align=center bgcolor=").append(rowColor).append("><font color=")
					.append(Boolean.TRUE.equals(highlightPk) ? "FF3333" : "bbbbbb").append(">")
					.append(entry.pkKills).append("</font></td>");
				html.append("<td align=center bgcolor=").append(rowColor).append("><font color=")
					.append(Boolean.FALSE.equals(highlightPk) ? "FF6600" : "bbbbbb").append(">")
					.append(entry.pvpKills).append("</font></td>");
				html.append("</tr>");
			}
		}

		html.append("</table>");
		return wrapLegacyLayout(title, html.toString());
	}

	private String buildMetricPage(String title, String metricLabel, List<RankingEntry> entries, String metricColor, boolean showPkPvp)
	{
		final StringBuilder html = new StringBuilder(8192);
		html.append(getMenuHtml());
		html.append("<br><table width=740 border=0 cellspacing=1 cellpadding=3 background=\"l2ui_ct1.ComboBox_DF_Dropmenu_Bg\">");
		html.append("<tr><td height=24 align=center bgcolor=222222 colspan=").append(showPkPvp ? 10 : 8).append("><font color=CCCC66>").append(title).append("</font></td></tr>");
		html.append("<tr>");
		html.append("<td width=30 align=center bgcolor=0e0d0d><font color=bbbbbb>#</font></td>");
		html.append("<td width=125 align=center bgcolor=0e0d0d><font color=bbbbbb>Name</font></td>");
		html.append("<td width=110 align=center bgcolor=0e0d0d><font color=bbbbbb>Clan</font></td>");
		html.append("<td width=70 align=center bgcolor=0e0d0d><font color=bbbbbb>Sex</font></td>");
		html.append("<td width=145 align=center bgcolor=0e0d0d><font color=bbbbbb>Profession</font></td>");
		html.append("<td width=70 align=center bgcolor=0e0d0d><font color=bbbbbb>Online</font></td>");
		html.append("<td width=95 align=center bgcolor=0e0d0d><font color=bbbbbb>Time</font></td>");
		if (showPkPvp)
		{
			html.append("<td width=45 align=center bgcolor=0e0d0d><font color=bbbbbb>PK</font></td>");
			html.append("<td width=50 align=center bgcolor=0e0d0d><font color=bbbbbb>PvP</font></td>");
		}
		html.append("<td width=").append(showPkPvp ? 100 : 215).append(" align=center bgcolor=0e0d0d><font color=")
			.append(metricColor).append(">*").append(metricLabel).append("*</font></td>");
		html.append("</tr>");

		if (entries.isEmpty())
		{
			html.append("<tr><td colspan=").append(showPkPvp ? 10 : 8).append(" align=center bgcolor=151515><br>No ranking data found.<br></td></tr>");
		}
		else
		{
			for (int index = 0; index < entries.size(); index++)
			{
				final RankingEntry entry = entries.get(index);
				final String rowColor = ((index % 2) == 0) ? "151515" : "212121";
				html.append("<tr>");
				html.append("<td align=center bgcolor=").append(rowColor).append(">" + getRankMarkup(index + 1) + "</td>");
				html.append("<td align=center bgcolor=").append(rowColor).append("><font color=").append(getNameColor(index + 1)).append(">")
					.append(entry.name).append("</font></td>");
				html.append("<td align=center bgcolor=").append(rowColor).append("><font color=bbbbbb>")
					.append(entry.clanName).append("</font></td>");
				html.append("<td align=center bgcolor=").append(rowColor).append("><font color=bbbbbb>")
					.append(entry.male ? "Male" : "Female").append("</font></td>");
				html.append("<td align=center bgcolor=").append(rowColor).append("><font color=bbbbbb>")
					.append(entry.className).append("</font></td>");
				html.append("<td align=center bgcolor=").append(rowColor).append(">")
					.append(entry.online ? "<font color=66FF66>Yes</font>" : "<font color=FF6666>No</font>").append("</td>");
				html.append("<td align=center bgcolor=").append(rowColor).append("><font color=669900>")
					.append(formatOnlineTime(entry.onlineTimeSeconds)).append("</font></td>");
				if (showPkPvp)
				{
					html.append("<td align=center bgcolor=").append(rowColor).append("><font color=bbbbbb>")
						.append(entry.pkKills).append("</font></td>");
					html.append("<td align=center bgcolor=").append(rowColor).append("><font color=bbbbbb>")
						.append(entry.pvpKills).append("</font></td>");
				}
				html.append("<td align=center bgcolor=").append(rowColor).append("><font color=")
					.append(metricColor).append(">")
					.append(formatMetricValue(entry.metricValue, metricLabel)).append("</font></td>");
				html.append("</tr>");
			}
		}

		html.append("</table>");
		return wrapLegacyLayout(title, html.toString());
	}

	private String buildUnsupportedPage(String section)
	{
		final String featureName = section.equals("cis") ? "Completed instances solo" : "Completed instances party";
		final String content = getMenuHtml()
			+ "<br><center><font color=LEVEL>" + featureName + "</font><br><br>"
			+ "RV does not contain a confirmed per-character Community Board data owner for this WI statistic branch yet.<br1>"
			+ "The section remains explicit instead of rendering fake leaderboard data.</center>";
		return wrapLegacyLayout(featureName, content);
	}

	private List<RankingEntry> loadRankings(String query)
	{
		final List<RankingEntry> result = new ArrayList<>();
		try (Connection connection = DatabaseFactory.getConnection();
			PreparedStatement statement = connection.prepareStatement(query))
		{
			statement.setInt(1, RESULT_LIMIT);
			try (ResultSet resultSet = statement.executeQuery())
			{
				while (resultSet.next())
				{
					result.add(readEntry(resultSet));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed loading statistics rankings.", e);
		}
		return result;
	}

	private List<RankingEntry> loadRichRankings()
	{
		final List<RankingEntry> result = new ArrayList<>();
		try (Connection connection = DatabaseFactory.getConnection();
			PreparedStatement statement = connection.prepareStatement(RICH_QUERY))
		{
			statement.setInt(1, ADENA_ITEM_ID);
			statement.setInt(2, RESULT_LIMIT);
			try (ResultSet resultSet = statement.executeQuery())
			{
				while (resultSet.next())
				{
					result.add(readEntry(resultSet));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed loading rich statistics rankings.", e);
		}
		return result;
	}

	private RankingEntry readEntry(ResultSet resultSet) throws Exception
	{
		final int clanId = resultSet.getInt("clanid");
		final Clan clan = ClanTable.getInstance().getClan(clanId);
		final int classId = resultSet.getInt("base_class");
		final String className = (ClassListData.getInstance().getClass(classId) != null) ? ClassListData.getInstance().getClass(classId).getClassName() : Integer.toString(classId);
		return new RankingEntry(
			resultSet.getString("char_name"),
			clan != null ? clan.getName() : "-",
			resultSet.getInt("sex") == 0,
			className,
			resultSet.getInt("online") > 0,
			resultSet.getLong("onlinetime"),
			resultSet.getInt("pkkills"),
			resultSet.getInt("pvpkills"),
			resultSet.getLong("metricValue"));
	}

	private String getMenuHtml()
	{
		final String html = HtmCache.getInstance().getHtm(null, MENU_PATH);
		return html != null ? html : "<center><font color=LEVEL>Statistics menu is missing.</font></center>";
	}

	private String wrapLegacyLayout(String title, String content)
	{
		return "<html noscrollbar><title>Community by L2CCCP</title><body><br><table width=755><tr><td align=center valign=top>"
			+ "<table border=0 cellpadding=0 cellspacing=0 width=769 background=\"l2ui_ct1.Windows_DF_TooltipBG\"><tr><td valign=top align=center>"
			+ "<table width=755><tr><td align=center><br><font color=LEVEL>" + title + "</font><br><br>" + content
			+ "<br><br><button value=\"Home\" action=\"bypass _bbshome\" width=200 height=31 back=\"L2UI_CT1.OlympiadWnd_DF_Back_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Back\"></td></tr></table>"
			+ "</td></tr></table></td></tr></table></body></html>";
	}

	private String getRankMarkup(int rank)
	{
		switch (rank)
		{
			case 1:
			{
				return "<img src=\"l2ui_ch3.party_summmon_num1\" width=16 height=16>";
			}
			case 2:
			{
				return "<img src=\"l2ui_ch3.party_summmon_num2\" width=16 height=16>";
			}
			case 3:
			{
				return "<img src=\"l2ui_ch3.party_summmon_num3\" width=16 height=16>";
			}
			default:
			{
				return Integer.toString(rank);
			}
		}
	}

	private String getNameColor(int rank)
	{
		switch (rank)
		{
			case 1:
			{
				return "ffd700";
			}
			case 2:
			{
				return "c0c0c0";
			}
			case 3:
			{
				return "b56c47";
			}
			default:
			{
				return "bbbbbb";
			}
		}
	}

	private String formatOnlineTime(long onlineTimeSeconds)
	{
		final long days = onlineTimeSeconds / 86400;
		final long hours = (onlineTimeSeconds % 86400) / 3600;
		final long minutes = (onlineTimeSeconds % 3600) / 60;
		if (days > 0)
		{
			return days + "d " + hours + "h";
		}
		if (hours > 0)
		{
			return hours + "h " + minutes + "m";
		}
		return minutes + "m";
	}

	private String formatMetricValue(long value, String metricLabel)
	{
		if ("Adena".equals(metricLabel))
		{
			return String.format("%,d", value);
		}
		return Long.toString(value);
	}

	private static class RankingEntry
	{
		private final String name;
		private final String clanName;
		private final boolean male;
		private final String className;
		private final boolean online;
		private final long onlineTimeSeconds;
		private final int pkKills;
		private final int pvpKills;
		private final long metricValue;

		private RankingEntry(String name, String clanName, boolean male, String className, boolean online, long onlineTimeSeconds, int pkKills, int pvpKills, long metricValue)
		{
			this.name = name;
			this.clanName = clanName;
			this.male = male;
			this.className = className;
			this.online = online;
			this.onlineTimeSeconds = onlineTimeSeconds;
			this.pkKills = pkKills;
			this.pvpKills = pvpKills;
			this.metricValue = metricValue;
		}
	}
}