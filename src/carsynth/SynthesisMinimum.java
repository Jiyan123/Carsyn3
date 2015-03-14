package carsynth;
import java.util.List;

import inpro.incremental.unit.ChunkIU;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.WordIU;
import inpro.incremental.unit.IU.IUUpdateListener;
import inpro.incremental.unit.IU.Progress;
import inpro.incremental.unit.SysSegmentIU;
import inpro.synthesis.MaryAdapter;
/**
 * Diese Synthese klappert einfach die Ansagen durch, ohne zB ein "Meter"
 * oder andere "Hesitation" einzuschieben, es geht hier nur darum die Ansagen
 * zu stretchen und falls maximal oder minimal nicht genügend sind, so wird
 * entweder gewartet oder es wird gleich zu der entsprechenden Stelle gesprungen
 * @author jiyan
 *
 */
public class SynthesisMinimum extends CarSynthesisAbstract
{
	protected boolean bHold = false;
	/**
	 * Während des wartens darauf, dass die Synthese weitermacht,
	 * kann getOngoingSegment(WordIU) nicht das entsprende Segment
	 * holen, weil das "aktuelle" Segment "Completed" sein muss
	 * (muss sein, sonst hört es sich schlecht an, deshalb wird
	 * diese Referenz benutzt um getOngoingSegment(WordIU) zu sagen
	 * wo es weiter geht (wenn referedSys nicht null ist).
	 */
	protected SysSegmentIU referedSys = null;
	
	public SynthesisMinimum(String turn)
	{
		super(turn);
		for(ChunkIU it : chunks)
		{
			// Das "Jetzt" Chunk soll nicht stoppen
			if(!it.equals(chunks.get(chunks.size()-1)))
			{
				it.getLastSegment().addUpdateListener(getNewListener());
			}
		}
	}
	
	public SynthesisMinimum(String turn, List<Integer> counts, List<String> commands)
	{
		super(turn, counts,commands);
		for(ChunkIU it : chunks)
		{
			// Das "Jetzt" Chunk soll nicht stoppen
			if(!it.equals(chunks.get(chunks.size()-1)))
			{
				it.getLastSegment().addUpdateListener(getNewListener());
			}
		}
	}
	
	@Override
	protected void planMeters(long dist, double sp, SysSegmentIU s, boolean theory, boolean hesIsSet)
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
		ChunkIU commandmentChunk = new ChunkIU(name);
		commandmentChunk.groundIn(MaryAdapter.getInstance().text2IUs(name));
		groundChunk(commandmentChunk);
		chunks.get(chunks.size()-1).getLastSegment().addNextSameLevelLink(commandmentChunk.getFirstSegment());
		chunks.add(commandmentChunk);
		mapper.put(commandmentChunk,meterVal);
	}
	
	@Override
	protected SysSegmentIU getOngoingSegment(WordIU param)
	{
		if(referedSys != null)return referedSys;
		return super.getOngoingSegment(param);
	}
	
	protected IUUpdateListener getNewListener()
	{
		return new IUUpdateListener()
		{
			Progress p;
			@Override
			public void update(IU updatedIU)
			{
				if(p != updatedIU.getProgress())
				{
					p = updatedIU.getProgress();
					if(p == Progress.COMPLETED)
					{
						if(bHold)
						{
							referedSys = (SysSegmentIU)updatedIU;
							dispatcher.interruptPlayback();
							if(updatedIU.getNextSameLevelLink() == null)
							{
								return;
							}
							while(oldDist > mapper.get(getGroundChunk(updatedIU.getNextSameLevelLink())))
							{
								try
								{
									Thread.sleep(100);
								} catch (InterruptedException e)
								{
									e.printStackTrace();
								}
							}
							referedSys = null;
							bHold = false;
							dispatcher.continuePlayback();
						}
					}
				}
			}
		};
	}
}