package net.sf.l2j.custom.pincode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author SweeTs
 */
public final class PincodeTable
{
	public static final String PINCODE = "Pincode";
	private static final CLogger LOGGER = new CLogger(PincodeTable.class.getName());
	
	private static final String SELECT = "SELECT charId, val FROM character_memo WHERE var=?";
	private static final String INSERT = "INSERT INTO character_memo VALUES (?, ?, ?)";
	private static final String UPDATE = "UPDATE character_memo SET val=? WHERE charId=? AND var=?";
	
	private final Map<Integer, String> _pincodes = new HashMap<>();
	
	protected PincodeTable()
	{
		try (final Connection con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement ps = con.prepareStatement(SELECT))
		{
			ps.setString(1, PINCODE);
			
			try (final ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
					_pincodes.put(rs.getInt("charId"), rs.getString("val"));
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded {} pincodes.", _pincodes.size());
		}
		catch (final SQLException e)
		{
			LOGGER.error(getClass().getSimpleName() + ": failed restoring data: ", e);
		}
	}
	
	public static void sendPinCodeWindow(final L2GameClient client, final int isNew, final int objId, final int charSlot, final String errMsg)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		final StringBuilder sb = new StringBuilder();
		
		sb.append("<html><title>Character Pincode</title><body><center>");
		sb.append("<br><img src=l2ui_ch3.herotower_deco width=256 height=32><br>");
		sb.append("<font color=FF6600>Character Pincode Security</font><br1>");
		sb.append("<img src=L2UI.SquareWhite width=280 height=1><img src=L2UI.SquareBlank width=280 height=1><br>");
		
		if (isNew == 1)
		{
			sb.append("Please create a <font color=LEVEL>new pincode</font> for your character.<br1>");
			sb.append("Pincode consists of <font color=LEVEL>numbers only</font>!<br1>");
			sb.append("Minimum pincode length: <font color=LEVEL>6</font>.<br1>");
			sb.append("Maximum pincode length: <font color=LEVEL>8</font>.<br>");
		}
		else
			sb.append("Please enter your existing character pincode.<br>");
		
		sb.append("<table width=200 cellpadding=5>");
		sb.append("<tr>");
		sb.append("<td><edit var=pass width=120 height=15 type=password></td>");
		sb.append("<td><button value=Submit action=\"bypass -h writepin " + isNew + " " + objId + " " + charSlot + " $pass\" width=65 height=20 back=L2UI_ch3.smallbutton2_down fore=L2UI_ch3.smallbutton2></td>");
		sb.append("</tr>");
		sb.append("</table>");
		
		if (!errMsg.isEmpty())
		{
			sb.append("<br>");
			sb.append("<font color=FF0000>" + errMsg + "</font>");
		}
		sb.append("</center></body></html>");
		
		html.setHtml(sb.toString());
		client.sendPacket(html);
	}
	
	public String getPincode(final int charId)
	{
		return _pincodes.get(charId);
	}
	
	public void updatePincode(final int charId, final String pincode, final int charSlot, final L2GameClient client)
	{
		final boolean isNew = _pincodes.put(charId, pincode) == null;
		try (final Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			if (isNew)
			{
				try (final PreparedStatement ps = con.prepareStatement(INSERT))
				{
					ps.setInt(1, charId);
					ps.setString(2, PINCODE);
					ps.setString(3, pincode);
					ps.execute();
					
					sendPinCodeWindow(client, 0, charId, charSlot, "");
				}
			}
			else
			{
				try (final PreparedStatement ps = con.prepareStatement(UPDATE))
				{
					ps.setString(1, pincode);
					ps.setInt(2, charId);
					ps.setString(3, PINCODE);
					ps.execute();
					
					client.getActiveChar().sendMessage("Pincode changed succesfully.");
				}
			}
		}
		catch (final SQLException e)
		{
			LOGGER.error(getClass().getSimpleName() + ": failed updating pincode: ", e);
		}
	}
	
	public static final PincodeTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static final class SingletonHolder
	{
		protected static final PincodeTable INSTANCE = new PincodeTable();
	}
}