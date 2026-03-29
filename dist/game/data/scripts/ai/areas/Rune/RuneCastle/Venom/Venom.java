package ai.areas.Rune.RuneCastle.Venom;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.events.holders.sieges.OnCastleSiegeFinish;
import org.l2jmobius.gameserver.model.events.holders.sieges.OnCastleSiegeStart;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

public class Venom extends Script
{
	private static final int CASTLE = 8;

	private static final int VENOM = 29054;
	private static final int TELEPORT_CUBE = 29055;
	private static final int DUNGEON_KEEPER = 35506;
	private static final boolean HAS_TELEPORT_CUBE = NpcData.getInstance().getTemplate(TELEPORT_CUBE) != null;

	private static final byte ALIVE = 0;
	private static final byte DEAD = 1;

	private static final int HOURS_BEFORE = 24;

	private static final Location[] TARGET_TELEPORTS =
	{
		new Location(12860, -49158, 976),
		new Location(14878, -51339, 1024),
		new Location(15674, -49970, 864),
		new Location(15696, -48326, 864),
		new Location(14873, -46956, 1024),
		new Location(12157, -49135, -1088),
		new Location(12875, -46392, -288),
		new Location(14087, -46706, -288),
		new Location(14086, -51593, -288),
		new Location(12864, -51898, -288),
		new Location(15538, -49153, -1056),
		new Location(17001, -49149, -1064)
	};

	private static final Location THRONE = new Location(11025, -49152, -537);
	private static final Location DUNGEON = new Location(11882, -49216, -3008);
	private static final Location TELEPORT = new Location(12589, -49044, -3008);
	private static final Location CUBE = new Location(12047, -49211, -3009);

	private static final SkillHolder VENOM_STRIKE = new SkillHolder(4993, 1);
	private static final SkillHolder SONIC_STORM = new SkillHolder(4994, 1);
	private static final SkillHolder VENOM_TELEPORT = new SkillHolder(4995, 1);
	private static final SkillHolder RANGE_TELEPORT = new SkillHolder(4996, 1);

	private static final int[] TARGET_TELEPORTS_OFFSET =
	{
		650, 100, 100, 100, 100, 650, 200, 200, 200, 200, 200, 650
	};

	private static final List<Player> TARGETS = new ArrayList<>();

	private Npc _venom;
	private Npc _massymore;
	private Location _loc;
	private boolean _aggroMode = false;
	private boolean _prisonIsOpen = false;

	private Venom()
	{
		addStartNpc(DUNGEON_KEEPER);
		addFirstTalkId(DUNGEON_KEEPER);
		addTalkId(DUNGEON_KEEPER);
		if (HAS_TELEPORT_CUBE)
		{
			addStartNpc(TELEPORT_CUBE);
			addFirstTalkId(TELEPORT_CUBE);
			addTalkId(TELEPORT_CUBE);
		}
		addSpawnId(VENOM, DUNGEON_KEEPER);
		addSpellFinishedId(VENOM);
		addAttackId(VENOM);
		addKillId(VENOM);
		addAggroRangeEnterId(VENOM);
		setCastleSiegeStartId(this::onSiegeStart, CASTLE);
		setCastleSiegeFinishId(this::onSiegeFinish, CASTLE);

		final long currentTime = System.currentTimeMillis();
		final long startSiegeDate = CastleManager.getInstance().getCastleById(CASTLE).getSiegeDate().getTimeInMillis();
		final long openingDungeonDate = startSiegeDate - (HOURS_BEFORE * 360000);
		if ((currentTime > openingDungeonDate) && (currentTime < startSiegeDate))
		{
			_prisonIsOpen = true;
		}
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}

