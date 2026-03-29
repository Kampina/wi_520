/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class ExDominionWarStart extends ServerPacket
{
	private final int _objectId;
	private final int _territoryId;
	private final int _disguisedTerritoryId;
	private final boolean _isDisguised;
	private final boolean _battlefieldChatActive;

	public ExDominionWarStart(Player player)
	{
		_objectId = player.getObjectId();
		_territoryId = 0;
		_disguisedTerritoryId = 0;
		_isDisguised = false;
		_battlefieldChatActive = false;
	}

	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_DOMINION_WAR_START.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeInt(_battlefieldChatActive ? 1 : 0);
		buffer.writeInt(_territoryId);
		buffer.writeInt(_isDisguised ? 1 : 0);
		buffer.writeInt(_disguisedTerritoryId);
	}
}