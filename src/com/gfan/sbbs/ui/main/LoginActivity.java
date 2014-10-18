package com.gfan.sbbs.ui.main;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.gfan.sbbs.bean.User;
import com.gfan.sbbs.file.utils.FileUtils;
import com.gfan.sbbs.http.HttpException;
import com.gfan.sbbs.othercomponent.BBSOperator;
import com.gfan.sbbs.othercomponent.MyApplication;
import com.gfan.sbbs.othercomponent.Preferences;
import com.gfan.sbbs.task.GenericTask;
import com.gfan.sbbs.task.TaskAdapter;
import com.gfan.sbbs.task.TaskListener;
import com.gfan.sbbs.task2.TaskResult;
import com.umeng.analytics.MobclickAgent;

public class LoginActivity extends SherlockFragmentActivity {

	private EditText userNameText, passwdText;
	private Button mLoginButton, mGuestButton;
	private GenericTask mLoginTask;

	private String mUserName, mPasswd, mToken, errorCause;
	private SharedPreferences mPreferences;
	private User user;
	private static final String TAG = "LoginActivity";
	public static final String LOGIN_OK = "login_ok";
	public static final String START_LOGIN = "start_login";

	private TaskListener mLoginTaskListener = new TaskAdapter() {
		ProgressDialog pdialog;

		@Override
		public String getName() {

			return "mLoginTaskListener";
		}

		@Override
		public void onPreExecute(GenericTask task) {
			pdialog = new ProgressDialog(LoginActivity.this);
			pdialog.setMessage(getString(R.string.login_message));
			pdialog.show();
			pdialog.setCanceledOnTouchOutside(false);
		}

		@Override
		public void onPostExecute(GenericTask task, TaskResult result) {
			super.onPostExecute(task, result);
			pdialog.dismiss();
			onLoginComplete(result);
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(com.actionbarsherlock.R.style.Theme_Sherlock_Light);
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.login);
		initArgs();
		initEvents();
		FileUtils.getInstance().cleanQueues();
	}

	@Override
	public void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}

	private void initArgs() {
		userNameText = (EditText) this.findViewById(R.id.txt_username);
		passwdText = (EditText) this.findViewById(R.id.txt_password);
		mLoginButton = (Button) this.findViewById(R.id.btn_login);
		mGuestButton = (Button) this.findViewById(R.id.btn_guestLogin);

		mPreferences = MyApplication.mPreference;

	}

	private void initEvents() {
		boolean remember = MyApplication.getInstance().isRememberMe();
		if (remember) {
			mUserName = mPreferences.getString(Preferences.USER_NAME, "");
			mPasswd = mPreferences.getString(Preferences.USER_PWD, "");
			userNameText.setText(mUserName);
			passwdText.setText(mPasswd);
		}
		mLoginButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				doLogin();
			}
		});
		mGuestButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onLoginFinish(false);
			}
		});

		boolean autoLogin = MyApplication.getInstance().isAutoLogin();
		boolean hasLogined = MyApplication.getInstance().getmPreference()
				.contains(Preferences.USER_NAME);
		if (autoLogin && hasLogined) {
			User currentUser = new User(mUserName, mPasswd);
			SharedPreferences preferences = MyApplication.getInstance()
					.getmPreference();
			String user_token = preferences.getString(Preferences.USER_TOKEN,
					"");
			currentUser.setToken(user_token);
			MyApplication.getInstance().setCurrentUser(currentUser);
			onLoginFinish(true);
		}
		passwdText.setOnKeyListener(new View.OnKeyListener() {

			@Override
			public boolean onKey(View arg0, int keyCode, KeyEvent event) {
				if (KeyEvent.KEYCODE_ENTER == keyCode
						&& event.getAction() == KeyEvent.ACTION_DOWN) {
					doLogin();
					return true;
				}
				return false;
			}
		});
	}

	private void showSuccess() {
		Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
	}

	private void showFailure() {
		Toast.makeText(this, errorCause, Toast.LENGTH_SHORT).show();
	}

	private void doLogin() {
		mUserName = userNameText.getText().toString().trim();
		mPasswd = passwdText.getText().toString().trim();
		if (validate(mUserName, mPasswd)) {
			mLoginTask = new LoginTask();
			mLoginTask.setListener(mLoginTaskListener);
			mLoginTask.execute(mUserName, mPasswd);
		}
	}

	private boolean validate(String userID, String passwd) {
		if (TextUtils.isEmpty(userID) || TextUtils.isEmpty(passwd)) {
			return false;
		}
		return true;
	}

	private void onLoginComplete(TaskResult taskResult) {
		if (TaskResult.Failed == taskResult) {
			showFailure();
			return;
		}
		showSuccess();
		mToken = user.getToken();
		updateInfo();
		onLoginFinish(true);
	}

	private void onLoginFinish(boolean isLogined) {

		if (isFromOtherActivity() && isLogined) {
			Bundle data = new Bundle();
			data.putBoolean(LOGIN_OK, true);
			Intent resultIntent = new Intent();
			resultIntent.putExtras(data);
			setResult(RESULT_OK, resultIntent);
		} else if (isFromOtherActivity() && !isLogined) {
			Bundle data = new Bundle();
			data.putBoolean(LOGIN_OK, false);
			Intent resultIntent = new Intent();
			resultIntent.putExtras(data);
			setResult(RESULT_OK, resultIntent);
		} else {
			startHomeActivity();

		}
		finish();
	}

	private boolean isFromOtherActivity() {
		Bundle bundle = getIntent().getBundleExtra(START_LOGIN);
		if (null == bundle) {
			return false;
		}
		return true;
	}

	private void startHomeActivity() {
		Intent intent = new Intent(this, Home.class);
		startActivity(intent);
	}

	private void updateInfo() {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString(Preferences.USER_NAME, mUserName);
		editor.putString(Preferences.USER_PWD, mPasswd);
		editor.putString(Preferences.USER_TOKEN, mToken);
		editor.putBoolean(Preferences.IS_LOGINED, true);
		editor.commit();
		// MyApplication.getInstance().setLogined(true);
		MyApplication.getInstance().setCurrentUser(user);
		Log.i(TAG, mUserName + " login success");
	}

	@Override
	protected void onDestroy() {
		if (mLoginTask != null
				&& mLoginTask.getStatus() == GenericTask.Status.RUNNING) {
			mLoginTask.cancel(true);
		}
		super.onDestroy();
	}

	private class LoginTask extends GenericTask {

		@Override
		protected TaskResult _doInBackground(String... params) {
			try {
				user = BBSOperator.getInstance().doLogin(mUserName, mPasswd);
			} catch (HttpException e) {
				e.printStackTrace();
				errorCause = e.getMessage();
				return TaskResult.Failed;
			}
			return TaskResult.OK;

		}
	}

}
