package com.calfx.autoscreen;

import com.calfx.autoscreen.R;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

public class DisplayDialogPage extends Activity {
    
    private SensorServiceOff mBoundService;
    boolean mIsBound;
    
    private ServiceConnection mConnection = new ServiceConnection() {
    	
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
        	//-debug-Log.d("----------0--------------", "-----------mBoundService----------");
            mBoundService = ((SensorServiceOff.LocalBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
        	//-debug-Log.d("----------0--------------", "-----------ServiceDisconnected----------");
            mBoundService = null;
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
    	
    	bindService(new Intent(DisplayDialogPage.this, SensorServiceOff.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        	//-debug-Log.d("----------0--------------", "-----------UNBIND----------");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }
	
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    	//String c = String.format("mBoundService=%h", mBoundService);
    	//-debug-Log.d("----------m0-------------", c);
	    String strUpsideDown;

    	doBindService();
    	
    	//retrieve our "bUpsideDown" from SensorServiceOff.java.  It will be in string format now
    	Bundle extras = getIntent().getExtras();
    	if (extras != null) {
    		strUpsideDown = extras.getString("bUpsideDown");
    	}else{
    		strUpsideDown = "0";
    	}
    	
    	//-debug-Log.d("---bUpsideDown---", strUpsideDown);
    	
    	if(strUpsideDown.equalsIgnoreCase("1")) {
    		setContentView(R.layout.custom_dialog_pocket_mode);
    	}else{
    		setContentView(R.layout.custom_dialog);
    	}

        // Watch for button clicks.
        Button button = (Button)findViewById(R.id.exit_dialog);
        button.setOnTouchListener(mTheAutoScreenTouchListener);

        //Rotate Button - only when phone is upside-down
    	if(strUpsideDown.equalsIgnoreCase("1") && this.getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
    		//rotate our button 180 degrees - so now it's upside down on the screen that's being held upside down
        	Animation animation = AnimationUtils.loadAnimation(this, R.anim.rotate_around_center_point_180);
    		button.startAnimation(animation);
    	}
    }
    
    private OnTouchListener mTheAutoScreenTouchListener = new OnTouchListener () {
    	  public boolean onTouch(View view, MotionEvent event) {
    	    if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
    	    	//-debug-Log.d("TouchTest", "Touch down");
    	    } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
    	    	//-debug-Log.d("TouchTest", "Touch up");
    	      
    	    	if(mIsBound) {
    	    		mBoundService.DialogWasTouched();
    	    	}
    	    	finish();
    	    }
    	    return true;
    	  }
    };

    @Override
    protected void onPause() {
    	doUnbindService();
        DisplayDialogPage.this.finish();
        super.onPause();
    }
}

