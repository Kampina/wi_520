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
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class ExReplyRegisterDominion extends ServerPacket
{
	private final int _dominionId;
	private final boolean _isClanRegistration;
	private final boolean _isRegistration;
	private final boolean _success;
	private final int _clanCount;
	private final int _playerCount;
	
	public ExReplyRegisterDominion(int dominionId, boolean isClanRegistration, boolean isRegistration, boolean success, int clanCount, int playerCount)
	{
		_dominionId = dominionId;
		_isClanRegistration = isClanRegistration;
		_isRegistration = isRegistration;
		_success = success;
		_clanCount = clanCount;
		_playerCount = playerCount;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_REPLY_REGISTER_DOMINION.writeId(this, buffer);
		buffer.writeInt(_dominionId);
		buffer.writeInt(_isClanRegistration ? 1 : 0);
		buffer.writeInt(_isRegistration ? 1 : 0);
		buffer.writeInt(_success ? 1 : 0);
		buffer.writeInt(_clanCount);
		buffer.writeInt(_playerCount);
	}
}
