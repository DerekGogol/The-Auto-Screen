package com.calfx.autoscreen;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;

public class SensorServiceOff extends Service {

    private SensorManager mSensorManager;
    private Sensor sensor, sensor_proximity;
    private float mAccel; // acceleration apart from gravity
    private float mAccelShake; // acceleration apart from gravity
    private float mAccelCurrent; // current acceleration including gravity
    private float mAccelCurrentShake; // current acceleration including gravity
    private float mAccelLast; // last acceleration including gravity
    private float mAccelLastShake; // last acceleration including gravity
    private float xLast, xCurrent = 0;
    private float yLast, yCurrent = 0;
    private float zLast, zCurrent = 0;
    private int tickCount, maxCount = 0;
    private float xTick = 0;
    private float yTick = 0;
    private float zTick = 0;
    private boolean bPowerOff, bIgnorePowerOffRequest, bProximitySensorNearTripped, bUpsideDown;
    private float fSensitivity;
    private SharedPreferences prefs;
    private int savedTimeOut, systemTimeOut = 0;
    private long lastTime, currTime, lastProximityTime, lastWakeUpTime, switchPowerOffTriggerTime = 0l;
    public static DevicePolicyManager mDPM;
    private Intent dialogScreen;
    private boolean iDialogTouched;

    private final IBinder mBinder = new LocalBinder();
    
    
    @Override
    public void onCreate() {
    	
    	//-debug-Log.i("OFF-onCreate()","-------------- SENSOR SERVICE OFF START --------------------------");

    	bPowerOff = false;
    	bUpsideDown = false;
    	iDialogTouched = false;
    	bIgnorePowerOffRequest = false;
    	dialogScreen = null;
    	bProximitySensorNearTripped = false;

    	//set our "lastWakeUpTime" variable right when we START this service:
    	if(lastWakeUpTime == 0l) {
    		//get last wakeup time:
        	lastWakeUpTime = System.currentTimeMillis();
    	}else{
    		//zero out last wakeup time:
    		lastWakeUpTime = 0l;
    	}

        /* do this in onCreate */
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensor_proximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorManager.registerListener(mSensorOffListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorProximityListener, sensor_proximity, SensorManager.SENSOR_DELAY_NORMAL);
               
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
        
        mAccelShake = 0.00f;
        mAccelCurrentShake = SensorManager.GRAVITY_EARTH;
        mAccelLastShake = SensorManager.GRAVITY_EARTH;
        
   		//get SharedPreferences pointer --- must use "MODE_MULTI_PROCESS" to read value from file rather than from memory
        //---------------------------------------------------------------------------------------------------------------
        prefs = this.getSharedPreferences("com.calfx.autoscreen", MODE_MULTI_PROCESS);
        
    	//get first user-specified system screen timeout value
    	savedTimeOut = prefs.getInt("com.calfx.autoscreen.system_screen_off_timeout", 0);
		systemTimeOut = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 120000);	//300,000 or 600,000 ?
		
