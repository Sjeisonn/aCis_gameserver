package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.custom.cafepoint.PcCafeConsumeType;

public class ExPCCafePointInfo extends L2GameServerPacket
{
	private final int _points;
	private final int _mAddPoint;
	private final int _mPeriodType;
	private final int _remainTime;
	private final PcCafeConsumeType _pointType;
	private final int _time;
	
	public ExPCCafePointInfo(int points, int pointsToAdd, PcCafeConsumeType type)
	{
		_points = points;
		_mAddPoint = pointsToAdd;
		_mPeriodType = 1;
		_remainTime = 0;
		_pointType = type;
		_time = 0;
		
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x31);
		writeD(_points); // Total Points
		writeD(_mAddPoint); // Earned Points
		writeC(_mPeriodType); // period(0=don't show window,1=acquisition,2=use points)
		writeD(_remainTime); // period hours left
		writeC(_pointType.ordinal()); // points inc display color(0=yellow, 1=cyan-blue, 2=red, all other black)
		writeD(_time); // value is in seconds * 3
	}
}