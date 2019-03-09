package net.sf.l2j.custom.pvpsystem;

import java.util.Set;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * @author SweeTs
 */
public final class PvpSystem
{
	/**
	 * Reward a {@link Player} for a pvp kill and update name and title color
	 * @param player : The Player to reward.
	 * @param isPvp : Check if it's a pvp kill, pk otherwise
	 */
	public static final void onPlayerKill(final Player player, final boolean isPvp)
	{
		rewardPlayer(player, isPvp ? Config.PVP_REWARD : Config.PK_REWARD);
		updatePlayerColors(player, isPvp, !isPvp);
	}
	
	/**
	 * Update {@link Player}'s name and title color
	 * @param player : The Player to reward.
	 * @param isPvp : Check if it's pvp case.
	 * @param isPk : Check if it's pk case.
	 */
	public static final void updatePlayerColors(final Player player, final boolean isPvp, final boolean isPk)
	{
		if (isPvp)
		{
			final Set<Integer> pvpColors = Config.PVP_COLOR_LIST.keySet();
			for (int i : pvpColors)
			{
				if (player.getPvpKills() >= i)
					player.getAppearance().setNameColor(Config.PVP_COLOR_LIST.get(i));
			}
		}
		
		if (isPk)
		{
			final Set<Integer> pkColors = Config.PK_COLOR_LIST.keySet();
			for (int i : pkColors)
			{
				if (player.getPkKills() >= i)
					player.getAppearance().setTitleColor(Config.PK_COLOR_LIST.get(i));
			}
		}
	}
	
	/**
	 * Reward a {@link Player} with items.
	 * @param player : The Player to reward.
	 * @param reward : The IntIntHolder container used as itemId / quantity holder.
	 */
	private static final void rewardPlayer(final Player player, final int[][] reward)
	{
		if (reward == null)
			return;
		
		for (int[] it : reward)
		{
			final ItemInstance item = player.getInventory().addItem("Reward", it[0], it[1], player, null);
			if (item == null)
				continue;
			
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(it[0]).addNumber(it[1]));
		}
	}
}