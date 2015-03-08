package carsynth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import inpro.apps.SimpleMonitor;
import inpro.audio.AudioUtils;
import inpro.audio.DispatchStream;
import inpro.incremental.unit.ChunkIU;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.PhraseIU;
import inpro.incremental.unit.SegmentIU;
import inpro.incremental.unit.SysSegmentIU;
import inpro.incremental.unit.WordIU;
import inpro.incremental.unit.IU.IUUpdateListener;
import inpro.incremental.unit.IU.Progress;
import inpro.synthesis.MaryAdapter5internal;
import inpro.synthesis.hts.IUBasedFullPStream;
import inpro.synthesis.hts.VocodingAudioStream;

public abstract class CarSynthesisAbstract
{
	/**
	 * Hat am Ende die Struktur:
	 * Turnansage - 100 - 80 - ... - jetzt
	 */
	protected List<ChunkIU> chunks = new ArrayList<ChunkIU>();
	/**
	 * Hat am Ende die Struktur (zumindest in der Klasse mit allen Funktionalitäten):
	 * "ähm nun ja" - meter - ... - meter(letztes nicht benutzt) - ähm nun ja"
	 */
	protected List<ChunkIU> chHes = new ArrayList<ChunkIU>();
	
	/**
	 * Enthält die verschiedenen ChunkIUs mit ihren zugehörigen Meterwerten
	 */
	protected SortedMap<ChunkIU,Integer> mapper = new TreeMap<ChunkIU,Integer>();
	
	final protected float minStretch = 0.4f;
	final protected float maxStretch = 1.5f;
	final protected float maxTurnStretch = 0.5f; // We NEED to hear the turn Info (cant be 1/10000.0 unlike maxStretch)
	
	protected Double dur; // Die 3 hätte ich gerne als Parameter übergeben aber ging nicht
	protected Boolean bMin;
	protected Boolean bMax;
	
	protected double speed;
	
	protected long oldDist = 10000000;
	protected boolean isSpeaking = false;
	
	/** audio stream that we dispatch to */
	protected DispatchStream dispatcher;
	
	/** set up the synthesis */
	public CarSynthesisAbstract(String turn)
	{
		// get an output object that plays back on the speakers/headphone
		dispatcher = SimpleMonitor.setupDispatcher();
		setInst(turn,120);
		((SysSegmentIU)(chunks.get(chunks.size()-1).getLastSegment())).addUpdateListener(new IUUpdateListener()
		{

			@Override
			public void update(IU updatedIU)
			{
				if(updatedIU.getProgress() == Progress.COMPLETED)
				{
					isSpeaking = false;
				}
			}
		});
	}
	
	abstract protected void setInst(String turn, long meter);
	
	/**
	 * Die Methode welche aufgerufen wird wenn eine neue Information über
	 * den aktuellen Meterwert verfügbar ist. Diese Methode kümmer sich auch darum,
	 * dass die entsprechenden Werte korrekt sind (speed darf nicht 0 sein, distance
	 * muss kleiner werden)
	 * @param dist Die Distanz (sollte nicht kleiner werden)
	 * @param sp Die Geschwindigkeit in m/s (nicht null - sonst wäre stretchChunk unsinn)
	 * @param turn Die Abbiegeinformation (sollte gleich bleiben)
	 */
	public void speak(long dist, double sp, String turn)
	{
		dist = dist > 0 ? dist : 0;
		System.out.println(sp + " speak " + dist);
		if((!isSpeaking) && (dist < oldDist || dist == 0) && sp > 0.0) // not good enough but for now
		{
			oldDist = dist;
			speed = sp;
			if(dist <= 120)
			{
				isSpeaking = true;
				planMeters(dist,sp,(SysSegmentIU)chunks.get(0).getFirstSegment(),true,false);
				dispatcher.playStream(AudioUtils.get16kAudioStreamForVocodingStream(new VocodingAudioStream(new IUBasedFullPStream(chunks.get(0).getWords().get(0)),MaryAdapter5internal.getDefaultHMMData(), true)), true);
			}
		}
		else if((dist < oldDist || dist == 0) && sp > 0.0)
		{
			oldDist = dist;
			speed = sp;
			SysSegmentIU ongoingSeg = null;
			while(ongoingSeg == null)
			{
				if(!isSpeaking) return;
				for(ChunkIU c : chunks)
				{
					ongoingSeg = getOngoingSegment(c);
					if(ongoingSeg != null)
					{
						break;
					}
				}
				if(ongoingSeg == null)
				{
					for(ChunkIU c : chHes)
					{
						ongoingSeg = getOngoingSegment(c);
						if(ongoingSeg != null)
						{
							break;
						}
					}
				}
			}
			planMeters(dist,sp,ongoingSeg,false,false);
		}
	}
	
	/**
	 *  Die Methode welche von den Synthese implementiert werden muss zu Planen
	 *  des Verlaufes der Synthese
	 * @param dist Distanz in Metern
	 * @param sp Geschwindigkeit
	 * @param s Das SegmentIU in dem wir uns befinden
	 * @param theory Ob das aktuelle SegmentIU auch ausgeführt wird
	 * @param hesIsSet  nutzlos
	 */
	protected abstract void planMeters(long dist, double sp, SysSegmentIU s, boolean theory, boolean hesIsSet);
	
