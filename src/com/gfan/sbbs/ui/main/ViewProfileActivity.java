package com.gfan.sbbs.ui.main;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.gfan.sbbs.bean.User;
import com.gfan.sbbs.http.HttpException;
import com.gfan.sbbs.othercomponent.BBSOperator;
import com.gfan.sbbs.othercomponent.MyApplication;
import com.gfan.sbbs.othercomponent.SBBSConstants;
import com.gfan.sbbs.task.GenericTask;
import com.gfan.sbbs.task.TaskAdapter;
import com.gfan.sbbs.task.TaskListener;
import com.gfan.sbbs.task2.TaskResult;
import com.gfan.sbbs.ui.Abstract.BaseActivity;
import com.umeng.analytics.MobclickAgent;

public class ViewProfileActivity extends BaseActivity {
	private User user;
	private String userID,errorCause;
	private TextView userName, nickName, lifeValue, identity, loginTime,
			postTime, performValue, experience, astrology, genderView,
			lsLoginTimeView;
	private GenericTask doRetrieveTask, doAddFriendTask;
	private static final int MENU_SEARCH = 100;
	private static final int MENU_MAIL = 101;
	private static final int MENU_ADD = 102;
	private static final int MENU_SHARE = 103;
	public static final String EXTRA_USER = "userID";

	private TaskListener mRetrieveTaskListener = new TaskAdapter() {
		private ProgressDialog pdialog;

		@Override
		public String getName() {
			return "mRetrieveTaskListener";
		}

		@Override
		public void onPreExecute(GenericTask task) {
			super.onPreExecute(task);
			pdialog = new ProgressDialog(ViewProfileActivity.this);
			pdialog.setMessage(getResources().getString(R.string.loading));
			pdialog.show();
			pdialog.setCanceledOnTouchOutside(false);
		}

		@Override
		public void onPostExecute(GenericTask task, TaskResult result) {
			super.onPostExecute(task, result);
			pdialog.dismiss();
			if (getResult(result)) {
				if (null != user) {
					setProfile();
				}
			}
		}

	};

	private TaskListener mAddFriendListener = new TaskAdapter() {

		@Override
		public String getName() {
			return "mAddFriendListener";
		}

		@Override
		public void onPostExecute(GenericTask task, TaskResult result) {
			processAdd(result);
		}
	};

