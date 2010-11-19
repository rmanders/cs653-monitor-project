package hw2;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;

public class Server implements Runnable
{
    
    public static int MONITOR_PORT;
    public static int LOCAL_PORT;
    
    public Thread runner;
    protected ServerSocket socServer = null;
    protected String IDENT;
    protected String PASSWORD;

    public Server (int p, int lp, String name, String password)
    {
        this.IDENT = name;
        this.PASSWORD = password;
        try
        {
            socServer = new ServerSocket(p);
            MONITOR_PORT = p;
            LOCAL_PORT = lp;
            int i = 1;
        }
        catch (IOException e)
        {
            System.out.println(e);
            System.exit(1);
        }
    }

    public void start()
    {
        System.out.println("\nStarting up the Server...\n");
        if (runner == null)
        {
            runner = new Thread(this);
            runner.start();
        }
    }

    public void run()
    {
        try
        {
            int i = 1;
            for (;;)
            {
                Socket incoming =  socServer.accept();
                new ConnectionHandler(incoming,i,IDENT,PASSWORD).start();
                //Spawn a new thread for each new connection
                i++;
            }
        }
        catch (Exception e)
        {
            System.out.println("Server [run]: Error in Server: "  + e);
        }
    }
}