	/**
	 * Anderer ansatz, funktioniert auch bei IUs mit schleifen
	 * (siehe getProgress von IUs die nicht vom Typ SysSegmentIU sind)
	 * @param param Die WordIU / ChunkIU deren Ongoing Segment gesucht wird
	 * @return Die OngoingSysSegmentIU, oder null
	 */
	protected SysSegmentIU getOngoingSegment(WordIU param)
	{
		for(SegmentIU u : param.getSegments())
		{
			if(((SysSegmentIU)u).isOngoing())
			{
				return (SysSegmentIU)u;
			}
		}
		return null;
	}
	
	
	/*private SysSegmentIU getOngoingSegment(IU param)
	{
		if(param != null && param.isOngoing())
		{
			if(param instanceof SysSegmentIU)
			{
				return (SysSegmentIU)param;
			}
			else return getOngoingSegment(param.getOngoingGroundedIU());
		}
		else
		{
			return null;
		}
	}*/
	
	/**
	 * Setzt einen SLL von der einen WordIU(ChunkIU) zu der anderen
	 * @param from
	 * @param to
	 * @param setTopSLL ob auch noch der TopSLL gesetzt werden soll
	 */
	protected void setSLL(WordIU from, WordIU to, boolean setTopSLL)
	{
		if(!(from.getLastSegment().getNextSameLevelLinks().contains(to.getFirstSegment())))
		{
			from.getLastSegment().addNextSameLevelLink(to.getFirstSegment());
		}
		if(setTopSLL)from.getLastSegment().setAsTopNextSameLevelLink(to.getFirstSegment().toPayLoad());
	}
	/**
	 * 
	 * @param eine grounded IU von einer ChunkIU
	 * @return Die "grounds" ChunkIU wenn sie existiert, ansonsten null
	 */
	
	protected ChunkIU getGroundChunk(IU i)
	{
		if(i == null)
		{
			return null;
		}
		else if(i instanceof ChunkIU && !(i instanceof PhraseIU)) // Habe übersehen dass PhraseIU ChunkIU extended
		{
			return (ChunkIU)i;
		}
		else
		{
			for(IU u : i.grounds())
			{
				if(getGroundChunk(u) != null)
				{
					return getGroundChunk(u);
				}
			}
		}
		return null;
	}
	
	/**
	 * Gibt zu einem meter Wert die entsprechende ChunkIU zurück
	 * @param meter
	 * @return Die ChunkIU die zu dem Meterwert passt
	 */
	protected ChunkIU getNextMeterChunk(int meter)
	{
		int minV = 10000;
		ChunkIU ret = null;
		for(ChunkIU it : chunks)
		{
			int oldV = minV;
			minV = mapper.get(it) < minV && mapper.get(it) >= meter ? mapper.get(it) : minV;
			if(oldV != minV)
			{
				ret = it;
			}
		}
		return ret;
	}
	
	/**
	 * Stretcht ein gewisses Chunk und gibt Informationen über dieses stretching
	 * @param dist Die Distanz auf der man sich befindet
	 * @param nextDist Die Distanz bis zum nächsten Zwischenpunkt
	 * @param actualSegment Das momentante SegmentIU
	 * @param dur Wird am ende die Duration des restlichen abzuspielenden Teils haben
	 * @param bMin Wenn es sehr kurz gestretcht wurde ist dies true
	 * @param bMax Wenn es sehr lang gestretcht wurde ist dies true
	 * @param theory
	 */
	protected void stretchChunk(long dist, long nextDist, SysSegmentIU actualSegment, boolean theory)
	{
		ChunkIU actualChunk = getGroundChunk(actualSegment);
		dur = 0.0;
		for(SegmentIU it:actualChunk.getSegments())
		{
			if(it.startTime()>actualSegment.startTime() || (theory && it.startTime()>=actualSegment.startTime()))
			{
				dur += ((SysSegmentIU)it).originalDuration();
			}
		}
		
		bMax = false;
		bMin = false;
		if(speed != 0.0 && dur != 0.0)
		{
			double t = (dist-nextDist) / (speed); // Zeit bis zum Ende
			double fx = t/dur; // Faktor zum stretchen
			if(fx > maxStretch || dist > mapper.get(actualChunk))
			{
				System.out.println("maxStart");
				bMax = true;
				fx = maxStretch;
			}
			else if(fx < minStretch || dist < nextDist)
			{
				bMin = true;
				fx = minStretch;
			}
			
			dur = 0.0; // soll am Ende die neue duration haben
			for(SegmentIU it:actualChunk.getSegments())
			{
				if(it.startTime()>actualSegment.startTime() || (theory && it.startTime()>=actualSegment.startTime()))
				{
					((SysSegmentIU)it).stretchFromOriginal(fx);
					dur+=it.duration();
				}
			}
		}
	}
	
	protected void groundChunk(ChunkIU i)
	{
		for(IU it : i.groundedIn())
		{
			it.ground(i);
		}
	}
	
	/**
	 * @return Die Meterwerte bei denen eine Ausgabe erfolgt
	 */
	public Collection<Integer> getSpeechValues()
	{
		return mapper.values();
	}
	
	public boolean isSpeaking()
	{
		return isSpeaking;
	}
}
