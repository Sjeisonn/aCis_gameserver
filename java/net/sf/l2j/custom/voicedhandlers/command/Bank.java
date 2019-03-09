package net.sf.l2j.custom.voicedhandlers.command;
 
import net.sf.l2j.custom.voicedhandlers.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.Player;
 
public class Bank implements IVoicedCommandHandler
{
    private static final String[] VOICED_COMMANDS =
    {
        "deposit",
        "withdraw"
    };
   
    private static final int FESTIVAL_ADENA = 6673;
    private static final int ADENA_AMOUNT = 500_000_000;
   
    @Override
    public void useVoicedCommand(final String command, final Player player)
    {
        if (command.equalsIgnoreCase("deposit"))
        {
            if (player.reduceAdena("Consume", ADENA_AMOUNT, player, true))
                player.addItem("Gold", FESTIVAL_ADENA, 1, player, true);
            else
                player.sendMessage("You need at least 500kk to use the command.");
        }
        else if (command.equalsIgnoreCase("withdraw"))
        {
            if (player.getAdena() >= 2_000_000_000)
            {
                player.sendMessage("You have almost reached the maximum amount of adena. You can not exchange anymore.");
                return;
            }
           
            if (player.destroyItemByItemId("Consume", FESTIVAL_ADENA, 1, null, true))
                player.addAdena("Adena", ADENA_AMOUNT, player, true);
        }
    }
   
    @Override
    public String[] getVoicedCommands()
    {
        return VOICED_COMMANDS;
    }
}