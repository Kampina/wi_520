package quests.Q11104_NoblesseOfLight;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Fraction;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.network.serverpackets.SkillList;
import org.l2jmobius.gameserver.model.script.QuestType;

public class Q11104_NoblesseOfLight extends Quest
{
    private static final int QUEST_ID = 11104;
    
    private static final int MAGISTER = 30176;
    private static final int ALLIANCE_LEADER = 31377;
    private static final int PILGRIM = 31641;
    private static final int RB_BRAKKI = 25305;
    private static final int RB_BARAKIEL = 25325;
    private static final int NOBLESSE_TIARA = 7694;
    private static final int ICE_HEART = 7244;
    private static final long REQUIRED_EXP = 250000;

    public Q11104_NoblesseOfLight()
    {
        super(QUEST_ID);
        addStartNpc(MAGISTER);
        addTalkId(MAGISTER, PILGRIM, ALLIANCE_LEADER);
        addKillId(RB_BRAKKI, RB_BARAKIEL);
        registerQuestItems(ICE_HEART);
    }
    
    private void makeNoble(Player player)
    {
        if (player == null || player.isNoble())
            return;
        player.setNoble(true);
        // Olympiad logic in mobius does auto noble sync when setNoble(true) is called.
        // Skills are also automatically added if class allows.
        player.broadcastUserInfo();
    }

    @Override
    public String onEvent(String event, Npc npc, Player player)
    {
        QuestState qs = getQuestState(player, true);
        if (qs == null) return event;

        switch (event)
        {
            case "1_quest_taken.htm":
                qs.unset("rb_25305");
                qs.unset("rb_25325");
                qs.setCond(1);
                qs.startQuest();
                playSound(player, "ItemSound.quest_accept");
                player.removeExpAndSp(REQUIRED_EXP, 0);
                break;
            case "6_spirit_mission_accepted.htm":
                qs.setCond(6);
                playSound(player, "ItemSound.quest_accept");
                break;
            case "9_last_boss_mission_accepted.htm":
                qs.setCond(9);
                playSound(player, "ItemSound.quest_accept");
                break;
            case "11_finish_dialog.htm":
                qs.setCond(12); // Actually not needed since we complete it
                qs.exitQuest(QuestType.ONE_TIME, true);
                playSound(player, "ItemSound.quest_finish");
                makeNoble(player);
                giveItems(player, NOBLESSE_TIARA, 1);
                break;
        }
        return "data/html/quests/_11104_NoblesseOfLight/" + event;
    }

    @Override
    public String onTalk(Npc npc, Player player)
    {
        QuestState qs = getQuestState(player, true);
        if (qs == null) return getNoQuestMsg(player);

        if (player.getLevel() <= 74) return getNoQuestMsg(player);

        switch (npc.getId())
        {
            case MAGISTER:
                switch (qs.getCond())
                {
                    case 0:
                        if (player.getFraction() != Fraction.LIGHT)
                            return "We doing deals only with Light fraction members. Get away from me.";
                        long expForLevel = ExperienceData.getInstance().getExpForLevel(player.getLevel());
                        if ((player.getExp() - expForLevel) < REQUIRED_EXP)
                            return "You don't have enough experience! Come back to me with " + REQUIRED_EXP + " or more exp!";
                        if (!hasQuestItems(player, 7225)) // Varka lv 5
                            return "Your level of loyalty to Varka is not enough. Come back with level 5.";
                        if (player.getPlayerClass().level() < 3)
                            return "I am doing deals only with professionals and I will not speak with you until you take 3rd profession.";
                        // Mobius completion check: qs.isCompleted doesn't check other quests, so we check Player state
                        QuestState qs1 = player.getQuestState("Q11102_SubClassLight");
                        QuestState qs2 = player.getQuestState("Q11103_SubClassDark");
                        boolean completed1 = (qs1 != null && qs1.isCompleted());
                        boolean completed2 = (qs2 != null && qs2.isCompleted());
                        if (!completed1 && !completed2)
                            return "To take this quest you need to complete the subclass quest first.";
                            
                        return "data/html/quests/_11104_NoblesseOfLight/0_quest_offer.htm";
                    case 1:
                        return "data/html/quests/_11104_NoblesseOfLight/1_quest_taken.htm";
                    case 7:
                        return "data/html/quests/_11104_NoblesseOfLight/8_came_to_magister.htm";
                    case 9:
                        if ("killed".equals(qs.get("rb_25325")))
                            return "data/html/quests/_11104_NoblesseOfLight/10_last_mission_done.htm";
                        else
                            return "data/html/quests/_11104_NoblesseOfLight/9_last_boss_mission_accepted.htm";
                }
                break;
            case PILGRIM:
                switch (qs.getCond())
                {
                    case 1:
                        qs.setCond(2);
                        playSound(player, "ItemSound.quest_accept");
                        return "data/html/quests/_11104_NoblesseOfLight/2_came_to_pilligrim.htm";
                    case 2:
                        return "data/html/quests/_11104_NoblesseOfLight/2_came_to_pilligrim.htm";
                    case 4:
                        return "data/html/quests/_11104_NoblesseOfLight/5_came_to_pilligrim.htm";
                    case 6:
                        if (hasQuestItems(player, ICE_HEART))
                        {
                            qs.setCond(7);
                            playSound(player, "ItemSound.quest_accept");
                            return "data/html/quests/_11104_NoblesseOfLight/7_came_with_stone.htm";
                        }
                        else
                            return "data/html/quests/_11104_NoblesseOfLight/6_spirit_mission_accepted.htm";
                }
                break;
            case ALLIANCE_LEADER:
                switch (qs.getCond())
                {
                    case 2:
                        qs.setCond(3);
                        playSound(player, "ItemSound.quest_accept");
                        return "data/html/quests/_11104_NoblesseOfLight/3_alliance_first_talk.htm";
                    case 3:
                        if ("killed".equals(qs.get("rb_25305")))
                        {
                            qs.setCond(4);
                            playSound(player, "ItemSound.quest_accept");
                            return "data/html/quests/_11104_NoblesseOfLight/4_alliance_complete.htm";
                        }
                        else
                            return "data/html/quests/_11104_NoblesseOfLight/3_alliance_first_talk.htm";
                }
                break;
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
            case RB_BRAKKI:
                if (qs.getCond() == 3)
                {
                    qs.set("rb_25305", "killed");
                    playSound(player, "ItemSound.quest_accept");
                }
                break;
            case RB_BARAKIEL:
                if (qs.getCond() == 9)
                {
                    qs.set("rb_25325", "killed");
                    playSound(player, "ItemSound.quest_accept");
                }
                break;
        }
        super.onKill(npc, player, isSummon);
    }
}
