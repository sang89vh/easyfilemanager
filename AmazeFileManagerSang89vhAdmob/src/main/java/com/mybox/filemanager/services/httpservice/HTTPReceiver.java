package com.mybox.filemanager.services.httpservice;

/**
 * Created by yashwanthreddyg on 09-06-2016.
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class HTTPReceiver extends BroadcastReceiver {

    static final String TAG = HTTPReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "Received: " + intent.getAction());

        try {
            if (intent.getAction().equals(HTTPService.ACTION_START_HTTPSERVER)) {
                Intent serverService = new Intent(context, HTTPService.class);
                if (!HTTPService.isRunning()) {
                    context.startService(serverService);
                }
            } else if (intent.getAction().equals(HTTPService.ACTION_STOP_HTTPSERVER)) {
                Intent serverService = new Intent(context, HTTPService.class);
                context.stopService(serverService);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start/stop on intent " + e.getMessage());
        }
    }


}
