package hw2;

import java.io.*;
import java.net.*;


public class ActiveClient extends MessageParser implements Runnable
{
    /** The name of the monitor host to connect to */
    public static String MonitorName;

    /** The monitor port number to connect to */
    public static int MONITOR_PORT;

    /** Local port TODO: verify */
    public static int LOCAL_PORT;

    /** TODO: What is SleepMode used for? */
    public int SleepMode;
    
    /** Thread class that runs the active client */
    Thread runner = null;

    /** The socket connection to the monitor */
    Socket toMonitor = null;
    
    /** Interval after which a new Active Client is started */
    int DELAY = 90000;

    /** TODO: what is prevTime used for? */
    long prevTime;

    /** TODO what is present used for? */
    long present;

    // <editor-fold defaultstate="collapsed" desc="Constructor (default)">
    public ActiveClient()
    {
        super("[no-name]", "[no-password]");
        MonitorName = "";
        toMonitor = null;
        MONITOR_PORT = 0;
        LOCAL_PORT = 0;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Constrcutor (2)">
    /**
     *
     * @param mname Monitor Host Name
     * @param p Monitor Port number
     * @param lp Local port number
     * @param sm sleep mode integer
     * @param name User login name
     * @param password User password
     */
    public ActiveClient(String mname, int p, int lp, int sm,
            String name, String password) {
        // Initialize the Message Parser superclass with username and password
        super(name, password);
        try {
            SleepMode = sm;
            MonitorName = mname;
            MONITOR_PORT = p;
            LOCAL_PORT = lp;
        } catch (NullPointerException n) {
            System.out.println("Active Client [Constructor]: TIMEOUT Error: " + n);
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="start">
    /**
     * Launches a new thread that runs this instance
     */
    public void start() {
        if (runner == null) {
            runner = new Thread(this);
            runner.start();
        }
    }
    // </editor-fold>
  
    /**
     * Implements the run method for the runnable interface
     */
    public void run()
    {
        // I guess this means to execute as long as this thread is open?
        while(Thread.currentThread() == runner)
        {
            try
            {
                // Print connection attempt message
                System.out.print("Active Client: trying monitor: " +
                        MonitorName + " port: "+MONITOR_PORT+"...");
                
                // Try to establish open socket connection with the monitor
                toMonitor = new Socket(MonitorName, MONITOR_PORT);

                // Indicate the socket was opened sucessfully
                System.out.println("completed.");
                
                // Establish reading and writing buffers for the socket
                out = new PrintWriter(toMonitor.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(toMonitor.getInputStream()));
                
                // Save the monitors hostname
                HOSTNAME = toMonitor.getLocalAddress().getHostName();

                // TODO: still no idea what ctype is for? (Client type?)
                CType = 0;

                // WTF?
                HOST_PORT = LOCAL_PORT;
                
                if ( !Login() )
                {
                    if (IsVerified == 0)
                    {
                        //System.exit(1);
                    }
                }

                /*
	    System.out.println("***************************");
	    if (Execute("GET_GAME_IDENTS")) {
	       String msg = GetMonitorMessage();
	       System.out.println("ActiveClient [GET_GAME_IDENTS]:\n\t"+msg);
	    }
	    if (Execute("RANDOM_PARTICIPANT_HOST_PORT")) {
	       String msg = GetMonitorMessage();
	       System.out.println("ActiveClient [RANDOM_PARTICIPANT_HOST_PORT]:\n\t"+msg);
	    }
	    if (Execute("PARTICIPANT_HOST_PORT", "FRANCO")) {
	       String msg = GetMonitorMessage();
	       System.out.println("ActiveClient [PARTICIPANT_HOST_PORT]:\n\t"+msg);
	    }
	    if (Execute("PARTICIPANT_STATUS")) {
	       String msg = GetMonitorMessage();
	       System.out.println("ActiveClient [PARTICIPANT_STATUS]:\n\t"+msg);
	    }
	    ChangePassword(PASSWORD);
	    System.out.println("Password:"+PASSWORD);
*/
            toMonitor.close(); 
            out.close(); 
            in.close();
            try { runner.sleep(DELAY); } catch (Exception e) {}
                            
         } catch (UnknownHostException e) {
         } catch (IOException e) {
            try { 
               toMonitor.close();  
               //toMonitor = new Socket(MonitorName,MONITOR_PORT);
            } catch (IOException ioe) {
            } catch (NullPointerException n) { 
               try {
                  toMonitor.close();  
                  //toMonitor = new Socket(MonitorName,MONITOR_PORT);
               } catch (IOException ioe) {}
            }
         }
      }
   }
}

