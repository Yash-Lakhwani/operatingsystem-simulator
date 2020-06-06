import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

public class processSchedular extends Thread implements ActionListener{
	
public  void run(){
	while (true){
		
	}
}


@Override
public void actionPerformed(ActionEvent e) {
	GUI example = new GUI();
	example.response.setText("Check IDE Console");
	String inputLine = GUI.inputtext.getText().toString();
	inputLine.trim();
	
	File inputfile = new File(inputLine);
	Scanner scanner2 = null;
	try {
		scanner2 = new Scanner(inputfile);
	} catch (FileNotFoundException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	
	if (!(inputLine.startsWith("/"))){
		inputLine = "/" + inputLine;	
	}
	if (!(inputLine.endsWith(".txt"))){
		inputLine = inputLine + ".txt";
	}
String filecontent = ""; 
	while (scanner2.hasNextLine()){
		filecontent = filecontent.concat(scanner2.nextLine() + "\n");
	}
	String [] lines = filecontent.split("\n");
	int numofprocesses = Integer.parseInt(lines[0]);
	Processes processesarray[] = new Processes[numofprocesses];
	
	for (int i=1; i<=(numofprocesses);i++){
		String [] parameter = lines[i].split("\\s+");
		double [] parameters = new double[4] ;
		for (int z=0; z<4 ; z++){
			
		
		double temp = Double.parseDouble(parameter[z]);
			parameters[z]=temp;
		}
		
		int SRNO = (int)parameters[0];
		double arrivalTime = parameters[1];
		int CPUBurst = (int)parameters[2];
		int priority = (int)parameters[3];		
		
		Processes current = new Processes(SRNO, arrivalTime, CPUBurst, priority);
		processesarray[(i-1)] = current;
	}
	
	//System.out.println("This simulator allows only the FCFS algorithym"); 
		algorithms input = new algorithms() ;
		System.out.println("This simulator allows only the FCFS algorithym"); 
	    		try {
					input.FCFS(processesarray,inputLine);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}	
	}
}

