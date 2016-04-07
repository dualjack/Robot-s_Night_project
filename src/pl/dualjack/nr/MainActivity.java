package pl.dualjack.nr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements LocationListener {
	
	private boolean isBlocked = true;
	private String password = "1234";
	private boolean isMuted = false;
	private boolean isConnected = false;
	private boolean isBtOn = false;
	private boolean isGpsOn = false;
	private int timerDelay = 5;				// Delay between next timer action will fire in seconds
	private int timerCount = timerDelay;	// Actual timer count
	
	private LocationManager locationManager;
	private ConnectivityManager connMgr;
	private BluetoothManager2 bluetoothManager;
	private NetworkInfo networkInfo;
	private Timer timer;
	
	private TextView gpsStatusField;
	private TextView btStatusField;
	private TextView latitudeField;
	private TextView longitudeField;
	private TextView speedMeter;
	private TextView speedLimit;
	private TextView speedfield;
	private TextView internetField;
	private TextView networkResponseField;
	private TextView connectingField;
	private ProgressBar progressBarTimer;
	
	private MediaPlayer alarmSound;
	
	private float speed = 0;
	private int currentSpeed = 0;
	private int currentLimit = 0;
	private String currentCity = "Nieznane";
	private String currentStreet = "Nieznana";
	private double latitude = 0; //51.083190;
	private double longitude = 0; //17.018251;
	private String networkResponseString = "Oczekuję na dane";
	private boolean goTooFast = false;
	private boolean isConnecting = false;
	private int debugLimitValue = 10;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.base_interface);
		
		
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        alarmSound=MediaPlayer.create(MainActivity.this, R.raw.beep );
        
        // Fields initialization
		
 		gpsStatusField = (TextView)findViewById(R.id.gpsStatus);
 		btStatusField = (TextView)findViewById(R.id.btStatus);
 		latitudeField = (TextView)findViewById(R.id.latitudeField);
 		longitudeField = (TextView)findViewById(R.id.longitudeField);
 		speedMeter = (TextView)findViewById(R.id.speedMeter);
 		speedLimit = (TextView)findViewById(R.id.speedLimit);
 		speedfield = (TextView)findViewById(R.id.speedField);
 		internetField = (TextView)findViewById(R.id.internetField);
 		connectingField = (TextView)findViewById(R.id.connectingField);
 		connectingField.setVisibility(TextView.GONE);
 		networkResponseField = (TextView)findViewById(R.id.networkResponseField);
 		progressBarTimer = (ProgressBar)findViewById(R.id.progressBarTimer);
 		
 		// Timer
 		
 		timer = new Timer();
 		
 		TimerTask updateTimer = new MainLoopTimer();
 		timer.scheduleAtFixedRate(updateTimer, 0, 1000);
		
		
		// GPS
		
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 400, 1, this);
		
		isGpsOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		
		// Bluetooth
		
		bluetoothManager = new BluetoothManager2();
		
	}
	
	// ===========================================================================
	// LOGIC TIMER
	// ===========================================================================
	
	
	class MainLoopTimer extends TimerTask {

		public void run() {

			runOnUiThread(new Runnable() {
				@Override
				public void run() {

					updateScreenInformation();	// Update UI

					generalCheckDriver();		// Check if driver go too fast, start alarm, etc.

					isConnecting = false;	// clear flag

					isBtOn = bluetoothManager.isBluetoothOn();	// Bluetooth state


					// NETWORK

					connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
					networkInfo = connMgr.getActiveNetworkInfo();

					if((networkInfo != null) && networkInfo.isConnected()){	// CHECK connection

						isConnected = true;	// <===

					} else {

						isConnected = false;	// <===

					}


					if(timerCount++ == timerDelay){	// Update timer counter

						timerCount = 0;	// reset timer counter					

						if(isConnected) {	// If we have connection

							if(latitude == 0 || longitude == 0){		// If location is not set, don't use connection

								networkResponseString = "Nie znam położenia";
								return;

							}

							isConnecting = true;	// Flag

							new DownloadWebpageTask().execute(latitude,longitude);	// Do new asynch download task

						} else {	// If we don't have connection

							networkResponseString = "Brak połączenia";

						}   
					}
				}    
			});
		}
	}
	
	/* General UI info update
	 * */
	public void updateScreenInformation(){
		
		latitudeField.setText("latitude: "+ new String(""+latitude).substring(0));
		longitudeField.setText("longitude: "+new String(""+longitude).substring(0));
		speedMeter.setText(""+currentSpeed);
		speedLimit.setText(""+currentLimit);
		
		if(isBlocked){
			speedLimit.setTextColor(Color.parseColor("#FF0000"));
		} else {
			speedLimit.setTextColor(Color.parseColor("#0C8530"));
		}
		
		speedfield.setText("Speed: "+ speed +" m/s");
		networkResponseField.setText(networkResponseString);
		
		progressBarTimer.setProgress(timerCount);	// Progressbar update
		
		if(isGpsOn){
			gpsStatusField.setTextColor(Color.parseColor("#0C8530"));
			gpsStatusField.setText("GPS: ON");
		} else {
			gpsStatusField.setTextColor(Color.parseColor("#A3000B"));
			gpsStatusField.setText("GPS: OFF");
		}
		
		if(isBtOn){
			btStatusField.setTextColor(Color.parseColor("#2B84FF"));
			btStatusField.setText("BT: ON");
			
			if(bluetoothManager.isConnected()){
				
				btStatusField.setTextColor(Color.parseColor("#0C8530"));
				btStatusField.setText("BT: CONN");
				
			}
			
		} else {
			btStatusField.setTextColor(Color.parseColor("#A3000B"));
			btStatusField.setText("BT: OFF");
		}
		
		if(isConnected){
			internetField.setTextColor(Color.parseColor("#0C8530"));
			internetField.setText("Internet: ON");
		} else {
			internetField.setTextColor(Color.parseColor("#A3000B"));
			internetField.setText("Internet: OFF");
		}
		
		if(isConnecting){
			connectingField.setVisibility(TextView.VISIBLE);
		} else {
			connectingField.setVisibility(TextView.GONE);
		}
		
	}
	/* General method to do stuff when for exmpl. driver will go faster than limit
	 * */
	public void generalCheckDriver(){
		
		if(currentSpeed >= currentLimit && currentLimit != 0){	// Check if go too fast
   			goTooFast = true;	
		} else {
			goTooFast = false;
		}
		
		if(isBlocked){
			bluetoothManager.sendData(currentLimit);
		} else {
			bluetoothManager.sendData(0);
		}
		
		if(!isMuted && goTooFast){	// Sound alarm
			
			alarmSound.start();
			
		}
		
	}
	
	
	// ===========================================================================
	// NETWORK STUFF
	// ===========================================================================
	
	public int getMaxSpeed(double latitude, double longitude, int speed) throws IOException{
		
		InputStream is = null;		// Input Stream
		int len = 300;				// Max length of stream
		
		try {
			
	        URL url = new URL("http://gosafe.pe.hu/getData.php?lat="+latitude+"&lng="+longitude+"&speed="+speed);
	        
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        conn.setReadTimeout(10000 /* milliseconds */);
	        conn.setConnectTimeout(15000 /* milliseconds */);
	        conn.setRequestMethod("GET");
	        conn.setDoInput(true);
	        
	        // Starts the query
	        conn.connect();
	        int response = conn.getResponseCode();
	        Log.d("Zapytanie", "Kod zwrotny: " + response);
	        is = conn.getInputStream();

	        // Convert the InputStream into a string
	        String contentAsString = readIt(is, len);	// <== DOWNLOADED CONTENT
	        
	        try {
				
	        	JSONObject data = convertResponse(contentAsString);
	        	
	        	currentCity = data.getString("city");		// city
	        	currentStreet = data.getString("street");	// street
	        	
	        	Log.d("Zapytanie", "Ograniczenie:" + data.getInt("maxspeed"));
	        	
				return data.getInt("maxspeed");	// RETURN MAX SPEED
				
			} catch (JSONException e) {
				
				Log.e("Zapytanie", "Błąd konwersji", e);
				return 0;
				
			}
	        
	        
	    // Makes sure that the InputStream is closed after the app is
	    // finished using it.
	    } finally {
	        if (is != null) {
	            is.close();
	        } 
	    }
		
	}
	
	// Reads an InputStream and converts it to a String.
	public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
	    Reader reader = null;
	    reader = new InputStreamReader(stream, "UTF-8");        
	    char[] buffer = new char[len];
	    reader.read(buffer);
	    return new String(buffer);
	}
	
	/*	Przetwarza ciąg znaków w formacie Json i zwraca JSONObject
	 */
	public JSONObject convertResponse(String JsonResponseString) throws JSONException{
		
		// Przetwarzanie danych
	    JSONObject object = new JSONObject(JsonResponseString);
	    
	    if(object.getInt("maxspeed") > 0){
	    	
	    	networkResponseString = "Znaleziono dane";
	    	
	    } else {
	    	
	    	networkResponseString = "Nie znaleziono danych w tej lokalizacji";
	    	
	    }
	    
	    return object;
		
	}
	
	class DownloadWebpageTask extends AsyncTask<Double, Void, Integer> {
	       @Override
	       protected Integer doInBackground(Double... data) {
	              
	    	   try {
	    		   
	    		   return getMaxSpeed(data[0],data[1],currentSpeed);	// send location to get maxpseed
	    		   
	    	   } catch (IOException e) {
	    		   
	    		   Log.e("Zapytanie", "Nie udało się osiągnąć adresu ;(", e);
	    		   return 0;
	    		   
	    	   }
	           
	       }
	       
	       // onPostExecute displays the results of the AsyncTask.
	       @Override
	       protected void onPostExecute(Integer result) {
	           
	    	   currentLimit = result;
	    	   
	       }
	}
	
	
	// ===========================================================================
	// BUTTON FUNCTIONS
	// ===========================================================================
	
	public void optionsButton(View v) {
	    openOptionsMenu();
	}
	
	@Override
	public void onBackPressed() {
		openOptionsMenu();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
	    return true;
	    
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    switch (item.getItemId())
	    {
	    	case R.id.toggle_mute:
	    		
	    		toggleMute(item);
	    		
	    	return true;
	    	
	    	case R.id.toggle_unblock:
	    		
	    		toggleBlock(item);
	    		
	    	return true;
	    	
	    	case R.id.addSpeed:
	    		
	    		currentSpeed = 60;
	    		currentLimit = 50;
	    		
	    	return true;
	    	
	    	case R.id.setLimit:
	    	
	    		currentLimit = debugLimitValue;
	    		
	    		debugLimitValue += 10;
	    		
	    		if(debugLimitValue > 100){
	    			debugLimitValue = 10;
	    		}
	    		
	    	return true;
	    	
	    	case R.id.sendBT:
	    		
	    		if(bluetoothManager.sendData(40)){
	    			Toast.makeText(getApplicationContext(), "Wysłano", Toast.LENGTH_SHORT).show();
	    		} else {
	    			Toast.makeText(getApplicationContext(), "Błąd", Toast.LENGTH_SHORT).show();
	    		}
	    		
	    	return true;
	    	
	    	case R.id.connectBT:
	    		
	    		bluetoothManager.openBT();
	    		
	    	return true;
	        
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	public void toggleMute(final MenuItem item){
		
		if(isMuted == false){
			isMuted = true;
			Toast.makeText(getApplicationContext(), "Wyciszono", Toast.LENGTH_SHORT).show();
		} else {
			isMuted = false;
			Toast.makeText(getApplicationContext(), "Włączono dźwięk", Toast.LENGTH_SHORT).show();
		}
		
		item.setChecked(isMuted);	// <== setCheck for item
		
	}
	
	public void toggleBlock(final MenuItem item){
		
		if(isBlocked){	// IF BLOCKED
		
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
	
			alert.setTitle("Hasło?");
			
			// Set an EditText view to get user input 
			final EditText input = new EditText(this);
			input.setInputType(InputType.TYPE_CLASS_NUMBER);
			input.setHint("password");
			
			alert.setView(input);
	
			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			  
				String value = input.getText().toString();
			  
				if(value.equals(password)){
					
					isBlocked = false;
					Toast.makeText(getApplicationContext(), "Blokada zdjęta", Toast.LENGTH_SHORT).show();
					
				} else {
					
					Toast.makeText(getApplicationContext(), "Podano błędne hasło", Toast.LENGTH_SHORT).show();
					
				}
				
				item.setChecked(!isBlocked);	// <== setCheck for item
			  
			  }
			});
	
			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int whichButton) {
			    // Canceled.
			  }
			});
	
			alert.show();
			
		} else {	// IF UNBLOCKED
			
			isBlocked = true;
			item.setChecked(!isBlocked);	// <== setCheck for item
			
		}
		
	}
	
	
	// ===========================================================================
	// GPS STUFF
	// ===========================================================================

	
	
	@Override
	public void onLocationChanged(Location location) {
		
		latitude = location.getLatitude();
		longitude = location.getLongitude();
		speed = location.getSpeed();
		currentSpeed = (int)(speed*(60f*60f/1000f));
		
	}

	@Override
	public void onProviderDisabled(String provider) {
	//TODO	
		isGpsOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		
		isGpsOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extra) {
		
	}
}
