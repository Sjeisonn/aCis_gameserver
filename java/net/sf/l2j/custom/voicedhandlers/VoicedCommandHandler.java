package net.sf.l2j.custom.voicedhandlers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.custom.voicedhandlers.command.Bank;
import net.sf.l2j.custom.voicedhandlers.command.Menu;
import net.sf.l2j.custom.voicedhandlers.command.Pincode;
import net.sf.l2j.custom.voicedhandlers.command.Premium;

public class VoicedCommandHandler
{
	private static final Map<Integer, IVoicedCommandHandler> VOICED_COMMANDS = new HashMap<>();
	
    protected VoicedCommandHandler()
    {
        registerVoicedCommand(new Bank());
        registerVoicedCommand(new Menu());
        registerVoicedCommand(new Pincode());
        registerVoicedCommand(new Premium());
    }
	
	private static void registerVoicedCommand(IVoicedCommandHandler voicedCommand)
	{
		Arrays.stream(voicedCommand.getVoicedCommands()).forEach(v -> VOICED_COMMANDS.put(v.intern().hashCode(), voicedCommand));
	}
	
	public IVoicedCommandHandler getVoicedCommand(String voicedCommand)
	{
		return VOICED_COMMANDS.get(voicedCommand.hashCode());
	}
	
	public int size()
	{
		return VOICED_COMMANDS.size();
	}
	
	public static final VoicedCommandHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static final class SingletonHolder
	{
		static final VoicedCommandHandler INSTANCE = new VoicedCommandHandler();
	}
}