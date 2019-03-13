package net.sf.l2j.gameserver.scripting.scripts.ai.group;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.scripting.scripts.ai.L2AttackableAIScript;

public class Barakiel extends L2AttackableAIScript
{
	private static final int BARAKIEL = 2229; //Add Barakiel Npc ID
	
	public Barakiel()
	{
		super("ai/group");
	}
	
	@Override
	protected void registerNpcs()
	{
		addKillId(BARAKIEL);
	}

	@Override
	public String onKill(Npc npc, Creature killer)
	{
		if (killer instanceof Playable)
		{
			Player player = killer.getActingPlayer();
			
			List<Player> players = new ArrayList<>();
			
			if (player.isInParty())
			{
				players.addAll(player.getParty().getMembers().stream().filter(s -> s.isInsideRadius(npc, 1700, false, false)).collect(Collectors.toList()));
			}
			else
			{
				players.add(player);
			}
			
			for (Player p : players)
			{
				if (p.isNoble())
				{
					continue;
				}
				
				p.setNoble(true, true);
				p.sendMessage("Congratulations, you feel the essense of noblesse blessing!");
				
				p.addItem("Tiara", 7694, 1, p, true);
			}
		}
		
		return super.onKill(npc, killer);
	}
}