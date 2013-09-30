package com.calfx.autoscreen;

import com.calfx.autoscreen.RepeatingPowerFlick;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;


public class ScreenReceiver extends BroadcastReceiver {
 
    private boolean screenOff;
 
    @Override
    public void onReceive(Context context, Intent intent) {
    	
    	Intent sensorService = new Intent(context, SensorService.class);
    	Intent sensorServiceOff = new Intent(context, SensorServiceOff.class);

        //-debug-Log.d("ScreenReceiver()","-------------- SCREEN RECEIVER --------------------");

    	//detect if the screen is really on now
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = pm.isScreenOn();
        
   		//get SharedPreferences pointer
        SharedPreferences prefs = context.getSharedPreferences("com.calfx.autoscreen", Context.MODE_MULTI_PROCESS);
        
        //see if we should stop "sensorService"
    	int iAutoOn = prefs.getInt("com.calfx.autoscreen.cb_auto_on", 1);
    	if(iAutoOn == 0 && isSensorServiceRunning(context)) {
    		context.stopService(sensorService);
    	}

        //see if we should stop "sensorServiceOff"
    	int iAutoOff = prefs.getInt("com.calfx.autoscreen.cb_auto_off", 1);
    	if(iAutoOff == 0 && isSensorServiceOffRunning(context)) {
   			context.stopService(sensorServiceOff);
    		android.provider.Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 120000);
    	}
    	
		//-debug-Log.d("iAutoOn =", Integer.toString(iAutoOn));
		//-debug-Log.d("iAutoOff =", Integer.toString(iAutoOff));

    	//*******************************************************
    	//SCREEN JUST BECAME DARK and this if() will be executed
    	//*******************************************************
    	if (!isScreenOn && intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
        	
            screenOff = true;
            //-debug-Log.d("ScreenReceiver()","-------------- OFF --------------------");

        	if(iAutoOff == 1) {
                //STOP SENSOR SERVICE OFF
        		boolean serviceState = context.stopService(sensorServiceOff);
        		//-debug-Log.d("SERVICE OFF STATE =", Boolean.toString(serviceState));
        	}

        	if(iAutoOn == 1) {
        	
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                	//START REPEATING POWER FLICK
                	//----------------------------
                	Intent i = new Intent(context, RepeatingPowerFlick.class);
                	PendingIntent sender = PendingIntent.getBroadcast(context, 0, i, 0);
            
                	// We want the alarm to go off 2 seconds from now.
                	long firstTime = SystemClock.elapsedRealtime();
                	firstTime += 2*1000;

                	// Schedule the alarm
                	AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
                	am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, 15*1000, sender);
                }
        	
        		//START SENSOR SERVICE
        		//-debug-Log.d("ScreenReceiver()","-------------- calling SensorService --------------------");
        		context.startService(sensorService);
        	}

       	//*********************************************************
       	//SCREEN JUST BECAME BRIGHT and this if() will be executed
       	//*********************************************************
        } else if (isScreenOn && intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            //-debug-Log.d("ScreenReceiver()","-------------- ON --------------------");
            //-debug-Log.d("ScreenReceiver()","-------------- stopping SensorService --------------------");
            
        	//-debug-Log.d("IS SCREEN ON? =", Boolean.toString(isScreenOn));
            
            screenOff = false;
            
        	if(iAutoOn == 1) {
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                	//CANCEL REPEATING POWER FLICK
                	//----------------------------
                	// Create the same intent, and thus a matching IntentSender, for
                	// the one that was scheduled.
                	Intent i = new Intent(context, RepeatingPowerFlick.class);
                	PendingIntent sender = PendingIntent.getBroadcast(context, 0, i, 0);
                
                	// And cancel the alarm.
                	AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
                	am.cancel(sender);
                }

                //STOP SENSOR SERVICE
        		boolean serviceState = context.stopService(sensorService);
        		//-debug-Log.d("SERVICE STATE =", Boolean.toString(serviceState));
        	}
        	
        	if(iAutoOff == 1 && !isSensorServiceOffRunning(context)) {
        		//START SENSOR SERVICE OFF
        		//-debug-Log.d("ScreenReceiver()","-------------- calling SensorServiceOff --------------------");
        		if(iAutoOn == 0) {
        			//delay here needed to allow accelerometer reset from 0,0,0 position to whatever it is now
        			SystemClock.sleep(1000);
        		}
        		context.startService(sensorServiceOff);
        	}
        }else if(isScreenOn && iAutoOn == 1 && iAutoOff == 1 && isSensorServiceOffRunning(context) == true && intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
       		//-debug-Log.d("ScreenReceiver()","-------------- ACTION_USER_PRESENT -----------------");
       		//RE-START SENSOR SERVICE OFF
        	context.stopService(sensorServiceOff);
       		context.startService(sensorServiceOff);
        }
    }
    
    //************************************//
    //handy method to check service status//
    //************************************//
    private boolean isSensorServiceRunning(Context context) {
        //determine if our SensorService is currently running
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SensorService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    //------------------- || --------------------
    private boolean isSensorServiceOffRunning(Context context) {
        //determine if our SensorServiceOff is currently running
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SensorServiceOff.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

 
}
