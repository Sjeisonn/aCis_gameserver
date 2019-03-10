package net.sf.l2j.gameserver.event;

import java.util.Collection;

import net.sf.l2j.gameserver.model.actor.instance.Player;

/**
 * @author Kara`
 *
 */
public abstract class AbstractMatch
{
	public abstract void onFinish();
	public abstract int getPartySize();
	public abstract int getId();
	
	public abstract String getName();
	
	public abstract Collection<Player> getPlayers();
	
	public abstract void onKill(final Player player);
	public abstract void onDisconnect(final Player player);
	public abstract void onRegister(final Player player);
	public abstract void onUnregister(final Player player);
}
