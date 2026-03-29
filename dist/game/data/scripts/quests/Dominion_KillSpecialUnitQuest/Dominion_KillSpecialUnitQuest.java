package quests.Dominion_KillSpecialUnitQuest;

import java.util.Collection;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerPvPKill;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.Siege;

/**
 * Base for Dominion Special Unit Kill Quests (adapted for L2J Mobius)
 */
public abstract class Dominion_KillSpecialUnitQuest extends Quest
{
        private static final int MAX_KILLS = 15;
        private static final int PARAM_CURRENT_KILLS = 0;
        private static final String[][] strParams = new String[Race.values().length][1];
        
        public Dominion_KillSpecialUnitQuest(int id)
        {
                super(id);
        }
        
        {
                for(Race r : getRaces())
                {
                        strParams[r.ordinal()][PARAM_CURRENT_KILLS] = "kills_".concat(r.name());
                }
        }

        protected abstract Collection<Race> getRaces();

        protected Collection<Race> getRaces(Player player)
        {
                return getRaces();
        }

        @RegisterEvent(EventType.ON_PLAYER_PVP_KILL)
        public void onPlayerPvPKill(OnPlayerPvPKill event)
        {
                Player player = event.getPlayer();
                Player killed = event.getTarget();
                if (player == null || killed == null) return;

                QuestState qs = player.getQuestState(getName());
                if (qs == null || qs.getState() == State.COMPLETED) return;

                Collection<Race> targetRaces = getRaces(player);
                if (targetRaces == null || targetRaces.isEmpty()) return;

                // Only strictly during a Territory War
                if (!Siege.isAnyDominionBattlefieldActive()) return;

                boolean canParticipate = false;
                if (player.getClan() != null) {
                        if (player.getClan().getCastleId() > 0) {
                                canParticipate = true;
                        } else if (player.getClan().getFortId() > 0) {
                                org.l2jmobius.gameserver.model.siege.Fort fort = org.l2jmobius.gameserver.managers.FortManager.getInstance().getFortById(player.getClan().getFortId());
                                if (fort != null && fort.getContractedCastleId() > 0) {
                                        canParticipate = true;
                                }
                        }
                }
                if (!canParticipate) return;

                Castle castle1 = CastleManager.getInstance().getCastle(player);
                Castle castle2 = CastleManager.getInstance().getCastle(killed);

                // Both players must be inside a combat zone of a Castle
                if (castle1 == null || castle2 == null) return;
                
                // That specific castle must be active in Dominion
                if (!Siege.isDominionBattlefieldActive(castle1)) return;
                if (!Siege.isDominionBattlefieldActive(castle2)) return;

                // Typically must be the same castle battlefield
                if (castle1 != castle2) return;

                if (killed.getEffectList().isAffectedBySkill(5660)) return;

                if (!targetRaces.contains(killed.getRace())) return;

                int kills = qs.getInt(strParams[killed.getRace().ordinal()][PARAM_CURRENT_KILLS]) + 1;
                if (kills < MAX_KILLS)
                {
                        qs.set(strParams[killed.getRace().ordinal()][PARAM_CURRENT_KILLS], kills);
                        playSound(player, "ItemSound.quest_itemget");
                }
                else
                {
                        qs.set(strParams[killed.getRace().ordinal()][PARAM_CURRENT_KILLS], MAX_KILLS);
                        int isCompleted = 1;
                        for(Race r : targetRaces)
                        {
                                if (qs.getInt(strParams[r.ordinal()][PARAM_CURRENT_KILLS]) < MAX_KILLS)
                                {
                                        isCompleted = 0;
                                        break;
                                }
                        }
                        if (isCompleted == 1)
                        {
                                qs.exitQuest(false, true);
                                playSound(player, "ItemSound.quest_finish");
                        }
                        else
                        {
                                playSound(player, "ItemSound.quest_middle");
                        }
                }
        }
}
