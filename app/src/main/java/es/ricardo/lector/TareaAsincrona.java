package es.ricardo.lector;

import java.io.FileInputStream;
import java.io.IOException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;


public class TareaAsincrona extends AsyncTask<String, String, Boolean> {

	/** application context. */
	private final Context context;
	private final ResultadoActivity activity;
	int error=0;
	String excepcion="";
	String texto=null;
	int veces=0;
	
	//private GoogleAccountCredential credential;
	//private String URLServidor="https://googledrive.com/host/0B4O65dFE5SPsNGZTR0puWjREWTQ/";
		
	public TareaAsincrona(ResultadoActivity activity) {
		this.context = activity.getApplicationContext();
		this.activity=activity;
	}

	protected void onPreExecute() {	}

	protected void onPostExecute(Boolean result) {
		activity.updateResults(texto,veces,error);
	}

	@Override
	protected Boolean doInBackground(String... args) {
		
		activity.mp=MediaPlayer.create(context, R.raw.procesando);
		activity.mp.start();
		
		try {
			HttpClient httpClient = new DefaultHttpClient();
			HttpParams httpParameters = httpClient.getParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, 50000);
			 
			HttpPost post = new HttpPost(context.getString(R.string.host));
			//HttpPost post = new HttpPost("http://192.168.2.102:9080/VOXlectoraWS/Autenticador/post");
			//HttpPost post = new HttpPost("http://192.168.1.40:9080/VOXlectoraWS/Autenticador/post");
		    post.setHeader(context.getString(R.string.type), context.getString(R.string.json));
		    JSONObject dato = new JSONObject();
		    dato.put(context.getString(R.string.parametroEntrada1), context.getString(R.string.esSAD));
		    dato.put(context.getString(R.string.parametroEntrada2), getIdCacharro());
		    dato.put(context.getString(R.string.parametroEntrada3), getImagenBase64());
		    StringEntity entity = new StringEntity(dato.toString());
	        post.setEntity(entity);
	        org.apache.http.HttpResponse resp = httpClient.execute(post);
			
			if(!isCancelled()){
				 String jsonRespuesta = EntityUtils.toString(resp.getEntity());
			        if(jsonRespuesta!=null){
			        	JsonParser parser=new JsonParser();
						JsonObject jsonObject=parser.parse(jsonRespuesta.trim()).getAsJsonObject();
						if(jsonObject.get(context.getString(R.string.parametroSalida1)).getAsInt()==1){
							excepcion+=" "+jsonObject.get(context.getString(R.string.parametroSalida2)).getAsString();
							if("reloj".equals(jsonObject.get(context.getString(R.string.parametroSalida2)).getAsString()))
								error=R.raw.reloj;
							else
								error=R.raw.error_drive;
						}else{
							texto=jsonObject.get(context.getString(R.string.parametroSalida3)).getAsString();
							veces=jsonObject.get(context.getString(R.string.parametroSalida4)).getAsInt();
						}
					}
				 }
	
		} catch (Exception e) {
			error=R.raw.error_drive;
			if(e!=null)	 
				excepcion+=" "+e.getMessage();
			else
				excepcion+=" ";
             return false;
         }
		 return true;
     }

	@Override
	protected void onProgressUpdate(String... values) {   }

	 public void showToast(final String toast) {
	        activity.showToast(toast);
	  }
		
	private String getIdCacharro(){
		return android.provider.Settings.Secure.getString(context.getContentResolver(),android.provider.Settings.Secure.ANDROID_ID);
	}
	
	@Override
	protected void onCancelled(){
		return;
	}
	
	private String getImagenBase64() throws IOException{
		java.io.File imagen = new java.io.File(CamaraActivity.directorio.getPath());
		FileInputStream fin = new FileInputStream(imagen);
		byte fileContent[] = new byte[(int)imagen.length()];
		
		int offset = 0;
		while ( offset < fileContent.length ) {
		    int count = fin.read(fileContent, offset, fileContent.length - offset);
		    offset += count;
		}
		
		fin.close();
		
		byte[] encoded = Base64.encodeBytesToBytes(fileContent);
		return new String(encoded);
	}
		
}