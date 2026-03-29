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
package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * Seven Signs custom configuration.
 */
public class SevenSignsConfig
{
	private static final String SEVEN_SIGNS_CONFIG_FILE = "./config/Custom/SevenSigns.ini";
	
	public static int ALT_SS_BLUE_STONE_VAL;
	public static int ALT_SS_GREEN_STONE_VAL;
	public static int ALT_SS_RED_STONE_VAL;
	public static int SS_ANNOUNCE_PERIOD;
	public static boolean ALT_SS_ALLOW_DEBUFF_PLAYERS;
	public static boolean ALT_SS_MAMMON_ALL_ACCESS;
	public static boolean ALT_SS_CATACOMBS_WINNER_ONLY;

        public static long ALT_SS_JOIN_COST;
        public static boolean ALT_SS_AA_TO_ADENA_ENABLED;
        public static float ALT_SS_AA_TO_ADENA_RATE;
        public static long ALT_SS_WEAPON_EXCHANGE_COST_A;
        public static long ALT_SS_WEAPON_EXCHANGE_COST_B;
        public static long ALT_SS_WEAPON_EXCHANGE_COST_C;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(SEVEN_SIGNS_CONFIG_FILE);
		ALT_SS_BLUE_STONE_VAL = config.getInt("AltSSBlueStoneVal", 1);
		ALT_SS_GREEN_STONE_VAL = config.getInt("AltSSGreenStoneVal", 1);
		ALT_SS_RED_STONE_VAL = config.getInt("AltSSRedStoneVal", 1);
		SS_ANNOUNCE_PERIOD = config.getInt("SSAnnouncePeriod", 0);
		ALT_SS_ALLOW_DEBUFF_PLAYERS = config.getBoolean("AltSSAllowDebuffPlayers", false);
		ALT_SS_MAMMON_ALL_ACCESS = config.getBoolean("AltSSMammonAllAccess", false);
		ALT_SS_CATACOMBS_WINNER_ONLY = config.getBoolean("AltSSCatacombsWinnerOnly", true);

                ALT_SS_JOIN_COST = config.getLong("AltSSJoinCost", 0);
                ALT_SS_AA_TO_ADENA_ENABLED = config.getBoolean("AltSSAAToAdenaEnabled", false);
                ALT_SS_AA_TO_ADENA_RATE = config.getFloat("AltSSAAToAdenaRate", 1.0f);
                ALT_SS_WEAPON_EXCHANGE_COST_A = config.getLong("AltSSWeaponExchangeCostA", 0);
                ALT_SS_WEAPON_EXCHANGE_COST_B = config.getLong("AltSSWeaponExchangeCostB", 0);
                ALT_SS_WEAPON_EXCHANGE_COST_C = config.getLong("AltSSWeaponExchangeCostC", 0);
	}
}
