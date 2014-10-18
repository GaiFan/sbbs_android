package com.gfan.sbbs.othercomponent;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.gfan.sbbs.bean.User;
import com.gfan.sbbs.ui.base.HomeViewModel;
import com.gfan.sbbs.utils.images.LazyImageLoader;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

public class MyApplication extends Application {
	public static Context mContext;
	private static MyApplication globalContext = null;
	private Activity activity;
	public static LazyImageLoader mImageLoader;
	private User currentUser = null;
	private int updateInterval, startPage;
	public static int screenWidth = 480;
	public static SharedPreferences mPreference;
	String[] blackList;
	public static final HomeViewModel mHomeViewModel = new HomeViewModel();

	@Override
	public void onCreate() {
		super.onCreate();
		mPreference = PreferenceManager.getDefaultSharedPreferences(this);
		blackList = new String[20];
		mContext = this.getApplicationContext();
		mImageLoader = new LazyImageLoader();
		globalContext = this;
		initImageLoader(getApplicationContext());
	}

	public static MyApplication getInstance() {
		return globalContext;
	}

	public Activity getActivity() {
		return this.activity;
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
	}

	public SharedPreferences getmPreference() {
		return mPreference;
	}

	public static void initImageLoader(Context context) {
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				context).threadPriority(Thread.NORM_PRIORITY - 2)
				.denyCacheImageMultipleSizesInMemory()
				.discCacheFileNameGenerator(new Md5FileNameGenerator())
				.tasksProcessingOrder(QueueProcessingType.LIFO).build();
		// Initialize ImageLoader with configuration.
		ImageLoader.getInstance().init(config);
	}

	/**
	 * TODO
	 * 
	 * @param passwd
	 * @return
	 */
	public String encrypt(String passwd) {
		byte[] array = passwd.getBytes();

		return array.toString();
	}

	public String[] getBlackList() {
		return this.blackList;
	}

	public void setBlackList(String[] blackList) {
		this.blackList = blackList;
	}

	public void setUpdateInterval(int updateInterval) {
		this.updateInterval = updateInterval;
	}

	public int getUpdateInterval() {
		return updateInterval;
	}

	public String getToken() {
		String token = getCurrentUser().getToken();
		try {
			token = URLEncoder.encode(token, SBBSConstants.SBBS_ENCODING);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
		return token;
	}

	public boolean isOne_topic() {
		if (mPreference.contains(Preferences.OneTopic)) {
			return mPreference.getBoolean(Preferences.OneTopic, false);
		} else {
			SharedPreferences.Editor editor = mPreference.edit();
			editor.putBoolean(Preferences.OneTopic, true);
			editor.commit();
		}
		return false;
	}

	public boolean isNightMode() {
		if (mPreference.contains(Preferences.NIGHT_MODE)) {
			return mPreference.getBoolean(Preferences.NIGHT_MODE, false);
		}
		return false;
	}

	public boolean isAutoLogin() {
		if (mPreference.contains(Preferences.AUTOLOGIN)) {
			return mPreference.getBoolean(Preferences.AUTOLOGIN, false);
		} else {
			if (mPreference.contains(Preferences.USER_NAME)) {
				SharedPreferences.Editor editor = mPreference.edit();
				editor.putBoolean(Preferences.REMEMBER_ME, true);
				editor.putBoolean(Preferences.AUTOLOGIN, true);
				editor.commit();
			}
		}
		return false;
	}

	public boolean isRememberMe() {
		if (mPreference.contains(Preferences.REMEMBER_ME)) {
			return mPreference.getBoolean(Preferences.REMEMBER_ME, false);
		} else {
			if (mPreference.contains(Preferences.USER_NAME)) {
				SharedPreferences.Editor editor = mPreference.edit();
				editor.putBoolean(Preferences.REMEMBER_ME, true);
				editor.commit();
			}
		}
		return false;
	}

	public boolean checkLogin() {
		// String userName = mPreference.getString(Preferences.USER_NAME, "");
		// if (TextUtils.isEmpty(userName)) {
		// return false;
		// }
		// return true;
		if (null == currentUser) {
			return false;
		}
		return true;
	}

	public HomeViewModel getmHomeViewModel() {
		return mHomeViewModel;
	}

	public void setStartPage(int startPage) {
		this.startPage = startPage;
	}

	public int getStartPage() {
		String str = mPreference.getString(Preferences.SELECT_PAGE, "0");
		startPage = Integer.valueOf(str);
		return startPage;
	}

	public User getCurrentUser() {
		return currentUser;
	}

	public void setCurrentUser(User currentUser) {
		this.currentUser = currentUser;
	}

}
