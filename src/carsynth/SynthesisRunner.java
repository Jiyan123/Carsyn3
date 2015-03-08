package carsynth;

import inpro.incremental.unit.ChunkIU;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.IU.IUUpdateListener;
import inpro.incremental.unit.IU.Progress;
import inpro.incremental.unit.SegmentIU;
import inpro.incremental.unit.SysSegmentIU;
import inpro.incremental.unit.WordIU;
import inpro.synthesis.MaryAdapter;


/**
 * Diese Synthese versucht erstmal zu stretchen und wenn dass nicht ausreicht
 * wird entweder ein SLL an die entsprechende Stelle gestellt, oder es wird
 * entweder ein "Meter" eingefügt und wenn das nicht ausreicht, wird zu einem
 * "ähm nun ja", welches sich beliebig oft wiederholt (kann eventuell
 * schlechter - weil nervig - sein).
 * @author jiyan
 *
 */
public class SynthesisRunner extends CarSynthesisAbstract
{
	ChunkIU copHes; // Die ChunkIU aus der die Features genommen werden und gecopiet werden
	
	long imDist[] = {0,0,0,0,0,0,0,0,0,0,0,0,0,0}; // unsauber aber mach ich erstmal so
	
	public SynthesisRunner(String turn)
	{
		super(turn);
	}
	