	@Override
	protected void _onCreate(Bundle savedInstanceState) {
		super._onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		this.setContentView(R.layout.user_profile);
		initArgs();
		doRetrieve();
	}

	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}


	private void initArgs() {
		try {
			userID = getIntent().getExtras().getString("userID");
		} catch (NullPointerException e) {
			userID = MyApplication.getInstance().getCurrentUser().getId();
		}
		userName = (TextView) this.findViewById(R.id.profile_userid);
		nickName = (TextView) this.findViewById(R.id.profile_user_nickname);
		lifeValue = (TextView) this.findViewById(R.id.profile_aliveness);
		identity = (TextView) this.findViewById(R.id.profile_identity);
		genderView = (TextView) this.findViewById(R.id.profile_gender);
		astrology = (TextView) this.findViewById(R.id.profile_astrololgy);
		loginTime = (TextView) this.findViewById(R.id.profile_login_times);
		postTime = (TextView) this.findViewById(R.id.profile_post_number);
		performValue = (TextView) this
				.findViewById(R.id.profile_user_perform_value);
		experience = (TextView) this.findViewById(R.id.profile_user_experience);
		lsLoginTimeView = (TextView) this.findViewById(R.id.profile_last_login);

	}

	private void doRetrieve() {
		doRetrieveTask = new RetrieveTask();
		doRetrieveTask.setListener(mRetrieveTaskListener);
		doRetrieveTask.execute(userID);
	}

	private void doAdd() {
		if (!isLogined) {
			Toast.makeText(this, R.string.login_indicate, Toast.LENGTH_SHORT).show();
			return;
		}
		doAddFriendTask = new DoAddFriendTask();
		doAddFriendTask.setListener(mAddFriendListener);
		doAddFriendTask.execute(userID, token);
	}

	private void processAdd(TaskResult result) {
		if (TaskResult.IO_ERROR == result) {
			Toast.makeText(this, R.string.load_io_error, Toast.LENGTH_SHORT).show();
			return;
		}
		if (TaskResult.Failed == result) {
			Toast.makeText(this, R.string.profile_add_friends_failed, Toast.LENGTH_SHORT).show();
			return;
		}
		if (TaskResult.OK == result) {
			Toast.makeText(this, R.string.profile_add_friends_success, Toast.LENGTH_SHORT).show();
			return;
		}
	}

	private boolean getResult(TaskResult result) {

		if (TaskResult.Failed == result) {
			Toast.makeText(this, errorCause, Toast.LENGTH_SHORT)
					.show();
			finish();
			return false;
		}
		return true;

	}

	private void setProfile() {
		userID = user.getId();
		setTitle(userID + getResources().getString(R.string.profile_view_title));
		userName.setText(user.getId());

		if (user.getGender().equals("other")) {
			userName.setTextColor(0xff000000);
			genderView.setText(R.string.profile_user_sex_unknown);
		} else {
			if (user.getGender().equals("M")) {
				userName.setTextColor(0xff0000ff);
				genderView.setText(R.string.profile_user_sex_m);
			} else {
				userName.setTextColor(0xffFF34B3);
				genderView.setText(R.string.profile_user_sex_f);
			}
		}
		nickName.setText(user.getNickName());
		lifeValue.setText(user.getLifeValue());
		identity.setText(user.getIdentity());
		loginTime.setText(user.getLoginTime());
		postTime.setText(user.getPostTime());
		performValue.setText(user.getPerformValue());
		experience.setText(user.getExperience());
		astrology.setText(user.getAstrology());
		lsLoginTimeView.setText(user.getLastLoginTime());
	}

	private class RetrieveTask extends GenericTask {
//		private UserUtils userUtils;

		@Override
		protected TaskResult _doInBackground(String... params) {
			//				userUtils = SBBSSupport.getUserProfileAPI(params[0].trim());
			try {
				user = BBSOperator.getInstance().getUserProfile(SBBSConstants.BASE_URL+"/api/user/"+params[0]+".json");
			} catch (HttpException e) {
				e.printStackTrace();
				errorCause = e.getMessage();
				return TaskResult.Failed;
			}
			return TaskResult.OK;
		}

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SEARCH:
			item.setActionView(R.layout.collapsible_edittext);
			final EditText searchInput = (EditText) item.getActionView()
					.findViewById(R.id.search_input);
			item.setOnActionExpandListener(new MenuItem.OnActionExpandListener(){

				@Override
				public boolean onMenuItemActionExpand(MenuItem item) {
					searchInput.post(new Runnable() {
						
						@Override
						public void run() {
							searchInput.requestFocus();
							InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				            imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
						}
					});
					return true;
				}

				@Override
				public boolean onMenuItemActionCollapse(MenuItem item) {
					return true;
				}});
			searchInput.setOnKeyListener(new View.OnKeyListener() {

				@Override
				public boolean onKey(View view, int keyCode, KeyEvent event) {
					if (KeyEvent.KEYCODE_ENTER == keyCode
							&& event.getAction() == KeyEvent.ACTION_DOWN) {
						String input = searchInput.getText().toString();
						doSearch(input);
						return true;
					}
					return false;
				}
			});
			break;
		case MENU_MAIL: {
			Intent intent = new Intent(ViewProfileActivity.this, WriteMail.class);
			Bundle bundle = new Bundle();
			bundle.putString("reciever", userID);
			intent.putExtras(bundle);
			startActivity(intent);
			break;
		}
		case MENU_ADD:
			doAdd();
			break;
		case MENU_SHARE: {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			String link = "http://bbs.seu.edu.cn/bbsqry.php?userid="
					+ user.getId();
			intent.putExtra(Intent.EXTRA_SUBJECT, "");
			intent.putExtra(Intent.EXTRA_TEXT,
					"#"+getResources().getString(R.string.profile_user_share)+"# ID:" + user.getId() + "：" + link);
			startActivity(Intent.createChooser(intent, getTitle()));
			break;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	private void doSearch(String input) {
		if (TextUtils.isEmpty(input.trim())) {
			return;
		}
		Intent intent = new Intent(ViewProfileActivity.this, ViewProfileActivity.class);
		Bundle bundle = new Bundle();
		try {
			bundle.putString("userID", URLEncoder.encode(input.trim(),
					SBBSConstants.SBBS_ENCODING));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return;
		}
		intent.putExtras(bundle);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_SEARCH, Menu.NONE, "search")
				.setIcon(R.drawable.ic_menu_search_inverse)
				.setActionView(R.layout.collapsible_edittext)
				.setShowAsAction(
						MenuItem.SHOW_AS_ACTION_ALWAYS
								| MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		menu.add(Menu.NONE, MENU_ADD, Menu.NONE, R.string.profile_user_add)
				.setIcon(R.drawable.ic_menu_add_inverse)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(Menu.NONE, MENU_MAIL, Menu.NONE, R.string.profile_user_mail)
				.setIcon(R.drawable.ic_menu_new_mail_inverse)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menu.add(Menu.NONE, MENU_SHARE, Menu.NONE, R.string.profile_user_share)
				.setIcon(R.drawable.ic_menu_share_inverse)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

		return true;

	}

	private class DoAddFriendTask extends GenericTask {

		@Override
		protected TaskResult _doInBackground(String... params) {
			String id = params[0];
			String token = params[1];
			String url = "http://bbs.seu.edu.cn/api/friends/add.json?token="
					+ token + "&id=" + id;
//				boolean success = SBBSSupport.dealFriends(url);
			boolean success = BBSOperator.getInstance().getBoolean(url);
			if (success) {
				return TaskResult.OK;
			} else {
				return TaskResult.Failed;
			}
		}
	}

	@Override
	protected void processUnLogin() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void setup() {
		// TODO Auto-generated method stub

	}
}
