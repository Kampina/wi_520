package quests.Q11106_PriceOfSins;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.QuestType;
import org.l2jmobius.gameserver.util.ArrayUtil;

public class Q11106_PriceOfSins extends Quest
{
    private static final int QUEST_ID = 11106;
    private static final int[] NPCS = { 30174, 30176, 30835 };
    private static final int[] CRYSTALS = { 4639, 4650, 4661 };

    public Q11106_PriceOfSins()
    {
        super(QUEST_ID);
        addStartNpc(NPCS);
        addTalkId(NPCS);
    }

    private boolean hasCrystal(Player player)
    {
        for (int c : CRYSTALS)
            if (hasQuestItems(player, c))
                return true;
        return false;
    }

    private boolean takeCrystal(Player player)
    {
        for (int c : CRYSTALS)
        {
            if (hasQuestItems(player, c))
            {
                takeItems(player, c, 1);
                return true;
            }
        }
        return false;
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
                break;
            case "3_completed.htm":
                if (takeCrystal(player))
                {
                    qs.setCond(3);
                    qs.exitQuest(QuestType.ONE_TIME, true);
                    playSound(player, "ItemSound.quest_finish");
                    player.setPkKills(0);
                    // player.updateUserInfo(); // Should be triggered automatically
                }
                else
                {
                    return "Get away from me dirty swindler!";
                }
                break;
        }
        return "data/html/quests/_11106_PriceOfSins/" + event;
    }

    @Override
    public String onTalk(Npc npc, Player player)
    {
        QuestState qs = getQuestState(player, true);
        if (qs == null) return getNoQuestMsg(player);

        if (!ArrayUtil.contains(NPCS, npc.getId()))
            return getNoQuestMsg(player);

        if (player.getReputation() < 0) // Meaning karma > 0
            return "data/html/quests/_11106_PriceOfSins/0_reject_due_pk.htm";

        switch (qs.getCond())
        {
            case 0:
                return "data/html/quests/_11106_PriceOfSins/0_quest_offer.htm";
            case 1:
                if (hasCrystal(player))
                    return "data/html/quests/_11106_PriceOfSins/2_came_with_crystal.htm";
                else
                    return "data/html/quests/_11106_PriceOfSins/2_came_without_crystal.htm";
        }
        return getNoQuestMsg(player);
    }
}
