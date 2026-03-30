/*
 * Seven Signs Manager — ported from WorldInfluence (com.l2cccp) to RoseVain (org.l2jmobius).
 * Original author: Tempy, refactored by Hack.
 *
 * Custom seven signs:
 * - Регистрация только за сторону (LIGHT→DAWN, DARK→DUSK).
 * - Камни: Red→Gnosis(DAWN), Blue→Strife(DUSK), Green→Avarice(оба).
 * - 2-недельный цикл: период сбора (7 дней) + период наград (7 дней), смена в пн 00:00.
 * - Фестиваль, Dimension Rift, бафферы убраны.
 * - Обмен камней 1:1 (конфигурируемо).
 * - Маммон спауется только если победила одна из сторон (не Avarice).
 */
package org.l2jmobius.gameserver.model.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.custom.AltSettingsConfig;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Fraction;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Broadcast;

public class SevenSigns
{
	private static final Logger LOGGER = Logger.getLogger(SevenSigns.class.getName());
	
	public static final String SEVEN_SIGNS_HTML_PATH = "seven_signs/";
	
	// Cabals
	public static final int CABAL_NULL = 0;
	public static final int CABAL_DUSK = 1;
	public static final int CABAL_DAWN = 2;
	
	// Seals
	public static final int SEAL_NULL = 0;
	public static final int SEAL_AVARICE = 1;
	public static final int SEAL_GNOSIS = 2;
	public static final int SEAL_STRIFE = 3;
	
	// Periods
	public static final int PERIOD_COMP_RECRUITING = 0;
	public static final int PERIOD_COMPETITION = 1;
	public static final int PERIOD_COMP_RESULTS = 2;
	public static final int PERIOD_SEAL_VALIDATION = 3;
	
	public static final int PERIOD_START_HOUR = 0;
	public static final int PERIOD_START_MINS = 0;
	public static final int PERIOD_START_DAY = Calendar.MONDAY;
	
	// The minor transition periods last ~1 second.
	public static final int PERIOD_MINOR_LENGTH = 1000;
	
	// Items
	public static final int ANCIENT_ADENA_ID = Inventory.ANCIENT_ADENA_ID;
	public static final int SEAL_STONE_BLUE_ID = 6360;
	public static final int SEAL_STONE_GREEN_ID = 6361;
	public static final int SEAL_STONE_RED_ID = 6362;
	
	// NPC IDs
	public static final int ORATOR_NPC_ID = 31094;
	public static final int PREACHER_NPC_ID = 31093;
	public static final int MAMMON_MERCHANT_ID = 31113;
	public static final int MAMMON_BLACKSMITH_ID = 31126;
	public static final int MAMMON_MARKETEER_ID = 31092;
	public static final int SPIRIT_IN_ID = 31111;
	public static final int SPIRIT_OUT_ID = 31112;
	public static final int LILITH_NPC_ID = 25283;
	public static final int ANAKIM_NPC_ID = 25286;
	public static final int CREST_OF_DAWN_ID = 31170;
	public static final int CREST_OF_DUSK_ID = 31171;
	
	// Contribution points per stone type
	public static final int BLUE_CONTRIB_POINTS = 1;
	public static final int GREEN_CONTRIB_POINTS = 1;
	public static final int RED_CONTRIB_POINTS = 1;
	
	// Ancient Adena value per stone (for exchange)
	public static final int SEAL_STONE_BLUE_VALUE = 3;
	public static final int SEAL_STONE_GREEN_VALUE = 5;
	public static final int SEAL_STONE_RED_VALUE = 10;
	
	public static final long MAXIMUM_PLAYER_CONTRIB = Long.MAX_VALUE;
	
	private final Calendar _calendar = Calendar.getInstance();
	
	protected int _activePeriod;
	protected int _currentCycle;
	protected int _previousWinner;
	protected long _redGnosisLightScore;
	protected long _blueStrifeDarkScore;
	protected long _greenAvaricePairScore;
	
	private final Map<Integer, StatSet> _signsPlayerData = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _signsSealOwners = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _signsDawnSealTotals = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _signsDuskSealTotals = new ConcurrentHashMap<>();
	
	private ScheduledFuture<?> _periodChange;
	
	protected SevenSigns()
	{
		try
		{
			restoreSevenSignsData();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "SevenSigns: Failed to load data.", e);
		}
		
		LOGGER.info("SevenSigns: Currently in the " + getCurrentPeriodName() + " period!");
		initializeSeals();
		
		if (isSealValidationPeriod())
		{
			if (getCabalHighestScore() == CABAL_NULL)
			{
				LOGGER.info("SevenSigns: The competition last week ended with a tie.");
			}
			else
			{
				LOGGER.info("SevenSigns: The " + getCabalName(getCabalHighestScore()) + " were victorious last week.");
			}
		}
		else if (getCabalHighestScore() == CABAL_NULL)
		{
			LOGGER.info("SevenSigns: The competition this week, if the trend continues, will end with a tie.");
		}
		else
		{
			LOGGER.info("SevenSigns: The " + getCabalName(getCabalHighestScore()) + " are in the lead this week.");
		}
		
