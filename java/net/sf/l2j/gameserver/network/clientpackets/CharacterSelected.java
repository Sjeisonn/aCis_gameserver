package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.custom.pincode.PincodeTable;
import net.sf.l2j.gameserver.model.CharSelectSlot;
import net.sf.l2j.gameserver.network.FloodProtectors;
import net.sf.l2j.gameserver.network.FloodProtectors.Action;
import net.sf.l2j.gameserver.network.L2GameClient;

public class CharacterSelected extends L2GameClientPacket
{
	private int _charSlot;
	
	@SuppressWarnings("unused")
	private int _unk1; // new in C4
	@SuppressWarnings("unused")
	private int _unk2; // new in C4
	@SuppressWarnings("unused")
	private int _unk3; // new in C4
	@SuppressWarnings("unused")
	private int _unk4; // new in C4
	
	@Override
	protected void readImpl()
	{
		_charSlot = readD();
		_unk1 = readH();
		_unk2 = readD();
		_unk3 = readD();
		_unk4 = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2GameClient client = getClient();
		if (!FloodProtectors.performAction(client, Action.CHARACTER_SELECT))
			return;
		
		// we should always be able to acquire the lock but if we cant lock then nothing should be done (ie repeated packet)
		if (client.getActiveCharLock().tryLock())
		{
			try
			{
				// should always be null but if not then this is repeated packet and nothing should be done here
				if (client.getActiveChar() == null)
				{
					final CharSelectSlot info = client.getCharSelectSlot(_charSlot);
					if (info == null || info.getAccessLevel() < 0)
						return;
					
					PincodeTable.sendPinCodeWindow(client, PincodeTable.getInstance().getPincode(info.getObjectId()) == null ? 1 : 0, info.getObjectId(), _charSlot, "");
				}
			}
			finally
			{
				client.getActiveCharLock().unlock();
			}
		}
	}
}