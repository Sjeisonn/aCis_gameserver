package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillCreateItems extends L2Skill
{
	private final int[] _createItemId;
	private final int[] _createItemCount;
	
	public L2SkillCreateItems(StatsSet set)
	{
		super(set);
		
		_createItemId = set.getIntegerArray("create_item_ids");
		_createItemCount = set.getIntegerArray("create_item_counts");
	}
	
	@Override
	public void useSkill(Creature activeChar, WorldObject[] targets)
	{
		final Player player = activeChar.getActingPlayer();
		if (!(activeChar instanceof Player))
			return;
		
		if (_createItemId == null || _createItemCount == null || _createItemId.length != _createItemCount.length)
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE).addSkillName(this));
			return;
		}
		
		for (int i = 0; i < _createItemId.length; i++)
			player.addItem("Skill", _createItemId[i], _createItemCount[i], activeChar, true);
	}
}