		setCalendarForNextPeriodChange();
		final long milliToChange = Math.max(getMilliToPeriodChange(), 10);
		_periodChange = ThreadPool.schedule(new SevenSignsPeriodChange(), milliToChange);
		
		final long seconds = milliToChange / 1000;
		final long numMins = (seconds / 60) % 60;
		final long numHours = (seconds / 3600) % 24;
		final long numDays = seconds / 86400;
		LOGGER.info("SevenSigns: Next period begins in " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");
		
		if (AltSettingsConfig.SS_ANNOUNCE_PERIOD > 0)
		{
			ThreadPool.schedule(new SevenSignsAnnounce(), AltSettingsConfig.SS_ANNOUNCE_PERIOD * 60000L);
		}
	}
	
	public static SevenSigns getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	// ========================== Static helpers ==========================
	
	public static long calcContributionScore(long blueCount, long greenCount, long redCount)
	{
		// Green excluded from contribution score (goes to Avarice).
		return (blueCount * BLUE_CONTRIB_POINTS) + (redCount * RED_CONTRIB_POINTS);
	}
	
	public static long calcAncientAdenaReward(long blueCount, long greenCount, long redCount)
	{
		return (blueCount * AltSettingsConfig.ALT_SS_BLUE_STONE_VAL) + (greenCount * AltSettingsConfig.ALT_SS_GREEN_STONE_VAL) + (redCount * AltSettingsConfig.ALT_SS_RED_STONE_VAL);
	}
	
	public static int getCabalNumber(String cabal)
	{
		if ("dawn".equalsIgnoreCase(cabal))
		{
			return CABAL_DAWN;
		}
		if ("dusk".equalsIgnoreCase(cabal))
		{
			return CABAL_DUSK;
		}
		return CABAL_NULL;
	}
	
	public static String getCabalShortName(int cabal)
	{
		switch (cabal)
		{
			case CABAL_DAWN:
				return "dawn";
			case CABAL_DUSK:
				return "dusk";
		}
		return "";
	}
	
	public static String getCabalName(int cabal)
	{
		switch (cabal)
		{
			case CABAL_DAWN:
				return "Lords of Dawn";
			case CABAL_DUSK:
				return "Revolutionaries of Dusk";
		}
		return "No Cabal";
	}
	
	public static String getSealName(int seal, boolean shortName)
	{
		String sealName = shortName ? "" : "Seal of ";
		switch (seal)
		{
			case SEAL_AVARICE:
				sealName += "Avarice";
				break;
			case SEAL_GNOSIS:
				sealName += "Gnosis";
				break;
			case SEAL_STRIFE:
				sealName += "Strife";
				break;
		}
		return sealName;
	}
	
	// ========================== Getters ==========================
	
	public int getCurrentCycle()
	{
		return _currentCycle;
	}
	
	public int getCurrentPeriod()
	{
		return _activePeriod;
	}
	
	public String getCurrentPeriodName()
	{
		switch (_activePeriod)
		{
			case PERIOD_COMP_RECRUITING:
				return "Quest Event Initialization";
			case PERIOD_COMPETITION:
				return "Competition (Quest Event)";
			case PERIOD_COMP_RESULTS:
				return "Quest Event Results";
			case PERIOD_SEAL_VALIDATION:
				return "Seal Validation";
		}
		return "Unknown";
	}
	
	public boolean isSealValidationPeriod()
	{
		return _activePeriod == PERIOD_SEAL_VALIDATION;
	}
	
	public boolean isCompResultsPeriod()
	{
		return _activePeriod == PERIOD_COMP_RESULTS;
	}
	
	public long getCurrentScore(int cabal)
	{
		switch (cabal)
		{
			case CABAL_NULL:
				return _greenAvaricePairScore;
			case CABAL_DAWN:
				return _redGnosisLightScore;
			case CABAL_DUSK:
				return _blueStrifeDarkScore;
		}
		return 0;
	}
	
	public int getCabalHighestScore()
        {
                if (_redGnosisLightScore == _blueStrifeDarkScore)
                {
                        return CABAL_NULL;
                }
                return (_redGnosisLightScore > _blueStrifeDarkScore) ? CABAL_DAWN : CABAL_DUSK;
        }
	
	public int getSealOwner(int seal)
	{
		return _signsSealOwners.getOrDefault(seal, CABAL_NULL);
	}
	
	public int getSealProportion(int seal, int cabal)
	{
		if (cabal == CABAL_DUSK)
		{
			return _signsDuskSealTotals.getOrDefault(seal, 0);
		}
		if (cabal == CABAL_DAWN)
		{
			return _signsDawnSealTotals.getOrDefault(seal, 0);
		}
		return 0;
	}
	
