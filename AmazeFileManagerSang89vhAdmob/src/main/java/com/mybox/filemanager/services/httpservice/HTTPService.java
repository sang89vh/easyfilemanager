package com.mybox.filemanager.services.httpservice;

/**
 * Created by yashwanthreddyg on 09-06-2016.
 */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mybox.filemanager.R;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;

public class HTTPService extends Service implements Runnable {
    private Context mContext;
    public static final int DEFAULT_PORT_HTTP = 8888;
    public static final String PORT_PREFERENCE_KEY = "httpPort";

    public static int getDefaultPortFromPreferences(SharedPreferences preferences) {
        try {
            return preferences.getInt(PORT_PREFERENCE_KEY, DEFAULT_PORT_HTTP);
        } catch (ClassCastException ex) {
            Log.e("HTTPService", "Default port preference is not an int. Resetting to default.");
            changeHTTPServerPort(preferences, DEFAULT_PORT_HTTP);

            return DEFAULT_PORT_HTTP;
        }
    }

    public static void changeHTTPServerPort(SharedPreferences preferences, int port) {
        preferences.edit()
                .putInt(PORT_PREFERENCE_KEY, port)
                .apply();
    }

    private static final String TAG = HTTPService.class.getSimpleName();
    /**
     * TODO: 25/10/16 This is ugly
     */
    private static int port = 8888;

    // Service will (global) broadcast when server start/stop
    static public final String ACTION_STARTED = "com.mybox.filemanager.services.httpservice.HTTPReceiver.HTTPSERVER_STARTED";
    static public final String ACTION_STOPPED = "com.mybox.filemanager.services.httpservice.HTTPReceiver.HTTPSERVER_STOPPED";
    static public final String ACTION_CHANGE_PASSWORD = "com.mybox.filemanager.services.httpservice.HTTPReceiver.HTTPSERVER_CHANGE_PASSWORD";
    static public final String ACTION_FAILEDTOSTART = "com.mybox.filemanager.services.httpservice.HTTPReceiver.HTTPSERVER_FAILEDTOSTART";

    // RequestStartStopReceiver listens for these actions to start/stop this server
    static public final String ACTION_START_HTTPSERVER = "com.mybox.filemanager.services.httpservice.HTTPReceiver.ACTION_START_HTTPSERVER";
    static public final String ACTION_STOP_HTTPSERVER = "com.mybox.filemanager.services.httpservice.HTTPReceiver.ACTION_STOP_HTTPSERVER";

    protected boolean shouldExit = false;

