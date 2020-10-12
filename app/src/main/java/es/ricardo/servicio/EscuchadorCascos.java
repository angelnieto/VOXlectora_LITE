package es.ricardo.servicio_voxlectora;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import es.ricardo.voxlectora.R;

/**
 * Clase que escucha el evento de extracción de los cascos
 */
public class EscuchadorCascos extends BroadcastReceiver {
	
	SharedPreferences settings;

	boolean auricularesInsertados = false;

	@Override
	public void onReceive(Context context, Intent intent) {

		settings = PreferenceManager.getDefaultSharedPreferences(context);
		int state = intent.getIntExtra(context.getString(R.string.estado), -1);

		Log.i(getClass().getName(), "acción recibida : " + intent.getAction() + " state : " + state );

		if (Intent.ACTION_HEADSET_PLUG.equals(intent.getAction()) && state == 0 && auricularesInsertados) {
				Intent i = new Intent();
				i.setClassName(context.getString(R.string.paquete), context.getString(R.string.clase));
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				context.startActivity(i);

				Toast.makeText(context, "Auricular extraído", Toast.LENGTH_SHORT).show();
				Log.i(getClass().getName(), "Auricular extraído");
		} else if(Intent.ACTION_HEADSET_PLUG.equals(intent.getAction()) && state == 1) {
			auricularesInsertados = true;
		}

	}

}