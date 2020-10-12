package es.ricardo.servicio_voxlectora;

import es.ricardo.voxlectora.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Clase que escucha el evento de arranque del Sistema Operativo
 */
public class EscuchadorArranque extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {

		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

			String lanzamiento = settings.getString(context.getString(R.string.lanzamiento), null);
			Log.i(getClass().getName(), "lanzamiento : " + lanzamiento);

			if(lanzamiento!=null && context.getString(R.string.cascos).equals(lanzamiento.trim())) {
				Intent serviceIntent = new Intent(context, Servicio.class);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					Log.i(getClass().getName(), "Starting the service in >=26 Mode from a BroadcastReceiver");
					context.startForegroundService(serviceIntent);
				} else {
					Log.i(getClass().getName(), "Starting the service in <26 Mode from a BroadcastReceiver");
					context.startService(serviceIntent);
				}
			}
		}
	}

}