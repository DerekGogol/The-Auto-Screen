package com.calfx.autoscreen;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class StartAutoScreenAtBootReceiver extends BroadcastReceiver {
	final private int NOTIFICATION = 19770331; //Any unique number for this notification

	@Override
    public void onReceive(Context context, Intent intent) {

        //get SharedPreferences pointer
		SharedPreferences prefs = context.getSharedPreferences("com.calfx.autoscreen", Context.MODE_PRIVATE);

  		//read stored preferences
    	int savedValue = prefs.getInt("com.calfx.autoscreen.cb_start_on_startup", 1);
		
        if (savedValue == 1 && "android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
        	
           	savedValue = prefs.getInt("com.calfx.autoscreen.cb_show_in_notification_bar", 1);
           	if(savedValue == 1) {
                //make ICON in Notification bar
    			Notification not;
    	    	if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
    	    		not = new Notification(R.drawable.ic_launcher, "AutoScreen service started", System.currentTimeMillis());
    	    	}else{
    	    		not = new Notification(R.drawable.ic_launcher_24, "AutoScreen service started", System.currentTimeMillis());
    	    	}
                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, TheAutoScreen.class), Notification.FLAG_ONGOING_EVENT);        
                not.flags = Notification.FLAG_ONGOING_EVENT;
                not.setLatestEventInfo(context, "The AutoScreen", "Touch for Settings", contentIntent);
                
                NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(NOTIFICATION, not);
    		}

            Intent autoScreen = new Intent("com.calfx.autoscreen.AutoScreen");
            context.startService(autoScreen);
        }
    }
}