	@Override
	public String onTalk(Npc npc, Player talker)
	{
		switch (npc.getId())
		{
			case TELEPORT_CUBE:
			{
				talker.teleToLocation(TeleportWhereType.TOWN);
				break;
			}
			case DUNGEON_KEEPER:
			{
				if (_prisonIsOpen)
				{
					talker.teleToLocation(TELEPORT);
				}
				else
				{
					return "35506-02.html";
				}
				break;
			}
		}

		return super.onTalk(npc, talker);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "tower_check":
			{
				if (CastleManager.getInstance().getCastleById(CASTLE).getSiege().getControlTowerCount() <= 1)
				{
					changeLocation(MoveTo.THRONE);
					if (_massymore != null)
					{
						_massymore.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.OH_NO_THE_DEFENSES_HAVE_FAILED_IT_IS_TOO_DANGEROUS_TO_REMAIN_INSIDE_THE_CASTLE_FLEE_EVERY_MAN_FOR_HIMSELF);
					}
					cancelQuestTimer("tower_check", npc, null);
					startQuestTimer("raid_check", 10000, npc, null, true);
				}
				break;
			}
			case "raid_check":
			{
				if ((npc != null) && !npc.isInsideZone(ZoneId.SIEGE) && !npc.isTeleporting())
				{
					npc.teleToLocation(_loc);
				}
				break;
			}
			case "cube_despawn":
			{
				if (npc != null)
				{
					npc.deleteMe();
				}
				break;
			}
		}

		return event;
	}

	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if (isSummon)
		{
			return;
		}

		if (_aggroMode && (TARGETS.size() < 10) && (getRandom(3) < 1) && !player.isDead())
		{
			TARGETS.add(player);
		}
	}

	private void onSiegeStart(OnCastleSiegeStart event)
	{
		_aggroMode = true;
		_prisonIsOpen = false;
		if ((_venom != null) && !_venom.isDead())
		{
			_venom.setCurrentHp(_venom.getMaxHp());
			_venom.setCurrentMp(_venom.getMaxMp());
			_venom.enableSkill(VENOM_TELEPORT.getSkill());
			_venom.enableSkill(RANGE_TELEPORT.getSkill());
			startQuestTimer("tower_check", 30000, _venom, null, true);
		}
	}

	private void onSiegeFinish(OnCastleSiegeFinish event)
	{
		_aggroMode = false;
		_prisonIsOpen = true;
		if ((_venom != null) && !_venom.isDead())
		{
			changeLocation(MoveTo.PRISON);
			_venom.disableSkill(VENOM_TELEPORT.getSkill(), -1);
			_venom.disableSkill(RANGE_TELEPORT.getSkill(), -1);
		}

		updateStatus(ALIVE);
		cancelQuestTimer("tower_check", _venom, null);
		cancelQuestTimer("raid_check", _venom, null);
	}

	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		switch (skill.getId())
		{
			case 4222:
			{
				npc.teleToLocation(_loc);
				break;
			}
			case 4995:
			{
				teleportTarget(player);
				npc.asAttackable().stopHating(player);
				break;
			}
			case 4996:
			{
				teleportTarget(player);
				npc.asAttackable().stopHating(player);
				if (!TARGETS.isEmpty())
				{
					for (Player target : TARGETS)
					{
						final long x = player.getX() - target.getX();
						final long y = player.getY() - target.getY();
						final long z = player.getZ() - target.getZ();
						final long range = 250;
						if (((x * x) + (y * y) + (z * z)) <= (range * range))
						{
							teleportTarget(target);
							npc.asAttackable().stopHating(target);
						}
					}

					TARGETS.clear();
				}
				break;
			}
		}
	}

	@Override
	public void onSpawn(Npc npc)
	{
		switch (npc.getId())
		{
			case DUNGEON_KEEPER:
			{
				_massymore = npc;
				break;
			}
			case VENOM:
			{
				_venom = npc;
				_loc = _venom.getLocation();
				_venom.disableSkill(VENOM_TELEPORT.getSkill(), -1);
				_venom.disableSkill(RANGE_TELEPORT.getSkill(), -1);
				_venom.doRevive();
				npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.WHO_DARES_TO_COVET_THE_THRONE_OF_OUR_CASTLE_LEAVE_IMMEDIATELY_OR_YOU_WILL_PAY_THE_PRICE_OF_YOUR_AUDACITY_WITH_YOUR_VERY_OWN_BLOOD);
				_venom.asAttackable().setCanReturnToSpawnPoint(false);
				if (checkStatus() == DEAD)
				{
					_venom.deleteMe();
				}
				break;
			}
		}

		if (checkStatus() == DEAD)
		{
			npc.deleteMe();
		}
		else
		{
			npc.doRevive();
		}
	}

	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final double distance = npc.calculateDistance2D(attacker);
		if (_aggroMode && (getRandom(100) < 25))
		{
			npc.setTarget(attacker);
			npc.doCast(VENOM_TELEPORT.getSkill());
		}
		else if (_aggroMode && (npc.getCurrentHp() < (npc.getMaxHp() / 3)) && (getRandom(100) < 25) && !npc.isCastingNow(SkillCaster::isAnyNormalType))
		{
			npc.setTarget(attacker);
			npc.doCast(RANGE_TELEPORT.getSkill());
		}
		else if ((distance > 300) && (getRandom(100) < 10) && !npc.isCastingNow(SkillCaster::isAnyNormalType))
		{
			npc.setTarget(attacker);
			npc.doCast(VENOM_STRIKE.getSkill());
		}
		else if ((getRandom(100) < 10) && !npc.isCastingNow(SkillCaster::isAnyNormalType))
		{
			npc.setTarget(attacker);
			npc.doCast(SONIC_STORM.getSkill());
		}
	}

	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		updateStatus(DEAD);
		npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.IT_S_NOT_OVER_YET_IT_WON_T_BE_OVER_LIKE_THIS_NEVER);
		if (HAS_TELEPORT_CUBE && !CastleManager.getInstance().getCastleById(CASTLE).getSiege().isInProgress())
		{
			final Npc cube = addSpawn(TELEPORT_CUBE, CUBE, false, 0);
			startQuestTimer("cube_despawn", 120000, cube, null);
		}

		cancelQuestTimer("raid_check", npc, null);
	}

	private void changeLocation(MoveTo loc)
	{
		switch (loc)
		{
			case THRONE:
			{
				_venom.teleToLocation(THRONE, false);
				break;
			}
			case PRISON:
			{
				if ((_venom == null) || _venom.isDead() || _venom.isDecayed())
				{
					_venom = addSpawn(VENOM, DUNGEON, false, 0);
				}
				else
				{
					_venom.teleToLocation(DUNGEON, false);
				}

				cancelQuestTimer("raid_check", _venom, null);
				cancelQuestTimer("tower_check", _venom, null);
				break;
			}
		}

		_loc.setLocation(_venom.getLocation());
	}

	private void teleportTarget(Player player)
	{
		if ((player != null) && !player.isDead())
		{
			final int rnd = getRandom(11);
			player.teleToLocation(TARGET_TELEPORTS[rnd], TARGET_TELEPORTS_OFFSET[rnd]);
			player.getAI().setIntention(Intention.IDLE);
		}
	}

	private int checkStatus()
	{
		int checkStatus = ALIVE;
		if (GlobalVariablesManager.getInstance().hasVariable("VenomStatus"))
		{
			checkStatus = GlobalVariablesManager.getInstance().getInt("VenomStatus");
		}
		else
		{
			GlobalVariablesManager.getInstance().set("VenomStatus", 0);
		}

		return checkStatus;
	}

	private void updateStatus(int status)
	{
		GlobalVariablesManager.getInstance().set("VenomStatus", Integer.toString(status));
	}

	private enum MoveTo
	{
		THRONE,
		PRISON
	}

	public static void main(String[] args)
	{
		new Venom();
	}
}