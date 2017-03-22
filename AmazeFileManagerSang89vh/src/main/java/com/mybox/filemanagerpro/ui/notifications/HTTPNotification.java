package com.mybox.filemanagerpro.ui.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.mybox.filemanagerpro.R;
import com.mybox.filemanagerpro.activities.MainActivity;
import com.mybox.filemanagerpro.services.httpservice.HTTPService;

import java.net.InetAddress;

/**
 * Created by yashwanthreddyg on 19-06-2016.
 */
public class HTTPNotification extends BroadcastReceiver{

    private static final int NOTIFICATION_ID = 8888;

    @Override
    public void onReceive(Context context, Intent intent) {
        switch(intent.getAction()){
            case HTTPService.ACTION_STARTED:
                createNotification(context);
                break;
            case HTTPService.ACTION_STOPPED:
                removeNotification(context);
                break;
        }
    }

    @SuppressWarnings("NewApi")
    private void createNotification(Context context){

        String notificationService = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(notificationService);

        InetAddress address = HTTPService.getLocalInetAddress(context);

        String iptext = "http://" + address.getHostAddress() + ":"
                + HTTPService.getPort() + "/";

        int icon = R.drawable.ic_http_light;
        CharSequence tickerText = context.getResources().getString(R.string.http_notif_starting);
        long when = System.currentTimeMillis();


        CharSequence contentTitle = context.getResources().getString(R.string.http_notif_title);
        CharSequence contentText = String.format(context.getResources().getString(R.string.http_notif_text), iptext);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        int stopIcon = android.R.drawable.ic_menu_close_clear_cancel;
        CharSequence stopText = context.getResources().getString(R.string.http_notif_stop_server);
        Intent stopIntent = new Intent(HTTPService.ACTION_STOP_HTTPSERVER);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, 0,
                stopIntent, PendingIntent.FLAG_ONE_SHOT);


        Notification.Builder notificationBuilder = new Notification.Builder(context)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(contentIntent)
                .setSmallIcon(icon)
                .setTicker(tickerText)
                .setWhen(when)
                .setOngoing(true);

        Notification notification = null;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
            notificationBuilder.setCategory(Notification.CATEGORY_SERVICE);
            notificationBuilder.setPriority(Notification.PRIORITY_MAX);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notificationBuilder.addAction(stopIcon, stopText, stopPendingIntent);
            notificationBuilder.setShowWhen(false);
            notification = notificationBuilder.build();
        } else {
            notification = notificationBuilder.getNotification();
        }

        // Pass Notification to NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification);

    }

    private void removeNotification(Context context){
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nm = (NotificationManager) context.getSystemService(ns);
        nm.cancelAll();
    }
}
