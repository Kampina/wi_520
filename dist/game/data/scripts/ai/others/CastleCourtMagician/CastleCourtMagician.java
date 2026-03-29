package ai.others.CastleCourtMagician;

import handlers.effecthandlers.CallPc;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanAccess;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class CastleCourtMagician extends Script
{
	private static final int[] NPCS =
	{
		35648,
		35649,
		35650,
		35651,
		35652,
		35653,
		35654,
		35655,
		35656,
	};

	private static final int CLAN_GATE_SKILL_ID = 3632;
	private static final int COND_ALL_FALSE = 0;
	private static final int COND_BUSY = 1;
	private static final int COND_OWNER = 2;

	private CastleCourtMagician()
	{
		addStartNpc(NPCS);
		addTalkId(NPCS);
		addFirstTalkId(NPCS);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "chat":
			case "chat 0":
			{
				return getHtml(player, npc, 0);
			}
			case "chat 2":
			{
				return getHtml(player, npc, 2);
			}
			case "gotoleader":
			{
				return teleportToClanLeader(npc, player);
			}
		}
		return null;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return getHtml(player, npc, 0);
	}

	private String getHtml(Player player, Npc npc, int value)
	{
		switch (validateCondition(player, npc))
		{
			case COND_BUSY:
			{
				return "CourtMagician-busy.html";
			}
			case COND_OWNER:
			{
				return value > 0 ? ("CourtMagician-" + value + ".html") : "CourtMagician.html";
			}
			default:
			{
				return "CourtMagician-no.html";
			}
		}
	}

	private String teleportToClanLeader(Npc npc, Player player)
	{
		final int condition = validateCondition(player, npc);
		if (condition == COND_BUSY)
		{
			return "CourtMagician-busy.html";
		}
		if (condition != COND_OWNER)
		{
			return "CourtMagician-no.html";
		}
		if (!player.hasAccess(ClanAccess.CASTLE_MANAGE_FUNCTIONS))
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return null;
		}

		final Clan clan = player.getClan();
		final Player clanLeader = ((clan != null) && clan.getLeader().isOnline()) ? clan.getLeader().getPlayer() : null;
		if ((clanLeader == null) || (clanLeader == player) || (clanLeader.getEffectList().getBuffInfoBySkillId(CLAN_GATE_SKILL_ID) == null))
		{
			return "CourtMagician-nogate.html";
		}
		if (!canLeaderReceiveTeleport(clanLeader) || !CallPc.checkSummonTargetStatus(player, clanLeader))
		{
			return "CourtMagician-nogate.html";
		}

		player.teleToLocation(clanLeader, 100, clanLeader.getInstanceWorld());
		return null;
	}

	private boolean canLeaderReceiveTeleport(Player clanLeader)
	{
		return !clanLeader.isInOlympiadMode() && !clanLeader.inObserverMode() && !clanLeader.isOnEvent() && !clanLeader.isFlyingMounted() && !clanLeader.isInsideZone(ZoneId.NO_SUMMON_FRIEND) && !clanLeader.isInsideZone(ZoneId.JAIL);
	}

	private int validateCondition(Player player, Npc npc)
	{
		if (player.isGM())
		{
			return COND_OWNER;
		}

		final Castle castle = npc.getCastle();
		if ((castle != null) && (player.getClan() != null))
		{
			if (castle.getSiege().isInProgress())
			{
				return COND_BUSY;
			}
			if (castle.getOwnerId() == player.getClanId())
			{
				return COND_OWNER;
			}
		}
		return COND_ALL_FALSE;
	}

	public static void main(String[] args)
	{
		new CastleCourtMagician();
	}
}