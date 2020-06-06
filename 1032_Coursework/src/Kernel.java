import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.StringTokenizer; 
public class Kernel {
	
	 static String dir = System.getProperty("user.dir") ;
	 private static processSchedular method = new processSchedular();
	    private static Hashtable vars = new Hashtable();
	    private static JavaFileSystem fs = new JavaFileSystem();
	 
	    public static void main(String[] args) throws java.io.IOException { 
			CPU Cpu = new CPU();
			Cpu.start();
			Cpu.initialise();
			  processSchedular secondthread = new processSchedular();
			secondthread.start();
			kernel( secondthread);
	}
	 
	 public static void kernel (processSchedular secondthread) throws IOException {
		 
		 String commandLine;
			ArrayList<String> history  = new ArrayList<String>();
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			Path currentRelativePath = Paths.get("");
			
			
			while (true) {
				try{
					
				
				System.out.print("jsh>");
				commandLine = console.readLine();
				ArrayList inputs = new ArrayList<String>();
				dir = currentRelativePath.toAbsolutePath().toString();
				
				for (String section: commandLine.split("\\s+")){
					inputs.add(section);
				}
				if (commandLine.equals("")) 
					continue;
				
				else if (commandLine.equals("history")) {
					for (int i = 0 ; i<history.size(); i++)
						System.out.println(i + " " + history.get(i));
					continue;
				}
				
				ProcessBuilder pb = new ProcessBuilder(inputs);
				history.add(commandLine);

				if (((String) inputs.get(0)).equalsIgnoreCase("Use")){
				
					if ((((String)inputs.get(1)).toLowerCase()).contains(("Process").toLowerCase())){
						GUI welcome = new GUI();
						welcome.GUIcalled();
					
					}
						
		
				}
	
				//Directories
					if (inputs.get(0).equals("cd")) {
						
						if (((String) inputs.get(1)).startsWith("/"))
							dir = (String) inputs.get(1);
						else
							dir = dir + "/" + inputs.get(1);
						try {
							dir = new URI(dir).normalize().getPath(); 
						}
						catch (URISyntaxException e) {
							e.printStackTrace();
						}
						System.out.println(dir);
						File f = new File(dir);
						if (f.exists())
							pb.directory(f);
						else
							System.out.println("Directory " + dir + " does not exist!");
					}
					else if (inputs.get(0).equals("ls")){
						Files.list(Paths.get(".")).forEach(System.out::println);
						
					}
					else {
						File f = new File(dir);
						if (f.exists())
							pb.directory(f);
						else
							System.out.println("Directory " + dir + " does not exist!");
						}
				
	                String target = null;
	                int equals = commandLine.indexOf('=');
	                if (equals > 0) {
	                    target = commandLine.substring(0,equals).trim();
	                    commandLine = commandLine.substring(equals+1).trim();
	                }

	        
	                StringTokenizer cmds = new StringTokenizer (commandLine);
	                String cmd = cmds.nextToken();

	               
	                int result = 0;
	                if (cmd.equalsIgnoreCase("formatDisk")
	                        || cmd.equalsIgnoreCase("format"))
	                {
	                    int arg1 = nextValue(cmds);
	                    int arg2 = nextValue(cmds);
	                    result = fs.formatDisk(arg1,arg2);
	                }
	                else if (cmd.equalsIgnoreCase("shutdown")) {
	                    result = fs.shutdown();
	                }
	                else if (cmd.equalsIgnoreCase("create")) {
	                    result = fs.create();
	                }
	                else if (cmd.equalsIgnoreCase("open")) {
	                    result = fs.open(nextValue(cmds));
	                } 
	                else if (cmd.equalsIgnoreCase("inumber")) {
	                    result = fs.inumber(nextValue(cmds));
	                } 
	                else if (cmd.equalsIgnoreCase("read")) {
	                    int arg1 = nextValue(cmds);
	                    int arg2 = nextValue(cmds);
	                    result = readTest(arg1,arg2);
	                } 
	                else if (cmd.equalsIgnoreCase("write")) {
	                    int arg1 = nextValue(cmds);
	                    String arg2 = cmds.nextToken();
	                    int arg3 = nextValue(cmds);
	                    result = writeTest(arg1,arg2,arg3);
	                } 
	                else if (cmd.equalsIgnoreCase("seek")) {
	                    int arg1 = nextValue(cmds);
	                    int arg2 = nextValue(cmds);
	                    int arg3 = nextValue(cmds);
	                    result = fs.seek(arg1,arg2,arg3);
	                } 
	                else if (cmd.equalsIgnoreCase("close")) {
	                    result = fs.close(nextValue(cmds));
	                } 
	                else if (cmd.equalsIgnoreCase("delete")) {
	                    result = fs.delete(nextValue(cmds));
	                } 
	                else if (cmd.equalsIgnoreCase("quit")) {
	                    System.exit(0);
	                } 
	                else if (cmd.equalsIgnoreCase("vars")) {
	                    for (Enumeration e = vars.keys(); e.hasMoreElements(); ) {
	                        Object key = e.nextElement();
	                        Object val = vars.get(key);
	                        System.out.println("\t" + key + " = " + val);
	                    }
	                    continue;
	                }
	                else if (cmd.equalsIgnoreCase("help")) {
	                    help();
	                    continue;
	                } 
	                if (target == null)
	                    System.out.println("    Result is " + result);
	                else {
	                    vars.put(target,new Integer(result));
	                    System.out.println("    " + target + " = " + result);
	                }

	            }
	            // Handler for Integer.parseInt(...)
	            catch (NumberFormatException e) {
	                System.out.println("Incorrect argument type");
	            }
	            // Handler for nextToken()
	            catch (NoSuchElementException e) {
	                System.out.println("Incorrect number of elements");
	            }
	            catch (IOException e) {
	                System.err.println(e);
	            }
				
				}
			}
	 
				
	 
	
	static private int nextValue(StringTokenizer cmds)
    {
        String arg = cmds.nextToken();
        Object val = vars.get(arg);
        return
            (val == null) ?  Integer.parseInt(arg) : ((Integer)val).intValue();
    }
	 private static void help() {
	        System.out.println ("\tformatDisk size iSize");
	        System.out.println ("\tshutdown");
	        System.out.println ("\tcreate");
	        System.out.println ("\topen inum");
	        System.out.println ("\tinumber fd");
	        System.out.println ("\tread fd size");
	        System.out.println ("\twrite fd pattern size");
	        System.out.println ("\tseek fd offset whence");
	        System.out.println ("\tclose fd");
	        System.out.println ("\tdelete inum");
	        System.out.println ("\tquit");
	        System.out.println ("\tvars");
	        System.out.println ("\thelp");
	    }
	 private static int readTest(int fd, int size) {
	        byte[] buffer = new byte[size];
	        int length;

	        // Fill buffer with junk first
	        for (int i = 0; i < size; i++)
	            buffer[i] = (byte) '*';
	        length = fs.read(fd, buffer);
	        for (int i = 0; i < length; i++) 
	            showchar(buffer[i]);
	        if (length != -1) System.out.println();
	        return length;
	    }

	 private static int writeTest (int fd, String str, int size) {
	        byte[] buffer = new byte[size];

	        for (int i = 0; i < buffer.length; i++) 
	            buffer[i] = (byte)str.charAt(i % str.length());

	        return fs.write(fd, buffer);
	    }
	 private static void showchar(byte b) {
	        if (b < 0) {
	            System.out.print("M-");
	            b += 0x80;                // Make b positive
	        }
	        if (b >= ' ' && b <= '~') {
	            System.out.print((char)b);
	            return;
	        }
	        switch (b) {
	            case '\0': System.out.print("\\0"); return;
	            case '\n': System.out.print("\\n"); return;
	            case '\r': System.out.print("\\r"); return;
	            case '\b': System.out.print("\\b"); return;
	            case 0x7f: System.out.print("\\?"); return;
	            default:   System.out.print("^" + (char)(b + '@')); return;
	        }
	    }
}






