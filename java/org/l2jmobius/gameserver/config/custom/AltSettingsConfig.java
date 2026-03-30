package org.l2jmobius.gameserver.config.custom;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.util.ConfigReader;

public class AltSettingsConfig
{
    private static final Logger LOGGER = Logger.getLogger(AltSettingsConfig.class.getName());

    public static boolean ALLOW_DISCARD_ITEM;
    public static boolean ALLOW_DEATH_DROP_ITEM;

    // Adapting from wi
    public static boolean CAN_GM_DROP_EQUIPMENT;
    public static List<Integer> LIST_OF_NON_DROPPABLE_ITEMS = new ArrayList<>();
    public static boolean DROP_AUGMENTED;
    public static boolean DROP_ON_DIE;
    public static double CHANCE_OF_NORMAL_DROP_BASE;
    public static double CHANCE_OF_PK_DROP_BASE;
    public static double CHANCE_OF_PKS_DROP_MOD;
    public static int CHANCE_OF_DROP_WEAPON;
    public static int CHANCE_OF_DROP_EQUIPPMENT;
    public static int CHANCE_OF_DROP_OTHER;
    public static int MAX_ITEMS_DROPPABLE;
    public static int MAX_DROP_THROW_DISTANCE;
    public static int MIN_PK_TO_DROP_ITEMS;
    public static boolean KARMA_NEEDED_TO_DROP;

    // PvP Vitality
    public static boolean ALT_VIT_SYSTEM_ENABLED;
    public static boolean ALT_VIT_SHOW_MESSAGES;
    public static int ALT_VIT_KILLS_TO_STAGE_UP;
    public static int ALT_VIT_MAX_ENRAGE_LVL;
    public static double ALT_VIT_DMG_PERCENT_PER_ENRAGE_LVL;
    public static long ALT_VIT_DECREASE_DELAY_MILLIS;
    public static long ALT_MAX_LVL_DECREASE_DELAY;

    // Seven Signs
    public static int ALT_SS_BLUE_STONE_VAL;
    public static int ALT_SS_GREEN_STONE_VAL;
    public static int ALT_SS_RED_STONE_VAL;
    public static int SS_ANNOUNCE_PERIOD;
    public static boolean ALT_SS_MAMMON_ALL_ACCESS;
    public static boolean ALT_SS_CATACOMBS_WINNER_ONLY;
    public static int ALT_SS_JOIN_COST;
    public static boolean ALT_SS_AA_TO_ADENA_ENABLED;
    public static double ALT_SS_AA_TO_ADENA_RATE;

    public static void load()
    {
        final ConfigReader config = new ConfigReader("./config/altsettings.ini");

        ALLOW_DISCARD_ITEM = config.getBoolean("AllowDiscardItem", true);
        ALLOW_DEATH_DROP_ITEM = config.getBoolean("AllowDeathDropItem", false);

        CAN_GM_DROP_EQUIPMENT = config.getBoolean("CanGMDropEquipment", true);

        LIST_OF_NON_DROPPABLE_ITEMS.clear();
        for (String id : config.getString("ListOfNonDroppableItems", "57,1147,425").split(",")) {
            try {
                if (!id.trim().isEmpty()) {
                    LIST_OF_NON_DROPPABLE_ITEMS.add(Integer.parseInt(id.trim()));
                }
            } catch (Exception e) {}
        }

        DROP_AUGMENTED = config.getBoolean("DropAugmented", false);
        DROP_ON_DIE = config.getBoolean("DropOnDie", true);
        CHANCE_OF_NORMAL_DROP_BASE = config.getDouble("ChanceOfNormalDropBase", 5.0);
        CHANCE_OF_PK_DROP_BASE = config.getDouble("ChanceOfPKDropBase", 20.0);
        CHANCE_OF_PKS_DROP_MOD = config.getDouble("ChanceOfPKsDropMod", 20.0);
        CHANCE_OF_DROP_WEAPON = config.getInt("ChanceOfDropWeapon", 5);
        CHANCE_OF_DROP_EQUIPPMENT = config.getInt("ChanceOfDropEquippment", 10);
        CHANCE_OF_DROP_OTHER = config.getInt("ChanceOfDropOther", 85);
        MAX_ITEMS_DROPPABLE = config.getInt("MaxItemsDroppable", 1);
        MAX_DROP_THROW_DISTANCE = config.getInt("MaxDropThrowDistance", 70);
        MIN_PK_TO_DROP_ITEMS = config.getInt("MinPKToDropItems", 3);
        KARMA_NEEDED_TO_DROP = config.getBoolean("KarmaNeededToDrop", true);

        // PvP Vitality
        ALT_VIT_SYSTEM_ENABLED = config.getBoolean("AltVitSystemEnabled", true);
        ALT_VIT_SHOW_MESSAGES = config.getBoolean("AltVitShowMessages", true);
        ALT_VIT_KILLS_TO_STAGE_UP = config.getInt("AltVitKillsToStageUp", 10);
        ALT_VIT_MAX_ENRAGE_LVL = config.getInt("AltVitMaxEnrageLvl", 5);
        ALT_VIT_DMG_PERCENT_PER_ENRAGE_LVL = config.getDouble("AltVitDmgPercentPerEnrageLvl", 2.0);
        ALT_VIT_DECREASE_DELAY_MILLIS = config.getLong("AltVitDecreaseDelayMillis", 60000);
        ALT_MAX_LVL_DECREASE_DELAY = config.getLong("AltMaxLvlDecreaseDelay", 300000);

        ALT_SS_BLUE_STONE_VAL = config.getInt("AltSsBlueStoneVal", 1);
        ALT_SS_GREEN_STONE_VAL = config.getInt("AltGreenStoneVal", 1);
        ALT_SS_RED_STONE_VAL = config.getInt("AltSsRedStoneVal", 1);
        SS_ANNOUNCE_PERIOD = config.getInt("SSAnnouncePeriod", 0);
        ALT_SS_MAMMON_ALL_ACCESS = config.getBoolean("AltSsMammonAllAccess", false);
        ALT_SS_CATACOMBS_WINNER_ONLY = config.getBoolean("AltSsCatacombsWinnerOnly", false);
        ALT_SS_JOIN_COST = config.getInt("AltSsJoinCost", 0);
        ALT_SS_AA_TO_ADENA_ENABLED = config.getBoolean("AltSsAaToAdenaEnabled", false);
        ALT_SS_AA_TO_ADENA_RATE = config.getDouble("AltSsAaToAdenaRate", 1.0);
    }
}