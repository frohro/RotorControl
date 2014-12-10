package edu.wallawalla.kl7na.rotorcontrol;



import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;


public class RotorControl extends Activity {
	Button connectButton, getHeadingButton;
	TextView textStatus, inputHeadingTextView, degreesTextView;
	NetworkTask networkTask;
	protected String rotateString;
	EditText editBearingText;
	ProgressBar rotateProgressBar;
	CountDownTimer countDownTimer;
	AppPreferences ourPreferences;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		connectButton = (Button)findViewById(R.id.connectButton);
		connectButton.setOnClickListener(connectButtonListener);
		getHeadingButton = (Button)findViewById(R.id.getHeadingButton);
		getHeadingButton.setOnClickListener(sendButtonListener);
		getHeadingButton.setVisibility(View.INVISIBLE);
		rotateProgressBar = (ProgressBar)findViewById(R.id.rotateProgressBar);
		rotateProgressBar.setVisibility(View.INVISIBLE);
		textStatus = (TextView)findViewById(R.id.textStatus);
		textStatus.setVisibility(View.INVISIBLE);
		inputHeadingTextView = (TextView)findViewById(R.id.inputHeadingTextView);
		inputHeadingTextView.setVisibility(View.INVISIBLE);
		editBearingText = (EditText)findViewById(R.id.editBearingText);
		editBearingText.setVisibility(View.INVISIBLE);
		editBearingText = (EditText)findViewById(R.id.editBearingText);
		degreesTextView = (TextView)findViewById(R.id.degreesTextView);
		degreesTextView.setVisibility(View.INVISIBLE);

		connectToRotor(); //This is an attempt to get out of pressing the connect button at startup.

