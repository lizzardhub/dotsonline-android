package com.owllabs.iter.dotsonline;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

/**
 * Created by Saga on 01.11.2016.
 */

class NetworkManagerThread extends Thread {
    private GameSurface gameSurface = null;
    private boolean running;
    private String host = "dotsonline-webarticle.rhcloud.com";
    private int port = 80;
    private Socket portal;
    private BufferedReader portal_in;
    private PrintWriter portal_out;
    private int READ_SIZE = 256;


    protected boolean received = false;
    protected boolean receiving = false;
    protected Point receive = new Point(true);
    protected Queue<String> send;


    public NetworkManagerThread(GameSurface gameSurface)
    {
        super();
        this.gameSurface = gameSurface;
        send = new LinkedList<String>();
    }

    public void setRunning(boolean flag) {
        running = flag;

        if (!running && !portal.isClosed()) {
            try {
                portal.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run()
    {
        while (running) {
            try {
                portal = new Socket();
                portal.connect(new InetSocketAddress(InetAddress.getByName(host), port));
                portal_in = new BufferedReader(new InputStreamReader(portal.getInputStream()));
                portal_out = new PrintWriter(portal.getOutputStream());
            } catch (Exception e) {
                //System.out.println("Network connect error");
                e.printStackTrace();
            }
            if (send.size() > 0) {
                try {
                    String request = send.remove();
                    portal_out.print(request);
                    portal_out.flush();
                } catch (Exception e) {
                    //System.out.println("Network send error");
                    e.printStackTrace();
                }
            } else if (receiving) {
                try {
                    //System.out.println("Receiving...");
                    String request = "GET /" + Integer.toString(gameSurface.current_player) + " HTTP/1.1\r\nHost: dotsonline-webarticle.rhcloud.com\r\n\r\n";
                    portal_out.print(request);
                    portal_out.flush();
                    
                    char[] c = new char[READ_SIZE];
                    portal_in.read(c, 0, READ_SIZE);
                    String text = "";

                    int i = 0;
                    while (i < READ_SIZE && c[i] != '@' && c[i] != '#') {
                        i++;
                    }
                    if (i < READ_SIZE && c[i] == '@') {
                        i++;
                        while (i < READ_SIZE && c[i] != '#' && c[i] != '\n') {
                            text += Character.toString(c[i]);
                            i++;
                        }
                    }

                    if (text.length() > 0) {
                        String[] xy = text.split("_");
                        receive = new Point(Integer.valueOf(xy[0]), Integer.valueOf(xy[1]));
                        received = true;
                        receiving = false;
                    } else {
                        //System.out.println("No data from server, try again");
                    }
                } catch (Exception e) {
                    //System.out.println("Network receive error");
                    e.printStackTrace();
                }
            }

            try
            {
                portal.close();
                this.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            };
        }
    }

    void send(String s) {
        send.add("GET /" + s + " HTTP/1.1\r\nHost: " + host + "\r\n\r\n");
    }

    void send(Point point) {
        send.add("GET /" + Integer.toString(gameSurface.current_player) + Integer.toString(point.x) + "_" + Integer.toString(point.y) + " HTTP/1.1\r\nHost: " + host + "\r\n\r\n");
    }

    void receive() {
        receiving = true;
        received = false;
    }
}
