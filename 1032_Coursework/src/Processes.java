
public class Processes {
	
	private int serialNo;
	private double arrivalTime;
	private int CPUBurst;
	private int priority;
	
	public Processes(int serialNo, double arrivalTime, int CPUBurst, int priority ){
		super();
		this.serialNo=serialNo;
		this.arrivalTime=arrivalTime;
		this.CPUBurst=CPUBurst;
		this.priority=priority;
		
	}
	public int getSerialNo() {
		return serialNo;
	}
	public double getArriveTime() {
		return arrivalTime;
	}
	
	public int getCPUBurst() {
		return CPUBurst;
	}
	
	public int getPriority() {
		return priority;
	}
	
}
