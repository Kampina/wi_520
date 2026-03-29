package handlers.communityboard;

import org.l2jmobius.gameserver.config.custom.NoblessMasterConfig;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Compatibility owner for the legacy WI career branch.
 */
public class CareerBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbscareer"
	};
	
	private static final int NOBLESS_TIARA = 7694;
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		if (command.startsWith("_bbscareer_noblesse"))
		{
			handleNoblesse(player);
			return onCommand("_bbscareer", player); // Redirect back to origin
		}

		CommunityBoardHandler.getInstance().addBypass(player, "Career", command);
		final String html = buildCareerHtml(player);
		CommunityBoardHandler.separateAndSend(html, player);
		return true;
	}

	private void handleNoblesse(Player player)
	{
		if (!NoblessMasterConfig.NOBLESS_MASTER_ENABLED)
		{
			player.sendMessage("Noblesse system is currently disabled.");
			return;
		}

		if (player.isNoble())
		{
			player.sendMessage("You are already a Noblesse.");
			return;
		}

		if (NoblessMasterConfig.NOBLESS_MASTER_ITEM_COUNT > 0)
		{
			if (player.getInventory().getInventoryItemCount(NoblessMasterConfig.NOBLESS_MASTER_ITEM_ID, -1) < NoblessMasterConfig.NOBLESS_MASTER_ITEM_COUNT)
			{
				final SystemMessage msg = new SystemMessage(SystemMessageId.YOU_NEED_S1_X_S2);
				msg.addItemName(NoblessMasterConfig.NOBLESS_MASTER_ITEM_ID);
				msg.addLong(NoblessMasterConfig.NOBLESS_MASTER_ITEM_COUNT);
				player.sendPacket(msg);
				return;
			}
		}

		if (player.getLevel() >= NoblessMasterConfig.NOBLESS_MASTER_LEVEL_REQUIREMENT)
		{
			if (NoblessMasterConfig.NOBLESS_MASTER_ITEM_COUNT > 0)
			{
				player.destroyItemByItemId(ItemProcessType.FEE, NoblessMasterConfig.NOBLESS_MASTER_ITEM_ID, NoblessMasterConfig.NOBLESS_MASTER_ITEM_COUNT, player, true);
			}

			if (NoblessMasterConfig.NOBLESS_MASTER_REWARD_TIARA)
			{
				player.addItem(ItemProcessType.REWARD, NOBLESS_TIARA, 1, player, true);
			}
			
			player.setNoble(true);
			player.sendPacket(QuestSound.ITEMSOUND_QUEST_FINISH.getPacket());
			player.sendMessage("Congratulations! You are now a Noblesse.");
		}
		else
		{
			player.sendMessage("You do not meet the level requirement to become a Noblesse.");
		}
	}
	
	private String buildCareerHtml(Player player)
	{
		final String currentClass = ClassListData.getInstance().getClass(player.getPlayerClass()).getClassName();
		final String actionBlock = buildActionBlock(player);
		final String noblessStatus = player.isNoble() ? "<font color=66FF66>Already active</font>" : "<font color=FF6666>Not active</font>";
		
		String noblessAction = "";
		if (!player.isNoble() && NoblessMasterConfig.NOBLESS_MASTER_ENABLED)
		{
			noblessAction = "<br><button value=\"Become Noblesse\" action=\"bypass _bbscareer_noblesse\" width=220 height=24 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\"><br>"
				+ "Requires Level: " + NoblessMasterConfig.NOBLESS_MASTER_LEVEL_REQUIREMENT;
				
			if (NoblessMasterConfig.NOBLESS_MASTER_ITEM_COUNT > 0)
			{
				noblessAction += " and " + NoblessMasterConfig.NOBLESS_MASTER_ITEM_COUNT + " of Item ID: " + NoblessMasterConfig.NOBLESS_MASTER_ITEM_ID;
			}
		}

		return "<html><body><br><center>"
			+ "<font color=LEVEL>Career</font><br><br>"
			+ "<table width=420 border=0>"
			+ "<tr><td align=left>Current class:</td><td align=right>" + currentClass + "</td></tr>"
			+ "<tr><td align=left>Level:</td><td align=right>" + player.getLevel() + "</td></tr>"
			+ "<tr><td align=left>Nobless:</td><td align=right>" + noblessStatus + "</td></tr>"
			+ "</table><br>"
			+ actionBlock
			+ noblessAction
			+ "<br><table width=420 border=0>"
			+ "<tr><td align=left>Supported in this board layer:</td></tr>"
			+ "<tr><td align=left>- Class transfer launcher via real RV ClassMaster bypass.</td></tr>"
			+ "<tr><td align=left>- Nobless status directly integrated.</td></tr>"
			+ "<tr><td height=8></td></tr>"
			+ "<tr><td align=left>Still not mapped here:</td></tr>"
			+ "<tr><td align=left>- Subclass service html exists, but no confirmed runnable RV owner for htmbypass_services.Subclass was found.</td></tr>"
			+ "</table><br>"
			+ "<button value=\"Home\" action=\"bypass _bbshome\" width=120 height=22 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">"
			+ "</center></body></html>";
	}
	
	private String buildActionBlock(Player player)
	{
		if (player.isInCategory(CategoryType.FIRST_CLASS_GROUP) && (player.getLevel() >= 40))
		{
			return "<button value=\"Open Class Transfer\" action=\"bypass -h Quest ClassMaster secondclass\" width=220 height=24 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\"><br>"
				+ "<font color=LEVEL>RV ClassMaster can handle the next available transfer from your current state.</font>";
		}
		if (player.isInCategory(CategoryType.SECOND_CLASS_GROUP) && (player.getLevel() >= 40))
		{
			return "<button value=\"Open Class Transfer\" action=\"bypass -h Quest ClassMaster secondclass\" width=220 height=24 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\"><br>"
				+ "<font color=LEVEL>Second class change is available through the native RV ClassMaster flow.</font>";
		}
		if (player.isInCategory(CategoryType.FIRST_CLASS_GROUP) && (player.getLevel() >= 20))
		{
			return "<button value=\"Open Class Transfer\" action=\"bypass -h Quest ClassMaster firstclass\" width=220 height=24 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\"><br>"
				+ "<font color=LEVEL>First class change is available through the native RV ClassMaster flow.</font>";
		}
		if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP) && (player.getLevel() >= 76))
		{
			return "<button value=\"Open Class Transfer\" action=\"bypass -h Quest ClassMaster thirdclass\" width=220 height=24 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\"><br>"
				+ "<font color=LEVEL>Third class transfer is available through the native RV ClassMaster flow.</font>";
		}
		if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP))
		{
			return "<font color=66FF66>Your character has already completed the supported class transfer stages.</font>";
		}
		return "<font color=FF6666>No supported class transfer stage is currently available for this character.</font>";
	}
}