	public int getTotalMembers(int cabal)
	{
		int count = 0;
		for (StatSet sevenDat : _signsPlayerData.values())
		{
			if (sevenDat.getInt("cabal") == cabal)
			{
				count++;
			}
		}
		return count;
	}
	
	public StatSet getPlayerStatsSet(Player player)
	{
		return _signsPlayerData.get(player.getObjectId());
	}
	
	public long getPlayerContribScore(Player player)
	{
		final StatSet data = _signsPlayerData.get(player.getObjectId());
		if (data == null)
		{
			return 0;
		}
		if (getPlayerCabal(player) == CABAL_DAWN)
		{
			return data.getInt("dawn_contribution_score");
		}
		return data.getInt("dusk_contribution_score");
	}
	
	public int getPlayerCabal(Player player)
	{
		final StatSet data = _signsPlayerData.get(player.getObjectId());
		if (data == null)
		{
			return CABAL_NULL;
		}
		return data.getInt("cabal");
	}
	
	public int getPriestCabal(int npcId)
	{
		switch (npcId)
		{
			case 31078:
			case 31079:
			case 31080:
			case 31081:
			case 31082:
			case 31083:
			case 31084:
			case 31168:
			case 31997:
			case 31692:
			case 31694:
				return CABAL_DAWN;
			case 31085:
			case 31086:
			case 31087:
			case 31088:
			case 31089:
			case 31090:
			case 31091:
			case 31169:
			case 31998:
			case 31693:
			case 31695:
				return CABAL_DUSK;
		}
		return CABAL_NULL;
	}
	
	// ========================== Period calendar ==========================
	
	private int getDaysToPeriodChange()
	{
		int numDays = _calendar.get(Calendar.DAY_OF_WEEK) - PERIOD_START_DAY;
		if (numDays < 0)
		{
			return -numDays;
		}
		return 7 - numDays;
	}
	
	public long getMilliToPeriodChange()
	{
		return _calendar.getTimeInMillis() - System.currentTimeMillis();
	}
	
	protected void setCalendarForNextPeriodChange()
	{
		switch (getCurrentPeriod())
		{
			case PERIOD_SEAL_VALIDATION:
			case PERIOD_COMPETITION:
			{
				int daysToChange = getDaysToPeriodChange();
				if (daysToChange == 7)
				{
					if (_calendar.get(Calendar.HOUR_OF_DAY) < PERIOD_START_HOUR)
					{
						daysToChange = 0;
					}
					else if ((_calendar.get(Calendar.HOUR_OF_DAY) == PERIOD_START_HOUR) && (_calendar.get(Calendar.MINUTE) < PERIOD_START_MINS))
					{
						daysToChange = 0;
					}
				}
				if (daysToChange > 0)
				{
					_calendar.add(Calendar.DATE, daysToChange);
				}
				_calendar.set(Calendar.HOUR_OF_DAY, PERIOD_START_HOUR);
				_calendar.set(Calendar.MINUTE, PERIOD_START_MINS);
				break;
			}
			case PERIOD_COMP_RECRUITING:
			case PERIOD_COMP_RESULTS:
			{
				_calendar.add(Calendar.MILLISECOND, PERIOD_MINOR_LENGTH);
				break;
			}
		}
	}
	
	// ========================== Database ==========================
	
	protected void restoreSevenSignsData()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			// Load player data.
			try (PreparedStatement ps = con.prepareStatement("SELECT char_obj_id, cabal, seal, dawn_red_stones, dawn_green_stones, dawn_blue_stones, dawn_ancient_adena_amount, dawn_contribution_score, dusk_red_stones, dusk_green_stones, dusk_blue_stones, dusk_ancient_adena_amount, dusk_contribution_score FROM seven_signs"))
			{
				try (ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
					{
						final int charObjId = rs.getInt("char_obj_id");
						final StatSet sevenDat = new StatSet();
						sevenDat.set("char_obj_id", charObjId);
						sevenDat.set("cabal", getCabalNumber(rs.getString("cabal")));
						sevenDat.set("seal", rs.getInt("seal"));
						sevenDat.set("dawn_red_stones", rs.getInt("dawn_red_stones"));
						sevenDat.set("dawn_green_stones", rs.getInt("dawn_green_stones"));
						sevenDat.set("dawn_blue_stones", rs.getInt("dawn_blue_stones"));
						sevenDat.set("dawn_ancient_adena_amount", rs.getInt("dawn_ancient_adena_amount"));
						sevenDat.set("dawn_contribution_score", rs.getInt("dawn_contribution_score"));
						sevenDat.set("dusk_red_stones", rs.getInt("dusk_red_stones"));
						sevenDat.set("dusk_green_stones", rs.getInt("dusk_green_stones"));
						sevenDat.set("dusk_blue_stones", rs.getInt("dusk_blue_stones"));
						sevenDat.set("dusk_ancient_adena_amount", rs.getInt("dusk_ancient_adena_amount"));
						sevenDat.set("dusk_contribution_score", rs.getInt("dusk_contribution_score"));
						_signsPlayerData.put(charObjId, sevenDat);
					}
				}
			}
			
