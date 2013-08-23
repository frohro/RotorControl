package edu.wallawalla.kl7na.rotorcontrol;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

public class Help extends Activity {
	ScrollView helpScrollView;
	TextView helpTextView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
		helpScrollView = (ScrollView)findViewById(R.id.helpScrollView);
		helpTextView = (TextView)findViewById(R.id.helpTextView);
	}

}