		editBearingText.setOnEditorActionListener(new OnEditorActionListener()
		{ //This whole business is to perform our rotation when the user hits the DONE key.
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				if(actionId == EditorInfo.IME_ACTION_DONE)
				{
					rotateString = v.getText().toString();
					goRotate();
				}
				return false;
			}
		});
	}

	private void connectToRotor() {
		try {  
			networkTask = new NetworkTask(); 
			Log.i("MainTask", "Setting up the network connection.\n");
			connectButton.setVisibility(View.INVISIBLE);
			connectButton.setVisibility(View.INVISIBLE);
			getHeadingButton.setVisibility(View.VISIBLE);
			editBearingText.setVisibility(View.VISIBLE);
			textStatus.setVisibility(View.VISIBLE);
			textStatus.setText("");
			inputHeadingTextView.setVisibility(View.VISIBLE);
			degreesTextView.setVisibility(View.VISIBLE);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				networkTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
			else
				//The above three lines were to solve a bug I
				// encountered and used the answer here:  
				// http://stackoverflow.com/questions/9119627/android-sdk-asynctask-doinbackground-not-running-subclass
				networkTask.execute((Void[])null);
			//getHeading();  Would be nice, but you need to give the network time to get set up.
		} 
		catch (Exception e) {
			e.printStackTrace();
			Log.i("MainTask", "Exception setting up the connection.  Perhaps you don't have networkTask.executeOnExecutorything set up yet.\n");
			connectButton.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.menu_activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.helpMenu:
			try {
				startActivity(new Intent(this,edu.wallawalla.kl7na.rotorcontrol.Help.class));
			}
			catch ( ActivityNotFoundException e) {
				e.printStackTrace();
			}
			return true;
		case R.id.menu_settings:
			try {
				startActivity(new Intent(this, edu.wallawalla.kl7na.rotorcontrol.AppPreferences.class));
			}
			catch ( ActivityNotFoundException e) {
				e.printStackTrace();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private OnClickListener connectButtonListener = new OnClickListener() {
		@Override
		@SuppressLint("NewApi")
		public void onClick(View v){
			connectToRotor();
		}
	};
	private OnClickListener sendButtonListener = new OnClickListener() {
		@Override
		public void onClick(View v){
			getHeading();
		}
	};

	private void getHeading() {
		textStatus.setText("Working on your request...  ");
		networkTask.SendDataToNetwork("p\n");
	}

	public void goRotate(){
		rotateProgressBar.setVisibility(View.VISIBLE);
		getHeadingButton.setVisibility(View.INVISIBLE);
		int bearing = Integer.valueOf(rotateString);
		while (bearing > 180) bearing -= 360;
		while (bearing < -180) bearing += 360;
		String cmdString = new String("P" + String.format("%3d",bearing) +" 0\n");
		Log.v("Log_tag","Sent the string " + cmdString);
		networkTask.SendDataToNetwork(cmdString);
		final long length_in_milliseconds=100000, period_in_milliseconds=1000;
		countDownTimer = new CountDownTimer(length_in_milliseconds, period_in_milliseconds) {

			@Override
			public void onTick(long millisUntilFinished) {
				int i=0;
				//Log.v("Log_tag", "Tick of Progress"+ i+ millisUntilFinished);
				i++;
				rotateProgressBar.setProgress(i);
			}
			@Override
			public void onFinish() {
				rotateProgressBar.setVisibility(View.INVISIBLE);
				getHeadingButton.setVisibility(View.VISIBLE);
			}
		}.start();
		getHeading();
	}

	public class NetworkTask extends AsyncTask<Void, byte[], Boolean> {
		Socket nsocket; //Network Socket
		InputStream nis; //Network Input Stream
		OutputStream nos; //Network Output Stream

		@Override
		protected void onPreExecute() {
			Log.i("AsyncTask", "onPreExecute");
		}

		@Override
		protected Boolean doInBackground(Void... params) { //This runs on a different thread
			boolean result = false;
			try {
				Log.i("AsyncTask", "doInBackground: Creating socket");
				SharedPreferences ourPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				String strIPAddress = ourPreferences.getString("ip_address", "192.168.2.237");
				String strPortNumber = ourPreferences.getString("port", "4533");
				SocketAddress sockaddr = new InetSocketAddress(strIPAddress, Integer.valueOf(strPortNumber));
				nsocket = new Socket();
				nsocket.connect(sockaddr, 10000); //10 second connection timeout (second parameter)
				if (nsocket.isConnected()) { 
					nis = nsocket.getInputStream();
					nos = nsocket.getOutputStream();
					Log.i("AsyncTask", "doInBackground: Socket created, streams assigned");
					Log.i("AsyncTask", "doInBackground: Waiting for inital data...");
					byte[] buffer = new byte[256];
					int read = nis.read(buffer, 0, 256); //This is blocking
					while(read != -1){
						byte[] tempdata = new byte[read];
						System.arraycopy(buffer, 0, tempdata, 0, read);
						publishProgress(tempdata);
						Log.i("AsyncTask", "doInBackground: Got some data");
						read = nis.read(buffer, 0, 256); //This is blocking.
					}
				}
				else Log.i("AsyncTask", "doInBackground: nsocket not connected");
			} catch (IOException e) {
				e.printStackTrace();
				Log.i("AsyncTask", "doInBackground: IOException");
				result = true;
			} catch (Exception e) {
				e.printStackTrace();
				Log.i("AsyncTask", "doInBackground: Exception");
				result = true;
			} finally {
				try {
					nis.close();
					nos.close();
					nsocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
				Log.i("AsyncTask", "doInBackground: Finished");
			}
			return result;
		}



		public void SendDataToNetwork(String cmd) { //You run this from the main thread.
			try {
				if (nsocket.isConnected()) {
					Log.i("AsyncTask", "SendDataToNetwork: Writing received message to socket");
					nos.write(cmd.getBytes());
				} else {
					Log.i("AsyncTask", "SendDataToNetwork: Cannot send message. Socket is closed");
				}
			} catch (Exception e) {
				Log.i("AsyncTask", "SendDataToNetwork: Message send failed. Caught an exception");
				e.printStackTrace();
			}
		}

		@Override
		protected void onProgressUpdate(byte[]... values) {
			if (values.length > 0) {
				Log.i("AsyncTask", "onProgressUpdate: " + values[0].length + " bytes received.");
				String returnedString = new String(values[0]);
				String[] splitString;// = new String;
				splitString = returnedString.split("\\.");
				// The following if statement is to fix a bug where I was getting the long format for the heading 
				// for some reason.  It seems to happen after setting the heading, but this is a work
				// around.
				if (splitString[0].toCharArray()[0] == new String("g").toCharArray()[0]) 
				{
					Log.i("AsyncTask", "Got that get string again!=\n");
					textStatus.setVisibility(View.INVISIBLE);
					getHeading();
				}
				else 
				{
					//String formattedBearing = new String(splitString[0].split(".")[0]);
					textStatus.setText(splitString[0] + " degrees");
					textStatus.setVisibility(View.VISIBLE);
				}
				rotateProgressBar.setVisibility(View.INVISIBLE);
				getHeadingButton.setVisibility(View.VISIBLE);
			}
		}
		@Override
		protected void onCancelled() {
			Log.i("AsyncTask", "Cancelled.");
			connectButton.setVisibility(View.VISIBLE);
		}
		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				Log.i("AsyncTask", "onPostExecute: Completed with an Error.");
				textStatus.setText("There was a connection error.");
			} else {
				Log.i("AsyncTask", "onPostExecute: Completed without error.");
				textStatus.setText("Connnected!");
			}
			connectButton.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		networkTask.cancel(true); //In case the task is currently running
	} 
	
}
