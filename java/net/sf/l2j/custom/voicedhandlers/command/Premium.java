package net.sf.l2j.custom.voicedhandlers.command;
 
import java.text.SimpleDateFormat;
import java.util.Date;
 
import net.sf.l2j.custom.premium.PremiumTaskManager;
import net.sf.l2j.custom.voicedhandlers.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
 
public class Premium implements IVoicedCommandHandler
{
    private static final String[] VOICED_COMMANDS =
    {
        "premium"
    };
   
    @Override
    public void useVoicedCommand(final String command, final Player player)
    {
        if (player.isPremium())
        {
            final SimpleDateFormat df = new SimpleDateFormat("dd MMMM, HH:mm zz");
            final long endTime = PremiumTaskManager.getInstance().getPremiumCharacters().get(player);
            final Date date = new Date(endTime);
            player.sendPacket(new CreatureSay(0, Say2.HERO_VOICE, "System", "Your premium period ends: " + df.format(date) + "."));
        }
        else
            player.sendMessage("You are not a premium user.");
    }
   
    @Override
    public String[] getVoicedCommands()
    {
        return VOICED_COMMANDS;
    }
}