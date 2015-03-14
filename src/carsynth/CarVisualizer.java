package carsynth;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.util.Collection;

import javax.swing.JPanel;

/**
 * Klasse zur Visualisierung des fahrenden Objektes, welches sich dem Ziel
 * nähert und die Ansagepunkte sind markiert.
 * @author jiyan
 *
 */
public class CarVisualizer extends JPanel
{
	private static final long serialVersionUID = 3010237297040489561L;

	private Image carPic;
	private int meter;
	private Collection<Integer> subPoints;
	
	private static final int carX = 34;
	private static final int carY = 77;
	
	public CarVisualizer(int maxVal, Collection<Integer> sPoints)
	{
		meter = maxVal;
		subPoints = sPoints;
		setImage(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("car.png")));
		repaint();
	}
	
	public void setCollection(Collection<Integer> params)
	{
		subPoints = params;
	}
	
	public void setImage(Image image) {
		this.carPic = image.getScaledInstance(carX, carY,Image.SCALE_SMOOTH);
		repaint();
	}

	public void setMeter(int val)
	{
		meter = val;
	}
	
	public void paint(Graphics g)
	{
		super.paint(g);
		
		for(Integer locMeter : subPoints)
		{
			g.drawLine(0,meterToPointY(locMeter),carX,meterToPointY(locMeter));
		}
		
		
		if(carPic!=null)g.drawImage(carPic, 0, meterToPointY(meter), this);
	}
	
	public Dimension getPreferredSize()
	{
		return new Dimension(carX,600);//new Dimension(carPic.getWidth(this), carPic.getHeight(this));
	}
	
	private int meterToPointY(int meterParam)
	{
		return meterParam*5;
	}
}