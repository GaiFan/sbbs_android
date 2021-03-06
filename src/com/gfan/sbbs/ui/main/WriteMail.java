package com.gfan.sbbs.ui.main;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.gfan.sbbs.othercomponent.BBSOperator;
import com.gfan.sbbs.othercomponent.MyApplication;
import com.gfan.sbbs.othercomponent.SBBSConstants;
import com.gfan.sbbs.task.GenericTask;
import com.gfan.sbbs.task.TaskAdapter;
import com.gfan.sbbs.task.TaskListener;
import com.gfan.sbbs.task2.TaskResult;
import com.gfan.sbbs.ui.Abstract.BaseActivity;
import com.umeng.analytics.MobclickAgent;

public class WriteMail extends BaseActivity {
	private EditText titleView, recieverView, contentView;
	private String reid, title, reciever, content, errorCause,token;
	private GenericTask doSendTask;
	private static final int MENU_SEND = 0;
	public static final String EXTRA_RECIEVER = "reciever";
	public static final String EXTRA_TITLE = "title";
	public static final String EXTRA_CONTENT = "content";
	public static final String EXTRA_REID = "reid";
	private String sendUrl = SBBSConstants.BASE_API_URL+"/mail/send.json?token=";

	private TaskListener mSendTaskListener = new TaskAdapter() {
		ProgressDialog pdialog;

		@Override
		public String getName() {
			return "mSendTaskListener";
		}

		@Override
		public void onPreExecute(GenericTask task) {
			super.onPreExecute(task);
			pdialog = new ProgressDialog(WriteMail.this);
			pdialog.setMessage(getString(R.string.mail_sending));
			pdialog.show();
			pdialog.setCanceledOnTouchOutside(false);
		}

		@Override
		public void onPostExecute(GenericTask task, TaskResult result) {
			super.onPostExecute(task, result);
			pdialog.dismiss();
			processResult(result);
		}
	};

	@Override
	protected void _onCreate(Bundle savedInstanceState) {
		super._onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}

	private void initUrls(){
		token = MyApplication.getInstance().getToken();
		sendUrl += token;
	}
	private boolean canSend() {
		if(TextUtils.isEmpty(token)){
			return false; 
		}
		title = titleView.getText().toString();
		reciever = recieverView.getText().toString().trim();
		content = contentView.getText().toString().trim();
		if (TextUtils.isEmpty(title)) {
			Toast.makeText(WriteMail.this, R.string.mail_title_empty, Toast.LENGTH_SHORT).show();
			return false;
		}
		if (TextUtils.isEmpty(reciever)) {
			Toast.makeText(WriteMail.this, R.string.mail_receiver_empty, Toast.LENGTH_SHORT)
					.show();
			return false;
		}
		return true;
	}

	private void doSend() {
		doSendTask = new DoSendTask();
		doSendTask.setListener(mSendTaskListener);
		doSendTask.execute(reciever, title, content, reid, token);
	}

	@Override
	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}

	private void initView() {
		titleView = (EditText) this.findViewById(R.id.new_mail_title);
		contentView = (EditText) this.findViewById(R.id.new_mail_content);
		recieverView = (EditText) this.findViewById(R.id.new_mail_to);
	}

	private void initArgs(){
		String reciever = "";
		Bundle bundle = getIntent().getExtras();
		if (null != bundle) {
			reciever = getIntent().getExtras().getString(EXTRA_RECIEVER);
			reid = getIntent().getExtras().getString(EXTRA_REID);
			if (reid == null) {
				reid = "0";
			}
			String getTitle = getIntent().getExtras().getString(EXTRA_TITLE);
			if (!TextUtils.isEmpty(reciever)) {
				recieverView.setText(reciever);
				recieverView.setEnabled(false);
				titleView.requestFocus();
				String getContent = getIntent().getExtras().getString(EXTRA_CONTENT);
				if(null != getContent){
					contentView.setHint(getContent);
				}
			}

			if (getTitle != null) {
				if (getTitle.contains("Re:")) {
					titleView.setText(getTitle);
				} else {
					titleView.setText("Re: " + getTitle);
				}
				contentView.requestFocus();
				contentView.setSelection(0);
			}
		}
	}
	private void processResult(TaskResult result) {

		if (TaskResult.Failed == result) {
			Toast.makeText(this, errorCause, Toast.LENGTH_SHORT).show();
			return;
		}
		Toast.makeText(this, R.string.send_mail_success, Toast.LENGTH_SHORT).show();
		finish();
	}

	@Override
	protected void onDestroy() {

		if (doSendTask != null
				&& doSendTask.getStatus() == GenericTask.Status.RUNNING) {
			doSendTask.cancel(true);
		}
		super.onDestroy();
	}

	private class DoSendTask extends GenericTask {

		@Override
		protected TaskResult _doInBackground(String... params) {
			boolean result = false;
			result = BBSOperator.getInstance().doPostMail(sendUrl, params[0], params[1],
					params[2], params[3]);
			if (result) {
				return TaskResult.OK;
			} else {
				return TaskResult.Failed;
			}
		}

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SEND:
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(contentView.getWindowToken(), 0);
			if (canSend()) {
				doSend();
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_SEND, 0, "send")
				.setIcon(R.drawable.ic_menu_send_holo_light_inverse)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void processUnLogin() {
		Toast.makeText(this, R.string.unlogin_notice, Toast.LENGTH_SHORT).show();
		finish();
		return;
	}

	@Override
	protected void setup() {
		this.setContentView(R.layout.write_mail);
		initView();
		initArgs();
		initUrls();
		setTitle(getString(R.string.mail_interface_title));

	}
}
