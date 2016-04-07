package pl.dualjack.nr;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothManager {
	
	private BluetoothAdapter blueAdapter;
	public BluetoothDevice blueDevice = null;
	private String DeviceAdress = "18:22:7E:7C:A9:25";	// Adress of device we will connect to
	
	ConnectedThread connectedThread;
	ConnectThread connectThread;
	
	public BluetoothManager(BluetoothAdapter blueAdapter){
		
		this.blueAdapter = blueAdapter;
		
		blueDevice = getPairedDevice(DeviceAdress);
		
	}
	
	public boolean isBluetoothOn(){
		
		if(blueAdapter.isEnabled()){
			return true;
		} else {
			return false;
		}
		
	}
	
	public void write(String data){
		
		if(connectedThread != null){
			Log.d("BT", "Wysyłam dane");
			connectedThread.write(data.getBytes());	
		} else {
			Log.d("BT", "Jeszcze nie mogę wysłać danych");
		}
		
	}
	
	public void connect(BluetoothDevice device){
 
        // Cancel any thread attempting to make a connection
		if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
		
        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
 
        // Start the thread to connect with the given device
        connectThread = new ConnectThread(device);
        connectThread.start();
		
	}
	
	public synchronized void connected(BluetoothSocket socket){
	
		// Cancel the thread that completed the connection
		if (connectThread != null) {
			connectThread.cancel();
			connectThread = null;
		}

		// Cancel any thread currently running a connection
		if (connectedThread != null) {
			connectedThread.cancel();
			connectedThread = null;
		}

		// Start the thread to manage the connection and perform transmissions
		connectedThread = new ConnectedThread(socket);
		connectedThread.start();
		
    }
	
	/* Returns device which we are working with */
	public BluetoothDevice getProjectDevice(){
		
		return blueDevice;
		
	}
	
	public BluetoothDevice getPairedDevice(String deviceAdress){
		
		Set<BluetoothDevice> pairedDevices = blueAdapter.getBondedDevices();	// List of paired devices
		
		// If there are paired devices
		if (pairedDevices.size() > 0) {
			
		    // Loop through paired devices
		    for (BluetoothDevice device : pairedDevices) {

		        Log.d("BT", device.getName() +" - "+ device.getAddress());
		        
		        if(device.getAddress().equalsIgnoreCase(deviceAdress)){	// find which paired device has that adress
		        
		        	return device;
		        	
		        }
		    }
		}
		
		Log.e("BT", "Nie znaleziono sparowanego urządzenia o adresie "+ deviceAdress);
		return null;	// if didn't find that paired device
	}
	
	class ConnectThread extends Thread {
		
	    private final BluetoothSocket mmSocket;
	 
	    public ConnectThread(BluetoothDevice device) {
	    	
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
	 
	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	        	
	        	UUID uuid = device.getUuids()[0].getUuid();
	        	
	            tmp = device.createInsecureRfcommSocketToServiceRecord(uuid);
	        } catch (IOException e) {
	        	
	        	Log.e("BT","Nie można utworzyć socketu",e);
	        	
	        }
	        
	        mmSocket = tmp;
	    }
	 
	    public void run() {
	 
	        try {
	        	
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            mmSocket.connect();
	            
	        } catch (IOException connectException) {
	        	
	            // Unable to connect; close the socket and get out
	            try {
	            	Log.e("BT","Nie udało się połączyć",connectException);
	                mmSocket.close();
	            } catch (IOException closeException) { }
	            
	            return;
	        }
	 
	        connected(mmSocket);
	    }
	 
	    /** Will cancel an in-progress connection, and close the socket */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
	
	class ConnectedThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final OutputStream mmOutStream;
	 
	    public ConnectedThread(BluetoothSocket socket) {
	        mmSocket = socket;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	 
	        mmOutStream = tmpOut;
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write(byte[] bytes) {
	        try {
	            mmOutStream.write(bytes);
	        } catch (IOException e) {
	        	
	        	Log.e("BT", "Błąd wysyłania bajtów", e);
	        	
	        }
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}

}
