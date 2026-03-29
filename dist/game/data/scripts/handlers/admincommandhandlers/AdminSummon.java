/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.admincommandhandlers;

import org.l2jmobius.gameserver.handler.AdminCommandHandler;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * @author poltomb, Mobius
 */
public class AdminSummon implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_summon",
		"admin_summon2"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		int id = 0;
		long count = 1;
		final String[] data = command.split(" ");
		if (data.length < 2)
		{
			activeChar.sendSysMessage(data[0].equals("admin_summon2") ? "Usage: //summon2 [count] <id>" : "Usage: //summon <id> [count]");
			return false;
		}
		
		try
		{
			if (data[0].equals("admin_summon2"))
			{
				count = Long.parseLong(data[1]);
				if (data.length > 2)
				{
					id = Integer.parseInt(data[2]);
				}
			}
			else // admin_summon
			{
				id = Integer.parseInt(data[1]);
				if (data.length > 2)
				{
					count = Long.parseLong(data[2]);
				}
			}
		}
		catch (NumberFormatException nfe)
		{
			activeChar.sendSysMessage("Incorrect format for command 'summon'");
			return false;
		}
		
		final String subCommand;
		if (id < 1000000)
		{
			subCommand = "admin_create_item";
		}
		else
		{
			subCommand = "admin_spawn_once";
			activeChar.sendSysMessage("This is only a temporary spawn.  The mob(s) will NOT respawn.");
			id -= 1000000;
		}
		
		if ((id > 0) && (count > 0))
		{
			AdminCommandHandler.getInstance().onCommand(activeChar, subCommand + " " + id + " " + count, true);
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
