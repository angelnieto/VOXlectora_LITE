package es.ricardo.servicio;

import es.ricardo.lector.R;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;

/**
 *  Clase que lanza el escuchador de eventos al iniciar el SO Android
 */
public class Servicio extends Service {

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
       super.onCreate();
       
       SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
       //Recurso del AVE MARÍA       
       SharedPreferences.Editor editor = settings.edit();
   	   editor.remove(getString(R.string.salir));
   	   editor.remove(getString(R.string.saltar));
   	   editor.commit();
      // Toast.makeText(this, "Servicio creado", Toast.LENGTH_LONG).show();

       String lanzamiento=settings.getString(getString(R.string.lanzamiento), null);
       if(lanzamiento!=null && getString(R.string.cascos).equals(lanzamiento.trim())){
	    	   IntentFilter receiverFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
	    	   Escuchador e=new Escuchador();
	    	  
	    	   registerReceiver( e, receiverFilter );
	    	   //Toast.makeText(this, "cascos registrados", Toast.LENGTH_LONG).show();
	   }
    }
	
}