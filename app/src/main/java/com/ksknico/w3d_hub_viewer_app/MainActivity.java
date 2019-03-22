package com.ksknico.w3d_hub_viewer_app;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


import android.util.ArrayMap;
import android.util.Log;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {



    private byte[] receiveData = new byte[1024];
    private ArrayMap<String, String[]> str = new ArrayMap<String, String[]>();
    private DatagramPacket receivePacket = new DatagramPacket(receiveData, 1024);
    private ArrayMap<String, DatagramSocket> socketMap = new ArrayMap<String, DatagramSocket>();
    private ArrayMap<String, DatagramPacket> packetMap = new ArrayMap<String, DatagramPacket>();
    private final int delay = 1000*10; // 1000 = 1 second
    public  Handler mHandler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Start of the application
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        Reads the xml file called ip.xml with a XmlPullParser and maps each servername to the
        corresponding server port and IP. The tags in the xml file need the correct order:
        server -> ip -> port -> note
        */
        //TODO: Make use of "note" for notifications
        ArrayList<ArrayList<String>> serverList = new ArrayList<ArrayList<String>>();
        XmlPullParser xpp = getResources().getXml(R.xml.ip);
        ArrayList<String> singleServer = new ArrayList<String>();
        try {
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (xpp.getEventType() == XmlPullParser.START_TAG) {
                    // Log.d("HERE", xpp.getName());
                    if (xpp.getName().equals("server")) {
                        singleServer.add(xpp.getAttributeValue(0));
                    }
                    else if (xpp.getName().equals("ip")) {
                        singleServer.add(xpp.nextText());
                    }
                    else if (xpp.getName().equals("port")) {
                        singleServer.add(xpp.nextText());
                    }
                    else if (xpp.getName().equals("note")) {
                        singleServer.add(xpp.nextText());
                    }
                } else if (xpp.getEventType() == XmlPullParser.END_TAG) {
                    if (xpp.getName().equals("server")) {
                        serverList.add(singleServer);
                        singleServer = new ArrayList<>();
                        xpp.next();
                        continue;
                    }
                }
                xpp.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException i) {
            i.printStackTrace();
        }


        final ArrayList<ArrayList<String>> localList = serverList;

        // Starts a runnable that starts the AsyncTask over and over again
        Runnable runnable = new Runnable() {
            public void run() {
                if (isConnectedToInternet(getApplicationContext())) {
                    new ReceiverService().execute(localList);
                }
                mHandler.postDelayed(this, delay);
            }
        };
        runnable.run();
    }

    // Checks for internet connection
    private boolean isConnectedToInternet(Context applicationContext){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            // There are no active networks.
            Toast.makeText(getApplicationContext(), "No Internet connetion!", Toast.LENGTH_LONG).show();
            return false;
        } else {
            return true;
        }
    }

    public class ReceiverService extends AsyncTask<ArrayList<ArrayList<String>>, Void, ArrayMap<String, String[]>> {

        DatagramPacket receivePacket = new DatagramPacket(receiveData, 1024);

        public ArrayMap<String, String[]> doInBackground(ArrayList<ArrayList<String>>... getter) {
            ArrayList<ArrayList<String>> serverList = getter[0];

            // Initializes the socketMap, server name -> DatagramSocket
            if (socketMap.size()== 0) {
                for (ArrayList<String> server : serverList) {
                    socketMap.put(server.get(0), initializeDatagramSocket(server));
                }
            }
            // Initializes the packetMap, server name -> DatagramPacket
            for (ArrayList<String> server : serverList) {
                packetMap.put(server.get(0), initializeDatagramPacket(server));
            }


            try {
                // send the right packet over the right socket
                for (String name : socketMap.keySet()) {
                    (socketMap.get(name)).send(packetMap.get(name));
                }

                // receive each packet
                for (String name : socketMap.keySet()) {
                    (socketMap.get(name)).receive(receivePacket);
                    String check[] = (new String(receivePacket.getData(), 0, receiveData.length)).split("\\\\");
                    if (check[1].equals("gamename")) {
                        str.put(name, check);
                    }
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, 1024);
                }
            }
            catch (IOException i) {
                i.printStackTrace();
            }
            return str;
        }

        // Gets a DatagramSocket for the corresponding server
        public DatagramSocket initializeDatagramSocket(ArrayList<String> server) {
            try {
                return new DatagramSocket(Integer.parseInt(server.get(2)));
            } catch (IOException e) {
                Log.e("Socket", "Couldn't initialize datagram socket!");
                e.printStackTrace();
                return null;
            }
        }

        // Gets a DatagramPacket for the corresponding server
        public DatagramPacket initializeDatagramPacket(ArrayList<String> server) {
            try {
                // status need to be send to acquire info
                byte[] data = "\\status\\".getBytes("UTF-8");
                return new DatagramPacket(data, data.length, InetAddress.getByName(server.get(1)),
                        Integer.parseInt(server.get(2)));
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return null;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
        }

        // Manipulates the table to use the data in the ArrayMap
        public void onPostExecute(ArrayMap<String, String[]> result) {
            TableLayout table = findViewById(R.id.tableview);
            for(int i = 0, j = table.getChildCount(); i < j; i++) {
                TableRow row = (TableRow) table.getChildAt(i);
                String text = (String) ((TextView) row.getChildAt(0)).getText();
                // Somewhat hardcoded
                ((TextView)row.getChildAt(1)).setText(result.get(text)[10]);
                ((TextView)row.getChildAt(2)).setText(result.get(text)[14] + "/" + result.get(text)[16]);
            }
        }
    }
}

