package net.sf.l2j.custom.premium.admincommand;
 
import java.util.StringTokenizer;
 
import net.sf.l2j.custom.premium.PremiumManager;
import net.sf.l2j.custom.premium.PremiumTaskManager;
import net.sf.l2j.custom.premium.PremiumType;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
 
/**
 * @author SweeTs
 */
public class AdminPremium implements IAdminCommandHandler
{
    private static final String[] ADMIN_COMMANDS =
    {
        "admin_premium",
        "admin_premium_remove"
    };
   
    @Override
    public boolean useAdminCommand(String command, Player activeChar)
    {
        final WorldObject target = activeChar.getTarget();
        final Player player = (target != null && target instanceof Player) ? target.getActingPlayer() : activeChar;
       
        if (command.startsWith("admin_premium"))
        {
            final StringTokenizer st = new StringTokenizer(command);
            st.nextToken();
           
            try
            {
                final String type = st.nextToken();
                int duration = 1;
               
                if (st.hasMoreTokens())
                    duration = Integer.parseInt(st.nextToken());
                else
                    activeChar.sendMessage("Duration not specified, using default value (1).");
               
                String gmMessage = activeChar.getName() + " has granted " + player.getName() + " with ";
                PremiumType premiumLevel = PremiumType.NONE;
               
                if (type.equalsIgnoreCase("none"))
                {
                    removeStatus(activeChar, player);
                    return false;
                }
               
                if (type.equalsIgnoreCase("bronze"))
                {
                    premiumLevel = PremiumType.BRONZE;
                    gmMessage += "Bronze";
                }
                else if (type.equalsIgnoreCase("silver"))
                {
                    premiumLevel = PremiumType.SILVER;
                    gmMessage += "Silver";
                }
                else if (type.equalsIgnoreCase("gold"))
                {
                    premiumLevel = PremiumType.GOLD;
                    gmMessage += "Gold";
                }
               
                PremiumManager.setPremium(player, duration, premiumLevel);
               
                // Notify all GMs.
                gmMessage += " VIP status for " + duration + " day(s).";
                AdminData.getInstance().broadcastToGMs(new CreatureSay(activeChar.getObjectId(), Say2.ALLIANCE, "Premium System", gmMessage));
            }
            catch (Exception e)
            {
                activeChar.sendMessage("Usage : //premium <none|bronze|silver|gold");
                return false;
            }
        }
        else if (command.equalsIgnoreCase("admin_premium_remove"))
        {
            if (!player.isPremium())
                return false;
           
            removeStatus(activeChar, player);
        }
       
        return true;
    }
   
    private static final void removeStatus(final Player activeChar, final Player player)
    {
        PremiumManager.removePremium(player);
        PremiumTaskManager.getInstance().remove(player);
       
        // Notify all GMs.
        final String gmMessage = activeChar.getName() + " successfully removed " + player + " VIP status.";
        AdminData.getInstance().broadcastToGMs(new CreatureSay(activeChar.getObjectId(), Say2.ALLIANCE, "Premium System", gmMessage));
    }
   
    @Override
    public String[] getAdminCommandList()
    {
        return ADMIN_COMMANDS;
    }
}