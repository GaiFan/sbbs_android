package com.gfan.sbbs.ui.main;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.widget.CheckBox;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.gfan.sbbs.othercomponent.MyApplication;
import com.gfan.sbbs.othercomponent.Preferences;

public class Preference extends SherlockPreferenceActivity implements
		OnPreferenceChangeListener,OnPreferenceClickListener {
//	private EditTextPreference blackListSettings;
//	private ListPreference mStartPage;
//	private ListPreference mFontAjustPreference;
//	private MyApplication application;
	private CheckBoxPreference rememberBox;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
		super.onCreate(savedInstanceState);
		setTitle(getString(R.string.pref_title));
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		addPreferencesFromResource(R.xml.preference);
		init();
	}

	private void init() {
		rememberBox = (CheckBoxPreference) this.findPreference(Preferences.REMEMBER_ME);
		rememberBox.setOnPreferenceChangeListener(this);
	}


	@Override
	public boolean onPreferenceChange(android.preference.Preference preference,
			Object newValue) {


		if(preference.getKey().equals(Preferences.REMEMBER_ME)){
			if((Boolean)newValue == false){
				SharedPreferences.Editor editor = MyApplication.getInstance().getmPreference().edit();
				editor.putBoolean(Preferences.AUTOLOGIN, false);
				editor.commit();
			}
			
		}
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case android.R.id.home:
			finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPreferenceClick(android.preference.Preference preference) {
		return true;
	}

}
