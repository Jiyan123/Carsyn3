package carsynth;

import java.util.List;

import inpro.incremental.unit.IU;
import inpro.incremental.unit.SysSegmentIU;
import inpro.incremental.unit.WordIU;
import inpro.incremental.unit.IU.IUUpdateListener;
import inpro.incremental.unit.IU.Progress;

/**
 * Arbeitet wie SynthesisRunner nur bricht nach einer
 * Anzahl von "äh nun ja" ab und wartet auf einen
 * Meterwert, welcher weitere Ausgabe ermöglicht
 * @author jiyan
 *
 */
public class SynthesisRunnerWait extends SynthesisRunner
{
	/**
	 * Während des wartens darauf, dass die Synthese weitermacht,
	 * kann getOngoingSegment(WordIU) nicht das entsprende Segment
	 * holen, weil das "aktuelle" Segment "Completed" sein muss
	 * (muss sein, sonst hört es sich schlecht an, deshalb wird
	 * diese Referenz benutzt um getOngoingSegment(WordIU) zu sagen
	 * wo es weiter geht (wenn referedSys nicht null ist).
	 */
	private SysSegmentIU referedSys = null;
	
	/**
	 * gibt an wie oft in dem "ähm nun ja" verhangen werden soll, bis
	 * abgebrochen wird
	 */
	final private int numberHes;
	
	public SynthesisRunnerWait(String turn)
	{
		super(turn);
		numberHes = 3;
	}
	
	public SynthesisRunnerWait(String turn, int hes)
	{
		super(turn);
		numberHes = hes;
	}
	
	public SynthesisRunnerWait(String turn, List<Integer> counts, List<String> commands)
	{
		super(turn, counts,commands);
		numberHes = 3;
	}
	
	public SynthesisRunnerWait(String turn, int hes, List<Integer> counts, List<String> commands)
	{
		super(turn, counts,commands);
		numberHes = hes;
	}
	
	@Override
	protected IUUpdateListener getListener()
	{
		return new IUUpdateListener() {
			Progress previousProgress;
			int number = 0;
			@Override
			public void update(IU updatedIU)
			{
				if(updatedIU.getProgress() != previousProgress && updatedIU.getProgress() == Progress.ONGOING)
				{
					imDist[chHes.indexOf(getGroundChunk(updatedIU))] -= updatedIU.duration()*speed;
					if(imDist[chHes.indexOf(getGroundChunk(updatedIU))] < mapper.get(chunks.get(getNextIndex(getGroundChunk(updatedIU)))))
					{
						number = 0;
						setSLL((WordIU)(updatedIU.grounds().get(0).grounds().get(0)),chunks.get(getNextIndex(getGroundChunk(updatedIU))),true);
					}
					else if(updatedIU.equals(getGroundChunk(updatedIU).getFirstSegment()))
					{
						((SysSegmentIU)getGroundChunk(updatedIU).getLastSegment()).copySynData((SysSegmentIU) copHes.getLastSegment());
					}
					else if(updatedIU.equals(getGroundChunk(updatedIU).getLastSegment()))
					{
						number++;
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
				if(updatedIU.getProgress() != previousProgress && updatedIU.getProgress() == Progress.COMPLETED && updatedIU.equals(getGroundChunk(updatedIU).getLastSegment()))
				{
					if(number >= numberHes)
					{
						setSLL((WordIU)getGroundChunk(updatedIU),chunks.get(getNextIndex(getGroundChunk(updatedIU))),true);
						referedSys = (SysSegmentIU)chunks.get(getNextIndex(getGroundChunk(updatedIU))).getFirstSegment();
						number = 0;
						try
						{
							Thread.sleep((long)updatedIU.duration());
						}catch (InterruptedException e1)
						{
							e1.printStackTrace();
						}
						dispatcher.interruptPlayback();
						while(oldDist > mapper.get(chunks.get(getNextIndex(getGroundChunk(updatedIU)))))
						{
							try
							{
								Thread.sleep(100);
							}catch (InterruptedException e)
							{
								e.printStackTrace();
							}
						}
						referedSys = null;
						dispatcher.continuePlayback();
						System.out.println("Go on");
					}
				}
			}
		};
	}
	
	@Override
	protected SysSegmentIU getOngoingSegment(WordIU param)
	{
		if(referedSys != null)return referedSys;
		return super.getOngoingSegment(param);
	}
}