    private HTTPServer server;
    protected static Thread serverThread = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mContext = this;
        shouldExit = false;
        int attempts = 10;
        while (serverThread != null) {
            if (attempts > 0) {
                attempts--;
                HTTPService.sleepIgnoreInterupt(1000);
            } else {
                return START_STICKY;
            }
        }

//        if (intent != null && intent.getStringExtra("password") != null) {
//
//            password = intent.getStringExtra("password");
//            isPasswordProtected = intent.getBooleanExtra("isPasswordProtected",false);
//        }
        serverThread = new Thread(this);
        serverThread.start();

        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void run() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);


        port = getDefaultPortFromPreferences(preferences);

        server = new HTTPServer(port, mContext);
        server.setPassword(getHTTPPassword(preferences));
        server.setPasswordProtected(getActiveHTTPPassword(preferences));
        try {

            server.start();
            sendBroadcast(new Intent(HTTPService.ACTION_STARTED));
        } catch (Exception e) {
            sendBroadcast(new Intent(HTTPService.ACTION_FAILEDTOSTART));
        }


    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy() Stopping server");
        shouldExit = true;
        if (serverThread == null) {
            Log.w(TAG, "Stopping with null serverThread");
            return;
        }
        serverThread.interrupt();
        try {
            serverThread.join(10000); // wait 10 sec for server thread to finish
        } catch (InterruptedException e) {
        }
        if (serverThread.isAlive()) {
            Log.w(TAG, "Server thread failed to exit");
        } else {
            Log.d(TAG, "serverThread join()ed ok");
            serverThread = null;
        }
        if (server != null) {
            server.stop();
            sendBroadcast(new Intent(HTTPService.ACTION_STOPPED));
        }
        Log.d(TAG, "HTTPServerService.onDestroy() finished");
    }

    //Restart the service if the app is closed from the recent list
    @Override
    public void onTaskRemoved(Intent rootIntent) {

        super.onTaskRemoved(rootIntent);

        Intent restartService = new Intent(getApplicationContext(), this.getClass());
        restartService.setPackage(getPackageName());
        PendingIntent restartServicePI = PendingIntent.getService(
                getApplicationContext(), 1, restartService, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getApplicationContext()
                .getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 2000, restartServicePI);
    }


    public static boolean isRunning() {
        // return true if and only if a server Thread is running
        if (serverThread == null) {
            Log.d(TAG, "Server is not running (null serverThread)");
            return false;
        }
        if (!serverThread.isAlive()) {
            Log.d(TAG, "serverThread non-null but !isAlive()");
        } else {
            Log.d(TAG, "Server is alive");
        }
        return true;
    }

    public static void sleepIgnoreInterupt(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }


    public static boolean isConnectedToLocalNetwork(Context context) {
        boolean connected = false;
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        connected = ni != null
                && ni.isConnected()
                && (ni.getType() & (ConnectivityManager.TYPE_WIFI | ConnectivityManager.TYPE_ETHERNET)) != 0;
        if (!connected) {
            Log.d(TAG, "isConnectedToLocalNetwork: see if it is an WIFI AP");
            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            try {
                Method method = wm.getClass().getDeclaredMethod("isWifiApEnabled");
                connected = (Boolean) method.invoke(wm);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!connected) {
            Log.d(TAG, "isConnectedToLocalNetwork: see if it is an USB AP");
            try {
                for (NetworkInterface netInterface : Collections.list(NetworkInterface
                        .getNetworkInterfaces())) {
                    if (netInterface.getDisplayName().startsWith("rndis")) {
                        connected = true;
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        return connected;
    }


    public static boolean isConnectedToWifi(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected()
                && ni.getType() == ConnectivityManager.TYPE_WIFI;
    }

    public static InetAddress getLocalInetAddress(Context context) {
        if (!isConnectedToLocalNetwork(context)) {
            Log.e(TAG, "getLocalInetAddress called and no connection");
            return null;
        }

        if (isConnectedToWifi(context)) {

            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            int ipAddress = wm.getConnectionInfo().getIpAddress();
            if (ipAddress == 0)
                return null;
            return intToInet(ipAddress);
        }

        try {
            Enumeration<NetworkInterface> netinterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (netinterfaces.hasMoreElements()) {
                NetworkInterface netinterface = netinterfaces.nextElement();
                Enumeration<InetAddress> adresses = netinterface.getInetAddresses();
                while (adresses.hasMoreElements()) {
                    InetAddress address = adresses.nextElement();
                    // this is the condition that sometimes gives problems
                    if (!address.isLoopbackAddress()
                            && !address.isLinkLocalAddress())
                        return address;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static InetAddress intToInet(int value) {
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = byteOfInt(value, i);
        }
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            // This only happens if the byte array has a bad length
            return null;
        }
    }

    public static byte byteOfInt(int value, int which) {
        int shift = which * 8;
        return (byte) (value >> shift);
    }

    public static int getPort() {
        return port;
    }

    public static boolean isPortAvailable(int port) {

        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                /* should not be thrown */
                }
            }
        }

        return false;
    }


    private String ACTIVE_PASSWORD_PREFERENCE_KEY = "ACTIVE_HTTP_PASSWORD";

    private Boolean getActiveHTTPPassword(SharedPreferences preferences) {
        try {
            return preferences.getBoolean(ACTIVE_PASSWORD_PREFERENCE_KEY, false);
        } catch (ClassCastException ex) {
            Log.e(TAG, "Default active password preference is not an string. Resetting to default.");
            return false;
        }
    }

    private String PASSWORD_PREFERENCE_KEY = "HTTP_PASSWORD";

    private String getHTTPPassword(SharedPreferences preferences) {
        try {
            return preferences.getString(PASSWORD_PREFERENCE_KEY, getResources().getString(R.string.http_default_password));
        } catch (ClassCastException ex) {
            Log.e(TAG, "Default password preference is not an string. Resetting to default.");
            return getResources().getString(R.string.http_default_password);
        }
    }
}
