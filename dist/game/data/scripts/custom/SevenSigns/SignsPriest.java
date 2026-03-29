/*
 * Seven Signs Priest NPC Handler — ported from WorldInfluence to RoseVain.
 * Handles Dawn/Dusk Priests, Black Marketeer, Merchant of Mammon, Blacksmith of Mammon.
 */
package custom.SevenSigns;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Fraction;
import org.l2jmobius.gameserver.config.custom.SevenSignsConfig;
import org.l2jmobius.gameserver.model.entity.SevenSigns;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class SignsPriest extends Script
{
	// @formatter:off
	// Dawn Priests
	private static final int[] DAWN_PRIESTS = {
		31078, 31079, 31080, 31081, 31082, 31083, 31084, 31168, 31692, 31694, 31997
	};
	// Dusk Priests
	private static final int[] DUSK_PRIESTS = {
		31085, 31086, 31087, 31088, 31089, 31090, 31091, 31169, 31693, 31695, 31998
	};
	// Mammon NPCs
	private static final int MAMMON_MARKETEER = 31092;
	private static final int MAMMON_MERCHANT = 31113;
	private static final int MAMMON_BLACKSMITH = 31126;
	// @formatter:on
	
	// Contribution links for dynamic HTML
	private static final String RED_GNOSIS_LIGHT_CONTRIB = "<Button ALIGN=\"LEFT\" ICON=\"NORMAL\" action=\"bypass -h npc_%objectId%_Quest SignsPriest stone 3\">Сдать красные камни печати.</Button>";
	private static final String BLUE_STRIFE_DARK_CONTRIB = "<Button ALIGN=\"LEFT\" ICON=\"NORMAL\" action=\"bypass -h npc_%objectId%_Quest SignsPriest stone 1\">Сдать синие камни печати.</Button>";
	private static final String GREEN_AVARICE_PAIR_CONTRIB = "<Button ALIGN=\"LEFT\" ICON=\"NORMAL\" action=\"bypass -h npc_%objectId%_Quest SignsPriest stone 2\">Сдать зеленые камни печати.</Button>";
	
	private static final String DAWN_COLOR_EXCHANGE = "<Button ALIGN=\"LEFT\" ICON=\"NORMAL\" action=\"bypass -h npc_%objectId%_Quest SignsPriest exchangeColors\">Обменять синие камни на зеленые.</Button>";
	private static final String DUSK_COLOR_EXCHANGE = "<Button ALIGN=\"LEFT\" ICON=\"NORMAL\" action=\"bypass -h npc_%objectId%_Quest SignsPriest exchangeColors\">Обменять красные камни на зеленые.</Button>";
	
	// Exchange links
	private static final String RED_EXCHANGE = "<Button ALIGN=\"LEFT\" ICON=\"NORMAL\" action=\"bypass -h npc_%objectId%_Quest SignsPriest stoneType 3\">Обменять красные камни печати.</Button>";
	private static final String BLUE_EXCHANGE = "<Button ALIGN=\"LEFT\" ICON=\"NORMAL\" action=\"bypass -h npc_%objectId%_Quest SignsPriest stoneType 1\">Обменять синие камни печати.</Button>";
	private static final String GREEN_EXCHANGE = "<Button ALIGN=\"LEFT\" ICON=\"NORMAL\" action=\"bypass -h npc_%objectId%_Quest SignsPriest stoneType 2\">Обменять зеленые камни печати.</Button>";
	
	private SignsPriest()
	{
		for (int id : DAWN_PRIESTS)
		{
			registerNpc(id);
		}
		for (int id : DUSK_PRIESTS)
		{
			registerNpc(id);
		}
		registerNpc(MAMMON_MARKETEER);
		registerNpc(MAMMON_MERCHANT);
		registerNpc(MAMMON_BLACKSMITH);
	}

	private void registerNpc(int npcId)
	{
		if (NpcData.getInstance().getTemplate(npcId) == null)
		{
			return;
		}

		addStartNpc(npcId);
		addTalkId(npcId);
		addFirstTalkId(npcId);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return getMainPage(npc, player);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (npc == null || player == null)
		{
			return null;
		}
		
		final int npcId = npc.getId();
		
		// Mammon NPCs: require Seven Signs registration (except GM)
		if ((npcId == MAMMON_MERCHANT) || (npcId == MAMMON_BLACKSMITH))
		{
			if ((SevenSigns.getInstance().getPlayerCabal(player) == SevenSigns.CABAL_NULL) && !player.isGM() && !SevenSignsConfig.ALT_SS_MAMMON_ALL_ACCESS)
                        {
                                return null;
                        }
		}
		
		final StringTokenizer st = new StringTokenizer(event, " ");
		final String cmd = st.nextToken();
		
		switch (cmd)
		{
			case "chat_0":
				return getMainPage(npc, player);
			
			// === Description pages ===
			case "desc_1":
			case "desc_2":
			case "desc_3":
			case "desc_4":
			case "desc_5":
			case "desc_6":
			case "desc_7":
			case "desc_8":
			case "desc_9":
				return cmd + ".htm";
			
			// === Result/stats page ===
			case "result":
				return showResultPage(npc, player);
			
			// === Join cabal ===
			case "join":
				return handleJoin(st, npc, player);
			
			// === Contribute stones ===
			case "contribute":
				return showContributePage(npc, player);
			
			case "stone":
				return handleStoneContrib(st, npc, player);
			
			// === Exchange stones for AA (validation period) ===
			case "exchange":
				return showExchangePage(npc, player);
			
			case "stoneType":
				return handleStoneTypeChoice(st, npc, player);
			
			case "stoneExchange":
				return handleStoneExchange(st, npc, player);

                        case "exchangeColors":
                                return handleColorExchange(npc, player);
			
			// === Reward ===
			case "reward":
				return handleReward(npc, player);
			
			// === Black Marketeer: Ancient Adena → Adena ===
			case "blkmrkt_exchange":
				return "blkmrkt_2.htm";
			
			case "7": // bypass: 7 <amount> — Exchange AA for Adena
				return handleAncientAdenaExchange(st, npc, player);
			
			// === Mammon subpages ===
			case "mammblack_1":
			case "mammblack_1a":
			case "mammblack_1b":
			case "mammmerch_1a":
			case "mammmerch_1b":
				return cmd + ".htm";
		}
		
		return null;
	}
	
	// ========================== Main page logic ==========================
	
	private String getMainPage(Npc npc, Player player)
	{
		final int npcId = npc.getId();
		final SevenSigns ss = SevenSigns.getInstance();
		final int playerCabal = ss.getPlayerCabal(player);
		final boolean isSealValidation = ss.isSealValidationPeriod();
		final int compWinner = ss.getCabalHighestScore();
		final int sealGnosisOwner = ss.getSealOwner(SevenSigns.SEAL_GNOSIS);
		final int sealAvariceOwner = ss.getSealOwner(SevenSigns.SEAL_AVARICE);
		
		// Dawn Priests
		if (isDawnPriest(npcId))
		{
			switch (playerCabal)
			{
				case SevenSigns.CABAL_DAWN:
					if (isSealValidation)
					{
						if (compWinner == SevenSigns.CABAL_DAWN)
						{
							return (compWinner != sealGnosisOwner) ? "dawn_priest_2c.htm" : "dawn_priest_2a.htm";
						}
						return "dawn_priest_2b.htm";
					}
					return "dawn_priest_1b.htm";
				case SevenSigns.CABAL_DUSK:
					return isSealValidation ? "dawn_priest_3b.htm" : "dawn_priest_3a.htm";
				default:
					if (isSealValidation)
					{
						return (compWinner == SevenSigns.CABAL_DAWN) ? "dawn_priest_4.htm" : "dawn_priest_2b.htm";
					}
					return "dawn_priest_1a.htm";
			}
		}
		
		// Dusk Priests
		if (isDuskPriest(npcId))
		{
			switch (playerCabal)
			{
				case SevenSigns.CABAL_DUSK:
					if (isSealValidation)
					{
						if (compWinner == SevenSigns.CABAL_DUSK)
						{
							return (compWinner != sealGnosisOwner) ? "dusk_priest_2c.htm" : "dusk_priest_2a.htm";
						}
						return "dusk_priest_2b.htm";
					}
					return "dusk_priest_1b.htm";
				case SevenSigns.CABAL_DAWN:
					return isSealValidation ? "dusk_priest_3b.htm" : "dusk_priest_3a.htm";
				default:
					if (isSealValidation)
					{
						return (compWinner == SevenSigns.CABAL_DUSK) ? "dusk_priest_4.htm" : "dusk_priest_2b.htm";
					}
					return "dusk_priest_1a.htm";
			}
		}
		
		// Black Marketeer (31092)
		if (npcId == MAMMON_MARKETEER)
		{
			return "blkmrkt_1.htm";
		}
		
		// Merchant of Mammon (31113)
		if (npcId == MAMMON_MERCHANT)
		{
			if (!player.isGM() && !SevenSignsConfig.ALT_SS_MAMMON_ALL_ACCESS)
                        {
                                if (compWinner == SevenSigns.CABAL_NULL)
                                {
                                        return "mammmerch_2.htm";
                                }
                                if ((compWinner == SevenSigns.CABAL_DAWN) && ((playerCabal != compWinner) || (playerCabal != sealAvariceOwner)))
                                {
                                        return "mammmerch_2.htm";
                                }
                                if ((compWinner == SevenSigns.CABAL_DUSK) && ((playerCabal != compWinner) || (playerCabal != sealAvariceOwner)))
                                {
                                        return "mammmerch_2.htm";
                                }
                        }
			return "mammmerch_1.htm";
		}
		
		// Blacksmith of Mammon (31126)
		if (npcId == MAMMON_BLACKSMITH)
		{
			if (!player.isGM() && !SevenSignsConfig.ALT_SS_MAMMON_ALL_ACCESS)
                        {
                                if (compWinner == SevenSigns.CABAL_NULL)
                                {
                                        return "mammblack_2.htm";
                                }
                                if ((compWinner == SevenSigns.CABAL_DAWN) && ((playerCabal != compWinner) || (playerCabal != sealGnosisOwner)))
                                {
                                        return "mammblack_2.htm";
                                }
                                if ((compWinner == SevenSigns.CABAL_DUSK) && ((playerCabal != compWinner) || (playerCabal != sealGnosisOwner)))
                                {
                                        return "mammblack_2.htm";
                                }
                        }
			return "mammblack_1.htm";
		}
		
		return null;
	}
	
	// ========================== Result page ==========================
	
	private String showResultPage(Npc npc, Player player)
	{
		final SevenSigns ss = SevenSigns.getInstance();
		final int playerCabal = ss.getPlayerCabal(player);
		
		String cabal = "none";
		String seal = "none";
		if (playerCabal == SevenSigns.CABAL_DAWN)
		{
			cabal = "Light";
			seal = "Gnosis";
		}
		else if (playerCabal == SevenSigns.CABAL_DUSK)
		{
			cabal = "Dark";
			seal = "Strife";
		}
		
		long greenStones = 0;
		long sealStones = 0;
		final StatSet playerSet = ss.getPlayerStatsSet(player);
		if (playerSet != null)
		{
			if (playerCabal == SevenSigns.CABAL_DAWN)
			{
				sealStones = playerSet.getLong("dawn_red_stones");
				greenStones = playerSet.getLong("dawn_green_stones");
			}
			else
			{
				greenStones = playerSet.getLong("dusk_green_stones");
				sealStones = playerSet.getLong("dusk_blue_stones");
			}
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/custom/SevenSigns/result_page.htm");
		html.replace("%objectId%", String.valueOf(npc.getObjectId()));
		html.replace("%cycle%", String.valueOf(ss.getCurrentCycle()));
		html.replace("%period%", ss.getCurrentPeriodName());
		html.replace("%cabal%", cabal);
		html.replace("%seal%", seal);
		html.replace("%stones%", String.valueOf(sealStones));
		html.replace("%avarice_stones%", String.valueOf(greenStones));
		html.replace("%Gnosis%", String.valueOf(ss.getCurrentScore(SevenSigns.CABAL_DAWN)));
		html.replace("%Strife%", String.valueOf(ss.getCurrentScore(SevenSigns.CABAL_DUSK)));
		html.replace("%Avarice%", String.valueOf(ss.getCurrentScore(SevenSigns.CABAL_NULL)));
		player.sendPacket(html);
		return null;
	}
	
	// ========================== Join cabal ==========================
	
	private String handleJoin(StringTokenizer st, Npc npc, Player player)
	{
		if (!st.hasMoreTokens())
		{
			return null;
		}
		
		final int cabal = Integer.parseInt(st.nextToken());
		final int newSeal = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
		
		// If seal=0, show the join intro page
		if (newSeal == 0)
		{
			return "signs_3_" + SevenSigns.getCabalShortName(cabal) + ".htm";
		}
		
		final SevenSigns ss = SevenSigns.getInstance();
		final int oldCabal = ss.getPlayerCabal(player);
		
		// Check faction alignment
		if (((player.getFraction() == Fraction.LIGHT) && (cabal != SevenSigns.CABAL_DAWN)) || ((player.getFraction() == Fraction.DARK) && (cabal != SevenSigns.CABAL_DUSK)))
		{
			player.sendMessage("Вы выбрали не тот путь!");
			return null;
		}

                // Validate seal choice
                if (cabal == SevenSigns.CABAL_DAWN && newSeal != SevenSigns.SEAL_GNOSIS)
                {
                        player.sendMessage("Light can only register for Gnosis.");
                        return null;
                }
                if (cabal == SevenSigns.CABAL_DUSK && newSeal != SevenSigns.SEAL_STRIFE)
                {
                        player.sendMessage("Dark can only register for Strife.");
                        return null;
                }
		
		if (oldCabal != SevenSigns.CABAL_NULL)
		{
			player.sendMessage("Вы уже являетесь участником " + SevenSigns.getCabalName(cabal) + ".");
			return null;
		}
		
		// Must have at least 1st class transfer
		if (player.getPlayerClass().level() == 0)
		{
			player.sendMessage("Вы должны пройти хотя бы первую смену класса для участия.");
			return null;
		}

		if (SevenSignsConfig.ALT_SS_JOIN_COST > 0)
		{
			if (player.getAdena() < SevenSignsConfig.ALT_SS_JOIN_COST)
			{
				player.sendMessage("Недостаточно адены для участия.");
				return null;
			}
			player.reduceAdena(ItemProcessType.FEE, SevenSignsConfig.ALT_SS_JOIN_COST, npc, true);
		}
		
		ss.setPlayerInfo(player.getObjectId(), cabal, newSeal);
		
		if (cabal == SevenSigns.CABAL_DAWN)
		{
			player.sendPacket(SystemMessageId.YOU_WILL_PARTICIPATE_IN_THE_SEVEN_SIGNS_AS_A_MEMBER_OF_THE_LORDS_OF_DAWN);
		}
		else
		{
			player.sendPacket(SystemMessageId.YOU_WILL_PARTICIPATE_IN_THE_SEVEN_SIGNS_AS_A_MEMBER_OF_THE_REVOLUTIONARIES_OF_DUSK);
		}
		
		switch (newSeal)
		{
			case SevenSigns.SEAL_AVARICE:
				player.sendPacket(SystemMessageId.YOU_HAVE_CHOSEN_THE_SEAL_OF_AVARICE);
				break;
			case SevenSigns.SEAL_GNOSIS:
				player.sendPacket(SystemMessageId.YOU_VE_CHOSEN_TO_FIGHT_FOR_THE_SEAL_OF_GNOSIS_DURING_THIS_QUEST_EVENT_PERIOD);
				break;
			case SevenSigns.SEAL_STRIFE:
				player.sendPacket(SystemMessageId.YOU_VE_CHOSEN_TO_FIGHT_FOR_THE_SEAL_OF_STRIFE_DURING_THIS_QUEST_EVENT_PERIOD);
				break;
		}
		
		return "signs_4_" + SevenSigns.getCabalShortName(cabal) + ".htm";
	}
	
	// ========================== Contribute stones ==========================
	
	private String showContributePage(Npc npc, Player player)
	{
		final int playerCabal = SevenSigns.getInstance().getPlayerCabal(player);
		
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/custom/SevenSigns/signs_5.htm");
		html.replace("%objectId%", String.valueOf(npc.getObjectId()));
		
		switch (playerCabal)
		{
			case SevenSigns.CABAL_DAWN:
				html.replace("%list%", RED_GNOSIS_LIGHT_CONTRIB + GREEN_AVARICE_PAIR_CONTRIB + DAWN_COLOR_EXCHANGE);
				break;
			case SevenSigns.CABAL_DUSK:
				html.replace("%list%", BLUE_STRIFE_DARK_CONTRIB + GREEN_AVARICE_PAIR_CONTRIB + DUSK_COLOR_EXCHANGE);
				break;
			default:
				html.replace("%list%", "Вы не зарегистрированы в Семи Печатях.");
				break;
		}
		
		player.sendPacket(html);
		return null;
	}
	
	private String handleStoneContrib(StringTokenizer st, Npc npc, Player player)
	{
		if (!st.hasMoreTokens())
		{
			return null;
		}
		
		final int stoneType = Integer.parseInt(st.nextToken());
		final SevenSigns ss = SevenSigns.getInstance();
		final long contribScore = ss.getPlayerContribScore(player);
		
		if (contribScore >= 1000000)
		{
			player.sendPacket(SystemMessageId.CONTRIBUTION_LEVEL_HAS_EXCEEDED_THE_LIMIT_YOU_MAY_NOT_CONTINUE);
			return null;
		}
		
		final long redStoneCount = getQuestItemsCount(player, SevenSigns.SEAL_STONE_RED_ID);
		final long greenStoneCount = getQuestItemsCount(player, SevenSigns.SEAL_STONE_GREEN_ID);
		final long blueStoneCount = getQuestItemsCount(player, SevenSigns.SEAL_STONE_BLUE_ID);
		
		long redContribCount = 0;
		long greenContribCount = 0;
		long blueContribCount = 0;
		boolean stonesFound = false;
		
		switch (stoneType)
		{
			case 1: // blue
				blueContribCount = Math.min((1000000 - contribScore) / SevenSigns.BLUE_CONTRIB_POINTS, blueStoneCount);
				break;
			case 2: // green
				greenContribCount = Math.min((1000000 - contribScore) / SevenSigns.GREEN_CONTRIB_POINTS, greenStoneCount);
				break;
			case 3: // red
				redContribCount = Math.min((1000000 - contribScore) / SevenSigns.RED_CONTRIB_POINTS, redStoneCount);
				break;
		}
		
		if (redContribCount > 0)
		{
			stonesFound = true;
			if (!player.destroyItemByItemId(ItemProcessType.QUEST, SevenSigns.SEAL_STONE_RED_ID, redContribCount, npc, true))
			{
				redContribCount = 0;
			}
		}
		if (greenContribCount > 0)
		{
			stonesFound = true;
			if (!player.destroyItemByItemId(ItemProcessType.QUEST, SevenSigns.SEAL_STONE_GREEN_ID, greenContribCount, npc, true))
			{
				greenContribCount = 0;
			}
		}
		if (blueContribCount > 0)
		{
			stonesFound = true;
			if (!player.destroyItemByItemId(ItemProcessType.QUEST, SevenSigns.SEAL_STONE_BLUE_ID, blueContribCount, npc, true))
			{
				blueContribCount = 0;
			}
		}
		
		if (!stonesFound)
		{
			player.sendMessage("У вас нет камней печати этого типа.");
			return null;
		}
		
		final long newContrib = ss.addPlayerStoneContrib(player, blueContribCount, greenContribCount, redContribCount);
		final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_CONTRIBUTION_SCORE_HAS_INCREASED_BY_S1);
		sm.addLong(newContrib);
		player.sendPacket(sm);
		
		return "signs_6.htm";
	}
	
	// ========================== Exchange stones for AA ==========================
	
	private String showExchangePage(Npc npc, Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/custom/SevenSigns/signs_16.htm");
		html.replace("%objectId%", String.valueOf(npc.getObjectId()));
		
		switch (SevenSigns.getInstance().getCabalHighestScore())
		{
			case SevenSigns.CABAL_DAWN:
				html.replace("%list%", RED_EXCHANGE);
				break;
			case SevenSigns.CABAL_DUSK:
				html.replace("%list%", BLUE_EXCHANGE);
				break;
			default:
				html.replace("%list%", GREEN_EXCHANGE);
				break;
		}
		
		player.sendPacket(html);
		return null;
	}
	
	private String handleStoneTypeChoice(StringTokenizer st, Npc npc, Player player)
	{
		if (!st.hasMoreTokens())
		{
			return null;
		}
		
		final int stoneType = Integer.parseInt(st.nextToken());
		int stoneId = 0;
		long stoneCount = 0;
		int stoneValue = 0;
		String stoneColor = null;
		
		switch (stoneType)
		{
			case 1:
				stoneColor = "синих";
				stoneId = SevenSigns.SEAL_STONE_BLUE_ID;
				stoneValue = SevenSignsConfig.ALT_SS_BLUE_STONE_VAL;
				break;
			case 2:
				stoneColor = "зеленых";
				stoneId = SevenSigns.SEAL_STONE_GREEN_ID;
				stoneValue = SevenSignsConfig.ALT_SS_GREEN_STONE_VAL;
				break;
			case 3:
				stoneColor = "красных";
				stoneId = SevenSigns.SEAL_STONE_RED_ID;
				stoneValue = SevenSignsConfig.ALT_SS_RED_STONE_VAL;
				break;
			default:
				return null;
		}
		
		stoneCount = getQuestItemsCount(player, stoneId);
		
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/custom/SevenSigns/signs_17.htm");
		html.replace("%objectId%", String.valueOf(npc.getObjectId()));
		html.replace("%stoneColor%", stoneColor);
		html.replace("%stoneValue%", String.valueOf(stoneValue));
		html.replace("%stoneCount%", String.valueOf(stoneCount));
		html.replace("%stoneItemId%", String.valueOf(stoneId));
		player.sendPacket(html);
		return null;
	}
	
	private String handleStoneExchange(StringTokenizer st, Npc npc, Player player)
	{
		if (st.countTokens() < 2)
		{
			return null;
		}
		
		final int convertStoneId = Integer.parseInt(st.nextToken());
		long convertCount;
		try
		{
			convertCount = Long.parseLong(st.nextToken().trim());
		}
		catch (NumberFormatException e)
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
			return null;
		}
		
		final long totalCount = getQuestItemsCount(player, convertStoneId);
		if (totalCount == 0)
		{
			player.sendMessage("У вас нет камней печати этого типа.");
			return null;
		}
		
		if ((convertCount <= 0) || (convertCount > totalCount))
		{
			player.sendMessage("Неверное количество.");
			return null;
		}
		
		long ancientAdenaReward = 0;
		switch (convertStoneId)
		{
			case SevenSigns.SEAL_STONE_BLUE_ID:
				ancientAdenaReward = SevenSigns.calcAncientAdenaReward(convertCount, 0, 0);
				break;
			case SevenSigns.SEAL_STONE_GREEN_ID:
				ancientAdenaReward = SevenSigns.calcAncientAdenaReward(0, convertCount, 0);
				break;
			case SevenSigns.SEAL_STONE_RED_ID:
				ancientAdenaReward = SevenSigns.calcAncientAdenaReward(0, 0, convertCount);
				break;
		}
		
		if (player.destroyItemByItemId(ItemProcessType.QUEST, convertStoneId, convertCount, npc, true))
		{
			player.addItem(ItemProcessType.QUEST, SevenSigns.ANCIENT_ADENA_ID, ancientAdenaReward, npc, true);
		}
		
		return null;
	}
	
	// ========================== Color Exchange ==========================
        private String handleColorExchange(Npc npc, Player player)
        {
                final SevenSigns ss = SevenSigns.getInstance();
                final int playerCabal = ss.getPlayerCabal(player);

                if (playerCabal == SevenSigns.CABAL_DAWN)
                {
                        final long blueCount = getQuestItemsCount(player, SevenSigns.SEAL_STONE_BLUE_ID);
                        if (blueCount > 0)
                        {
                                if (player.destroyItemByItemId(ItemProcessType.QUEST, SevenSigns.SEAL_STONE_BLUE_ID, blueCount, npc, true))
                                {
                                        player.addItem(ItemProcessType.QUEST, SevenSigns.SEAL_STONE_GREEN_ID, blueCount, npc, true);
                                        player.sendMessage("Успешно обменяно " + blueCount + " синих камней на зеленые.");
                                }
                        }
                        else
                        {
                                player.sendMessage("У вас нет синих камней для обмена.");
                        }
                }
                else if (playerCabal == SevenSigns.CABAL_DUSK)
                {
                        final long redCount = getQuestItemsCount(player, SevenSigns.SEAL_STONE_RED_ID);
                        if (redCount > 0)
                        {
                                if (player.destroyItemByItemId(ItemProcessType.QUEST, SevenSigns.SEAL_STONE_RED_ID, redCount, npc, true))
                                {
                                        player.addItem(ItemProcessType.QUEST, SevenSigns.SEAL_STONE_GREEN_ID, redCount, npc, true);
                                        player.sendMessage("Успешно обменяно " + redCount + " красных камней на зеленые.");
                                }
                        }
                        else
                        {
                                player.sendMessage("У вас нет красных камней для обмена.");
                        }
                }
                return null;
        }

        // ========================== Reward ==========================
	
	private String handleReward(Npc npc, Player player)
	{
		if (!SevenSigns.getInstance().isSealValidationPeriod())
		{
			return null;
		}
		
		final long ancientAdenaReward = SevenSigns.getInstance().getAncientAdenaReward(player, true);
		if (ancientAdenaReward < 3)
		{
			return "signs_9_b.htm";
		}
		
		player.addItem(ItemProcessType.QUEST, SevenSigns.ANCIENT_ADENA_ID, ancientAdenaReward, npc, true);
		return "signs_9_a.htm";
	}
	
	// ========================== Black Marketeer: AA → Adena ==========================
	
	private String handleAncientAdenaExchange(StringTokenizer st, Npc npc, Player player)
	{
		if (!st.hasMoreTokens())
		{
			return "blkmrkt_3.htm";
		}
		
		long ancientAdenaConvert;
		try
		{
			ancientAdenaConvert = Long.parseLong(st.nextToken().trim());
		}
		catch (NumberFormatException e)
		{
			return "blkmrkt_3.htm";
		}
		
		if (ancientAdenaConvert < 1)
		{
			return "blkmrkt_3.htm";
		}
		
		final long ancientAdenaAmount = getQuestItemsCount(player, SevenSigns.ANCIENT_ADENA_ID);
		if (ancientAdenaAmount < ancientAdenaConvert)
		{
			return "blkmrkt_4.htm";
		}
		
		if (!SevenSignsConfig.ALT_SS_AA_TO_ADENA_ENABLED)
		{
			return null;
		}

		if (player.destroyItemByItemId(ItemProcessType.QUEST, SevenSigns.ANCIENT_ADENA_ID, ancientAdenaConvert, npc, true))
		{
			player.addAdena(ItemProcessType.QUEST, (long)(ancientAdenaConvert * SevenSignsConfig.ALT_SS_AA_TO_ADENA_RATE), npc, true);
		}
		
		return "blkmrkt_5.htm";
	}
	
	// ========================== Helpers ==========================
	
	private static boolean isDawnPriest(int npcId)
	{
		for (int id : DAWN_PRIESTS)
		{
			if (id == npcId)
			{
				return true;
			}
		}
		return false;
	}
	
	private static boolean isDuskPriest(int npcId)
	{
		for (int id : DUSK_PRIESTS)
		{
			if (id == npcId)
			{
				return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args)
	{
		new SignsPriest();
	}
}

