package edu.wallawalla.edu.kl7na.rotorcontrol;

//import android.os.Bundle;
//import android.app.Activity;
//import android.view.Menu;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;


public class RotorControl extends Activity {
    Button connectButton, sendButton, rotateAntennaButton;
    TextView textStatus;
    NetworkTask networktask;
    protected String rotateString;
    EditText editBearingText;
    ProgressBar rotateProgressBar;
    CountDownTimer countDownTimer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectButton = (Button)findViewById(R.id.connectButton);
        sendButton = (Button)findViewById(R.id.getHeadingButton);
        rotateProgressBar = (ProgressBar)findViewById(R.id.rotateProgressBar);
        rotateProgressBar.setVisibility(View.INVISIBLE);
        textStatus = (TextView)findViewById(R.id.textStatus);
        editBearingText = (EditText)findViewById(R.id.editBearingText);
        
        connectButton.setOnClickListener(connectButtonListener);
        sendButton.setOnClickListener(sendButtonListener);
       // editBearingText.addTextChangedListener(editBearingTextListener);

        networktask = new NetworkTask(); //Create initial instance so SendDataToNetwork doesn't throw an error.
    
        editBearingText= (EditText)findViewById(R.id.editBearingText);
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

    private OnClickListener connectButtonListener = new OnClickListener() {
        @SuppressLint("NewApi")
		public void onClick(View v){
            connectButton.setVisibility(View.INVISIBLE);
            networktask = new NetworkTask(); //New instance of NetworkTask
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                networktask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
            else
                networktask.execute((Void[])null);
            //networktask.execute();  
            //The above two lines were to solve a bug I
            // encountered and used the answer here:  
            // http://stackoverflow.com/questions/9119627/android-sdk-asynctask-doinbackground-not-running-subclass
        }
    };
    private OnClickListener sendButtonListener = new OnClickListener() {
        public void onClick(View v){
            textStatus.setText("Getting Heading:  ");
            networktask.SendDataToNetwork("p\n");
        }
    };
    
    public void goRotate(){
    	rotateProgressBar.setVisibility(View.VISIBLE);
    	int bearing = Integer.valueOf(rotateString);
    	while (bearing > 180) bearing -= 360;
    	while (bearing < -180) bearing += 360;
    	String cmdString = new String("P" + String.format("%3d",bearing) +" 0\n");
    	Log.v("Log_tag","Sent the string " + cmdString);
    	networktask.SendDataToNetwork(cmdString);
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
	        }
	    }.start();
		//rotateProgressBar.setVisibility(View.INVISIBLE);
		//rotateProgressBar.setProgress(1);
	    textStatus.setText("Getting Heading:  ");
        networktask.SendDataToNetwork("p/n");
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
                // This ip address needs to be changed for your setup.
                // Eventually we need to add some preferences.
                SocketAddress sockaddr = new InetSocketAddress("192.168.2.237", 4533);
                nsocket = new Socket();
                nsocket.connect(sockaddr, 10000); //10 second connection timeout (second parameter)
                if (nsocket.isConnected()) { 
                    nis = nsocket.getInputStream();
                    nos = nsocket.getOutputStream();
                    Log.i("AsyncTask", "doInBackground: Socket created, streams assigned");
                    Log.i("AsyncTask", "doInBackground: Waiting for inital data...");
                    byte[] buffer = new byte[4096];
                    int read = nis.read(buffer, 0, 4096); //This is blocking
                    while(read != -1){
                        byte[] tempdata = new byte[read];
                        System.arraycopy(buffer, 0, tempdata, 0, read);
                        publishProgress(tempdata);
                        Log.i("AsyncTask", "doInBackground: Got some data");
                        read = nis.read(buffer, 0, 4096); //This is blocking.
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
            }
        }

        @Override
        protected void onProgressUpdate(byte[]... values) {
            if (values.length > 0) {
                Log.i("AsyncTask", "onProgressUpdate: " + values[0].length + " bytes received.");
                textStatus.setText(new String(values[0]));
                rotateProgressBar.setVisibility(View.INVISIBLE);
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
                Log.i("AsyncTask", "onPostExecute: Completed.");
            }
            connectButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        networktask.cancel(true); //In case the task is currently running
    } 
}
