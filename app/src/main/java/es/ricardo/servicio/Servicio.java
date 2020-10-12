package es.ricardo.servicio;

import es.ricardo.lector.R;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 *  Clase que lanza el escuchador de eventos de la clavija jack al iniciar el SO Android
 */
public class Servicio extends Service {

	private EscuchadorCascos escuchador = new EscuchadorCascos();
	SharedPreferences settings;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		Log.i(getClass().getName(), "onStartCommand(" + intent.getAction() +"," + flags +"," + startId + ")");

		IntentFilter receiverFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);

		registerReceiver( escuchador, receiverFilter );

		Log.i(getClass().getName(), "cascos registrados");

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "Servicio::MyWakelockTag");
        wakeLock.acquire();

		return START_STICKY;
	}
	
	@Override
	public void onCreate() {
       super.onCreate();

		Log.i(getClass().getName(), "onCreate()");

		settings = PreferenceManager.getDefaultSharedPreferences(this);
       //Recurso del AVE MARÍA       
       SharedPreferences.Editor editor = settings.edit();
   	   editor.remove(getString(R.string.salir));
   	   editor.remove(getString(R.string.saltar));
   	   editor.apply();

   	   Log.i(getClass().getName(), "Servicio creado");

		String NOTIFICATION_CHANNEL_ID = "es.ricardo.voxlectora";

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startMyOwnForeground(NOTIFICATION_CHANNEL_ID);
		} else {
			// If earlier version channel ID is not used
			// https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
			startService(NOTIFICATION_CHANNEL_ID);
		}
    }

	@RequiresApi(api = Build.VERSION_CODES.O)
	private void startMyOwnForeground(String channelId){

		String channelName = "VOXlectora - Servicio";
		NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
		chan.setLightColor(Color.BLUE);
		chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
		NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		assert manager != null;
		manager.createNotificationChannel(chan);

		startService(channelId);
	}

	private void startService(String channelId) {
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
		Notification notification = notificationBuilder.setOngoing(true)
				.setSmallIcon(R.drawable.icono_notificacion)
				.setLargeIcon(BitmapFactory.decodeResource(this.getApplicationContext().getResources(),	R.drawable.icono))
				.setColor(Color.YELLOW)
				.setContentTitle(getString(R.string.contenidoNotificacion))
				.setPriority(NotificationManager.IMPORTANCE_MIN)
				.setCategory(Notification.CATEGORY_SERVICE)
				.build();
		startForeground(1, notification);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(getClass().getName(), "onDestroy()");

		unregisterReceiver(escuchador);
	}
	
}