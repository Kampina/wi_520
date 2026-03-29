package quests.Q11103_SubClassDark;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Fraction;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.model.script.QuestType;

public class Q11103_SubClassDark extends Quest
{
    private static final int QUEST_ID = 11103;
    private static final int MAGISTER = 30832;
    
    private static final int REIRIAS_SOUL_ORB = 4666;
    private static final int HALLATES_SCEPTER = 4669;
    private static final int KERNONS_SCEPTER = 4667;
    private static final int GOLKONDA_SCEPTER = 4668;
    
    private static final int RB_CABRIO = 25035;
    private static final int RB_HALLATE = 25220;
    private static final int RB_KERNON = 25263;
    private static final int RB_GOLKONDA = 25126;

    public Q11103_SubClassDark()
    {
        super(QUEST_ID);
        addStartNpc(MAGISTER);
        addTalkId(MAGISTER);
        addKillId(RB_CABRIO, RB_HALLATE, RB_KERNON, RB_GOLKONDA);
        registerQuestItems(REIRIAS_SOUL_ORB, HALLATES_SCEPTER, KERNONS_SCEPTER, GOLKONDA_SCEPTER);
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
                player.removeExpAndSp(1, 0); // requiredExp = 1
                break;
            case "3_go_to_pilligrim.htm":
                takeItems(player, REIRIAS_SOUL_ORB, 1);
                qs.setCond(3);
                playSound(player, "ItemSound.quest_accept");
                return "data/html/quests/_11103_SubClassDark/4_came_to_pilligrim.htm";
            case "5_player_instructed.htm":
                qs.setCond(5);
                playSound(player, "ItemSound.quest_accept");
                break;
            case "6_complete.htm":
                takeItems(player, -1, HALLATES_SCEPTER, KERNONS_SCEPTER, GOLKONDA_SCEPTER);
                qs.exitQuest(QuestType.ONE_TIME, true);
                playSound(player, "ItemSound.quest_finish");
                break;
        }
        return "data/html/quests/_11103_SubClassDark/" + event;
    }

    @Override
    public String onTalk(Npc npc, Player player)
    {
        QuestState qs = getQuestState(player, true);
        if (qs == null) return getNoQuestMsg(player);

        if (player.getLevel() <= 74) return getNoQuestMsg(player);

        if (npc.getId() == MAGISTER)
        {
            switch (qs.getCond())
            {
                case 0:
                    if (player.getFraction() != Fraction.DARK)
                        return "We doing deals only with Dark fraction members. Get away from me.";
                    long expForLevel = ExperienceData.getInstance().getExpForLevel(player.getLevel());
                    if (player.getExp() - expForLevel < 1)
                        return "You don't have enough experience!";
                    if (player.getPlayerClass().level() < 3)
                        return "I am doing deals only with professionals and I will not speak with you until you take 3rd profession.";
                    return "data/html/quests/_11103_SubClassDark/0_quest_offer.htm";
                case 1:
                    return "Don't waste your time! Go!";
                case 2:
                    return "data/html/quests/_11103_SubClassDark/2_came_with_orb.htm";
                case 3:
                    return "data/html/quests/_11103_SubClassDark/4_came_to_pilligrim.htm";
                case 5:
                    if (hasQuestItems(player, HALLATES_SCEPTER) && hasQuestItems(player, KERNONS_SCEPTER) && hasQuestItems(player, GOLKONDA_SCEPTER))
                        return "data/html/quests/_11103_SubClassDark/5_collected.htm";
                    return "data/html/quests/_11103_SubClassDark/5_didnt_collected.htm";
            }
        }
        return getNoQuestMsg(player);
    }

    @Override
    public void onKill(Npc npc, Player player, boolean isSummon)
    {
        QuestState qs = getQuestState(player, false);
        if (qs == null)
        {
            super.onKill(npc, player, isSummon);
            return;
        }

        switch (npc.getId())
        {
            case RB_CABRIO:
                if (qs.getCond() == 1)
                {
                    qs.setCond(2);
                    giveItems(player, REIRIAS_SOUL_ORB, 1);
                    playSound(player, "ItemSound.quest_itemget");
                }
                break;
            case RB_HALLATE:
                if (qs.getCond() == 5 && !hasQuestItems(player, HALLATES_SCEPTER))
                {
                    giveItems(player, HALLATES_SCEPTER, 1);
                    playSound(player, "ItemSound.quest_itemget");
                }
                break;
            case RB_KERNON:
                if (qs.getCond() == 5 && !hasQuestItems(player, KERNONS_SCEPTER))
                {
                    giveItems(player, KERNONS_SCEPTER, 1);
                    playSound(player, "ItemSound.quest_itemget");
                }
                break;
            case RB_GOLKONDA:
                if (qs.getCond() == 5 && !hasQuestItems(player, GOLKONDA_SCEPTER))
                {
                    giveItems(player, GOLKONDA_SCEPTER, 1);
                    playSound(player, "ItemSound.quest_itemget");
                }
                break;
        }
            super.onKill(npc, player, isSummon);
    }
}