			// Load status data.
			try (PreparedStatement ps = con.prepareStatement("SELECT * FROM seven_signs_status"))
			{
				try (ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
					{
						_currentCycle = rs.getInt("current_cycle");
						_activePeriod = rs.getInt("active_period");
						_previousWinner = rs.getInt("previous_winner");
						_redGnosisLightScore = rs.getLong("dawn_stone_score");
						_blueStrifeDarkScore = rs.getLong("dusk_stone_score");
						_greenAvaricePairScore = rs.getLong("avarice_score");
						
						_signsSealOwners.put(SEAL_AVARICE, 2);
						_signsSealOwners.put(SEAL_GNOSIS, 2);
						_signsSealOwners.put(SEAL_STRIFE, 2);
						
						_signsDawnSealTotals.put(SEAL_AVARICE, 0);
						_signsDawnSealTotals.put(SEAL_GNOSIS, 0);
						_signsDawnSealTotals.put(SEAL_STRIFE, 0);
						_signsDuskSealTotals.put(SEAL_AVARICE, 0);
						_signsDuskSealTotals.put(SEAL_GNOSIS, 0);
						_signsDuskSealTotals.put(SEAL_STRIFE, 0);
					}
				}
			}
			
			// Update date field.
			try (PreparedStatement ps = con.prepareStatement("UPDATE seven_signs_status SET date=?"))
			{
				ps.setInt(1, Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
				ps.execute();
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "SevenSigns: Unable to load data.", e);
		}
	}
	
	public synchronized void saveSevenSignsData(int playerId, boolean updateSettings)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("UPDATE seven_signs SET cabal=?, seal=?, dawn_red_stones=?, dawn_green_stones=?, dawn_blue_stones=?, dawn_ancient_adena_amount=?, dawn_contribution_score=?, dusk_red_stones=?, dusk_green_stones=?, dusk_blue_stones=?, dusk_ancient_adena_amount=?, dusk_contribution_score=? WHERE char_obj_id=?"))
			{
				if (playerId > 0)
				{
					final StatSet data = _signsPlayerData.get(playerId);
					if (data != null)
					{
						processStatement(ps, data);
					}
				}
				else
				{
					for (StatSet sevenDat : _signsPlayerData.values())
					{
						processStatement(ps, sevenDat);
					}
				}
			}
			
			if (updateSettings)
			{
				try (PreparedStatement ps = con.prepareStatement("UPDATE seven_signs_status SET current_cycle=?, active_period=?, date=?, previous_winner=?, dawn_stone_score=?, dusk_stone_score=?, avarice_score=?"))
				{
					ps.setInt(1, _currentCycle);
					ps.setInt(2, _activePeriod);
					ps.setInt(3, Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
					ps.setInt(4, _previousWinner);
					ps.setLong(5, _redGnosisLightScore);
					ps.setLong(6, _blueStrifeDarkScore);
					ps.setLong(7, _greenAvaricePairScore);
					ps.executeUpdate();
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "SevenSigns: Unable to save data.", e);
		}
	}
	
	private static void processStatement(PreparedStatement ps, StatSet sevenDat) throws SQLException
	{
		ps.setString(1, getCabalShortName(sevenDat.getInt("cabal")));
		ps.setInt(2, sevenDat.getInt("seal"));
		ps.setInt(3, sevenDat.getInt("dawn_red_stones"));
		ps.setInt(4, sevenDat.getInt("dawn_green_stones"));
		ps.setInt(5, sevenDat.getInt("dawn_blue_stones"));
		ps.setInt(6, sevenDat.getInt("dawn_ancient_adena_amount"));
		ps.setInt(7, sevenDat.getInt("dawn_contribution_score"));
		ps.setInt(8, sevenDat.getInt("dusk_red_stones"));
		ps.setInt(9, sevenDat.getInt("dusk_green_stones"));
		ps.setInt(10, sevenDat.getInt("dusk_blue_stones"));
		ps.setInt(11, sevenDat.getInt("dusk_ancient_adena_amount"));
		ps.setInt(12, sevenDat.getInt("dusk_contribution_score"));
		ps.setInt(13, sevenDat.getInt("char_obj_id"));
		ps.executeUpdate();
	}
	
	// ========================== Player registration ==========================
	
	public int setPlayerInfo(int charObjId, int chosenCabal, int chosenSeal)
	{
		StatSet currPlayer = _signsPlayerData.get(charObjId);
		if (currPlayer != null)
		{
			currPlayer.set("cabal", chosenCabal);
			currPlayer.set("seal", chosenSeal);
		}
		else
		{
			currPlayer = new StatSet();
			currPlayer.set("char_obj_id", charObjId);
			currPlayer.set("cabal", chosenCabal);
			currPlayer.set("seal", chosenSeal);
			currPlayer.set("dawn_red_stones", 0);
			currPlayer.set("dawn_green_stones", 0);
			currPlayer.set("dawn_blue_stones", 0);
			currPlayer.set("dawn_ancient_adena_amount", 0);
			currPlayer.set("dawn_contribution_score", 0);
			currPlayer.set("dusk_red_stones", 0);
			currPlayer.set("dusk_green_stones", 0);
			currPlayer.set("dusk_blue_stones", 0);
			currPlayer.set("dusk_ancient_adena_amount", 0);
			currPlayer.set("dusk_contribution_score", 0);
			_signsPlayerData.put(charObjId, currPlayer);
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("INSERT INTO seven_signs (char_obj_id, cabal, seal) VALUES (?,?,?)"))
			{
				ps.setInt(1, charObjId);
				ps.setString(2, getCabalShortName(chosenCabal));
				ps.setInt(3, chosenSeal);
				ps.execute();
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.SEVERE, "SevenSigns: Failed to save new player data.", e);
			}
		}
		
		// Add existing contributions to global scores.
		final int greens = currPlayer.getInt("dawn_green_stones");
		_greenAvaricePairScore += greens * GREEN_CONTRIB_POINTS;
		
		switch (chosenCabal)
		{
			case CABAL_DAWN:
			{
				final long contribScore = calcContributionScore(currPlayer.getInt("dawn_blue_stones"), greens, currPlayer.getInt("dawn_red_stones"));
				_redGnosisLightScore += contribScore;
				break;
			}
			case CABAL_DUSK:
			{
				final long contribScore = calcContributionScore(currPlayer.getInt("dusk_blue_stones"), greens, currPlayer.getInt("dusk_red_stones"));
				_blueStrifeDarkScore += contribScore;
				break;
			}
		}
		
		saveSevenSignsData(charObjId, true);
		return chosenCabal;
	}
	
	// ========================== Stone contribution ==========================
	
	public long addPlayerStoneContrib(Player player, long blueCount, long greenCount, long redCount)
	{
		return addPlayerStoneContrib(player.getObjectId(), blueCount, greenCount, redCount);
	}
	
	public long addPlayerStoneContrib(int charObjId, long blueCount, long greenCount, long redCount)
	{
		final StatSet currPlayer = _signsPlayerData.get(charObjId);
		if (currPlayer == null)
		{
			return 0;
		}
		
		final long contribScore = calcContributionScore(blueCount, greenCount, redCount);

_greenAvaricePairScore += greenCount;
				_redGnosisLightScore += redCount;
				_blueStrifeDarkScore += blueCount;

				if (currPlayer.getInt("cabal") == CABAL_DAWN)
		{
			final long totalAncientAdena = currPlayer.getInt("dawn_ancient_adena_amount") + calcAncientAdenaReward(blueCount, greenCount, redCount);
			final long totalContribScore = currPlayer.getInt("dawn_contribution_score") + contribScore;
			if (totalContribScore > MAXIMUM_PLAYER_CONTRIB)
			{
				return -1;
			}
			currPlayer.set("dawn_red_stones", currPlayer.getInt("dawn_red_stones") + redCount);
			currPlayer.set("dawn_green_stones", currPlayer.getInt("dawn_green_stones") + greenCount);
			currPlayer.set("dawn_blue_stones", currPlayer.getInt("dawn_blue_stones") + blueCount);
			currPlayer.set("dawn_ancient_adena_amount", totalAncientAdena);
			currPlayer.set("dawn_contribution_score", totalContribScore);
		} else if (currPlayer.getInt("cabal") == CABAL_DUSK) { final long totalAncientAdena = currPlayer.getInt("dusk_ancient_adena_amount") + calcAncientAdenaReward(blueCount, greenCount, redCount);
			final long totalContribScore = currPlayer.getInt("dusk_contribution_score") + contribScore;
			if (totalContribScore > MAXIMUM_PLAYER_CONTRIB)
			{
				return -1;
			}
			currPlayer.set("dusk_red_stones", currPlayer.getInt("dusk_red_stones") + redCount);
			currPlayer.set("dusk_green_stones", currPlayer.getInt("dusk_green_stones") + greenCount);
			currPlayer.set("dusk_blue_stones", currPlayer.getInt("dusk_blue_stones") + blueCount);
			currPlayer.set("dusk_ancient_adena_amount", totalAncientAdena);
			currPlayer.set("dusk_contribution_score", totalContribScore);
		}
		
		saveSevenSignsData(charObjId, true);
		return contribScore;
	}
	
	// ========================== Rewards ==========================
	
	public long getAncientAdenaReward(Player player, boolean removeReward)
        {
                final int charObjId = player.getObjectId();
                final StatSet currPlayer = _signsPlayerData.get(charObjId);     
                if (currPlayer == null)
                {
                        return 0;
                }

                long rewardAmount = 0;
                int highestCabal = getCabalHighestScore();
                int pCabal = currPlayer.getInt("cabal");
                
                if (highestCabal != CABAL_NULL && pCabal == highestCabal)
                {
                        if (highestCabal == CABAL_DAWN)
                        {
								rewardAmount = (currPlayer.getInt("dawn_red_stones") * AltSettingsConfig.ALT_SS_RED_STONE_VAL) +
								               (currPlayer.getInt("dawn_green_stones") * AltSettingsConfig.ALT_SS_GREEN_STONE_VAL);
                                if (removeReward) {
                                        currPlayer.set("dawn_red_stones", 0);
                                        currPlayer.set("dawn_green_stones", 0);
                                }
                        }
                        else if (highestCabal == CABAL_DUSK)
                        {
								rewardAmount = (currPlayer.getInt("dusk_blue_stones") * AltSettingsConfig.ALT_SS_BLUE_STONE_VAL) +
								               (currPlayer.getInt("dusk_green_stones") * AltSettingsConfig.ALT_SS_GREEN_STONE_VAL);
                                if (removeReward) {
                                        currPlayer.set("dusk_blue_stones", 0);
                                        currPlayer.set("dusk_green_stones", 0);
                                }
                        }
                }

                if (removeReward)
                {
                        _signsPlayerData.put(charObjId, currPlayer);
                        saveSevenSignsData(charObjId, false);
                }
                return rewardAmount;
        }
	
	// ========================== Period messages ==========================
	
	public void sendCurrentPeriodMsg(Player player)
	{
		switch (_activePeriod)
		{
			case PERIOD_COMP_RECRUITING:
				player.sendPacket(SystemMessageId.SEVEN_SIGNS_PREPARATIONS_HAVE_BEGUN_FOR_THE_NEXT_QUEST_EVENT);
				break;
			case PERIOD_COMPETITION:
				player.sendPacket(SystemMessageId.SEVEN_SIGNS_THE_QUEST_EVENT_PERIOD_HAS_BEGUN_SPEAK_WITH_A_PRIEST_OF_DAWN_OR_PRIESTESS_OF_DUSK_IF_YOU_WISH_TO_PARTICIPATE_IN_THE_EVENT);
				break;
			case PERIOD_COMP_RESULTS:
				player.sendPacket(SystemMessageId.SEVEN_SIGNS_THE_COMPETITION_PERIOD_HAS_ENDED_THE_NEXT_QUEST_EVENT_WILL_START_IN_ONE_WEEK);
				break;
			case PERIOD_SEAL_VALIDATION:
				player.sendPacket(SystemMessageId.SEVEN_SIGNS_THE_SEAL_VALIDATION_PERIOD_HAS_BEGUN);
				break;
		}
	}
	
	// ========================== Seal logic ==========================
	
	protected void initializeSeals()
	{
		for (Map.Entry<Integer, Integer> entry : _signsSealOwners.entrySet())
		{
			final int sealOwner = entry.getValue();
			final int currSeal = entry.getKey();
			if (sealOwner != CABAL_NULL)
			{
				if (isSealValidationPeriod())
				{
					LOGGER.info("SevenSigns: The " + getCabalName(sealOwner) + " have won the " + getSealName(currSeal, false) + ".");
				}
				else
				{
					LOGGER.info("SevenSigns: The " + getSealName(currSeal, false) + " is currently owned by " + getCabalName(sealOwner) + ".");
				}
			}
			else
			{
				LOGGER.info("SevenSigns: The " + getSealName(currSeal, false) + " remains unclaimed.");
			}
		}
	}
	
	protected void resetSeals()
	{
		_signsDawnSealTotals.put(SEAL_AVARICE, 0);
		_signsDawnSealTotals.put(SEAL_GNOSIS, 0);
		_signsDawnSealTotals.put(SEAL_STRIFE, 0);
		_signsDuskSealTotals.put(SEAL_AVARICE, 0);
		_signsDuskSealTotals.put(SEAL_GNOSIS, 0);
		_signsDuskSealTotals.put(SEAL_STRIFE, 0);
	}
	
	protected void resetPlayerData()
{
for (StatSet sevenDat : _signsPlayerData.values())
{
final int charObjId = sevenDat.getInt("char_obj_id");

// burn stones only for the winning seal (Rule 20)
if (_previousWinner == CABAL_NULL)
{
sevenDat.set("dawn_green_stones", 0);
sevenDat.set("dusk_green_stones", 0);
}
else if (_previousWinner == CABAL_DAWN)
{
sevenDat.set("dawn_red_stones", 0);
sevenDat.set("dawn_contribution_score", 0);
}
else if (_previousWinner == CABAL_DUSK)
{
sevenDat.set("dusk_blue_stones", 0);
sevenDat.set("dusk_contribution_score", 0);
}

sevenDat.set("cabal", CABAL_NULL);
sevenDat.set("seal", SEAL_NULL);
_signsPlayerData.put(charObjId, sevenDat);
	}
}
	
	protected void calcNewSealOwners()
	{
		for (int currSeal : _signsDawnSealTotals.keySet())
		{
			final int prevSealOwner = _signsSealOwners.getOrDefault(currSeal, CABAL_NULL);
			int newSealOwner = CABAL_NULL;
			final int dawnProportion = getSealProportion(currSeal, CABAL_DAWN);
			final int totalDawnMembers = Math.max(getTotalMembers(CABAL_DAWN), 1);
			final int duskProportion = getSealProportion(currSeal, CABAL_DUSK);
			final int totalDuskMembers = Math.max(getTotalMembers(CABAL_DUSK), 1);
			
			switch (prevSealOwner)
			{
				case CABAL_NULL:
				{
					switch (getCabalHighestScore())
					{
						case CABAL_NULL:
							newSealOwner = (dawnProportion >= Math.round(0.35 * totalDawnMembers) && (dawnProportion > duskProportion)) ? CABAL_DAWN : ((duskProportion >= Math.round(0.35 * totalDuskMembers) && (duskProportion > dawnProportion)) ? CABAL_DUSK : prevSealOwner);
							break;
						case CABAL_DAWN:
							newSealOwner = (dawnProportion >= Math.round(0.35 * totalDawnMembers)) ? CABAL_DAWN : ((duskProportion >= Math.round(0.35 * totalDuskMembers)) ? CABAL_DUSK : prevSealOwner);
							break;
						case CABAL_DUSK:
							newSealOwner = (duskProportion >= Math.round(0.35 * totalDuskMembers)) ? CABAL_DUSK : ((dawnProportion >= Math.round(0.35 * totalDawnMembers)) ? CABAL_DAWN : prevSealOwner);
							break;
					}
					break;
				}
				case CABAL_DAWN:
				{
					switch (getCabalHighestScore())
					{
						case CABAL_NULL:
						case CABAL_DAWN:
							newSealOwner = (dawnProportion >= Math.round(0.10 * totalDawnMembers)) ? prevSealOwner : ((duskProportion >= Math.round(0.35 * totalDuskMembers)) ? CABAL_DUSK : CABAL_NULL);
							break;
						case CABAL_DUSK:
							newSealOwner = (duskProportion >= Math.round(0.35 * totalDuskMembers)) ? CABAL_DUSK : ((dawnProportion >= Math.round(0.10 * totalDawnMembers)) ? prevSealOwner : CABAL_NULL);
							break;
					}
					break;
				}
				case CABAL_DUSK:
				{
					switch (getCabalHighestScore())
					{
						case CABAL_NULL:
						case CABAL_DUSK:
							newSealOwner = (duskProportion >= Math.round(0.10 * totalDuskMembers)) ? prevSealOwner : ((dawnProportion >= Math.round(0.35 * totalDawnMembers)) ? CABAL_DAWN : CABAL_NULL);
							break;
						case CABAL_DAWN:
							newSealOwner = (dawnProportion >= Math.round(0.35 * totalDawnMembers)) ? CABAL_DAWN : ((duskProportion >= Math.round(0.10 * totalDuskMembers)) ? prevSealOwner : CABAL_NULL);
							break;
					}
					break;
				}
			}
			
			_signsSealOwners.put(currSeal, newSealOwner);
			
			// Broadcast seal ownership changes.
			switch (currSeal)
			{
				case SEAL_AVARICE:
					if (newSealOwner == CABAL_DAWN)
					{
						Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.SEVEN_SIGNS_THE_LORDS_OF_DAWN_HAVE_CONQUERED_THE_SEAL_OF_AVARICE));
					}
					else if (newSealOwner == CABAL_DUSK)
					{
						Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.SEVEN_SIGNS_THE_REVOLUTIONARIES_OF_DUSK_HAVE_CONQUERED_THE_SEAL_OF_AVARICE));
					}
					break;
				case SEAL_GNOSIS:
					if (newSealOwner == CABAL_DAWN)
					{
						Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.SEVEN_SIGNS_THE_LORDS_OF_DAWN_HAVE_OBTAINED_THE_SEAL_OF_GNOSIS));
					}
					else if (newSealOwner == CABAL_DUSK)
					{
						Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.SEVEN_SIGNS_THE_REVOLUTIONARIES_OF_DUSK_HAVE_OBTAINED_THE_SEAL_OF_GNOSIS));
					}
					break;
				case SEAL_STRIFE:
					if (newSealOwner == CABAL_DAWN)
					{
						Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.SEVEN_SIGNS_THE_LORDS_OF_DAWN_HAVE_OBTAINED_THE_SEAL_OF_STRIFE));
					}
					else if (newSealOwner == CABAL_DUSK)
					{
						Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.SEVEN_SIGNS_THE_REVOLUTIONARIES_OF_DUSK_HAVE_OBTAINED_THE_SEAL_OF_STRIFE));
					}
					break;
			}
		}
	}
	
	private void resetWinnerPoints()
	{
		switch (_previousWinner)
		{
			case CABAL_NULL:
				_greenAvaricePairScore = 0;
				break;
			case CABAL_DAWN:
				_redGnosisLightScore = 0;
				break;
			case CABAL_DUSK:
				_blueStrifeDarkScore = 0;
				break;
		}
	}
	
	// ========================== Admin helpers ==========================
	
	public void changePeriod()
	{
		if (_periodChange != null)
		{
			_periodChange.cancel(false);
		}
		_periodChange = ThreadPool.schedule(new SevenSignsPeriodChange(), 10);
	}
	
	public void changePeriod(int period, int seconds)
	{
		_activePeriod = period - 1;
		if (_activePeriod < 0)
		{
			_activePeriod += 4;
		}
		if (_periodChange != null)
		{
			_periodChange.cancel(false);
		}
		_periodChange = ThreadPool.schedule(new SevenSignsPeriodChange(), seconds * 1000L);
	}
	
	// ========================== Periodic announce ==========================
	
	private class SevenSignsAnnounce implements Runnable
	{
		@Override
		public void run()
		{
			for (Player player : World.getInstance().getPlayers())
			{
				sendCurrentPeriodMsg(player);
			}
			if (AltSettingsConfig.SS_ANNOUNCE_PERIOD > 0)
			{
				ThreadPool.schedule(new SevenSignsAnnounce(), AltSettingsConfig.SS_ANNOUNCE_PERIOD * 60000L);
			}
		}
	}
	
	// ========================== Period change controller ==========================
	
	private class SevenSignsPeriodChange implements Runnable
	{
		@Override
		public void run()
		{
			final int periodEnded = _activePeriod;
			_activePeriod++;
			
			switch (periodEnded)
			{
				case PERIOD_COMP_RECRUITING:
				{
					Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.SEVEN_SIGNS_THE_COMPETITION_PERIOD_HAS_BEGUN_VISIT_A_PRIEST_OF_DAWN_OR_PRIESTESS_OF_DUSK_TO_PARTICIPATE_IN_THE_EVENT));
					break;
				}
				case PERIOD_COMPETITION:
				{
					Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.SEVEN_SIGNS_THE_COMPETITION_PERIOD_HAS_ENDED_THE_NEXT_QUEST_EVENT_WILL_START_IN_ONE_WEEK));
					final int compWinner = getCabalHighestScore();
					calcNewSealOwners();
					if (compWinner == CABAL_DUSK)
					{
						// DUSK won - use generic fallback since specific DUSK-won message may not exist in RV.
						LOGGER.info("SevenSigns: The Revolutionaries of Dusk have won.");
					}
					else
					{
						LOGGER.info("SevenSigns: The Lords of Dawn have won.");
					}
					_previousWinner = compWinner;
					break;
				}
				case PERIOD_COMP_RESULTS:
				{
					initializeSeals();
					Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.SEVEN_SIGNS_THE_SEAL_VALIDATION_PERIOD_HAS_BEGUN));
					LOGGER.info("SevenSigns: The " + getCabalName(_previousWinner) + " have won the competition with " + getCurrentScore(_previousWinner) + " points!");
					break;
				}
				case PERIOD_SEAL_VALIDATION:
				{
					_activePeriod = PERIOD_COMP_RECRUITING;
					Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.SEVEN_SIGNS_THE_SEAL_VALIDATION_PERIOD_HAS_ENDED));
					resetPlayerData();
					resetSeals();
					_currentCycle++;
					resetWinnerPoints();
					break;
				}
			}
			
			saveSevenSignsData(0, true);
			
			try
			{
				LOGGER.info("SevenSigns: The " + getCurrentPeriodName() + " period has begun!");
				setCalendarForNextPeriodChange();
				final long milliToChange = Math.max(getMilliToPeriodChange(), 10);
				_periodChange = ThreadPool.schedule(new SevenSignsPeriodChange(), milliToChange);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "SevenSigns: Error scheduling next period change.", e);
			}
		}
	}
	
	// ========================== Singleton ==========================
	
	private static class SingletonHolder
	{
		protected static final SevenSigns INSTANCE = new SevenSigns();
	}



}