	@Override
	protected void setInst(String turn, long meter)
	{
		ChunkIU turnChunk = new ChunkIU(turn);
		turnChunk.groundIn(MaryAdapter.getInstance().text2IUs(turn));
		groundChunk(turnChunk);
		chunks.add(turnChunk);
		mapper.put( turnChunk , (int)meter);
		
		addHes();
		
		copHes = new ChunkIU("äh nun ja");
		copHes.groundIn(MaryAdapter.getInstance().text2IUs("äh nun ja"));
		groundChunk(copHes);
		
		for(SegmentIU i : copHes.getSegments())((SysSegmentIU)i).stretchFromOriginal(maxStretch);
		
		chunkIt("hundert",100);
		chunkIt("achtzig",80);
		chunkIt("fünfzig",50);
		chunkIt("zwanzig",20);
		chunkIt("jetzt",5); // hat noch meter, muss aber nicht haben
		
		for(ChunkIU  u: chHes)
		{
			if(u.getWord().equals("Meter") && chHes.indexOf(u) < chHes.size()-2)
			{
				int nextInd = getNextIndex(u);
				setSLL(u,chunks.get(nextInd),true);
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
				// wenn max setze sll
				if(bMin)
				{
					setSLL(u,getNextMeterChunk((int) ((int)dist-dur*sp)),true);
					planMeters((long)(dist-sp*dur),sp,(SysSegmentIU) getNextMeterChunk((int) ((int)dist-dur*sp)).getFirstSegment(),true,hesIsSet);
					return;
				}

				// wenn stretch wert min dann gehe danach zu Hesitation
				if(bMax)
				{
					if(dist-sp*dur > mapper.get(chunks.get(1)))
					{
						setSLL(u,chHes.get(0),true);
						planMeters((long)(dist-sp*dur),sp,(SysSegmentIU)chHes.get(0).getFirstSegment(),true,hesIsSet);
						return;
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
				
				// wenn stretch wert min dann gehe danach zu Meter
				if(bMax)
				{
					if(dist-sp*dur > mapper.get(chunks.get(nextIndex)))
					{
						setSLL(u,chHes.get(2*nextIndex-3),true);
						planMeters((long)(dist-sp*dur),sp,(SysSegmentIU)chHes.get(2*nextIndex-3).getFirstSegment(),true,hesIsSet);
						return;
					}
				}
				u.getLastSegment().setAsTopNextSameLevelLink(chunks.get(nextIndex).getFirstSegment().toPayLoad());
				planMeters((long)(dist-sp*dur),sp,(SysSegmentIU)chunks.get(nextIndex).getFirstSegment(),true,hesIsSet);
			}
		}
		else if(chHes.contains(u) && u.getWord().equals("Meter")) // Meter
		{
			if(!theory)System.out.println("Meter ansage");
			// Versuche zu stretchen
			int nextIndex = getNextIndex(u);
			stretchChunk(dist,mapper.get(chunks.get(nextIndex)),s,theory);
			
			if(dur>0)
			{
				// wenn max setze sll - kann man sich wohl sparen
				if(bMin)
				{
					setSLL(u,getNextMeterChunk((int) ((int)dist-dur*sp)),true);
					planMeters((long)(dist-sp*dur),sp,(SysSegmentIU) getNextMeterChunk((int) ((int)dist-dur*sp)).getFirstSegment(),true,hesIsSet);
					return;
				}

				// wenn stretch wert min dann gehe danach zu hes
				if(bMax)
				{
					if(dist-sp*dur > mapper.get(chunks.get(nextIndex)))
					{
						setSLL(u,chHes.get(chHes.indexOf(u)+1),true);
						planMeters((long)(dist-sp*dur),sp,(SysSegmentIU)chHes.get(chHes.indexOf(u)+1).getFirstSegment(),true,hesIsSet);
						return;
					}
				}
				u.getLastSegment().setAsTopNextSameLevelLink(chunks.get(nextIndex).getFirstSegment().toPayLoad());
				planMeters((long)(dist-sp*dur),sp,(SysSegmentIU)chunks.get(nextIndex).getFirstSegment(),true,hesIsSet);
			}
		}
		else if(chHes.contains(u)) // Hesitations
		{
			if(!theory)System.out.println("Hesitation");
			
			imDist[chHes.indexOf(u)] = dist; // mapper.get(getNextMeterChunk((int)dist));
			long newDist = mapper.get(chunks.get(getNextIndex(u)));
			
			long calcDist = dist;
			double calcDur = 0.0;
			// zunächst berechnen wir die gerade ablaufbaren segmente
			for(SegmentIU it:u.getSegments())
			{
				if(it.startTime() > s.startTime() || (theory && it.startTime() >= s.startTime()))
				{
					calcDur += it.duration();
				}
				if(calcDist -sp*calcDur < newDist)
				{
					planMeters((long)(calcDist - sp*calcDur),sp,(SysSegmentIU)chunks.get(getNextIndex(u)).getFirstSegment(),true,true);
				}
			}
			calcDist -= sp*calcDur;
			// dann berechnen wir wo wir rauskommen würden
			while(calcDist - sp*u.duration() >= newDist)
			{
				calcDist -= sp*u.duration();
			}
			// entlang der wörter
			int j = 0;
			while(calcDist > newDist)
			{
				WordIU fW = u.getWords().get(j);
				calcDist -= fW.duration()*sp;
				j++;
			}
			
			// calc Chunk  - geht das iwie besser?
			planMeters(calcDist,sp,(SysSegmentIU)chunks.get(getNextIndex(u)).getFirstSegment(),true,true);
		}
		else
		{
			System.out.println("Fehler");
		}
	}
	
	protected int getNextIndex(ChunkIU u)
	{
		int calc = chHes.indexOf(u);
		calc = calc % 2 == 0 ? calc : calc+1;
		calc /= 2;
		calc += 1;
		return calc;
	}
	
	private void addHes()
	{
		ChunkIU he = new ChunkIU("äh nun ja");
		he.groundIn(MaryAdapter.getInstance().text2IUs("äh nun ja"));
		groundChunk(he);
		chHes.add(he);
		
		for(SegmentIU it : he.getSegments())
		{
			((SysSegmentIU)it).addUpdateListener(getListener());
			((SysSegmentIU)it).stretchFromOriginal(maxStretch); // hört sich natürlicher an
		}
	}
	
	private void chunkIt(String name, int meterVal)
	{
		ChunkIU commandmentChunk = new ChunkIU(name);
		commandmentChunk.groundIn(MaryAdapter.getInstance().text2IUs(name));
		groundChunk(commandmentChunk);
		ChunkIU meter = new ChunkIU("Meter");
		meter.groundIn(MaryAdapter.getInstance().text2IUs("Meter"));
		groundChunk(meter);
		chHes.add(meter);
		
		chunks.get(chunks.size()-1).getLastSegment().addNextSameLevelLink(commandmentChunk.getFirstSegment());
		chunks.add(commandmentChunk);
		mapper.put(commandmentChunk,meterVal);
		
		addHes();
	}
	
	protected IUUpdateListener getListener()
	{
		return new IUUpdateListener() {
			Progress previousProgress;
			@Override
			public void update(IU updatedIU)
			{
				if(updatedIU.getProgress() != previousProgress && updatedIU.getProgress() == Progress.ONGOING)
				{
					imDist[chHes.indexOf(getGroundChunk(updatedIU))] -= updatedIU.duration()*speed;
					if(imDist[chHes.indexOf(getGroundChunk(updatedIU))] < mapper.get(chunks.get(getNextIndex(getGroundChunk(updatedIU)))))
					{
						setSLL((WordIU)(updatedIU.grounds().get(0).grounds().get(0)),chunks.get(getNextIndex(getGroundChunk(updatedIU))),true);
					}
					else if(updatedIU.equals(getGroundChunk(updatedIU).getFirstSegment()))
					{
						((SysSegmentIU)getGroundChunk(updatedIU).getLastSegment()).copySynData((SysSegmentIU) copHes.getLastSegment());
					}
					else if(updatedIU.equals(getGroundChunk(updatedIU).getLastSegment()))
					{
						setSLL(getGroundChunk(updatedIU),getGroundChunk(updatedIU),true);

						// SynData wird kopiert - sollte iwan nicht mehr nötig sein
						SysSegmentIU itA = (SysSegmentIU)getGroundChunk(updatedIU).getWords().get(0).getFirstSegment();
						SysSegmentIU itB = (SysSegmentIU)copHes.getWords().get(0).getFirstSegment();
						do
						{
							itA.copySynData(itB);
							itA = (SysSegmentIU)itA.getNextSameLevelLink();
							itB = (SysSegmentIU)itB.getNextSameLevelLink();
						}while((!(itA.equals(getGroundChunk(updatedIU).getLastSegment()))) && (getGroundChunk(updatedIU).getSegments().contains(itA)));
					}
				}
			}
		};
	}
}
