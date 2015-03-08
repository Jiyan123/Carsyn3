package carsynth;

import java.util.Date;

public class MeterSimulator
{
	Date originDate;
	long oldMeter;
	
	public MeterSimulator(int meters)
	{
		originDate = new Date();
		oldMeter = meters;
	}
	
	public long getMeter(int speed)
	{
		Date newDate = new Date();
		long proceed = (newDate.getTime() - originDate.getTime())*speed/1000;
		if(proceed >= 1)
		{
			oldMeter -= proceed;
			originDate = newDate;
			return oldMeter;
		}
		else
		{
			return oldMeter;
		}
	}
}
