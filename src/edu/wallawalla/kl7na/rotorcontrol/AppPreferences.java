package edu.wallawalla.kl7na.rotorcontrol;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

/*public class AppPreferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

}  The above is a simple way to do it, but it is depreciated, 
so I used the stuff below.*/
public class AppPreferences extends PreferenceActivity {
	private static int prefs=R.xml.preferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            onCreatePreferenceActivity();
        } else {
            onCreatePreferenceFragment();
        }
    }

    //
    // Wraps legacy {@link #onCreate(Bundle)} code for Android < 3 (i.e. API lvl
    // < 11).
    //
    @SuppressWarnings("deprecation")
    private void onCreatePreferenceActivity() {
        addPreferencesFromResource(R.xml.preferences);
    }

    //
    // Wraps {@link #onCreate(Bundle)} code for Android >= 3 (i.e. API lvl >=
    // 11).
    //
    @SuppressLint("NewApi")
    public static class MyPreferenceFragment extends PreferenceFragment
    {       
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(AppPreferences.prefs); //outer class private members seem to be visible for inner class, and making it static made things so much easier
        }
    }
    @SuppressLint("NewApi")
    private void onCreatePreferenceFragment() {
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new MyPreferenceFragment ())
                .commit();
    }
}

