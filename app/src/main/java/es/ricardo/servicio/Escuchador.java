package es.ricardo.servicio;

import java.util.ArrayList;
import java.util.List;

import es.ricardo.lector.R;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class Escuchador extends BroadcastReceiver {
	
	private boolean centinela=false;

	@Override
	public void onReceive(Context context, Intent intent) {
				
	  Intent serviceIntent = new Intent();
      serviceIntent.setAction(context.getString(R.string.servicioLanzamiento));
      context.startService(serviceIntent);
      
       //Si está instalada la versión FULL impido que se lance la versión FREE con los auriculares 
 	   boolean VOXlectora=false;
 	   ActivityManager localActivityManager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
 	   List<RunningServiceInfo> servicios = localActivityManager .getRunningServices(100);
 	   for(RunningServiceInfo servicio:servicios)
 		   if("es.ricardo.voxlectora".equals(servicio.service.getPackageName()))
 			  VOXlectora=true;  
 	           
        if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG) && !VOXlectora) {
            int state = intent.getIntExtra(context.getString(R.string.estado), -1);
            
            switch (state) {
	            case 0:
	            	if(centinela){
		            	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		            	SharedPreferences.Editor editor = settings.edit();
		            	editor.putBoolean(context.getString(R.string.escuchador), true);
		            	editor.commit();
		            	
		            	Intent i = new Intent();
		                i.setClassName(context.getString(R.string.paquete), context.getString(R.string.clase));
		                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		                context.startActivity(i);
		        		
		        		Toast.makeText(context, "Auricular extraído",Toast.LENGTH_SHORT).show();
		        		centinela=false;
	            	}
	            break;
	            case 1:
	            	centinela=true;
	            	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
	            	
	            	switch(settings.getInt(context.getString(R.string.activity), 0)){
		            	case 0:
		            	case 1:
		            		context.sendBroadcast(new Intent("1"));
		            	break;
		            	case 2:
		            		context.sendBroadcast(new Intent("2"));
		            	break;
		            	case 3:
		            		context.sendBroadcast(new Intent("3"));
		            	break;
		            	case 4:
		            		context.sendBroadcast(new Intent("4"));
		            	break;
	            	}
	            break;
	       }
        }
	}

}