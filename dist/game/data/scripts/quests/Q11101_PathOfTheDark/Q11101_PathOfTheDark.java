package quests.Q11101_PathOfTheDark;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Fraction;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.model.script.QuestType;

public class Q11101_PathOfTheDark extends Quest
{
    private static final int QUEST_ID = 11101;
    
    private static final int HIERARCH_KEKROPUS = 32138;
    private static final int PILGRIM_OF_DARKNESS = 31641;
    private static final int DIVINE_STONE_OF_WISDOM = 7081;
    private static final int REWARD_ITEM = 2734;
    private static final long REQUIRED_EXP = 100000;

    public Q11101_PathOfTheDark()
    {
        super(QUEST_ID);
        addStartNpc(HIERARCH_KEKROPUS);
        addTalkId(HIERARCH_KEKROPUS, PILGRIM_OF_DARKNESS);
        registerQuestItems(DIVINE_STONE_OF_WISDOM);
    }

    private boolean enoughExp(Player player)
    {
        long expForLevel = ExperienceData.getInstance().getExpForLevel(player.getLevel());
        return (player.getExp() - expForLevel) >= REQUIRED_EXP;
    }

    @Override
    public String onEvent(String event, Npc npc, Player player)
    {
        QuestState qs = getQuestState(player, true);
        if (qs == null) return event;

        switch (event)
        {
            case "1_quest_taken.htm":
                qs.setCond(1);
                qs.startQuest();
                playSound(player, "ItemSound.quest_accept");
                player.removeExpAndSp(REQUIRED_EXP, 0);
                break;
            case "3_player_instructed_and_accepted.htm":
                qs.setCond(3);
                playSound(player, "ItemSound.quest_accept");
                break;
            case "5_obtain_reward.htm":
                playSound(player, "ItemSound.quest_finish");
                giveItems(player, REWARD_ITEM, 1);
                qs.exitQuest(QuestType.ONE_TIME, true);
                break;
        }
        return "data/html/quests/_11101_PathOfTheDark/" + event;
    }

    @Override
    public String onTalk(Npc npc, Player player)
    {
        QuestState qs = getQuestState(player, true);
        if (qs == null) return getNoQuestMsg(player);

        if (player.getLevel() < 74)
            return getNoQuestMsg(player);

        switch (npc.getId())
        {
            case HIERARCH_KEKROPUS:
                if (qs.isCreated() || qs.getCond() == 0)
                {
                    if (player.getFraction() != Fraction.DARK)
                        return "We doing deals only with Dark fraction members. Get away from me.";
                    if (!enoughExp(player))
                        return "You don't have enough experience! Come back to me with " + REQUIRED_EXP + " or more exp!";
                    return "data/html/quests/_11101_PathOfTheDark/0_quest_offer.htm";
                }
                break;
            case PILGRIM_OF_DARKNESS:
                if (qs.getCond() == 1)
                    return "data/html/quests/_11101_PathOfTheDark/2_first_piligrim_speak.htm";
                if (qs.getCond() == 3)
                {
                    // Check ketra >= 3 (items 7213, 7214, 7215)
                    if (!hasQuestItems(player, 7213) && !hasQuestItems(player, 7214) && !hasQuestItems(player, 7215))
                        return "data/html/quests/_11101_PathOfTheDark/4_not_enough_ketra_lvl.htm";
                    if (hasQuestItems(player, DIVINE_STONE_OF_WISDOM))
                        return "data/html/quests/_11101_PathOfTheDark/4_got_stone.htm";
                    return "data/html/quests/_11101_PathOfTheDark/4_didnt_get_stone.htm";
                }
                break;
        }
        return getNoQuestMsg(player);
    }
}
