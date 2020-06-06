import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
public class algorithms {
	
	public void FCFS (Processes processesarray[] ,String commandLine) throws IOException{;
		HashMap <Double,Integer> check = new HashMap <Double,Integer>();
		HashMap <Double,Double> CPUtime = new HashMap <Double,Double>();
		HashMap <Double,Integer> prioritymap = new HashMap <Double,Integer>();
		double arrivalTime[] = new double[processesarray.length] ; 
		double CPUBurst[] = new double[processesarray.length] ; 
		int priority[]= new int [processesarray.length];
		int serialNo[] = new int[processesarray.length];
		String order = "";
		int totaltime=0;
		double turnaroundtime[]=new double[processesarray.length];
		double wturnaround[]=new double[processesarray.length];
		
		
		GUI example2 = new GUI();
		for (int i =0; i<processesarray.length; i++){
			
			
			arrivalTime[i]=processesarray[i].getArriveTime();
			serialNo[i]= processesarray[i].getSerialNo();
			CPUBurst[i]=processesarray[i].getCPUBurst();
			priority[i]=processesarray[i].getPriority();
			totaltime +=  processesarray[i].getCPUBurst();
			check.put(arrivalTime[i],serialNo[i]);
		
			CPUtime.put(arrivalTime[i], CPUBurst[i]);
			prioritymap.put(arrivalTime[i], priority[i]);
		}
		Arrays.sort(arrivalTime);
		
		for (int z = 0; z< arrivalTime.length; z++){
			System.out.println("Process "+ (check.get(arrivalTime[z])));
			order= order.concat("Process " + check.get(arrivalTime[z]));
		}
		
		System.out.println("Text files have been made and assigned to the output directory given"); 
		
		String commandLine1 = example2.outputtext.getText().toString();
		
		if (!(commandLine1.startsWith("/"))){
			commandLine1 = "/" + commandLine1;	
		}
		if (!(commandLine1.endsWith("/"))){
			commandLine1 = commandLine1 + "/";
		}
		
		
		String Configuration = "";
		Configuration = Configuration + "# The name of schedulting algorithm: \n";
		Configuration = Configuration + "FCFS \n";
		Configuration = Configuration + "# Input file path: \n";
		Configuration = Configuration + commandLine +  "\n";
		Configuration = Configuration + "# Output file path: \n";
		Configuration = Configuration + commandLine1 ;
		Configuration = Configuration + "FCFS \n";
		
		
		String Result = "";
		double finish_time = arrivalTime[0] ;
		Result = Result + "# process_id, arrival_time, finish_time, run_time, priority, turnaround_time, weighted_turnaround_time  \n";
		double totaltt = 0;
		double totalwtt = 0;
		double avgturnaroundtime;
		double avgwturnaroundtime;
		
		for (int z = 0; z< arrivalTime.length; z++){
			if(z==0){
				turnaroundtime[z]=CPUBurst[z];
				
			}
			else{
				turnaroundtime[z]=   CPUtime.get(arrivalTime[z])+turnaroundtime[z-1] -(arrivalTime[z]-arrivalTime[0]);
			}
			wturnaround[z]=(double)turnaroundtime[z]/CPUtime.get(arrivalTime[z]);
			finish_time = finish_time +CPUtime.get(arrivalTime[z]); 
			Result = Result + (check.get(arrivalTime[z]) + " "  + arrivalTime[z]+ " "  + finish_time + " " + CPUtime.get(arrivalTime[z])+ " "  + prioritymap.get(arrivalTime[z]) + " " + turnaroundtime[z]  + " "+   wturnaround[z]) ;      
			Result = Result + "\n";
				totaltt+=turnaroundtime[z];
				totalwtt+=wturnaround[z];
		
	}
		avgturnaroundtime=totaltt/processesarray.length;
		avgwturnaroundtime=totalwtt/processesarray.length;
		Result = Result + "\n";
		Result = Result + "\n";
		Result = Result + "# performance evaluation metrics  \n";
		Result = Result +( "Average turnaround time = "+avgturnaroundtime);
		Result = Result + "\n";
		Result = Result +( "Average waited turnaround = "+avgwturnaroundtime);
		
		String commandLine1conf = commandLine1 +"Configuration.txt";
		String commandLine1res = commandLine1 +"Result.txt";
		FileWriter writer1 = new FileWriter(commandLine1conf);
		writer1.write(Configuration);
		writer1.close();
		FileWriter writer2 = new FileWriter(commandLine1res);
		writer2.write(Result);
		writer2.close();
	}
	
}
