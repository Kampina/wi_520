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
package org.l2jmobius.gameserver.config;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.network.serverpackets.ExChangeClientEffectInfo;

/**
 * This class loads all the feature related configurations.
 * @author Mobius
 */
public class FeatureConfig
{
	// File
	private static final String FEATURE_CONFIG_FILE = "./config/Feature.ini";
	private static final String RESIDENCE_CONFIG_FILE = "./config/residence.ini";
	private static final String CLAN_CONFIG_FILE = "./config/alternative/clan.ini";
	
	// Constants
	public static List<Integer> SIEGE_HOUR_LIST;
	public static long DOMINION_REGISTRATION_CLOSE_WINDOW_MILLIS;
	public static long DOMINION_BATTLEFIELD_START_MILLIS;
	public static long DOMINION_BATTLEFIELD_STOP_MILLIS;
	public static int CASTLE_BUY_TAX_NEUTRAL;
	public static int CASTLE_BUY_TAX_LIGHT;
	public static int CASTLE_BUY_TAX_DARK;
	public static int CASTLE_SELL_TAX_NEUTRAL;
	public static int CASTLE_SELL_TAX_LIGHT;
	public static int CASTLE_SELL_TAX_DARK;
	public static long CS_TELE_FEE_RATIO;
	public static int CS_TELE1_FEE;
	public static int CS_TELE2_FEE;
	public static long CS_SUPPORT_FEE_RATIO;
	public static int CS_SUPPORT1_FEE;
	public static int CS_SUPPORT2_FEE;
	public static long CS_MPREG_FEE_RATIO;
	public static int CS_MPREG1_FEE;
	public static int CS_MPREG2_FEE;
	public static long CS_HPREG_FEE_RATIO;
	public static int CS_HPREG1_FEE;
	public static int CS_HPREG2_FEE;
	public static long CS_EXPREG_FEE_RATIO;
	public static int CS_EXPREG1_FEE;
	public static int CS_EXPREG2_FEE;
	public static int OUTER_DOOR_UPGRADE_PRICE2;
	public static int OUTER_DOOR_UPGRADE_PRICE3;
	public static int OUTER_DOOR_UPGRADE_PRICE5;
	public static int INNER_DOOR_UPGRADE_PRICE2;
	public static int INNER_DOOR_UPGRADE_PRICE3;
	public static int INNER_DOOR_UPGRADE_PRICE5;
	public static int WALL_UPGRADE_PRICE2;
	public static int WALL_UPGRADE_PRICE3;
	public static int WALL_UPGRADE_PRICE5;
	public static int TRAP_UPGRADE_PRICE1;
	public static int TRAP_UPGRADE_PRICE2;
	public static int TRAP_UPGRADE_PRICE3;
	public static int TRAP_UPGRADE_PRICE4;
	public static double CLAN_HALL_LEASE_MULTIPLIER;
	public static double CLAN_HALL_MIN_BID_MULTIPLIER;
	public static int CLAN_HALL_CLAN_MIN_LEVEL_FOR_BID;
	public static long CLAN_HALL_AUCTION_TIME;
	public static double RESIDENCE_LEASE_FUNCTION_MULTIPLIER;
	public static long FS_TELE_FEE_RATIO;
	public static int FS_TELE1_FEE;
	public static int FS_TELE2_FEE;
	public static long FS_SUPPORT_FEE_RATIO;
	public static int FS_SUPPORT1_FEE;
	public static int FS_SUPPORT2_FEE;
	public static long FS_MPREG_FEE_RATIO;
	public static int FS_MPREG1_FEE;
	public static int FS_MPREG2_FEE;
	public static long FS_HPREG_FEE_RATIO;
	public static int FS_HPREG1_FEE;
	public static int FS_HPREG2_FEE;
	public static long FS_EXPREG_FEE_RATIO;
	public static int FS_EXPREG1_FEE;
	public static int FS_EXPREG2_FEE;
	public static int FS_UPDATE_FRQ;
	public static int FS_BLOOD_OATH_COUNT;
	public static int FS_MAX_SUPPLY_LEVEL;
	public static int FS_FEE_FOR_CASTLE;
	public static int FS_MAX_OWN_TIME;
	public static int TAKE_FORT_POINTS;
	public static int LOOSE_FORT_POINTS;
	public static int TAKE_CASTLE_POINTS;
	public static int LOOSE_CASTLE_POINTS;
	public static int CASTLE_DEFENDED_POINTS;
	public static int FESTIVAL_WIN_POINTS;
	public static int HERO_POINTS;
	public static int ROYAL_GUARD_COST;
	public static int KNIGHT_UNIT_COST;
	public static int KNIGHT_REINFORCE_COST;
	public static int BALLISTA_POINTS;
	public static int BLOODALLIANCE_POINTS;
	public static int BLOODOATH_POINTS;
	public static int KNIGHTSEPAULETTE_POINTS;
	public static int REPUTATION_SCORE_PER_KILL;
	public static int[] CLAN_WAR_REPUTATION_POINTS;
	public static int JOIN_ACADEMY_MIN_REP_SCORE;
	public static int JOIN_ACADEMY_MAX_REP_SCORE;
	public static int CLAN_CREATE_ITEM_ID;
	public static long CLAN_CREATE_ITEM_COUNT;
	public static int CLAN_CREATE_LEVEL;
	public static int CLAN_CREATE_POINTS;
	public static int ACADEMY_SUB_LIMIT;
	public static int ACADEMY_SUB_LIMIT_LEVEL11;
	public static int ROYAL_SUB_LIMIT_1;
	public static int ROYAL_SUB_LIMIT_2;
	public static int ROYAL_SUB_LIMIT_1_LEVEL11;
	public static int ROYAL_SUB_LIMIT_2_LEVEL11;
	public static int KNIGHT_SUB_LIMIT_1;
	public static int KNIGHT_SUB_LIMIT_2;
	public static int KNIGHT_SUB_LIMIT_3;
	public static int KNIGHT_SUB_LIMIT_4;
	public static int KNIGHT_SUB_LIMIT_1_LEVEL9;
	public static int KNIGHT_SUB_LIMIT_2_LEVEL9;
	public static int KNIGHT_SUB_LIMIT_3_LEVEL10;
	public static int KNIGHT_SUB_LIMIT_4_LEVEL10;
	public static int CLAN_LEVEL_1_COST;
	public static int CLAN_LEVEL_2_COST;
	public static int CLAN_LEVEL_3_COST;
	public static int CLAN_LEVEL_4_COST;
	public static int CLAN_LEVEL_5_COST;
	public static int LVL_UP_20_AND_25_REP_SCORE;
	public static int LVL_UP_26_AND_30_REP_SCORE;
	public static int LVL_UP_31_AND_35_REP_SCORE;
	public static int LVL_UP_36_AND_40_REP_SCORE;
	public static int LVL_UP_41_AND_45_REP_SCORE;
	public static int LVL_UP_46_AND_50_REP_SCORE;
	public static int LVL_UP_51_AND_55_REP_SCORE;
	public static int LVL_UP_56_AND_60_REP_SCORE;
	public static int LVL_UP_61_AND_65_REP_SCORE;
	public static int LVL_UP_66_AND_70_REP_SCORE;
	public static int LVL_UP_71_AND_75_REP_SCORE;
	public static int LVL_UP_76_AND_80_REP_SCORE;
	public static int LVL_UP_81_AND_90_REP_SCORE;
	public static int LVL_UP_91_PLUS_REP_SCORE;
	public static double LVL_OBTAINED_REP_SCORE_MULTIPLIER;
	public static int CLAN_LEVEL_6_COST;
	public static int CLAN_LEVEL_7_COST;
	public static int CLAN_LEVEL_8_COST;
	public static int CLAN_LEVEL_9_COST;
	public static int CLAN_LEVEL_10_COST;
	public static int CLAN_LEVEL_11_COST;
	public static int CLAN_LEVEL_1_REQUIREMENT;
	public static int CLAN_LEVEL_2_REQUIREMENT;
	public static int CLAN_LEVEL_3_REQUIREMENT;
	public static int CLAN_LEVEL_4_REQUIREMENT;
	public static int CLAN_LEVEL_5_REQUIREMENT;
	public static int CLAN_LEVEL_6_REQUIREMENT;
	public static int CLAN_LEVEL_7_REQUIREMENT;
	public static int CLAN_LEVEL_8_REQUIREMENT;
	public static int CLAN_LEVEL_9_REQUIREMENT;
	public static int CLAN_LEVEL_10_REQUIREMENT;
	public static int CLAN_LEVEL_11_REQUIREMENT;
	public static int CLAN_LEVEL_9_BLOOD_OATH_REQUIREMENT;
	public static int CLAN_LEVEL_10_BLOOD_PLEDGE_REQUIREMENT;
	public static int CLAN_LEVEL_11_ITEM_ID;
	public static long CLAN_LEVEL_11_ITEM_COUNT;
	public static int[] CLAN_MEMBER_LIMITS;
	public static int CLAN_EXPELLED_MEMBER_PENALTY_HOURS;
	public static int CLAN_JOIN_PENALTY_HOURS;
	public static int CLAN_CREATE_PENALTY_DAYS;
	public static int CLAN_CHANGE_LEADER_WAIT_HOURS;
	public static int CLAN_DISBAND_TIME_HOURS;
	public static int CLAN_DISBAND_PENALTY_DAYS;
	public static boolean PK_PENALTY_LIST;
	public static int PK_PENALTY_LIST_MINIMUM_COUNT;
	public static int ITEM_PENALTY_RESTORE_ADENA;
	public static int ITEM_PENALTY_RESTORE_LCOIN;
	public static ExChangeClientEffectInfo GIRAN_DAY_EFFECT;
	public static ExChangeClientEffectInfo GIRAN_NIGHT_EFFECT;
	public static boolean ALLOW_WYVERN_ALWAYS;
	public static boolean ALLOW_WYVERN_DURING_SIEGE;
	public static boolean ALLOW_MOUNTS_DURING_SIEGE;
	private static final int CLAN_WAR_LEVEL_DIFFERENCE_THRESHOLD = 20;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(FEATURE_CONFIG_FILE);
		final ConfigReader residenceConfig = new ConfigReader(RESIDENCE_CONFIG_FILE);
		final ConfigReader clanConfig = new ConfigReader(CLAN_CONFIG_FILE);
		SIEGE_HOUR_LIST = new ArrayList<>();
		for (String hour : config.getString("SiegeHourList", "").split(","))
		{
			if (StringUtil.isNumeric(hour))
			{
				SIEGE_HOUR_LIST.add(Integer.parseInt(hour));
			}
		}
		DOMINION_REGISTRATION_CLOSE_WINDOW_MILLIS = config.getLong("DominionRegistrationCloseWindow", 7200000L);
		DOMINION_BATTLEFIELD_START_MILLIS = config.getLong("DominionBattlefieldStartWindow", 1200000L);
		DOMINION_BATTLEFIELD_STOP_MILLIS = config.getLong("DominionBattlefieldStopWindow", 7800000L);
		CASTLE_BUY_TAX_NEUTRAL = config.getInt("BuyTaxForNeutralSide", 15);
		CASTLE_BUY_TAX_LIGHT = config.getInt("BuyTaxForLightSide", 0);
		CASTLE_BUY_TAX_DARK = config.getInt("BuyTaxForDarkSide", 30);
		CASTLE_SELL_TAX_NEUTRAL = config.getInt("SellTaxForNeutralSide", 0);
		CASTLE_SELL_TAX_LIGHT = config.getInt("SellTaxForLightSide", 0);
		CASTLE_SELL_TAX_DARK = config.getInt("SellTaxForDarkSide", 20);
		CS_TELE_FEE_RATIO = config.getLong("CastleTeleportFunctionFeeRatio", 604800000);
		CS_TELE1_FEE = config.getInt("CastleTeleportFunctionFeeLvl1", 1000);
		CS_TELE2_FEE = config.getInt("CastleTeleportFunctionFeeLvl2", 10000);
		CS_SUPPORT_FEE_RATIO = config.getLong("CastleSupportFunctionFeeRatio", 604800000);
		CS_SUPPORT1_FEE = config.getInt("CastleSupportFeeLvl1", 49000);
		CS_SUPPORT2_FEE = config.getInt("CastleSupportFeeLvl2", 120000);
		CS_MPREG_FEE_RATIO = config.getLong("CastleMpRegenerationFunctionFeeRatio", 604800000);
		CS_MPREG1_FEE = config.getInt("CastleMpRegenerationFeeLvl1", 45000);
		CS_MPREG2_FEE = config.getInt("CastleMpRegenerationFeeLvl2", 65000);
		CS_HPREG_FEE_RATIO = config.getLong("CastleHpRegenerationFunctionFeeRatio", 604800000);
		CS_HPREG1_FEE = config.getInt("CastleHpRegenerationFeeLvl1", 12000);
		CS_HPREG2_FEE = config.getInt("CastleHpRegenerationFeeLvl2", 20000);
		CS_EXPREG_FEE_RATIO = config.getLong("CastleExpRegenerationFunctionFeeRatio", 604800000);
		CS_EXPREG1_FEE = config.getInt("CastleExpRegenerationFeeLvl1", 63000);
		CS_EXPREG2_FEE = config.getInt("CastleExpRegenerationFeeLvl2", 70000);
		OUTER_DOOR_UPGRADE_PRICE2 = config.getInt("OuterDoorUpgradePriceLvl2", 3000000);
		OUTER_DOOR_UPGRADE_PRICE3 = config.getInt("OuterDoorUpgradePriceLvl3", 4000000);
		OUTER_DOOR_UPGRADE_PRICE5 = config.getInt("OuterDoorUpgradePriceLvl5", 5000000);
		INNER_DOOR_UPGRADE_PRICE2 = config.getInt("InnerDoorUpgradePriceLvl2", 750000);
		INNER_DOOR_UPGRADE_PRICE3 = config.getInt("InnerDoorUpgradePriceLvl3", 900000);
		INNER_DOOR_UPGRADE_PRICE5 = config.getInt("InnerDoorUpgradePriceLvl5", 1000000);
		WALL_UPGRADE_PRICE2 = config.getInt("WallUpgradePriceLvl2", 1600000);
		WALL_UPGRADE_PRICE3 = config.getInt("WallUpgradePriceLvl3", 1800000);
		WALL_UPGRADE_PRICE5 = config.getInt("WallUpgradePriceLvl5", 2000000);
		TRAP_UPGRADE_PRICE1 = config.getInt("TrapUpgradePriceLvl1", 3000000);
		TRAP_UPGRADE_PRICE2 = config.getInt("TrapUpgradePriceLvl2", 4000000);
		TRAP_UPGRADE_PRICE3 = config.getInt("TrapUpgradePriceLvl3", 5000000);
		TRAP_UPGRADE_PRICE4 = config.getInt("TrapUpgradePriceLvl4", 6000000);
		CLAN_HALL_LEASE_MULTIPLIER = config.getDouble("ClanHallLeaseMultiplier", 1.0d);
		CLAN_HALL_MIN_BID_MULTIPLIER = config.getDouble("ClanHallMinBidMultiplier", 1.0d);
		CLAN_HALL_CLAN_MIN_LEVEL_FOR_BID = config.getInt("ClanHallBidClanLevel", 5);
		CLAN_HALL_AUCTION_TIME = config.getLong("ClanHallAuctionTime", 10800000L);
		RESIDENCE_LEASE_FUNCTION_MULTIPLIER = residenceConfig.getDouble("ResidenceLeaseFuncMultiplier", 1.0d);
		FS_TELE_FEE_RATIO = config.getLong("FortressTeleportFunctionFeeRatio", 604800000);
		FS_TELE1_FEE = config.getInt("FortressTeleportFunctionFeeLvl1", 1000);
		FS_TELE2_FEE = config.getInt("FortressTeleportFunctionFeeLvl2", 10000);
		FS_SUPPORT_FEE_RATIO = config.getLong("FortressSupportFunctionFeeRatio", 86400000);
		FS_SUPPORT1_FEE = config.getInt("FortressSupportFeeLvl1", 7000);
		FS_SUPPORT2_FEE = config.getInt("FortressSupportFeeLvl2", 17000);
		FS_MPREG_FEE_RATIO = config.getLong("FortressMpRegenerationFunctionFeeRatio", 86400000);
		FS_MPREG1_FEE = config.getInt("FortressMpRegenerationFeeLvl1", 6500);
		FS_MPREG2_FEE = config.getInt("FortressMpRegenerationFeeLvl2", 9300);
		FS_HPREG_FEE_RATIO = config.getLong("FortressHpRegenerationFunctionFeeRatio", 86400000);
		FS_HPREG1_FEE = config.getInt("FortressHpRegenerationFeeLvl1", 2000);
		FS_HPREG2_FEE = config.getInt("FortressHpRegenerationFeeLvl2", 3500);
		FS_EXPREG_FEE_RATIO = config.getLong("FortressExpRegenerationFunctionFeeRatio", 86400000);
		FS_EXPREG1_FEE = config.getInt("FortressExpRegenerationFeeLvl1", 9000);
		FS_EXPREG2_FEE = config.getInt("FortressExpRegenerationFeeLvl2", 10000);
		FS_UPDATE_FRQ = config.getInt("FortressPeriodicUpdateFrequency", 360);
		FS_BLOOD_OATH_COUNT = config.getInt("FortressBloodOathCount", 1);
		FS_MAX_SUPPLY_LEVEL = config.getInt("FortressMaxSupplyLevel", 6);
		FS_FEE_FOR_CASTLE = config.getInt("FortressFeeForCastle", 25000);
		FS_MAX_OWN_TIME = config.getInt("FortressMaximumOwnTime", 168);
		TAKE_FORT_POINTS = config.getInt("TakeFortPoints", 200);
		LOOSE_FORT_POINTS = config.getInt("LooseFortPoints", 0);
		TAKE_CASTLE_POINTS = config.getInt("TakeCastlePoints", 1500);
		LOOSE_CASTLE_POINTS = config.getInt("LooseCastlePoints", 3000);
		CASTLE_DEFENDED_POINTS = config.getInt("CastleDefendedPoints", 750);
		FESTIVAL_WIN_POINTS = config.getInt("FestivalOfDarknessWin", 200);
		HERO_POINTS = config.getInt("HeroPoints", 1000);
		ROYAL_GUARD_COST = config.getInt("CreateRoyalGuardCost", 5000);
		KNIGHT_UNIT_COST = config.getInt("CreateKnightUnitCost", 10000);
		KNIGHT_REINFORCE_COST = config.getInt("ReinforceKnightUnitCost", 5000);
		BALLISTA_POINTS = config.getInt("KillBallistaPoints", 500);
		BLOODALLIANCE_POINTS = config.getInt("BloodAlliancePoints", 500);
		BLOODOATH_POINTS = config.getInt("BloodOathPoints", 200);
		KNIGHTSEPAULETTE_POINTS = config.getInt("KnightsEpaulettePoints", 20);
		REPUTATION_SCORE_PER_KILL = config.getInt("ReputationScorePerKill", 1);
		CLAN_WAR_REPUTATION_POINTS = clanConfig.getIntArray("WarPoint", ",", REPUTATION_SCORE_PER_KILL + "," + REPUTATION_SCORE_PER_KILL);
		if (CLAN_WAR_REPUTATION_POINTS.length < 2)
		{
			CLAN_WAR_REPUTATION_POINTS = new int[]
			{
				REPUTATION_SCORE_PER_KILL,
				REPUTATION_SCORE_PER_KILL
			};
		}
		JOIN_ACADEMY_MIN_REP_SCORE = config.getInt("CompleteAcademyMinPoints", 190);
		JOIN_ACADEMY_MAX_REP_SCORE = config.getInt("CompleteAcademyMaxPoints", 650);
		CLAN_CREATE_ITEM_ID = clanConfig.getInt("ItemIdForClanCreation", 0);
		CLAN_CREATE_ITEM_COUNT = clanConfig.getLong("ItemCountForClanCreation", 0L);
		CLAN_CREATE_LEVEL = clanConfig.getInt("Level", 0);
		CLAN_CREATE_POINTS = clanConfig.getInt("Point", 0);
		ACADEMY_SUB_LIMIT = clanConfig.getInt("AcademyLimit", 20);
		ACADEMY_SUB_LIMIT_LEVEL11 = clanConfig.getInt("AcademyLimit11", 20);
		ROYAL_SUB_LIMIT_1 = clanConfig.getInt("RoyalLimit1", 20);
		ROYAL_SUB_LIMIT_2 = clanConfig.getInt("RoyalLimit2", 20);
		ROYAL_SUB_LIMIT_1_LEVEL11 = clanConfig.getInt("RoyalLimit1Level11", ROYAL_SUB_LIMIT_1);
		ROYAL_SUB_LIMIT_2_LEVEL11 = clanConfig.getInt("RoyalLimit2Level11", ROYAL_SUB_LIMIT_2);
		KNIGHT_SUB_LIMIT_1 = clanConfig.getInt("KnightLimit1", 10);
		KNIGHT_SUB_LIMIT_2 = clanConfig.getInt("KnightLimit2", 10);
		KNIGHT_SUB_LIMIT_3 = clanConfig.getInt("KnightLimit3", 10);
		KNIGHT_SUB_LIMIT_4 = clanConfig.getInt("KnightLimit4", 10);
		KNIGHT_SUB_LIMIT_1_LEVEL9 = clanConfig.getInt("KnightLimit1Level9", 25);
		KNIGHT_SUB_LIMIT_2_LEVEL9 = clanConfig.getInt("KnightLimit2Level9", 25);
		KNIGHT_SUB_LIMIT_3_LEVEL10 = clanConfig.getInt("KnightLimit3Level10", 25);
		KNIGHT_SUB_LIMIT_4_LEVEL10 = clanConfig.getInt("KnightLimit4Level10", 25);
		CLAN_LEVEL_1_COST = clanConfig.getInt("ReputationScoreForLevelUpTo1", 100);
		CLAN_LEVEL_2_COST = clanConfig.getInt("ReputationScoreForLevelUpTo2", 500);
		CLAN_LEVEL_3_COST = clanConfig.getInt("ReputationScoreForLevelUpTo3", 1000);
		CLAN_LEVEL_4_COST = clanConfig.getInt("ReputationScoreForLevelUpTo4", 2000);
		CLAN_LEVEL_5_COST = clanConfig.getInt("ReputationScoreForLevelUpTo5", 3500);
		LVL_UP_20_AND_25_REP_SCORE = config.getInt("LevelUp20And25ReputationScore", 4);
		LVL_UP_26_AND_30_REP_SCORE = config.getInt("LevelUp26And30ReputationScore", 8);
		LVL_UP_31_AND_35_REP_SCORE = config.getInt("LevelUp31And35ReputationScore", 12);
		LVL_UP_36_AND_40_REP_SCORE = config.getInt("LevelUp36And40ReputationScore", 16);
		LVL_UP_41_AND_45_REP_SCORE = config.getInt("LevelUp41And45ReputationScore", 25);
		LVL_UP_46_AND_50_REP_SCORE = config.getInt("LevelUp46And50ReputationScore", 30);
		LVL_UP_51_AND_55_REP_SCORE = config.getInt("LevelUp51And55ReputationScore", 35);
		LVL_UP_56_AND_60_REP_SCORE = config.getInt("LevelUp56And60ReputationScore", 40);
		LVL_UP_61_AND_65_REP_SCORE = config.getInt("LevelUp61And65ReputationScore", 54);
		LVL_UP_66_AND_70_REP_SCORE = config.getInt("LevelUp66And70ReputationScore", 63);
		LVL_UP_71_AND_75_REP_SCORE = config.getInt("LevelUp71And75ReputationScore", 75);
		LVL_UP_76_AND_80_REP_SCORE = config.getInt("LevelUp76And80ReputationScore", 90);
		LVL_UP_81_AND_90_REP_SCORE = config.getInt("LevelUp81And90ReputationScore", 120);
		LVL_UP_91_PLUS_REP_SCORE = config.getInt("LevelUp91PlusReputationScore", 150);
		LVL_OBTAINED_REP_SCORE_MULTIPLIER = config.getDouble("LevelObtainedReputationScoreMultiplier", 1.0d);
		CLAN_LEVEL_6_COST = clanConfig.getInt("ReputationScoreForLevelUpTo6", config.getInt("ClanLevel6Cost", 15000));
		CLAN_LEVEL_7_COST = clanConfig.getInt("ReputationScoreForLevelUpTo7", config.getInt("ClanLevel7Cost", 450000));
		CLAN_LEVEL_8_COST = clanConfig.getInt("ReputationScoreForLevelUpTo8", config.getInt("ClanLevel8Cost", 1000000));
		CLAN_LEVEL_9_COST = clanConfig.getInt("ReputationScoreForLevelUpTo9", config.getInt("ClanLevel9Cost", 2000000));
		CLAN_LEVEL_10_COST = clanConfig.getInt("ReputationScoreForLevelUpTo10", config.getInt("ClanLevel10Cost", 4000000));
		CLAN_LEVEL_11_COST = clanConfig.getInt("ReputationScoreForLevelUpTo11", 45000);
		CLAN_LEVEL_1_REQUIREMENT = clanConfig.getInt("MemberClanLevelUpTo1", 1);
		CLAN_LEVEL_2_REQUIREMENT = clanConfig.getInt("MemberClanLevelUpTo2", 1);
		CLAN_LEVEL_3_REQUIREMENT = clanConfig.getInt("MemberClanLevelUpTo3", 1);
		CLAN_LEVEL_4_REQUIREMENT = clanConfig.getInt("MemberClanLevelUpTo4", 1);
		CLAN_LEVEL_5_REQUIREMENT = clanConfig.getInt("MemberClanLevelUpTo5", 1);
		CLAN_LEVEL_6_REQUIREMENT = clanConfig.getInt("MemberClanLevelUpTo6", config.getInt("ClanLevel6Requirement", 40));
		CLAN_LEVEL_7_REQUIREMENT = clanConfig.getInt("MemberClanLevelUpTo7", config.getInt("ClanLevel7Requirement", 40));
		CLAN_LEVEL_8_REQUIREMENT = clanConfig.getInt("MemberClanLevelUpTo8", config.getInt("ClanLevel8Requirement", 40));
		CLAN_LEVEL_9_REQUIREMENT = clanConfig.getInt("MemberClanLevelUpTo9", config.getInt("ClanLevel9Requirement", 40));
		CLAN_LEVEL_10_REQUIREMENT = clanConfig.getInt("MemberClanLevelUpTo10", config.getInt("ClanLevel10Requirement", 40));
		CLAN_LEVEL_11_REQUIREMENT = clanConfig.getInt("MemberClanLevelUpTo11", 1);
		CLAN_LEVEL_9_BLOOD_OATH_REQUIREMENT = clanConfig.getInt("BloodOathsForLevelUpTo9", 100);
		CLAN_LEVEL_10_BLOOD_PLEDGE_REQUIREMENT = clanConfig.getInt("BloodPledgesForLevelUpTo10", 5);
		CLAN_LEVEL_11_ITEM_ID = clanConfig.getInt("ItemIdForClanLevelUpTo11", 1419);
		CLAN_LEVEL_11_ITEM_COUNT = clanConfig.getLong("ItemCountForClanLevelUpTo11", 12L);
		CLAN_MEMBER_LIMITS = new int[]
		{
			clanConfig.getInt("ClanLimitInLevel0", 10),
			clanConfig.getInt("ClanLimitInLevel1", 15),
			clanConfig.getInt("ClanLimitInLevel2", 20),
			clanConfig.getInt("ClanLimitInLevel3", 30),
			clanConfig.getInt("ClanLimitInLevel4", 40),
			clanConfig.getInt("ClanLimitInLevel5", 40),
			clanConfig.getInt("ClanLimitInLevel6", 40),
			clanConfig.getInt("ClanLimitInLevel7", 40),
			clanConfig.getInt("ClanLimitInLevel8", 40),
			clanConfig.getInt("ClanLimitInLevel9", 40),
			clanConfig.getInt("ClanLimitInLevel10", 40),
			clanConfig.getInt("ClanLimitInLevel11", 40)
		};
		CLAN_EXPELLED_MEMBER_PENALTY_HOURS = clanConfig.getInt("ExpelledMemberPenalty", 24);
		CLAN_JOIN_PENALTY_HOURS = clanConfig.getInt("JoinPenalty", 24);
		CLAN_CREATE_PENALTY_DAYS = clanConfig.getInt("CreatePenalty", 10);
		CLAN_CHANGE_LEADER_WAIT_HOURS = clanConfig.getInt("ChangeLeader", 48);
		CLAN_DISBAND_TIME_HOURS = clanConfig.getInt("DisbandTime", 48);
		CLAN_DISBAND_PENALTY_DAYS = clanConfig.getInt("DisbandPenalty", 7);
		PK_PENALTY_LIST = config.getBoolean("PkPenaltyList", true);
		PK_PENALTY_LIST_MINIMUM_COUNT = config.getInt("PkPenaltyMinimumCount", 9);
		ITEM_PENALTY_RESTORE_ADENA = config.getInt("PkPenaltyRestoreAdenaCost", 100);
		ITEM_PENALTY_RESTORE_LCOIN = config.getInt("PkPenaltyRestoreLCoinCost", 100);
		switch (config.getString("GiranDayEffect", ""))
		{
			case "Petals":
			{
				GIRAN_DAY_EFFECT = ExChangeClientEffectInfo.GIRAN_PETALS;
				break;
			}
			case "Snow":
			{
				GIRAN_DAY_EFFECT = ExChangeClientEffectInfo.GIRAN_SNOW;
				break;
			}
			case "Flowers":
			{
				GIRAN_DAY_EFFECT = ExChangeClientEffectInfo.GIRAN_FLOWERS;
				break;
			}
			case "Water":
			{
				GIRAN_DAY_EFFECT = ExChangeClientEffectInfo.GIRAN_WATER;
				break;
			}
			case "Autumn":
			{
				GIRAN_DAY_EFFECT = ExChangeClientEffectInfo.GIRAN_AUTUMN;
				break;
			}
			default:
			{
				GIRAN_DAY_EFFECT = ExChangeClientEffectInfo.GIRAN_NORMAL;
				break;
			}
		}
		switch (config.getString("GiranNightEffect", ""))
		{
			case "Petals":
			{
				GIRAN_NIGHT_EFFECT = ExChangeClientEffectInfo.GIRAN_PETALS;
				break;
			}
			case "Snow":
			{
				GIRAN_NIGHT_EFFECT = ExChangeClientEffectInfo.GIRAN_SNOW;
				break;
			}
			case "Flowers":
			{
				GIRAN_NIGHT_EFFECT = ExChangeClientEffectInfo.GIRAN_FLOWERS;
				break;
			}
			case "Water":
			{
				GIRAN_NIGHT_EFFECT = ExChangeClientEffectInfo.GIRAN_WATER;
				break;
			}
			case "Autumn":
			{
				GIRAN_NIGHT_EFFECT = ExChangeClientEffectInfo.GIRAN_AUTUMN;
				break;
			}
			default:
			{
				GIRAN_NIGHT_EFFECT = ExChangeClientEffectInfo.GIRAN_NORMAL;
				break;
			}
		}
		ALLOW_WYVERN_ALWAYS = config.getBoolean("AllowRideWyvernAlways", false);
		ALLOW_WYVERN_DURING_SIEGE = config.getBoolean("AllowRideWyvernDuringSiege", true);
		ALLOW_MOUNTS_DURING_SIEGE = config.getBoolean("AllowRideMountsDuringSiege", false);
	}

	public static int getClanLevelUpCost(int targetLevel)
	{
		switch (targetLevel)
		{
			case 1:
			{
				return CLAN_LEVEL_1_COST;
			}
			case 2:
			{
				return CLAN_LEVEL_2_COST;
			}
			case 3:
			{
				return CLAN_LEVEL_3_COST;
			}
			case 4:
			{
				return CLAN_LEVEL_4_COST;
			}
			case 5:
			{
				return CLAN_LEVEL_5_COST;
			}
			case 6:
			{
				return CLAN_LEVEL_6_COST;
			}
			case 7:
			{
				return CLAN_LEVEL_7_COST;
			}
			case 8:
			{
				return CLAN_LEVEL_8_COST;
			}
			case 9:
			{
				return CLAN_LEVEL_9_COST;
			}
			case 10:
			{
				return CLAN_LEVEL_10_COST;
			}
			case 11:
			{
				return CLAN_LEVEL_11_COST;
			}
			default:
			{
				return 0;
			}
		}
	}

	public static int getClanLevelUpRequirement(int targetLevel)
	{
		switch (targetLevel)
		{
			case 1:
			{
				return CLAN_LEVEL_1_REQUIREMENT;
			}
			case 2:
			{
				return CLAN_LEVEL_2_REQUIREMENT;
			}
			case 3:
			{
				return CLAN_LEVEL_3_REQUIREMENT;
			}
			case 4:
			{
				return CLAN_LEVEL_4_REQUIREMENT;
			}
			case 5:
			{
				return CLAN_LEVEL_5_REQUIREMENT;
			}
			case 6:
			{
				return CLAN_LEVEL_6_REQUIREMENT;
			}
			case 7:
			{
				return CLAN_LEVEL_7_REQUIREMENT;
			}
			case 8:
			{
				return CLAN_LEVEL_8_REQUIREMENT;
			}
			case 9:
			{
				return CLAN_LEVEL_9_REQUIREMENT;
			}
			case 10:
			{
				return CLAN_LEVEL_10_REQUIREMENT;
			}
			case 11:
			{
				return CLAN_LEVEL_11_REQUIREMENT;
			}
			default:
			{
				return 0;
			}
		}
	}

	public static int getClanMemberLimit(int clanLevel)
	{
		if (clanLevel < 0)
		{
			return 0;
		}
		if (clanLevel >= CLAN_MEMBER_LIMITS.length)
		{
			return CLAN_MEMBER_LIMITS[CLAN_MEMBER_LIMITS.length - 1];
		}
		return CLAN_MEMBER_LIMITS[clanLevel];
	}

	public static int getClanWarReputationPoints(int killerLevel, int victimLevel)
	{
		return ((victimLevel - killerLevel) >= CLAN_WAR_LEVEL_DIFFERENCE_THRESHOLD) ? CLAN_WAR_REPUTATION_POINTS[0] : CLAN_WAR_REPUTATION_POINTS[1];
	}

	public static long getClanExpelledMemberPenaltyMillis()
	{
		return CLAN_EXPELLED_MEMBER_PENALTY_HOURS * 3600000L;
	}

	public static long getClanJoinPenaltyMillis()
	{
		return CLAN_JOIN_PENALTY_HOURS * 3600000L;
	}

	public static int getClanJoinPenaltyMinutes()
	{
		return (int) (getClanJoinPenaltyMillis() / 60000L);
	}

	public static long getClanCreatePenaltyMillis()
	{
		return CLAN_CREATE_PENALTY_DAYS * 86400000L;
	}

	public static long getClanChangeLeaderWaitMillis()
	{
		return CLAN_CHANGE_LEADER_WAIT_HOURS * 3600000L;
	}

	public static long getClanDisbandTimeMillis()
	{
		return CLAN_DISBAND_TIME_HOURS * 3600000L;
	}

	public static long getClanDisbandPenaltyMillis()
	{
		return CLAN_DISBAND_PENALTY_DAYS * 86400000L;
	}
}
