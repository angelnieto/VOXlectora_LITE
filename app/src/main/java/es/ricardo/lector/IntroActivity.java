package es.ricardo.lector;

import java.util.List;

import android.hardware.Camera.CameraInfo;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.VideoView;

public class IntroActivity extends Activity{
	
	static final int ACTION_VALUE=1;

	private Button irPrevisualizacion;
	private Window window=null;
	Thread hiloAsincrono1=null;
	VideoView v=null;
	boolean continuar=false;
	boolean soportaBarraTitulo=false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		soportaBarraTitulo = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();
		if(!settings.getBoolean(getString(R.string.escuchador), false)){
			if(!settings.getBoolean(getString(R.string.back), false))
			editor.remove(getString(R.string.cascos));
		}else
			editor.putBoolean(getString(R.string.cascos),true);
		editor.remove(getString(R.string.escuchador));
		editor.commit();
		
		////		DESCOMENTAR PARA DEPURAR		//////
		//this.sendBroadcast(new Intent("Escuchador"));
		
		registerReceiver(abcd, new IntentFilter("1"));
	}
		
	@Override
	protected void onResume() {
		super.onResume();
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();
		
		editor.remove(getString(R.string.texto));
		editor.remove(getString(R.string.veces));
		editor.remove(getString(R.string.back));
		
		//editor.remove("saltar");
		
		editor.commit();
		
		if(!settings.getBoolean(getString(R.string.salir), false) && !settings.getBoolean(getString(R.string.saltar), false)){
			if(!(settings.getBoolean(getString(R.string.home), false) && settings.getBoolean(getString(R.string.cascos), false) && !settings.getBoolean(getString(R.string.cascosAnterior), false)) 
				&& !(settings.getBoolean(getString(R.string.home), false) && settings.getBoolean(getString(R.string.cascosAnterior), false) && !settings.getBoolean(getString(R.string.cascos), false))){
				 
		    	//registro la variable de comunicación con el Escuchador
				editor.putInt(getString(R.string.activity),1);
				editor.commit();
				
				window = getWindow();
				 window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
				            + WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				            + WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				            + WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
								
				setContentView(R.layout.layout_intro);
				
				if(soportaBarraTitulo)
					window.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.layout_titulo);
				
				LayoutInflater myInflater=LayoutInflater.from(this);
				View overView=myInflater.inflate(R.layout.segundacapa, null);
				this.addContentView(overView, new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
				
				irPrevisualizacion = (Button) findViewById(R.id.button);
							
				v=(VideoView)findViewById(R.id.surfaceIntro);
				
				 if(hasBackCamera()){
						if(isOnline()){
								irPrevisualizacion.setEnabled(true);
								setVideoUri(R.raw.video_presentacion);
								continuar=true;
						 }else{
							 setVideoUri(R.raw.video_no_internet);
						 }
				}else{
					setVideoUri(R.raw.video_no_camara);
				}
				 
				v.setOnCompletionListener(new OnCompletionListener() {
						
						@Override
						public void onCompletion(MediaPlayer arg0) {
							if(continuar)
								irPrevisualizacion();
							else
								finish();
						}
				});
				
				irPrevisualizacion.setOnClickListener(new OnClickListener() {
					
					public void onClick(View v) {
						irPrevisualizacion.setEnabled(false);
						
						if(continuar){
							IntroActivity.this.v.stopPlayback();
							irPrevisualizacion();
						}
					}
				});
				
				v.start();
				
				if(settings.getBoolean(getString(R.string.cascos), false) && settings.getBoolean(getString(R.string.cascosAnterior), false))
					editor.remove(getString(R.string.home));
				if(settings.getBoolean(getString(R.string.cascos), false))
					editor.putBoolean(getString(R.string.cascosAnterior),true);
				else
					editor.remove(getString(R.string.cascosAnterior));
			}else{
				editor.remove(getString(R.string.home));
				editor.remove(getString(R.string.cascosAnterior));
								
				finish();
			}
		}else{
			editor.remove(getString(R.string.home));
				
			finish();
		}
		editor.commit();
	}
	
	private void setVideoUri(int IDVideo) {
		Uri uri = Uri.parse("android.resource://"+getPackageName()+"/"+IDVideo);
		v.setVideoURI(uri);
	}

	protected void irPrevisualizacion(){
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();
		editor.remove(getString(R.string.home));
		editor.commit();
		
		Intent i=new Intent(this,CamaraActivity.class);
		startActivityForResult(i, ACTION_VALUE);
	}
	
	//Método una vez se vuelve a esta ventana
	protected void onActivityResult(int requestCode,int resultCode,Intent data){
		switch(requestCode){
		case ACTION_VALUE:
			if(resultCode==RESULT_CANCELED){
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
				if(settings.getBoolean(getString(R.string.salir), false))
					detener();
				if(settings.getBoolean(getString(R.string.saltar), false)){
					SharedPreferences.Editor editor = settings.edit();
					editor.remove(getString(R.string.saltar));
					editor.commit();
				}
					
			}
		}
	}

	@Override
	protected void onPause(){
		super.onPause();

		if(v!=null && v.isPlaying())
			v.stopPlayback();
		
		if(isHomeButtonPressed()){
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean(getString(R.string.home), true);
			editor.commit();
		}
		detener();
	}
	
	protected void detener(){
		//Borro la variable centinela
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();
		if(settings.getBoolean(getString(R.string.salir), false)){
			editor.remove(getString(R.string.salir));
			if(!this.isFinishing()){
				finish();
			}
		}
		editor.commit();
	}
	
	private boolean hasBackCamera() {
        int n = android.hardware.Camera.getNumberOfCameras();
        CameraInfo info = new CameraInfo();
        for (int i = 0; i < n; i++) {
            android.hardware.Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                 return true;
            }
        }
        return false;
    }
	
	private boolean isOnline() {
	    ConnectivityManager cm =(ConnectivityManager) this.getSystemService(this.CONNECTIVITY_SERVICE);

	    return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
	}

	@Override
	protected void onStop(){
		super.onStop();
		
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		
		//Borro la variable centinela
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		if(settings.getBoolean(getString(R.string.salir), false)){
			SharedPreferences.Editor editor = settings.edit();
			editor.remove(getString(R.string.salir));
			editor.commit();
		}
				
		unregisterReceiver(abcd);
	}
	
	private final BroadcastReceiver abcd = new BroadcastReceiver() {
        
		@Override
        public void onReceive(Context context, Intent intent) {
			
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(IntroActivity.this);
			if(!settings.getBoolean(getString(R.string.home), false))
				finish();
        }
		
	};
	
	private boolean isHomeButtonPressed(){
		Context context = getApplicationContext();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        if (!taskInfo.isEmpty()) {
        	ComponentName topActivity = taskInfo.get(0).topActivity; 
        	if (!topActivity.getPackageName().equals(context.getPackageName())) 
        		return true;
         }
        return false;
	}
	
}