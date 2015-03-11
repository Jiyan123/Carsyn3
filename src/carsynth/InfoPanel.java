package carsynth;

import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

import carsynth.DecisionFrame.SynChooser;

public class InfoPanel extends JPanel
{
	private static final long serialVersionUID = -2741670892652326896L;

	JLabel synthText = new JLabel();
	JPanel eastPanel = new JPanel();
	JPanel northPanel = new JPanel();
	JCheckBox chunkBox = new JCheckBox("Eigene Chunks");
	JCheckBox waitBox = new JCheckBox("Runner Wert");
	JTextArea area = new JTextArea(2,30);
	SpinnerNumberModel spinnerModel = new SpinnerNumberModel(3,0,1000,1);

	
	boolean ownChunks = false;
	boolean wait = false;
	
	public InfoPanel(SynChooser syn)
	{
		this.setLayout(new BorderLayout());
		northPanel.setLayout(new GridLayout(1,2));
		northPanel.add(synthText);
		JSpinner spinner = new JSpinner(spinnerModel);
		northPanel.add(spinner);
		this.add(northPanel,BorderLayout.NORTH);
		area.setText("100-100\n"+
				"80-80\n"+
				"50-50\n"+
				"20-20\n"+
				"5-Jetzt");
		JScrollPane scroll = new JScrollPane(area);
		this.add(scroll,BorderLayout.CENTER);
		eastPanel.setLayout(new GridLayout(2,1));
		
		eastPanel.add(chunkBox);
		eastPanel.add(waitBox);
		this.add(eastPanel,BorderLayout.EAST);
		
		calculateSynText(syn);
	}
	
	public List<String> getStringList()
	{
		ArrayList<String> ret = new ArrayList<String>();
		String op = area.getText();
		String commands[] = op.split("\n");
		for(String val : commands)
		{
			String tok[] = val.split("-");
			ret.add(tok[tok.length-1]);
		}
		return ret;
	}
	
	public ArrayList<Integer> getIntList() throws NumberFormatException
	{
		ArrayList<Integer> ret = new ArrayList<Integer>();
		String op = area.getText();
		String commands[] = op.split("\n");
		for(String val : commands)
		{
			String tok[] = val.split("-");
			ret.add(Integer.parseInt(tok[0]));
		}
		return ret;
	}
	
	public boolean isWait()
	{
		return waitBox.isSelected();
	}
	
	public boolean isOwnChunks()
	{
		return chunkBox.isSelected();
	}
	
	public void calculateSynText(SynChooser syn)
	{
		switch(syn)
		{
		case min:
			synthText.setText("SynthesisMinimum");
			break;
		case meter:
			synthText.setText("SynthesisMeter");
			break;
		case run:
			synthText.setText("SynthesisRunner");
			break;
		case wait:
			synthText.setText("SynthesisRunnerWait");
			break;
		default:
			synthText.setText("Undefined");
		}
	}
}
