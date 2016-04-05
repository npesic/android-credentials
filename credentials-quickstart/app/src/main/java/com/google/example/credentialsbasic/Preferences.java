package com.google.example.credentialsbasic;

import android.content.SharedPreferences;

public class Preferences {
	
	private static String PREFERENCES = "UserPreferences";

	public static String getSettingsParam(String paramName) {
		SharedPreferences settings = getPreferences();
		return settings.getString(paramName, "");
	}

	public static SharedPreferences getPreferences() {
		SharedPreferences settings = ApplicationContextProvider.getContext()
				.getSharedPreferences(PREFERENCES, 0);
		return settings;
	}

	public static void setSettingsParam(String paramName, String paramValue) {
		SharedPreferences settings = getPreferences();
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(paramName, paramValue);
		editor.commit();
	}

	public static void setSettingsParamLong(String paramName, long paramValue) {
		SharedPreferences settings = getPreferences();
		SharedPreferences.Editor editor = settings.edit();
		editor.putLong(paramName, paramValue);
		editor.commit();
	}

}
