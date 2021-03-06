package carsynth;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Die Start Klasse, besteht aus einem CarVisualizer und einem InfoPanel,
 * es werden die Synthesen bereitgestellt und je nachdem, was bei der
 * Ausführung einer Synthese benötigt wird, wird bei Simulation der
 * MeterSimulator ausgeführt, oder der OsmandConnector bei Verbindung zu
 * Osmand.
 * @author jiyan
 *
 */
public class DecisionFrame extends JFrame
{
	private static final long serialVersionUID = 111297707793705887L;
	public CarSynthesisAbstract run = null;
	CarVisualizer carVis = null;
	final OsmandConnector osmConn = new OsmandConnector();
	final String testString = "Nach links in";
	int speed = 10; // in m/s
	
	public enum SynChooser {min, meter, run, wait};
	SynChooser choosenSyn = SynChooser.min;
	
	JPanel mainPanel = new JPanel();
	InfoPanel midPanel = new InfoPanel(choosenSyn);
	
	boolean bRun = false;
	
	public DecisionFrame()
	{
		this.setTitle("Synthesen-Auswahl");
		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		mainPanel.setLayout( new java.awt.BorderLayout() );
		JSlider slider = new JSlider(JSlider.HORIZONTAL,
		0, 60, speed);
		slider.setMinorTickSpacing(5);
		slider.setMajorTickSpacing(10);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				JSlider source = (JSlider)e.getSource();
				if (!source.getValueIsAdjusting())
				{
					speed = (int)source.getValue();
				}
			}
		});
		mainPanel.add(slider,java.awt.BorderLayout.PAGE_START);
		JPanel eastPanel = new JPanel();
		eastPanel.setLayout(new java.awt.GridLayout(4, 1));
		JButton buttonSynth1 = new JButton("SynthesisMinimum");
		JButton buttonSynth2 = new JButton("SynthesisMeter");
		JButton buttonSynth3 = new JButton("SynthesisRunner");
		JButton buttonSynth4 = new JButton("SynthesisRunnerWait");
		
		buttonSynth1.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				choosenSyn = SynChooser.min;
				midPanel.calculateSynText(choosenSyn);
			}
			
		});
		buttonSynth2.addActionListener(new ActionListener()
		{
			
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				choosenSyn = SynChooser.meter;
				midPanel.calculateSynText(choosenSyn);
			}
			
		});
		buttonSynth3.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				choosenSyn = SynChooser.run;
				midPanel.calculateSynText(choosenSyn);
			}
			
		});
		buttonSynth4.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				choosenSyn = SynChooser.wait;
				midPanel.calculateSynText(choosenSyn);
			}
			
		});
		
		eastPanel.add(buttonSynth1);
		eastPanel.add(buttonSynth2);
		eastPanel.add(buttonSynth3);
		eastPanel.add(buttonSynth4);
		mainPanel.add(eastPanel,java.awt.BorderLayout.EAST);
		
		//StartButtons
		JPanel southPanel = new JPanel();
		southPanel.setLayout(new java.awt.FlowLayout());
		JButton simButton = new JButton("Simulation");
		simButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				synchronized(this)//if(!bRun)
				{
					if(!bRun)
					{
						bRun = true;
						run = giveSynthInfo();
						if(run != null)
						{
							new Thread(new Runnable()
							{
								MeterSimulator meterSim = new MeterSimulator(150);

								@Override
								public void run()
								{
									long meterVal;
									carVis.setCollection(run.getSpeechValues());
									while(true)
									{
										meterVal = meterSim.getMeter(speed);
										meterVal = meterVal <= 0 ? 0 : meterVal;
										System.out.println(meterVal);
										carVis.setMeter((int)meterVal);
										carVis.repaint();
										if(meterVal < 120 && speed > 0.0)
										{
											run.speak(meterVal,speed,testString);
											if(!run.isSpeaking())
											{
												bRun = false;
												break;
											}
										}
									}
								}
							}).start();
						}
						else bRun = false;
					}
				}
			}
		});
		JButton osmButton = new JButton("Osmand");
		osmButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				synchronized(this)//if(!bRun)
				{
					if(!bRun)
					{
						bRun = true;
						
						osmConn.connect();
						osmConn.getConnValues();
						run = giveSynthInfo(osmConn.getTurn());
						
						if(run != null)
						{
							carVis.setCollection(run.getSpeechValues());
							
							new Thread(new Runnable()
							{
								@Override
								public void run()
								{
									osmConn.setChanged(false);
									while(true)
									{
										osmConn.getConnValues();
										final long meterVal = osmConn.getMeter() <= 0 || osmConn.isChanged() ?
												0 : osmConn.getMeter();
										System.out.println("Meter: " + meterVal);
										carVis.setMeter((int)meterVal);
										carVis.repaint();
										
										if(osmConn.isChanged())
										{
											for(int i = 0; i < 4;i++)System.out.println("--------------");
										}
										System.out.println(meterVal + " " + osmConn.getSpeed());
										if(meterVal < 120 && osmConn.getSpeed() > 0.0)
										{
											System.out.println("SPEAK");
											
											run.speak(meterVal,osmConn.getSpeed(),osmConn.getTurn());
											if(!run.isSpeaking()&&osmConn.isChanged())
											{
												System.out.println("Ende vom sprechen");
												run = giveSynthInfo(osmConn.getTurn());
												osmConn.setChanged(false);
												//bRun = false;
												//break;
											}
										}
										else if(!run.isSpeaking() && osmConn.isChanged())
										{
											run = giveSynthInfo(osmConn.getTurn());
											osmConn.setChanged(false);
										}
									}
									
									//bRun = false;
								}
							}).start();
						}
						//else 
						bRun = false;
					}
				}
			}
		});

		southPanel.add(simButton);
		southPanel.add(osmButton);
		mainPanel.add(southPanel,java.awt.BorderLayout.SOUTH);
		mainPanel.add(midPanel,java.awt.BorderLayout.CENTER);
		
		// Starte mit keinen Meterwerten, sondern hole diese später
		ArrayList<Integer> meterCommands = new ArrayList<Integer>();
		carVis = new CarVisualizer(50, meterCommands);
		mainPanel.add(carVis,java.awt.BorderLayout.WEST);
		this.getContentPane().add(mainPanel);
		
		pack();
		this.setVisible(true);
	}
	
	private CarSynthesisAbstract giveSynthInfo()
	{
		return giveSynthInfo(testString);
	}
	
	private CarSynthesisAbstract giveSynthInfo(String turn)
	{
		if(midPanel.isOwnChunks())
		{
			try
			{
				switch(choosenSyn)
				{
				case min:
					return new SynthesisMinimum(turn,midPanel.getIntList(),midPanel.getStringList());
				case meter:
					return new SynthesisMeter(turn,midPanel.getIntList(),midPanel.getStringList());
				case run:
					return new SynthesisRunner(turn,midPanel.getIntList(),midPanel.getStringList());
				case wait:
					if(midPanel.isWait())
					{
						return new SynthesisRunnerWait(turn,midPanel.getWaitNumber(),midPanel.getIntList(),midPanel.getStringList());
					}
					else
					{
						return new SynthesisRunnerWait(turn, midPanel.getIntList(), midPanel.getStringList());
					}
				default:
					return null;
				}
			}
			catch(NumberFormatException e)
			{
				JOptionPane.showMessageDialog(null, "Das Eingegebene Format ist ungültig, " +
						"eventuell gibt es auch ein Windowsspezifisches Zeilenumbruchsproblem, " +
						"verwende das Format:\n" +
						"100-hundert\n" +
						"80-achtzig\n" +
						"50-fünfzig\n" +
						"20-zwanzig\n" +
						"5-hier");
				return null;
			}
		}
		else
		{
			switch(choosenSyn)
			{
			case min:
				return new SynthesisMinimum(turn);
			case meter:
				return new SynthesisMeter(turn);
			case run:
				return new SynthesisRunner(turn);
			case wait:
				if(midPanel.isWait())
				{
					return new SynthesisRunnerWait(turn, midPanel.getWaitNumber());
				}
				else
				{
					return new SynthesisRunnerWait(turn);
				}
			default:
				return null;
			}
		}
	}
	
	public static void main(String[] argv)
	{
		new DecisionFrame();
	}
}
