package es.ricardo.lector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import es.ricardo.lector.TareaAsincrona;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
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
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.graphics.Rect;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Clase encargada de la gestión de la Activity de la pantalla de texto descodificado
 */
public class ResultadoActivity extends Activity implements TextToSpeech.OnInitListener, OnGesturePerformedListener{

	static final int ACTION_VALUE=1;
	
	private TextView tv;
	private TextToSpeech tts;
	
	GestureOverlayView gestosView;
	GestureLibrary libreriaGestos;
	
	MediaPlayer mp;
	private TareaAsincrona tareaAsincrona=null;
	private boolean soportaBarraTitulo=false;

    private final BroadcastReceiver abcd = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ResultadoActivity.this);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(getString(R.string.salir), true);
            editor.commit();

            ResultadoActivity.this.setResult(RESULT_CANCELED);

            finish();
        }

    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		soportaBarraTitulo = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		
		registerReceiver(abcd, new IntentFilter("3"));
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

	    if(!settings.getBoolean(getString(R.string.salir), false) && !settings.getBoolean(getString(R.string.home), false) && !settings.getBoolean(getString(R.string.saltar), false)){
			setContentView(R.layout.layout_resultado);
			
			if(soportaBarraTitulo)
				getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.layout_titulo);
			
	    	//registro la variable de comunicación con el Escuchador
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt(getString(R.string.activity),3);
			editor.commit();
			
			if(tts==null)
				tts = new TextToSpeech(this, this);
	    	tts.setSpeechRate(Float.valueOf("0.7"));
						
		    String texto=settings.getString(getString(R.string.texto), null);
		    
		    tv = (TextView)findViewById(R.id.texto);
		    
		    //¡¡¡  BORRAR  !!! 
		    //registerForContextMenu(tv);
		    		    
		    //Agrego la capa para detectar gestos
			 LayoutInflater myInflater=LayoutInflater.from(ResultadoActivity.this);
			 gestosView=(GestureOverlayView) myInflater.inflate(R.layout.capa_gestos, null);
			 libreriaGestos = GestureLibraries.fromRawResource(ResultadoActivity.this, R.raw.gestures);
			 ResultadoActivity.this.addContentView(gestosView, new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
			 if (!libreriaGestos.load()) 
				 ResultadoActivity.this.finish();
	
		    if(texto==null){
		    	//Escalo la barra de progreso acorde a la resolución de la pantalla
		    	WindowManager windowManager =  (WindowManager) getSystemService(WINDOW_SERVICE);
		    	Rect pantalla=new Rect();
		    	windowManager.getDefaultDisplay().getRectSize(pantalla);
		    	
		    	DisplayMetrics dm = getResources().getDisplayMetrics(); 
		    	
		    	ProgressBar circulo=(ProgressBar)findViewById(R.id.progressBar);
		
		    	circulo.setScaleX(Math.round(Math.floor(0.015*pantalla.width()/dm.density)));
		    	circulo.setScaleY(circulo.getScaleX());
		    	
		    	circulo.setVisibility(View.VISIBLE);
		    	
		    	establecerFuente();
		    			    	
		    	tv.setText(R.string.conectando);
		    	
		    	tareaAsincrona=new TareaAsincrona(this);
		    	tareaAsincrona.execute();
		    }else{
		    	findViewById(R.id.progressBar).setVisibility(View.GONE);
		    	establecerFuente();
		    	habilitarGestos();
		    	mostrarVeces();
		    }
	    }else{
	    	setResult(RESULT_CANCELED);
			finish();
	    }
	}
	
	protected void updateResults(String texto,int veces,int mensajeError,String excepcion) {
		findViewById(R.id.progressBar).setVisibility(View.GONE);
		
		//Dado que la ResultadoActivity se destruye al dejar en segundo plano la aplicación, tengo que indicarle cuando vuelvo lo que debe hacer
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String textoAlmacenado=settings.getString(getString(R.string.texto), null);

        if(mensajeError==0){
		    if(textoAlmacenado==null){
		    	SharedPreferences.Editor editor = settings.edit();
		    	editor.putString(getString(R.string.texto), texto);
		    	if(veces!=0)
		    		editor.putInt(getString(R.string.veces), veces);
		    	editor.commit();
		    }
		    
		    mostrarVeces();
			
		   filtrarResultados(texto);
        }else{
        	if(mp!=null && mp.isPlaying())
        		mp.stop();
        	mp=MediaPlayer.create(this, mensajeError);
			mp.start();
			
			mostrarMensaje(getString(R.string.error));
			//mostrarMensaje(excepcion);
	    }
	}	
	
	private void mostrarMensaje(String texto){
		tv.setTextColor(getResources().getColor(R.color.morado));
		tv.setText(texto);
	}

    /**
     * Método llamado al iniciarse el sintetizador de voz
     *
     * @param status
     */
	public void onInit(int status) {
		
		 if (status == TextToSpeech.SUCCESS) {
			 
	           tts.setLanguage(new Locale("spa", "ESP"));
	           
	           TextToSpeech.OnUtteranceCompletedListener ttsListener=new TextToSpeech.OnUtteranceCompletedListener() {
					
					@Override
					public void onUtteranceCompleted(String utteranceId) {
						 if (getString(R.string.fin).equals(utteranceId))
							 habilitarGestos();
					}
				};
	            
	            tts.setOnUtteranceCompletedListener(ttsListener);
	            
	            String texto=tv.getText().toString();
	            if(checkTextIsNotNull(texto) && checkImagenDescodificada(texto))
	            	decirMensaje(texto);
	        }
	}

    private boolean checkImagenDescodificada(String texto) {
        return !getString(R.string.nada).equals(texto.trim()) && !getString(R.string.limite).equals(texto.trim()) && !getString(R.string.conectando).equals(texto.trim()) && !getString(R.string.error).equals(texto.trim());
    }

    private boolean checkTextIsNotNull(String texto) {
        return texto != null && !"".equals(texto.trim());
    }

    private void establecerFuente() {
		WindowManager windowManager =  (WindowManager) getSystemService(WINDOW_SERVICE);
    	Rect pantalla=new Rect();
    	windowManager.getDefaultDisplay().getRectSize(pantalla);
    	
		if(pantalla.width()<600)
    		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, Float.parseFloat(getString(R.string.telefono)));
    	else if(pantalla.width()<800)
    		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, Float.parseFloat(getString(R.string.hibrido)));
    	else
    		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, Float.parseFloat(getString(R.string.tableta)));
	}

	@Override
    protected void onPause() {
		super.onPause();

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if(mp!=null && mp.isPlaying())
        	mp.stop();
        if(tareaAsincrona!=null)
			tareaAsincrona.cancel(true);
        if(isHomeButtonPressed()){
        	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			SharedPreferences.Editor editor = settings.edit();
		    editor.putBoolean(getString(R.string.home), true);
		    //variable para que la recoja IntroActivity.java
		    editor.putBoolean(getString(R.string.saltar), true);
		    editor.commit();
		     
		    setResult(RESULT_CANCELED);
			
			finish();
        }
    }
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		
		if (mp!=null)
			mp.release();
		
		unregisterReceiver(abcd);
	}

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
      if(requestCode == ACTION_VALUE && resultCode==RESULT_CANCELED){
					SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
				    if(settings.getBoolean(getString(R.string.salir), false) || settings.getBoolean(getString(R.string.home), false) || settings.getBoolean(getString(R.string.saltar), false))
				    	salir();
	  }



    }

    /**
     * Método que muestra un mensaje emergente
     *
     * @param toast
     */
  	 public void showToast(final String toast) {
	  	    runOnUiThread(new Runnable() {
		  	      @Override
		  	      public void run() {
		  	        Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
		  	      }
	  	    });
  	  }
  	 
		private void decirMensaje(String texto) {
			HashMap<String, String> secuenciaHablada = new HashMap<>();
			secuenciaHablada.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,getString(R.string.fin));
			tts.speak(arreglar(texto), TextToSpeech.QUEUE_FLUSH, secuenciaHablada);
		}

		@Override
		public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
			
			 ArrayList<Prediction> predictions = libreriaGestos.recognize(gesture);
				
			if (!predictions.isEmpty() && predictions.get(0).score > 5.0) {
				String result = predictions.get(0).name;
				
				if (getString(R.string.horario).equalsIgnoreCase(result) || getString(R.string.horizontal).equalsIgnoreCase(result) || getString(R.string.vaiven).equalsIgnoreCase(result) || getString(R.string.c).equalsIgnoreCase(result))
					pararAudio();
			 
			     if (getString(R.string.horario).equalsIgnoreCase(result)){
			    	 //showToast("círculo");
			    	 salir();
			     }else if(getString(R.string.horizontal).equalsIgnoreCase(result)){
			    	 //showToast("horizontal");
			    	 repetirMensaje();
			     }else if(getString(R.string.vaiven).equalsIgnoreCase(result)){
			    	 //showToast("vaivén");
			    	 volver();
			     }else if(getString(R.string.c).equalsIgnoreCase(result)){
			    	 //showToast("C");
			    	 Intent results = new Intent( ResultadoActivity.this, ConfirmacionActivity.class);
				   	startActivityForResult(results, ACTION_VALUE);
			     }
			}
		}
		
		private void pararAudio(){
			if(mp.isPlaying())
				mp.stop();
		}
		
		private void repetirMensaje(){
			
			String texto=tv.getText().toString();
			
			gestosView.removeOnGesturePerformedListener(this);
			
		    filtrarResultados(texto);
		}

		private void volver(){
			setResult(RESULT_OK);
			finish();
		}
		
	private void salir(){
	       if(mp!=null) {
			   pararAudio();
		   }
	        mp = MediaPlayer.create(ResultadoActivity.this, R.raw.salir);
			mp.setOnCompletionListener(new OnCompletionListener() {

					public void onCompletion(MediaPlayer mp) {
						SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ResultadoActivity.this);
					    if(!settings.getBoolean(getString(R.string.salir), false) && !settings.getBoolean(getString(R.string.home), false)){
					    	//Dado que la CamaraActivity se destruye al entrar en esta Activity, tengo que indicarle cuando vuelvo lo que debe hacer
					        SharedPreferences.Editor editor = settings.edit();
					        editor.putBoolean(getString(R.string.salir), true);
					        editor.commit();
					    }
					    						
						setResult(RESULT_CANCELED);
						finish();
					}
					
				});
			  
			  mp.start();
		}
		
		private void habilitarGestos(){
			
			if(tv.getText()==null || "".equals(tv.getText())){
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		        String textoAlmacenado=settings.getString(getString(R.string.texto), null);
		        
		        if(checkTextIsNotNull(textoAlmacenado))
					mostrarMensaje(textoAlmacenado);
				else
					mostrarMensaje(getString(R.string.nada));
			}
						
			  //Inserto la superficie táctil para operar las siguientes operaciones 
			  gestosView.addOnGesturePerformedListener(ResultadoActivity.this);
					
			  mp = MediaPlayer.create(ResultadoActivity.this, R.raw.repetir);
			  mp.setOnCompletionListener(new OnCompletionListener() {
	
					public void onCompletion(MediaPlayer mp) {	
					
						SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ResultadoActivity.this);
						 String accionLanzamiento=settings.getString(getString(R.string.lanzamiento), null);
						 if(accionLanzamiento==null)
							 ResultadoActivity.this.mp = MediaPlayer.create(ResultadoActivity.this, R.raw.vincular);
						 else if(getString(R.string.cascos).equals(accionLanzamiento))
							 ResultadoActivity.this.mp = MediaPlayer.create(ResultadoActivity.this, R.raw.desvincular);
						
						 ResultadoActivity.this.mp.start();	 
					}
				
			  });
			  
			  mp.start();
		}
		
		void sinResultados(int mensajeTexto,int mensajeVoz){
			mostrarMensaje(getString(mensajeTexto));
			mp = MediaPlayer.create(this, mensajeVoz);
			
			mp.setOnCompletionListener(new OnCompletionListener() {

					public void onCompletion(MediaPlayer mp) {	
						habilitarGestos();	 
					}
					
			});
			
			mp.start();
		}

		private void mostrarVeces(){
			
			if(!Boolean.parseBoolean(getString(R.string.esSAD))){
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
				int veces=settings.getInt(getString(R.string.veces), 0);
		        if(veces!=0){
		        	//Escalo la barra inferior acorde a la resolución de la pantalla
			    	WindowManager windowManager =  (WindowManager) getSystemService(WINDOW_SERVICE);
			    	Rect pantalla=new Rect();
			    	windowManager.getDefaultDisplay().getRectSize(pantalla);
			    	        	
		        	LinearLayout marcador=(LinearLayout) findViewById(R.id.rayas1);
		        	marcador.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, Math.round(pantalla.height()/12),Gravity.BOTTOM));
		        	
		        	for(int i=15;i>15-veces && i>0;i--){
		        		ImageView imagen= (ImageView) marcador.getChildAt(i-1);
		        		imagen.setImageResource(R.drawable.raya_off);
		        	}
		        	
		        	findViewById(R.id.rayas1).setVisibility(View.VISIBLE);
		        }
			}
		}
		
		public boolean isHomeButtonPressed(){
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
		
	    private String arreglar(String mensaje){
            String texto = mensaje;
			texto=texto.replaceAll(" I ", " Primero ");
			texto=texto.replaceAll(" II ", " Segunda ");
			texto=texto.replaceAll(" Il ", " Segunda ");
			texto=texto.replaceAll(" III ", " Tercero ");
			texto=texto.replaceAll(" IV ", " Cuarto ");
			texto=texto.replaceAll(" V ", " Quinto ");
			texto=texto.replaceAll(" VI ", " Sexto ");
			texto=texto.replaceAll(" VII ", " Séptimo ");
			texto=texto.replaceAll(" VIII ", " Octavo ");
			texto=texto.replaceAll(" IX ", " Noveno ");
			texto=texto.replaceAll("LTR ", " letra ");
			texto=texto.replaceAll("Cl ", " calle ");
			texto=texto.replaceAll("CL ", " calle ");
			texto=texto.replaceAll(" PTA.", " puerta");
			texto=texto.replaceAll("\\|", "I");
			texto=texto.replaceAll("N°", "Número");
			texto=texto.replaceAll("1°", "primero");
			texto=texto.replaceAll("2°", "segundo");
			texto=texto.replaceAll("3°", "tercero");
			texto=texto.replaceAll("4°", "cuarto");
			texto=texto.replaceAll("5°", "quinto");
			texto=texto.replaceAll("6°", "sexto");
			texto=texto.replaceAll("7°", "séptimo");
			texto=texto.replaceAll("8°", "octavo");
			texto=texto.replaceAll("9°", "noveno");
			texto=texto.replaceAll("10°", "décimo");
			texto=texto.replaceAll("ï", "i");
			texto=texto.replaceAll("kWh", " kilovatios hora");
			texto=texto.replaceAll("!", "I");
			texto=texto.replaceAll("¡", "I");
			texto=texto.replaceAll(" cm", " centímetros");
			texto=texto.replaceAll(" mm", " milímetros");
			
			return texto;
		}
		
		private void filtrarResultados(String texto){
			if(texto==null || "".equals(texto.trim()) || getString(R.string.nada).equals(texto.trim())){
		    	sinResultados(R.string.nada,R.raw.sin_resultados);
		    }else if(getString(R.string.limite).equals(texto.trim())){
				sinResultados(R.string.limite,R.raw.limite);
		    }else{
				mostrarMensaje(texto);
				if(tts.getLanguage()!=null)
					decirMensaje(texto);
			}
		}
}