package com.calfx.autoscreen;

import com.calfx.autoscreen.TheAutoScreen;
import com.calfx.autoscreen.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.SystemClock;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TheAutoScreen extends Activity {
    Toast mToast;
    SharedPreferences prefs;
	final private int NOTIFICATION = 19770331; //Any unique number for this notification
	
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //-debug-Log.d("TheAutoScreen()","-------------- TheAutoScreen - onCreate() --------------------");

        setContentView(R.layout.alarm_controller);
        
        //get SharedPreferences pointer
        prefs = this.getSharedPreferences("com.calfx.autoscreen", MODE_PRIVATE | MODE_MULTI_PROCESS);

    	//*** PERFERENCES ***//
    	//  READ FROM FILE   //
    	//*******************//

        //read stored preferences
    	int savedValue = 1;
    	CheckBox checkbox;

    	//get saved Sensitivity value
    	EditText edittext = (EditText) findViewById(R.id.tf_sensitivity);
    	if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        	//lower Android versions seem to have less sensitive accelerometer (2.3.4 for example)
        	edittext.setText(prefs.getString("com.calfx.autoscreen.tf_sensitivity", "8.9"));
        }else{
        	edittext.setText(prefs.getString("com.calfx.autoscreen.tf_sensitivity", "7.5"));
        }

    	savedValue = prefs.getInt("com.calfx.autoscreen.cb_auto_on", 1);
    	checkbox = (CheckBox) findViewById(R.id.cb_auto_on);
    	if(savedValue == 1) {
    		checkbox.setChecked(true);
    	}else{
    		checkbox.setChecked(false);
    	}
        //... also, watch for "cb_auto_on" clicks.
        checkbox.setOnClickListener(mAutoOnListener);

    	savedValue = prefs.getInt("com.calfx.autoscreen.cb_auto_off", 1);
    	checkbox = (CheckBox) findViewById(R.id.cb_auto_off);
    	if(savedValue == 1) {
    		checkbox.setChecked(true);
    	}else{
    		checkbox.setChecked(false);
    	}
        //... also, watch for "cb_auto_off" clicks.
        checkbox.setOnClickListener(mAutoOffListener);

    	savedValue = prefs.getInt("com.calfx.autoscreen.cb_show_in_notification_bar", 1);
    	checkbox = (CheckBox) findViewById(R.id.cb_show_in_notification_bar);
    	if(savedValue == 1) {
    		checkbox.setChecked(true);
    	}else{
    		checkbox.setChecked(false);
    	}
        //... also, watch for "cb_show_in_notification_bar" clicks.
        checkbox.setOnClickListener(mShowIconListener);

    	savedValue = prefs.getInt("com.calfx.autoscreen.cb_start_on_startup", 1);
    	checkbox = (CheckBox) findViewById(R.id.cb_start_on_startup);
    	if(savedValue == 1) {
    		checkbox.setChecked(true);
    	}else{
    		checkbox.setChecked(false);
    	}

        // Watch for button clicks.
        Button button = (Button)findViewById(R.id.one_shot);
        button.setOnClickListener(mOneShotListener);
        
        button = (Button)findViewById(R.id.start_repeating);
        button.setOnClickListener(mStartRepeatingListener);
        
        button = (Button)findViewById(R.id.stop_repeating);
        button.setOnClickListener(mStopRepeatingListener);
        
        button = (Button)findViewById(R.id.enter_help);
        button.setOnClickListener(mDisplayHelpPage);
        
    	TextView view = (TextView) findViewById(R.id.service_status);
        if (isAutoScreenRunning()) {
        	view.setText("AutoScreen service is Running \n");
            //-debug-Log.d("AutoScreen()","-------------- RUNNING --------------------");
        } else {
        	view.setText("AutoScreen service is Not Running \n");
            //-debug-Log.d("AutoScreen()","-------------- STOPPED --------------------");
        }
        
        mToast = Toast.makeText(TheAutoScreen.this, "Please submit bug reports... thank you!",
                Toast.LENGTH_LONG);
        mToast.show();
    }
    
    //************************************//
    //handy method to check service status//
    //************************************//
    private boolean isAutoScreenRunning() {
        //determine if our AutoScreen is currently running
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (AutoScreen.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    private OnClickListener mOneShotListener = new OnClickListener() {
        public void onClick(View v) {
        	
        	//** PERFERENCES **//
        	//  SAVE TO FILE   //
        	//*****************//
        	
      		//read displayed preferences
        	CheckBox checkbox;
        	
        	//save the Sensitivity value
        	EditText edittext = (EditText) findViewById(R.id.tf_sensitivity);
    		prefs.edit().putString("com.calfx.autoscreen.tf_sensitivity", edittext.getText().toString()).commit();

        	//save all the checkboxes values
        	checkbox = (CheckBox) findViewById(R.id.cb_auto_on);
        	if(checkbox.isChecked()) {
        		prefs.edit().putInt("com.calfx.autoscreen.cb_auto_on", 1).commit();
        	}else{
        		prefs.edit().putInt("com.calfx.autoscreen.cb_auto_on", 0).commit();
        	}

        	checkbox = (CheckBox) findViewById(R.id.cb_auto_off);
        	if(checkbox.isChecked()) {
        		prefs.edit().putInt("com.calfx.autoscreen.cb_auto_off", 1).commit();
        	}else{
        		prefs.edit().putInt("com.calfx.autoscreen.cb_auto_off", 0).commit();
        	}

        	checkbox = (CheckBox) findViewById(R.id.cb_show_in_notification_bar);
        	if(checkbox.isChecked()) {
        		prefs.edit().putInt("com.calfx.autoscreen.cb_show_in_notification_bar", 1).commit();
        	}else{
        		prefs.edit().putInt("com.calfx.autoscreen.cb_show_in_notification_bar", 0).commit();
        	}

        	checkbox = (CheckBox) findViewById(R.id.cb_start_on_startup);
        	if(checkbox.isChecked()) {
        		prefs.edit().putInt("com.calfx.autoscreen.cb_start_on_startup", 1).commit();
        	}else{
        		prefs.edit().putInt("com.calfx.autoscreen.cb_start_on_startup", 0).commit();
        	}

        	// Tell the user about what we did.
            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(TheAutoScreen.this, R.string.one_shot_scheduled,
                    Toast.LENGTH_SHORT);
            mToast.show();
            
            ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
            Runnable task = new Runnable() {
                public void run() {
                	//System.exit(0);
                	finish();	//do it gently
                }
              };
              worker.schedule(task, 1, TimeUnit.SECONDS);
        }
    };

    private OnClickListener mStartRepeatingListener = new OnClickListener() {
        public void onClick(View v) {
        	
        	//save the current Sensitivity value entered in Text field, it will be needed when Service starts
        	EditText edittext = (EditText) findViewById(R.id.tf_sensitivity);
        	SharedPreferences preferences = getSharedPreferences("com.calfx.autoscreen", MODE_MULTI_PROCESS);
    		Editor editor1 = preferences.edit();
    		String s = edittext.getText().toString();
    		editor1.putString("com.calfx.autoscreen.tf_sensitivity", (String) s);
    		editor1.commit();

           	//display or clear notification icon depending on "cb_show_in_notification_bar" checkbox
    		CheckBox checkbox = (CheckBox) findViewById(R.id.cb_show_in_notification_bar);
    		if(checkbox.isChecked()) {
                //make ICON in Notification bar
    			Notification not;
    	    	if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
    	    		not = new Notification(R.drawable.ic_launcher, "AutoScreen service started", System.currentTimeMillis());
    	    	}else{
    	    		not = new Notification(R.drawable.ic_launcher_24, "AutoScreen service started", System.currentTimeMillis());
    	    	}
                PendingIntent contentIntent = PendingIntent.getActivity(TheAutoScreen.this, 0, new Intent(TheAutoScreen.this, TheAutoScreen.class), Notification.FLAG_ONGOING_EVENT);        
                not.flags = Notification.FLAG_ONGOING_EVENT;
                not.setLatestEventInfo(TheAutoScreen.this, "The AutoScreen", "Touch for Settings", contentIntent);
                
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                mNotificationManager.notify(NOTIFICATION, not);
    		}else{
    			//cancel notification icon
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                mNotificationManager.cancel(NOTIFICATION);
    		}

    		//START UPDATE SERVICE
        	Intent autoScreen = new Intent(TheAutoScreen.this, AutoScreen.class);
        	SystemClock.sleep(500);   //this .5 second pause helps it to work with 3rd-party screen-off apps
        	TheAutoScreen.this.startService(autoScreen);

        	TextView view = (TextView) findViewById(R.id.service_status);
           	view.setText("AutoScreen service is Running \n");
           	
    		// Tell the user about what we did.
            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(TheAutoScreen.this, R.string.repeating_scheduled,
                    Toast.LENGTH_SHORT);
            mToast.show();
        }
    };

    private OnClickListener mStopRepeatingListener = new OnClickListener() {
        public void onClick(View v) {
        	Intent autoScreen = new Intent(TheAutoScreen.this, AutoScreen.class);
        	
        	//this helps it to work with 3rd-party screen-off apps
        	SystemClock.sleep(500);
        	TheAutoScreen.this.stopService(autoScreen);

        	TextView view = (TextView) findViewById(R.id.service_status);
           	view.setText("AutoScreen service is Not Running \n");
           	
            // Tell the user about what we did.
            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(TheAutoScreen.this, R.string.repeating_unscheduled,
                    Toast.LENGTH_LONG);
            mToast.show();
        }
    };

    private OnClickListener mDisplayHelpPage = new OnClickListener() {
        public void onClick(View v) {
        	Intent helpScreen = new Intent(TheAutoScreen.this, DisplayHelpPage.class);

        	startActivity(helpScreen);
        }
    };

    private OnClickListener mShowIconListener = new OnClickListener() {
        public void onClick(View v) {
        	
        	if(isAutoScreenRunning()) {
        		CheckBox checkbox = (CheckBox) findViewById(R.id.cb_show_in_notification_bar);
        		if(checkbox.isChecked()) {
                    //make ICON in Notification bar
        			Notification not;
        	    	if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        	    		not = new Notification(R.drawable.ic_launcher, "AutoScreen service is running", System.currentTimeMillis());
        	    	}else{
        	    		not = new Notification(R.drawable.ic_launcher_24, "AutoScreen service is running", System.currentTimeMillis());
        	    	}
                    PendingIntent contentIntent = PendingIntent.getActivity(TheAutoScreen.this, 0, new Intent(TheAutoScreen.this, TheAutoScreen.class), Notification.FLAG_ONGOING_EVENT);        
                    not.flags = Notification.FLAG_ONGOING_EVENT;
                    not.setLatestEventInfo(TheAutoScreen.this, "The AutoScreen", "Touch for Settings", contentIntent);
                    
                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    mNotificationManager.notify(NOTIFICATION, not);
        		}else{
        			//cancel notification icon
                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    mNotificationManager.cancel(NOTIFICATION);
        		}
        	}else{
                // Tell the user about Service not running currently
                if (mToast != null) {
                    mToast.cancel();
                }
                mToast = Toast.makeText(TheAutoScreen.this, "Service is currently not running.",
                        Toast.LENGTH_LONG);
                mToast.show();
        	}
        }
    };

    private OnClickListener mAutoOnListener = new OnClickListener() {
        public void onClick(View v) {
        	
        	if(isAutoScreenRunning()) {
        		CheckBox checkbox = (CheckBox) findViewById(R.id.cb_auto_on);
        		if(checkbox.isChecked()) {
        			//save to preferences now
            		prefs.edit().putInt("com.calfx.autoscreen.cb_auto_on", 1).commit();
        		}else{
        			//save to preferences now
            		prefs.edit().putInt("com.calfx.autoscreen.cb_auto_on", 0).commit();
        		}
                // Tell the user about need to restart service
                if (mToast != null) {
                    mToast.cancel();
                }
                mToast = Toast.makeText(TheAutoScreen.this, "Please restart service for the change to take effect.",
                        Toast.LENGTH_LONG);
                mToast.show();
        	}
        }
    };

    private OnClickListener mAutoOffListener = new OnClickListener() {
        public void onClick(View v) {
        	
        	if(isAutoScreenRunning()) {
        		CheckBox checkbox = (CheckBox) findViewById(R.id.cb_auto_off);
        		if(checkbox.isChecked()) {
        			//save to preferences now
            		prefs.edit().putInt("com.calfx.autoscreen.cb_auto_off", 1).commit();
        		}else{
        			//save to preferences now
            		prefs.edit().putInt("com.calfx.autoscreen.cb_auto_off", 0).commit();
        		}
                // Tell the user about need to restart service
                if (mToast != null) {
                    mToast.cancel();
                }
                mToast = Toast.makeText(TheAutoScreen.this, "Please restart service for the change to take effect.",
                        Toast.LENGTH_LONG);
                mToast.show();
        	}
        }
    };
}

