package carsynth;


import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ST
{
	static String turn = null;
	static long dist;
	static double speed;
	static Date lastDate = null;
	public static String mutex; // Used as Mutex - not used for now
	static CarSynthesisAbstract run = new SynthesisRunner("Nach links abbiegen in");
	static final int FPS_MIN = 0;
	static final int FPS_MAX = 60;
	static final int FPS_INIT = 10; //initial frames per second
	static int val = FPS_INIT;
	static JTextField t = new JTextField("15");
	static JSlider PS = new JSlider(JSlider.HORIZONTAL,
			FPS_MIN, FPS_MAX, FPS_INIT);
	public static void main(String[] args) throws IOException
	{
		GridLayout fl = new GridLayout(1,3);
		CarVisualizer c = new CarVisualizer(150,run.getSpeechValues());
		JFrame frm = new JFrame();
		frm.setTitle("Visualizer");
		frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frm.setSize(800,600);
		//frm.setLocation(800,50);
		JPanel jp = new JPanel();
		jp.setLayout(new FlowLayout());
		jp.add(PS);
		JPanel h = new JPanel();h.add(c);h.setSize(new Dimension(100,600));
		jp.add(new JLabel("hallo Freund"));
		jp.add(new JLabel("hallo Freund"));
		frm.getContentPane().add(jp);
		frm.pack();
		
		frm.setVisible(true);
		//frm.add(c);
		
		//frm.add(fl);
		Socket echoSocket = null;
		//ObjectOutputStream out = null;
		ObjectInputStream in = null;
		lastDate = new Date();
		// Create socket connection with host address as localhost and port number with 38300
		//echoSocket = new Socket("localhost", 38300);
		System.out.println("Try Conn...");
		// Communicating with the server
		//JFrame jp = new JFrame();
		//jp.setSize(500, 500);
		//jp.setVisible(true);
		//jp.add(PS);
		PS.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider)e.getSource();
				if (!source.getValueIsAdjusting()) {
					int fps = (int)source.getValue();
					val = fps;
					//System.out.println(fps);
				}
			}
		});
		//jp.add(t);
		PS.setMajorTickSpacing(10);
		PS.setMinorTickSpacing(1);
		PS.setPaintTicks(true);
		PS.setPaintLabels(true);
		Date d = new Date();
		int m = 150;
		double proceed = 0;
		int ival = m;
		String ms = new String ("");
		while(true){
			Date dn = new Date();
			long diff = dn.getTime()-d.getTime();
			if(diff > 200)
			{
				proceed += ((diff/1000.0)*val);
				if(proceed > m)
				{
					ms = "0m";
				}
				else
				{
					if((int)(m-proceed) >= 0 && ival != (int)(m-proceed))
					{
						turn = "Turn left";
						ms = new String((int)(m-proceed) + "m");
						System.out.println((int)(m-proceed) + "m");
					}
					else
					{
						turn = "Turn left";
						if(proceed > m) ms = "0m";
						else ms = new String((int)(m-proceed) + "m");
					}
					ival = (int)(m-proceed);
					d = dn;
				}
			}
			//System.out.println("server>" + ms);
			{
				String newTurn = new String();
				if(giveTurn(newTurn,ms))
				{
					newTurn = new String(ms);
					turn = newTurn;
				}
				long newDist;
				Date newDate = new Date();
				if(isDist(ms) && lastDate != null && turn != null && newDate.getTime()-lastDate.getTime() != 0)
				{
					newDist = giveDist(ms);
					System.out.println(newDist);
					frm.repaint();
					c.repaint();
					c.setMeter((int)newDist);
					frm.repaint();
					speed = ((dist-newDist)*1000)/(newDate.getTime()-lastDate.getTime());
					lastDate = newDate;
					dist = newDist;
					if(newSpeekableDist(dist) && speed > 0.0)
					{
						run.speak(dist,val,turn);
					}
				}
				else if(isDist(ms) && lastDate == null)
				{
					lastDate = new Date();
				}
			}
		}
	}
	private static long giveDist(String ms)
	{
		return Integer.parseInt(ms.substring(0, ms.indexOf('m')));
	}
	private static boolean giveTurn(String s,String ms)
	{
		if(ms.equals("Go ahead")||
				ms.equals("Turn slightly left")||
				ms.equals("Turn left")||
				ms.equals("Turn sharply left")||
				ms.equals("Turn slightly right")||
				ms.equals("Turn right")||
				ms.equals("Turn sharply right")||
				ms.equals("Make uturn")||
				ms.equals("Keep left")||
				ms.equals("Keep right")) // TODO: all turns
		{
			return true;
		}
		return false;
	}
	private static boolean isDist(String ms)
	{
		return ms.matches("[0-9]*m");
	}
	private static boolean newSpeekableDist(long d)
	{
		return d <= 120;
	}
	static synchronized void schreibeNachricht(java.net.Socket socket, String nachricht) throws IOException {
		PrintWriter printWriter =
				new PrintWriter(
						new OutputStreamWriter(
								socket.getOutputStream()));
		printWriter.print(nachricht);
		printWriter.flush();
	}
	static synchronized String leseNachricht(java.net.Socket socket) throws IOException {
		BufferedReader bufferedReader =
				new BufferedReader(
						new InputStreamReader(
								socket.getInputStream()));
		char[] buffer = new char[200];
		int anzahlZeichen = bufferedReader.read(buffer, 0, 200); // blockiert bis Nachricht empfangen
		String nachricht = new String(buffer, 0, anzahlZeichen);
		return nachricht;
	}
}

