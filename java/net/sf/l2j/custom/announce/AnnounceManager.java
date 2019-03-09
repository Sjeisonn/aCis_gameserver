package net.sf.l2j.custom.announce;

import net.sf.l2j.Config;
import net.sf.l2j.custom.topplayer.TopPlayerManager;
import net.sf.l2j.custom.topplayer.TopPlayerManager.TopPlayer;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * @author SweeTs
 */
public final class AnnounceManager
{
	private static final String HERO_ANNOUNCE = Config.HERO_ANNOUNCE;
	private static final String CASTLE_LORD_ANNOUNCE = Config.CASTLE_LORD_ANNOUNCE;
	private static final String TOP_PLAYER_ANNOUNCE = Config.TOP_PLAYER_ANNOUNCE;
	
	private static final String PVP_MESSAGE = Config.PVP_MESSAGE;
	private static final String PK_MESSAGE = Config.PK_MESSAGE;
	
	/**
	 * Broadcast hero, castle lord and top player log-in notification
	 * @param player the player
	 */
	public static void onPlayerLogin(final Player player)
	{
		if (player.isHero())
		{
			final String className = PlayerData.getInstance().getTemplate(player.getBaseClass()).getClassName();
			Broadcast.announceToOnlinePlayers(String.format(HERO_ANNOUNCE, player.getName(), className));
		}
		
		if (player.isClanLeader() && player.getClan().hasCastle())
		{
			final String castleName = CastleManager.getInstance().getCastleById(player.getClan().getCastleId()).getName();
			Broadcast.announceToOnlinePlayers(String.format(CASTLE_LORD_ANNOUNCE, player.getName(), castleName));
		}
		
		final TopPlayer topPlayer = TopPlayerManager.getInstance().getTopPlayer();
		if (topPlayer != null)
		{
			if (player.getName().equalsIgnoreCase(topPlayer.getName()))
				Broadcast.announceToOnlinePlayers(String.format(TOP_PLAYER_ANNOUNCE, player.getName(), topPlayer.getPvp(), topPlayer.getPk()));
		}
	}
	
	/**
	 * Broadcast message about a kill to all online players
	 * @param killer the killer
	 * @param victim the victim
	 * @param isPvp check if it's a pvp kill, pk otherwise
	 */
	public static void onPlayerKill(final Player killer, final Player victim, final boolean isPvp)
	{
		Broadcast.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.S1_S2).addString(String.format(isPvp ? PVP_MESSAGE : PK_MESSAGE, killer.getName(), victim.getName()) + " - ").addZoneName(killer.getPosition()));
	}
}