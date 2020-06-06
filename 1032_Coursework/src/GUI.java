import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class GUI {
	
	static JPanel panel;
	static JFrame Frame;
	static JLabel inputLabel;
	static JTextField inputtext;
	static JLabel outputlabel;
	static JTextField outputtext;
	static JButton submit;
	static JLabel response;
		

	public void GUIcalled() throws IOException{
		 panel = new JPanel();
		 Frame = new JFrame();
		Frame.setSize(500,225);
		Frame.setLocationRelativeTo(null);
		Frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Frame.setResizable(true);
	
		Frame.add(panel);
		
		panel.setLayout(null);
		
		 inputLabel = new JLabel("Input Directory");
		inputLabel.setBounds(10,20,200,25);
		panel.add(inputLabel);
		
		
		 inputtext= new JTextField();
		inputtext.setBounds(170,20,300,25);
		panel.add(inputtext);
		
		 outputlabel = new JLabel("Output folder Directory");
		outputlabel.setBounds(10,50,200,25);
		panel.add(outputlabel);
		
		outputtext= new JTextField();
		outputtext.setBounds(170,50,300,25);
		panel.add(outputtext);
		
		 submit = new JButton("Simulate");
		submit.setBounds(200,100,80,25);
		submit.addActionListener(new processSchedular());
		panel.add(submit);
		
		 response = new JLabel("");
		response.setBounds(200,150,200,25);
		panel.add(response);
		
		
		Frame.setVisible(true);
	
	}
	

}
