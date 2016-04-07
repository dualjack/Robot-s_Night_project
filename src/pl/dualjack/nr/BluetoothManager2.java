package pl.dualjack.nr;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothManager2 {
	
	private BluetoothAdapter blueAdapter;
	private BluetoothSocket mmSocket;
	private OutputStream mmOutputStream;
	private BluetoothDevice blueDevice;
	private ConnectThread connectThread;
	private String TAG = "BluetoothManager";
	
	// dv6500 00:1A:6B:F3:67:C7  arduino board 30:14:10:27:16:18
	private String deviceAdress = "30:14:10:27:16:18";	// Adress of device we will connect to
	private boolean isConnected = false;

	public BluetoothManager2(){
		
		blueAdapter = BluetoothAdapter.getDefaultAdapter();
		
	}
	
	boolean isBluetoothOn(){
		
		return blueAdapter.isEnabled();
		
	}
	
	void setDevice(String adress){
		
		this.blueDevice = getPairedDevice(adress);
		
	}

	public BluetoothDevice getPairedDevice(String deviceAdress){
		
		Set<BluetoothDevice> pairedDevices = blueAdapter.getBondedDevices();	// List of paired devices
		
		// If there are paired devices
		if (pairedDevices.size() > 0) {
			
		    // Loop through paired devices
		    for (BluetoothDevice device : pairedDevices) {

		        Log.d(TAG, device.getName() +" - "+ device.getAddress());
		        
		        if(device.getAddress().equalsIgnoreCase(deviceAdress)){	// find which paired device has that adress
		        	
		        	Log.d(TAG,"Znaleziono sparowane urządzenie o podanym adresie");
		        	return device;
		        	
		        }
		    }
		}
		
		// if didn't find that paired device
		Log.e(TAG, "Nie znaleziono sparowanego urządzenia o adresie "+ deviceAdress);
		return null;
	}

	void openBT(){
		
		if(!isBluetoothOn()) return;	// if bt is off, stop
		
		setDevice(deviceAdress);
		if(blueDevice == null) return;	// if didn't find paired device, stop
		
		
		if(connectThread != null){
			connectThread.cancel();
		}
		
		connectThread = new ConnectThread();
		connectThread.start();
		
		// bluetooth opened
	}
	
	/* Check if connection works. Return true, if socket between devices is connected */
	boolean isConnected(){
		
		return isConnected;
		
	}
	
	class ConnectThread extends Thread {
		
		public ConnectThread() {
			
			final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard //SerialPortService ID

			try {
				mmSocket = blueDevice.createRfcommSocketToServiceRecord(uuid);
				Log.d(TAG, "Utworzono socket");
			} catch (IOException e) {
				Log.e(TAG, "Nie utworzono socketu", e);
			}
			
		}
		
		@Override
		public void run() {
			
			blueAdapter.cancelDiscovery();
			
			try {
				mmSocket.connect();
				isConnected = true;
				
				Log.d(TAG,"Połączono socket! :)");
			} catch (IOException e) {
				isConnected = false;
				
				Log.e(TAG, "Błąd połączenia", e);
			}

			try {
				mmOutputStream = mmSocket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "Błąd outputStream", e);
			}
		}
		
		/** Will cancel an in-progress connection, and close the socket and Stream */
	    public void cancel() {
	        try {
	        	mmOutputStream.close();
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
		
	}

	boolean sendData(Integer data){
		
		if(mmOutputStream != null && isConnected()){
			try {
				
				mmOutputStream.write(data.byteValue());
				Log.d(TAG,""+data.byteValue());
				//mmOutputStream.write(data.byteValue());		// For sending integer
	
				Log.d(TAG,"Wysłano!");
				return true;
				
			} catch (IOException e) {
				
				isConnected = false;
				
				Log.e(TAG, "Nie udało się wysłać danych", e);
				return false;
				
			}
		} else {
			Log.d(TAG,"Usiłowano wysłać dane bez nawiązania łączności");
			return false;
		}

	}

	void closeBT() throws IOException {

		mmOutputStream.close();
		mmSocket.close();

	}

}
