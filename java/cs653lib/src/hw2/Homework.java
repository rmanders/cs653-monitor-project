package hw2;

class Homework
{
    /** Name of the monitor host */
    public static String MONITOR_NAME = "helios.ececs.uc.edu";

    /** The monitor port number */
    public static int MONITOR_PORT = 8180;
    
    /** The local server port number (random) */
    public static int HOST_PORT = 20000 +(int)(Math.random()*1000);

    /** TODO: The maximum number of ??? */
    public static int MAX = 5;
    
    /** The local active client instance */
    ActiveClient ac;

    /** The local server instance */
    Server s;

    // <editor-fold defaultstate="collapsed" desc="Constructor">
    /**
     * Constructor
     * @param name The name to log into the monitor with
     * @param password The password to log into the monitor with
     */
    public Homework(String name, String password) {
        System.out.println("Project Begin:\n\tMonitor: "
                + MONITOR_NAME + " random port: "
                + HOST_PORT + " monitor port: " + MONITOR_PORT);

        ac = new ActiveClient(
                MONITOR_NAME,
                MONITOR_PORT,
                HOST_PORT,
                0,
                name,
                password);
        s = new Server(
                HOST_PORT,
                HOST_PORT,
                name,
                password);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Main">
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java Homework monitor monitor-port ident");
        } else {
            MONITOR_NAME = args[0];
            MONITOR_PORT = Integer.parseInt(args[1]);
            Homework hw = new Homework(args[2], "-----");
            hw.ac.start(); //Start the Active Client
            hw.s.start();  //Start the Server
        }
    }
    // </editor-fold>
}

//B0DO009U5A0CWF0I679