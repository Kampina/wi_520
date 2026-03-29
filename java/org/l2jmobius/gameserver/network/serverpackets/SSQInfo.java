package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.entity.SevenSigns;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class SSQInfo extends ServerPacket
{
private int _state = 0;

public SSQInfo()
{
final int compWinner = SevenSigns.getInstance().getCabalHighestScore();
if (SevenSigns.getInstance().isSealValidationPeriod())
{
if (compWinner == SevenSigns.CABAL_DAWN)
{
_state = 2; // Dawn Sky
}
else if (compWinner == SevenSigns.CABAL_DUSK)
{
_state = 1; // Dusk Sky
}
}
}

public SSQInfo(int state)
{
_state = state;
}

@Override
public void writeImpl(GameClient client, WritableBuffer buffer)
{
ServerPackets.SSQ_INFO.writeId(this, buffer);
switch (_state)
{
case 1:
buffer.writeShort(257); // Dusk Sky
break;
case 2:
buffer.writeShort(258); // Dawn Sky
break;
default:
buffer.writeShort(256); // Normal Sky
break;
}
}
}
