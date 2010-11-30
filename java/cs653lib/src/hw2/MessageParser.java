package hw2;

import java.util.*;
import java.lang.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.math.*;

public class MessageParser {
    //Monitor Handling Declarations

    /** The host name of the monitor one is connected to (TODO: move */
    public static String HOSTNAME;
    static String COOKIE = null;
    public static int IsVerified;
    static String InputFileName = "Input.dat";
    static String ResourceFileName = "Resources.dat";
    static String MyKey;

    /** Maximum number of commands */
    int COMMAND_LIMIT = 25;
    /** TODO: What is CType used for? Something to do with client type */
    public int CType;
    /** Used to write to a specified output stream */
    PrintWriter out = null;
    /** An input buffer for holding received messages */
    BufferedReader in = null;
    protected String mesg;
    private String sentmessage;
    private String filename;
    StringTokenizer t;
    String IDENT = "Skipper";
    String PASSWORD = "franco";
    String PPCHECKSUM = "";
    int HOST_PORT;

    //File I/O Declarations
    BufferedReader fIn = null;
    PrintWriter fOut = null;
    String[] cmdArr = new String[COMMAND_LIMIT];
    String MonitorKey;
    String first;
    ObjectInputStream oin = null;
    ObjectOutputStream oout = null;

    // <editor-fold defaultstate="collapsed" desc="Constructor (Default)">
    public MessageParser() {
        filename = "passwd.dat";
        GetIdentification();
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Constructor (2)">
    /**
     *
     * @param ident User login
     * @param password User password
     */
    public MessageParser(String ident, String password) {
        filename = ident + ".dat";
        PASSWORD = password;
        IDENT = ident;

        // Loads identity information from a file
        GetIdentification();
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="GetMonitorMessage">
    /**
     * Reads a message from the remote monitor.
     * TODO: Does this decrypt the message?
     * @return Plaintext monitor message
     */
    public String GetMonitorMessage() {
        String sMesg = "";
        String decrypt = "";

        try {
            // Read a line from the input buffer
            String temp = in.readLine();

            first = temp; // 1st
            sMesg = temp;
            decrypt = temp;

            //After IDENT has been sent-to handle partially encrypted msg group
            while (!(decrypt.trim().equals("WAITING:"))) {

                // Read the incoming monitor message
                temp = in.readLine();
                sMesg = sMesg.concat(" ");
                decrypt = temp;
                sMesg = sMesg.concat(decrypt);
            }

            //sMesg now contains the Message Group sent by the Monitor
        } catch (IOException e) {
            System.out.println("MessageParser [getMonitorMessage]: error "
                    + "in GetMonitorMessage:\n\t" + e + this);
            sMesg = "";
        } catch (NullPointerException n) {
            sMesg = "";
        } catch (NumberFormatException o) {
            System.out.println("MessageParser [getMonitorMessage]: number "
                    + "format error:\n\t" + o + this);
            sMesg = "";
        } catch (NoSuchElementException ne) {
            System.out.println("MessageParser [getMonitorMessage]: no such "
                    + "element exception occurred:\n\t" + this);
        } catch (ArrayIndexOutOfBoundsException ae) {
            System.out.println("MessageParser [getMonitorMessage]: AIOB "
                    + "EXCEPTION!\n\t" + this);
            sMesg = "";
        }
        return sMesg;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="GetNextCommand">
    //Handling Cookie and PPChecksum
    public String GetNextCommand(String mesg, String sCommand) {
        try {
            String sDefault = "REQUIRE";
            if (!sCommand.equals("")) {
                sDefault = sCommand;
            }

            StringTokenizer st = new StringTokenizer(mesg, " :\n");

            //Search for the REQUIRE Command
            String temp = st.nextToken();
            while (!temp.trim().equals(sDefault.trim())) {
                temp = st.nextToken();
            }
            temp = st.nextToken();
            System.out.println("MessageParser [getNextCommand]: returning:\n\t"
                    + temp);

            //returns what the monitor wants
            return temp;
        } catch (NoSuchElementException e) {
            return null;
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Login">
    /**
     * Attempt to log into the monitor
     *
     * @return {@link boolean} true if login was successful, false otherwise
     */
    public boolean Login() {
        boolean loginSucceeded = false;

        if( this.CType == 0)
        {
            loginSucceeded = doClientLogin();
        }
        else
        {
            loginSucceeded = doServerLogin();
        }

        System.out.println("Login succeded? : " + loginSucceeded);
        return loginSucceeded;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="doClientLogin">
    /**
     * Handles the active client login to the monitor server
     * @return
     */
    protected boolean doClientLogin() {
        boolean success = false;
        try {

            // (1) Expect an IDENT command here
            mesg = GetMonitorMessage();
            String nxtCmd = GetNextCommand(mesg, "REQUIRE");
            if (!nxtCmd.equals("IDENT")) {
                System.out.println("Login Error: Expected IDENT from monitor,"
                        + " got " + nxtCmd);
                return success;
            }
            //System.out.println(nxtCmd);
            Execute(nxtCmd);

            // (2) Get the next message group
            mesg = GetMonitorMessage();
            //System.out.println(monMsg);
            nxtCmd = GetNextCommand(mesg, "REQUIRE");
            //System.out.println(nxtCmd);

            if (nxtCmd.equals("PASSWORD")) {
                Execute("PASSWORD");

                mesg = GetMonitorMessage();
                nxtCmd = GetNextCommand(mesg,"RESULT");
                if( nxtCmd.trim().equals("PASSWORD") ) {
                    COOKIE = GetNextCommand(mesg,"PASSWORD").trim();
                    WritePersonalData(PASSWORD, COOKIE);
                }
                else {
                    System.out.println("LOGIN: Expected cookie, got none");
                    System.out.println("\t" + mesg);
                    return success;
                }

                // Now expect HOST_PORT command
                nxtCmd = GetNextCommand(mesg,"REQUIRE");
                if( nxtCmd.trim().equals("HOST_PORT") ) {
                    Execute("HOST_PORT");
                }
                else {
                    System.out.println("LOGIN: Expected HOST_PORT, got: " + 
                            nxtCmd);
                    return false;
                }
                return true;
            }
            else if( nxtCmd.equals("ALIVE")) 
            {
                Execute("ALIVE");

                mesg = GetMonitorMessage();
                nxtCmd = GetNextCommand(mesg,"REQUIRE");
                if( nxtCmd.trim().equals("HOST_PORT"))
                {
                    Execute("HOST_PORT");
                }

                success = true;
                return success;
            }
            else {
                System.out.println("Login Error: Expected PASSWORD OR ALIVE"
                        + "from monitor, got " + nxtCmd);
                return success;
            }
        } catch (NullPointerException n) {
            System.out.println("MessageParser [Login]: null pointer error "
                    + "at login:\n\t" + n);
            success = false;
        }
        return success;
    }
    // </editor-fold>

    private boolean doServerLogin() {
        try {
            mesg = GetMonitorMessage();
            String nxtCmd = GetNextCommand(mesg,"REQUIRE");

            if( !nxtCmd.trim().equals("IDENT") ) {
                System.out.println("SLOGIN: Expected IDENT, got: " + nxtCmd);
                return false;
            }
            boolean succeeded = Execute("IDENT");
            if( !succeeded ) {
                return false;
            }

            mesg = GetMonitorMessage();
            nxtCmd = GetNextCommand(mesg,"REQUIRE");

            if( !nxtCmd.trim().equals("ALIVE") ) {
                System.out.println("SLOGIN: Expected IDENT, got: " + nxtCmd);
                return false;
            }

            succeeded = Execute("ALIVE");
            if( !succeeded ) {
                return false;
            }

            mesg = GetMonitorMessage();
            nxtCmd = GetNextCommand(mesg,"REQUIRE");
            if( !nxtCmd.trim().equals("QUIT") ) {
                System.out.println("SLOGIN: Expected QUIT, got: " + nxtCmd);
                return false;
            }
            succeeded = Execute("QUIT");
            if( !succeeded ) {
                return false;
            }
            
            mesg = GetMonitorMessage();
            System.out.println(mesg);
            //nxtCmd = GetNextCommand(mesg,"RESULT");
            //if( !nxtCmd.trim().equals("QUIT") ) {
            //    System.out.println("SLOGIN: Expected Result QUIT, got: " + nxtCmd);
            //    return false;
            //}

            return true;
        } catch (Exception e) {
            System.out.println(e);
        }
        return false;
    }

    // <editor-fold defaultstate="collapsed" desc="Execute (1)">
    /**
     * Handle Directives and Execute appropriate commands with one argument
     *
     * @param sentmessage
     * @param arg
     * @return
     */
    public boolean Execute(String sentmessage, String arg) {
        boolean succeeded = false;
        try {
            if (sentmessage.trim().equals("PARTICIPANT_HOST_PORT")) {
                sentmessage = sentmessage.concat(" ");
                sentmessage = sentmessage.concat(arg);
                SendIt(sentmessage);
                succeeded = true;
            }
        } catch (IOException e) {
            System.out.println("IOError:\n\t" + e);
            succeeded = false;
        } catch (NullPointerException n) {
            System.out.println("Null Error has occured");
            succeeded = false;
        }
        return succeeded;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Execute (2)">
    //Handle Directives and Execute appropriate commands
    public boolean Execute(String sentmessage) {
        boolean succeeded = false;
        try {
            if (sentmessage.trim().equals("IDENT")) {
                sentmessage = sentmessage.concat(" ");
                sentmessage = sentmessage.concat(IDENT);
                SendIt(sentmessage);
                succeeded = true;
            } else if (sentmessage.trim().equals("PASSWORD")) {
                sentmessage = sentmessage.concat(" ");
                sentmessage = sentmessage.concat(PASSWORD);
                SendIt(sentmessage.trim());
                succeeded = true;
            } else if (sentmessage.trim().equals("HOST_PORT")) {
                sentmessage = sentmessage.concat(" ");
                sentmessage = sentmessage.concat(HOSTNAME);//hostname
                sentmessage = sentmessage.concat(" ");
                sentmessage = sentmessage.concat(String.valueOf(HOST_PORT));
                SendIt(sentmessage);
                succeeded = true;
            } else if (sentmessage.trim().equals("ALIVE")) {
                sentmessage = sentmessage.concat(" ");
                sentmessage = sentmessage.concat(COOKIE);
                SendIt(sentmessage);
                succeeded = true;
            } else if (sentmessage.trim().equals("QUIT")) {
                SendIt(sentmessage);
                succeeded = true;
            } else if (sentmessage.trim().equals("SIGN_OFF")) {
                SendIt(sentmessage);
                succeeded = true;
            } else if (sentmessage.trim().equals("GET_GAME_IDENTS")) {
                SendIt(sentmessage);
                succeeded = true;
            } else if (sentmessage.trim().equals("PARTICIPANT_STATUS")) {
                SendIt(sentmessage);
                succeeded = true;
            } else if (sentmessage.trim().equals("RANDOM_PARTICIPANT_HOST_PORT")) {
                SendIt(sentmessage);
                succeeded = true;
            }
        } catch (IOException e) {
            System.out.println("IOError:\n\t" + e);
            succeeded = false;
        } catch (NullPointerException n) {
            System.out.println("Null Error has occured");
            succeeded = false;
        }
        return succeeded;
    }// </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="SentIt">
    public void SendIt(String message) throws IOException {
        try {
            System.out.println("MessageParser [SendIt]: sent:\n\t" + message);
            out.println(message);
            if (out.checkError() == true) {
                throw (new IOException());
            }
            out.flush();
            if (out.checkError() == true) {
                throw (new IOException());
            }
        } catch (IOException e) {
            System.out.println(e);
        } //Bubble the Exception upwards
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="ProcessExtraMessages">
    //In future send parameters here so that diff commands are executed
    public boolean ProcessExtraMessages() {
        boolean success = false;
        System.out.println("MessageParser [ExtraCommand]: received:\n\t"
                + mesg.trim());

        if ((mesg.trim().equals("")) || (mesg.trim() == null)) {
            mesg = GetMonitorMessage();
            System.out.println("MessageParser [ExtraCommand]: received (2):\n\t"
                    + mesg.trim());
        }

        String id = GetNextCommand(mesg, "");

        if (id == null) { // No Require, can Launch Free Form Commands Now
            if (Execute("PARTICIPANT_STATUS")) { //Check for Player Status
                mesg = GetMonitorMessage();
                success = true;
                try {
                    SaveResources(mesg);  //Save the data to a file
                    SendIt("SYNTHESIZE WEAPONS");
                    mesg = GetMonitorMessage();
                    SendIt("SYNTHESIZE COMPUTERS");
                    mesg = GetMonitorMessage();
                    SendIt("SYNTHESIZE VEHICLES");
                    mesg = GetMonitorMessage();
                    if (Execute("PARTICIPANT_STATUS")) { //Check for Player Status
                        mesg = GetMonitorMessage();
                        success = true;
                        SaveResources(mesg);//Save the data to a file
                    }
                } catch (IOException e) {
                }
            }
        } else {
            mesg = GetMonitorMessage();
            System.out.println("MessageParser [ExtraCommand]: failed "
                    + "extra message parse");
        }
        return success;
    }
    // </editor-fold>

    public void MakeFreeFlowCommands() throws IOException {
    }

    // <editor-fold defaultstate="collapsed" desc="SaveResources">
    public void SaveResources(String res) throws IOException {
        System.out.println("MessageParser [SaveResources]:");
        try {  // If an error occurs then don't update the Resources File
            String temp = GetNextCommand(res, "COMMAND_ERROR");
            if ((temp == null) || (temp.equals(""))) {
                fOut = new PrintWriter(new FileWriter(ResourceFileName));
                t = new StringTokenizer(res, " :\n");
                try {
                    temp = t.nextToken();
                    temp = t.nextToken();
                    temp = t.nextToken();
                    System.out.println("MessageParser [SaveResources]: got "
                            + "token before write: " + temp);
                    for (int i = 0; i < 20; i++) {
                        fOut.println(temp);
                        fOut.flush();
                        temp = t.nextToken();
                    }
                } catch (NoSuchElementException ne) {
                    temp = "";
                    fOut.close();
                }
            }
            fOut.close();
        } catch (IOException e) {
            fOut.close();
        }
    }
    // </editor-fold>

    public void HandleTradeResponse(String cmd) throws IOException {
    }

    public boolean IsTradePossible(String TradeMesg) {
        return false;
    }

    public int GetResource(String choice) throws IOException {
        return 0;
    }

    public void HandleWarResponse(String cmd) throws IOException {
    }

    public void DoTrade(String cmd) throws IOException {
    }

    public void DoWar(String cmd) throws IOException {
    }

    // <editor-fold defaultstate="collapsed" desc="ChangePassword">
    public void ChangePassword(String newpassword) {
        GetIdentification(); //Gives u the previous values of Cookie and Password
        String quer = "CHANGE_PASSWORD " + PASSWORD + " " + newpassword;
        UpdatePassword(quer, newpassword);
    }
    // </editor-fold>

    //Update Password
    //throws IOException
    public void UpdatePassword(String cmd, String newpassword) {
    }

    // <editor-fold defaultstate="collapsed" desc="GetIdentification">
    /**
     * Gets Password and Cookie from 'passwd.dat' file
     */
    public final void GetIdentification() {
        File file = null;
        Scanner scanner = null;
        try {
            file = new File(filename);
            scanner = new Scanner(new FileReader(file));

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.equals("PASSWORD")) {
                    PASSWORD = scanner.nextLine().trim();
                } else if (line.equals("COOKIE")) {
                    COOKIE = scanner.nextLine().trim();
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="WritePersonalData">
    // Write Personal data such as Password and Cookie
    public boolean WritePersonalData(String Passwd, String Cookie) {
        boolean success = false;
        PrintWriter pout = null;
        try {
            if ((Passwd != null) && !(Passwd.equals(""))) {
                pout = new PrintWriter(new FileWriter(filename));
                pout.println("PASSWORD");
                pout.println(Passwd); //(PASSWORD);
            }
            if ((Cookie != null) && !(Cookie.equals(""))) {
                pout.println("COOKIE");
                pout.flush();
                pout.println(Cookie);
                pout.flush();
            }
            pout.close();
            success = true;
        } catch (IOException e) {
            pout.close();
            return success;
        } catch (NumberFormatException n) {
        }
        return success;
    }
    // </editor-fold>

    //Check whether the Monitor is Authentic
    public boolean Verify(String passwd, String chksum) {
        return false;
    }

    public boolean IsMonitorAuthentic(String MonitorMesg) {
        return false;
    }
}