    	if(savedTimeOut == 0 || systemTimeOut >= 60000) {
    		//THE FIRST TIME THE APP IS RUN savedTimeOut will be 0.
    		//This will save in our prefs the screen timeout from system settings
    		prefs.edit().putInt("com.calfx.autoscreen.system_screen_off_timeout", systemTimeOut).commit();
    		
    		//this should help keep the screen on for the screen timeout specified below...
        	PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
    	    final WakeLock wakeLock1 = mgr.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE), "TAG");
        	wakeLock1.acquire();
           	wakeLock1.release();

            //set screen timeout now
        	android.provider.Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, systemTimeOut);
        	//-debug-Log.d("----------1-----------", Integer.toString(systemTimeOut));
    	}else if(savedTimeOut >= 60000){
    		//restore saved default screen timeout for now
        	android.provider.Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, savedTimeOut);
        	//-debug-Log.d("----------2-----------", Integer.toString(savedTimeOut));
    	}else{
    		//something is screwy - we will put the screen timeout at 2 minutes for now
        	android.provider.Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 120000);
        	//-debug-Log.d("----------3-----------", "just 120,000");
    	}
    	//-debug-Log.d("OFF-DEFAULT_TIMEOUT ======>", Integer.toString(savedTimeOut));
        
    	//lower Android versions seem to stop the accelerometer ticking if phone is static
    	//--------------------------------------------------------------------------------
    	if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        	maxCount = 1;
        }else{
        	maxCount = 5;
        }
        
        //get saved "tf_sensitivity" value from file, or default to 5.5
    	String s = prefs.getString("com.calfx.autoscreen.tf_sensitivity", "5.5").toString();
    	fSensitivity = Float.parseFloat(s);
    	fSensitivity = 10.2f - fSensitivity;
    	if(fSensitivity < 0.2f) {
    		fSensitivity = 0.2f;
    	}else if(fSensitivity > 9.2f) {
    		fSensitivity = 9.2f;
    	}
    	fSensitivity = fSensitivity/20.00f;
    	//-debug-Log.d("OFF-ffffffffff =========>", Float.toString(fSensitivity));
    }

    @Override
    public void onDestroy() {
		//restore saved system default screen timeout for now
    	savedTimeOut = prefs.getInt("com.calfx.autoscreen.system_screen_off_timeout", 120000);
       	android.provider.Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, savedTimeOut);

       	mSensorManager.unregisterListener(mSensorOffListener,sensor);
    	//-debug-Log.d("OFF-OFF-onCreate()","-------------- OFF SENSOR SERVICE STOP ----------------------");
        //-debug-Log.d("OFF-onDestroy()","-------------- OFF SENSOR.unregisterListener ------------");
    	mSensorManager.unregisterListener(mSensorProximityListener,sensor_proximity);
        //-debug-Log.d("OFF-onDestroy()","-------------- PROX SENSOR.unregisterListener -----------");
    }

    private final SensorEventListener mSensorProximityListener = new SensorEventListener() {
    	public void onSensorChanged(SensorEvent se) {
        	
    		if (bPowerOff == false && se.values[0] <= 5 && lastProximityTime == 0){
            	
   				//-debug-Log.d("---PROXIMITY ---",  Float.toString(se.values[0]));
   				//-debug-Log.i("---PROXIMITY ---","-------------- NEAR ---------------");
    				
    			bProximitySensorNearTripped = true;
    			lastProximityTime = currTime;
   				
    		}else if(bPowerOff == false && se.values[0] > 5 && (lastProximityTime + 4000 > currTime)) {
    			
   				//-debug-Log.i("---PROXIMITY ---","-------------- FAR < 3 -------------");
   				//-debug-Log.i("---PROXIMITY ---","-------------- CANCELING -----------");
   				
    			//cancel the "screen off" that was initiated above
    			bProximitySensorNearTripped = false;
    			lastProximityTime = 0;
    			
	    		//restore saved system default screen timeout for now
    	    	savedTimeOut = prefs.getInt("com.calfx.autoscreen.system_screen_off_timeout", 120000);
   	        	android.provider.Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, savedTimeOut);
    		}
    	}
    	
    	public void onAccuracyChanged(Sensor sensor, int accuracy) {
        	//-debug-String a = String.format("proximity_accuracy=%d", accuracy);
        	//-debug-Log.d("PROXIMITY-SensorEventListener()", a);
    	}
    };
    
    
    private final SensorEventListener mSensorOffListener = new SensorEventListener() {
    	public void onSensorChanged(SensorEvent se) {
    		currTime = System.currentTimeMillis();
    		
    		//this will limit taking accelerometer measurement to every 200 milliseconds (5 times a second)
    		if((currTime - 200) > lastTime) {
    			lastTime = currTime;
    		
    			float x = se.values[0];
    			float y = se.values[1];
    			float z = se.values[2];
    			//-debug-String coords = String.format("x=%f, y=%f, z=%f", x,y,z);
    			//-debug-Log.d("OFF-SensorEventListener()", coords);
    			//-debug-String c = String.format("listener=%h", mSensorOffListener);
    			//-debug-Log.d("OFF-SensorEventListener()", c);
        	
    			if(tickCount++ == 0) {
    				xTick = x;
    				yTick = y;
    			} else if(tickCount > maxCount) {
    				tickCount = 0;
    			}
    			
    			if( !iDialogTouched && bPowerOff == false && tickCount == maxCount && (yTick < 1 && yTick > -1 && y < 1 && y > -1 && xTick < 2 && xTick > -2 ) {
    				//switch power off only if device is awake now for 2 seconds or more since the last screen-on
    		    	//otherwise, disable powering off until such time when the device is moved (see if() statement above)
    		    	if((lastWakeUpTime + 2000) < currTime) {
    		    		SwitchThePowerOff();
    		    		//-debug-Log.i("OFF-POWER-OFF() *************", "------------- 2 ----------");
    		    	}else{
    		    		//-debug-Log.i("OFF-POWER-OFF() *************", "------------- 3 ----------");
    		    		bIgnorePowerOffRequest = true;
    		    	}
    			}else if( !iDialogTouched && bPowerOff == false && y > -10 && y < -9 && x < 3 && x >-3 && z < 3 && z >-3 ) {
    				//-debug-Log.i("OFF-POWER-OFF() *UPSIDE-DOWN*", "------------- 4 ----------");
    		    	bUpsideDown = true;
    				SwitchThePowerOff();
    			}
    			
    			if(dialogScreen != null && bPowerOff == true) {
    				if(iDialogTouched) {
    			    	//-debug-Log.i("onCreate()","-------------- KEEP ON ---------------------");
    					bPowerOff = false;
    				}
    			}
            }
    	}

    	public void onAccuracyChanged(Sensor sensor, int accuracy) {
    		//-debug-String a = String.format("accuracy=%d", accuracy);
        	//-debug-Log.d("OFF-SensorEventListener()", a);
    	}
    };
    
    public void SwitchThePowerOff() {
    	
    	bPowerOff = true;
    	iDialogTouched = false;
    	switchPowerOffTriggerTime = currTime;
    	
    	//get current system screen timeout and see if we should save it in our preferences file
		systemTimeOut = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 120000);	//300,000 or 600,000 ?
    	if(systemTimeOut >= 60000) {
    		//and save it to our preferences file for now
    		prefs.edit().putInt("com.calfx.screengenie.system_screen_off_timeout", systemTimeOut).commit();
    	}

    	ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    	Runnable task = new Runnable() {
        	public void run() {
            	wakeLock1.release();
            	//-debug-Log.i("onCreate()","-------------- OFF PARTIAL_WAKE_LOCK release -----------------");
            }
          };
          worker.schedule(task, 500, TimeUnit.MILLISECONDS);

          android.provider.Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 4000);
    	
		//-debug-Log.i("onCreate()","-------------- DIALOG BOX ---------------------");
    	dialogScreen = new Intent(this, DisplayDialogPage.class);
    	dialogScreen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	if(bUpsideDown)
    		dialogScreen.putExtra("bUpsideDown", "1");
    	else
    		dialogScreen.putExtra("bUpsideDown", "0");
    	startActivity(dialogScreen);
    };

    //This function will be called from DisplayDialogPage.java
    //when the user presses the "Keep Screen ON" button...
    //
    public void DialogWasTouched() {
    	//-debug-Log.d("----------100--------------", "-----------TOUCHED!----------");
    	
    	savedTimeOut = prefs.getInt("com.calfx.autoscreen.system_screen_off_timeout", 60000);
    	if(savedTimeOut < 30000) {
			systemTimeOut = 30000;
		}
    	android.provider.Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, savedTimeOut);
    	iDialogTouched = true;
    	bPowerOff = false;
    	bUpsideDown = false;
    	switchPowerOffTriggerTime = 0;
    }
    
    //Class for clients to access.
    //
    public class LocalBinder extends Binder {
    	SensorServiceOff getService() {
            return SensorServiceOff.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}

