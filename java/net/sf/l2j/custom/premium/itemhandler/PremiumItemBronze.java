package net.sf.l2j.custom.premium.itemhandler;

import net.sf.l2j.custom.premium.PremiumManager;
import net.sf.l2j.custom.premium.PremiumType;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;

/**
 * @author SweeTs
 */
public class PremiumItemBronze implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		
		final Player activeChar = (Player) playable;
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		int duration = -1;
		switch (item.getItemId())
		{
			case 9301:
				duration = 7;
				break;
			
			case 9302:
				duration = 14;
				break;
			
			case 9303:
				duration = 28;
				break;
		}
		
		activeChar.destroyItem("Consume", item.getObjectId(), 1, activeChar, true);
		PremiumManager.setPremium(activeChar, duration, PremiumType.BRONZE);
	}
}