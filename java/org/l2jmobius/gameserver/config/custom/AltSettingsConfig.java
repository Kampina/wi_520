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

    }
}
