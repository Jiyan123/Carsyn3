package carsynth;

import java.util.List;

import inpro.incremental.unit.ChunkIU;
import inpro.incremental.unit.SysSegmentIU;
import inpro.synthesis.MaryAdapter;

/**
 * Arbeitet wiede SynthesisMinimum, aber schiebt noch
 * ein Meter bei Bedarf ein, welches gestrecht werden kann
 * @author jiyan
 *
 */
public class SynthesisMeter extends SynthesisMinimum
{
	
	public SynthesisMeter(String turn)
	{
		super(turn);
		for(ChunkIU  u: chHes)
		{
			if(u.getWord().equals("Meter"))
			{
				int nextInd = chHes.indexOf(u)+2;
				if(nextInd < chunks.size())setSLL(u,chunks.get(nextInd),true);
			}
		}
		for(ChunkIU it : chHes)
		{
			it.getLastSegment().addUpdateListener(getNewListener());
		}
		chunks.get(0).getLastSegment().addUpdateListener(getNewListener());
	}
	
	public SynthesisMeter(String turn, List<Integer> counts, List<String> commands)
	{
		super(turn, counts,commands);
		for(ChunkIU  u: chHes)
		{
			if(u.getWord().equals("Meter"))
			{
				int nextInd = chHes.indexOf(u)+2;
				if(nextInd < chunks.size())setSLL(u,chunks.get(nextInd),true);
			}
		}
		for(ChunkIU it : chHes)
		{
			it.getLastSegment().addUpdateListener(getNewListener());
		}
		chunks.get(0).getLastSegment().addUpdateListener(getNewListener());
	}
	
	@Override
	protected void planMeters(long dist, double sp, SysSegmentIU s,
			boolean theory, boolean hesIsSet)
	{
		dur = 0.0;
		bMin = false;
		bMax = false;
		ChunkIU u = getGroundChunk(s);
		if(u.equals(chunks.get(0))) // Turn Info
		{
			if(!theory)System.out.println("Anfang");
			
			stretchChunk(dist,mapper.get(chunks.get(1)),s,theory);
			
			if(dur > 0)
			{
				if(!theory && !bMax)
				{
					bHold = false;
				}
				
				// wenn max setze sll
				if(bMin)
				{
					setSLL(u,getNextMeterChunk((int) ((int)dist-dur*sp)),true);
					planMeters((long)(dist-sp*dur),sp,(SysSegmentIU) getNextMeterChunk((int) ((int)dist-dur*sp)).getFirstSegment(),true,hesIsSet);
					return;
				}

				/* wenn stretch wert min dann ist halt die Frage was zu machen ist ...
				 hab mich hier entschlossen einfach die Turn ansage abzuspielen
				  und ...
				*/
				if(bMax)
				{
					// es wird dann einfach abgebrochen und die sprachsynthese macht nach einer zeit weiter
					if(!theory)
					{
						bHold = true;
					}
				}
				u.getLastSegment().setAsTopNextSameLevelLink(chunks.get(1).getFirstSegment().toPayLoad());
				planMeters((long)(dist-sp*dur),sp,(SysSegmentIU)chunks.get(1).getFirstSegment(),true,hesIsSet);
			}
		}
		else if(chunks.indexOf(u) > 0 && chunks.indexOf(u) < chunks.size()) // Count Info
		{
			if(!theory)System.out.println("Counter");
			
			int nextIndex = chunks.indexOf(u)+1;
			if(nextIndex >= chunks.size())
			{
				if(!theory)System.out.println("Jetzt");
				stretchChunk(dist,0,s,theory);
				return;
			}
			stretchChunk(dist,mapper.get(chunks.get(nextIndex)),s,theory);
			
			if(dur>0)
			{
				
				// wenn max setze sll
				if(bMin)
				{
					setSLL(u,getNextMeterChunk((int) ((int)dist-dur*sp)),true);
					planMeters((long)(dist-sp*dur),sp,(SysSegmentIU) getNextMeterChunk((int) ((int)dist-dur*sp)).getFirstSegment(),true,hesIsSet);
					return;
				}
				
				// wenn stretch wert min dann gehe zu meter
				if(bMax)
				{
					setSLL(u,chHes.get(chunks.indexOf(u)-1),true);
					planMeters((long)(dist-sp*dur),sp,(SysSegmentIU) chHes.get(chunks.indexOf(u)-1).getFirstSegment(),true,hesIsSet);
					return;
				}
				u.getLastSegment().setAsTopNextSameLevelLink(chunks.get(nextIndex).getFirstSegment().toPayLoad());
				planMeters((long)(dist-sp*dur),sp,(SysSegmentIU)chunks.get(nextIndex).getFirstSegment(),true,hesIsSet);
			}
		}
		else if(chHes.contains(u) && u.getWord().equals("Meter")) // Meter
		{
			if(!theory)System.out.println("Meter ansage");
			
			int nextIndex = chHes.indexOf(u)+2;
			if(nextIndex >= chunks.size())
			{
				if(!theory)System.out.println("Jetzt");
				stretchChunk(dist,0,(SysSegmentIU)chunks.get(chunks.size()-1).getLastSegment(),theory);
				return;
			}
			stretchChunk(dist,mapper.get(chunks.get(nextIndex)),s,theory);
			
			if(dur>0)
			{
				if(!bMax && !theory)
				{
					bHold = false;
				}
				
				// wenn max setze sll
				if(bMin)
				{
					setSLL(u,getNextMeterChunk((int) ((int)dist-dur*sp)),true);
					planMeters((long)(dist-sp*dur),sp,(SysSegmentIU) getNextMeterChunk((int) ((int)dist-dur*sp)).getFirstSegment(),true,hesIsSet);
					return;
				}
				
				// wenn stretch wert min dann halte an
				if(bMax)
				{
					// es wird dann einfach abgebrochen und die sprachsynthese macht nach einer zeit weiter
					if(!theory)
					{
						bHold = true;
					}
				}
				u.getLastSegment().setAsTopNextSameLevelLink(chunks.get(nextIndex).getFirstSegment().toPayLoad());
				planMeters((long)(dist-sp*dur),sp,(SysSegmentIU)chunks.get(nextIndex).getFirstSegment(),true,hesIsSet);
			}
		}
		else
		{
			System.out.println("Fehler");
		}

	}
	
	@Override
	protected void chunkIt(String name, int meterVal)
	{
		super.chunkIt(name, meterVal);
		ChunkIU meter = new ChunkIU("Meter");
		meter.groundIn(MaryAdapter.getInstance().text2IUs("Meter"));
		groundChunk(meter);
		chHes.add(meter);
	}
}
