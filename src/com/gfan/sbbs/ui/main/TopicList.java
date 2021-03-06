package com.gfan.sbbs.ui.main;

import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.gfan.sbbs.bean.Topic;
import com.gfan.sbbs.dao.topic.PostHelper;
import com.gfan.sbbs.http.HttpException;
import com.gfan.sbbs.othercomponent.BBSOperator;
import com.gfan.sbbs.othercomponent.MyApplication;
import com.gfan.sbbs.task.GenericTask;
import com.gfan.sbbs.task.TaskAdapter;
import com.gfan.sbbs.task.TaskListener;
import com.gfan.sbbs.task2.TaskResult;
import com.gfan.sbbs.ui.Abstract.BaseActivity;
import com.gfan.sbbs.ui.Adapter.TopicListAdapter;
import com.gfan.sbbs.utils.MyListView;
import com.gfan.sbbs.utils.MyListView.OnLoadMoreDataListener;
import com.umeng.analytics.MobclickAgent;

/**
 * 版面界面
 * 
 * @author Nine
 * 
 */
public class TopicList extends BaseActivity implements OnLoadMoreDataListener {
	private TextView moreBtn;
	private View moreView;
	private LinearLayout progressbar;

	private String boardID, baseUrl, errorCause;

	public MyListView allTopics;
	private List<Topic> topicList, cList, fList, tList, dList, mList, thList;
	private boolean isFirstLoad = true, hasMoreData = true;
	private TopicListAdapter myAdapter;
	private GenericTask doRetrieveTask;

	private static final int LOADNUM = 20;
	private int start = 0, cStart = 0, fStart = 0, tStart = 0, dStart = 0,
			mStart = 0, th_Start = 0;
	private int mode = 0;
	private int headPosition = 1, cHPosition = 1, fHPosition = 1,
			tHPosition = 1, dHPosition = 1, mHPosition = 1, thHPosition = 1;
	private static final int CLASSIC_MODE = 1;
	private static final int THREAD_MODE = 2;
	private static final int FORUM_MODE = 3;
	private static final int ON_TOP_MODE = 4;
	private static final int DIGEST_MODE = 5;
	private static final int MARK_MODE = 6;

	private static final int MENU_NEW = 10;
	private static final String TAG = "TopicList";

	public static final String EXTRA_MODE = "mode";

	private TaskListener mRetrieveTaskListener = new TaskAdapter() {
		private ProgressDialog pdialog;

		@Override
		public String getName() {
			return "mRetrieveTaskListener";
		}

		@Override
		public void onPreExecute(GenericTask task) {
			super.onPreExecute(task);
			if (isFirstLoad) {
				pdialog = new ProgressDialog(TopicList.this);
				pdialog.setMessage(getResources().getString(R.string.loading));
				pdialog.show();
				pdialog.setCanceledOnTouchOutside(false);
			}
		}

		@Override
		public void onPostExecute(GenericTask task, TaskResult result) {
			super.onPostExecute(task, result);
			if (null != pdialog) {
				pdialog.dismiss();
			}
			allTopics.onRefreshComplete();
			if (getResult(result)) {
				draw();
				goTop();
			}
		}
	};

	@Override
	protected void _onCreate(Bundle savedInstanceState) {
		super._onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.list_without_header);
		initArgs();
		bindListener();
		allTopics.addFooterView(moreView);
		doRetrieve();
	}

	private void initArgs() {
		allTopics = (MyListView) this.findViewById(R.id.my_list);
		myAdapter = new TopicListAdapter(this);
		allTopics.setAdapter(myAdapter);
		moreView = getLayoutInflater().inflate(R.layout.moredata, null);
		moreBtn = (TextView) moreView.findViewById(R.id.load_more_btn);
		progressbar = (LinearLayout) moreView.findViewById(R.id.more_progress);

		topicList = new ArrayList<Topic>();
		cList = new ArrayList<Topic>();
		tList = new ArrayList<Topic>();
		dList = new ArrayList<Topic>();
		mList = new ArrayList<Topic>();
		fList = new ArrayList<Topic>();
		thList = new ArrayList<Topic>();

		Bundle bundle = getIntent().getExtras();
		boardID = bundle.getString(PostHelper.EXTRA_BOARD);
		if (bundle.containsKey(EXTRA_MODE)) {
			mode = bundle.getInt(EXTRA_MODE);
		}

		baseUrl = "http://bbs.seu.edu.cn/api/board/" + boardID + ".json?"
				+ "limit=" + LOADNUM;
		if (isLogined()) {
			baseUrl = baseUrl.concat("&token=" + MyApplication.getInstance().getToken());
		}
		if ("recommend".equals(boardID.toLowerCase())) {
			setTitle("推荐文章");
		} else {
			setTitle(boardID + "版");
		}
	}

	private void bindListener() {
		moreBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				doLoadMore();
			}
		});
		allTopics.setLoadMoreListener(this);
		allTopics.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long arg1) {
				Topic topic = getContextItemTopic(position);
				if (null == topic) {
					return;
				}
				boolean flag = mode == FORUM_MODE - 1;
				Intent intent = null;
				Bundle bundle = new Bundle();
				if (!flag) {
					intent = new Intent(TopicList.this,
							SinglePostActivity.class);

				} else {
					intent = new Intent(TopicList.this, ThreadList.class);
				}
				bundle.putInt(PostHelper.EXTRA_ID, topic.getId());
				bundle.putString(PostHelper.EXTRA_TITLE, topic.getTitle());
				bundle.putString(PostHelper.EXTRA_BOARD, boardID);
				topic.setUnRead(false);
				intent.putExtras(bundle);
				startActivity(intent);
				draw();
			}
		});
		allTopics.setonRefreshListener(new MyListView.OnRefreshListener() {

			@Override
			public void onRefresh() {
				isFirstLoad = true;
				start = 0;
				doRetrieve();
			}
		});

	}

	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
		myAdapter.refresh();
	}

	@Override
	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}

	private void doRetrieve() {
		if (null != doRetrieveTask
				&& doRetrieveTask.getStatus() == GenericTask.Status.RUNNING) {
			return;
		}
		doRetrieveTask = new RetrieveTopicTask();
		doRetrieveTask.setListener(mRetrieveTaskListener);
		String url = getBaseUrl().concat("&start=" + start + "&mode=" + mode);
		doRetrieveTask.execute(url);
	}

	private void doLoadMore() {
		moreBtn.setVisibility(View.GONE);
		progressbar.setVisibility(View.VISIBLE);
		doRetrieve();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		int position = item.getItemId();
		Log.i(TAG, "positon is " + position);
		if (MENU_NEW == position) {
			Intent intent = new Intent(TopicList.this, WritePost.class);
			Bundle bundle = new Bundle();
			bundle.putInt(PostHelper.EXTRA_TYPE, PostHelper.TYPE_NEW);
			bundle.putString(PostHelper.EXTRA_BOARD, boardID);
			intent.putExtras(bundle);
			startActivity(intent);
		} else if (position > 0 && position <= 6) {
			changeView(position - 1);
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		menu.add(0, MENU_NEW, 0, "new post")
				.setIcon(R.drawable.ic_compose_inverse)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

		SubMenu submenu = menu.addSubMenu("menu more");
		submenu.add(Menu.NONE, CLASSIC_MODE, Menu.NONE, R.string.board_classic);
		submenu.add(Menu.NONE, THREAD_MODE, Menu.NONE, R.string.board_thread);
		submenu.add(Menu.NONE, FORUM_MODE, Menu.NONE, R.string.board_forum);
		submenu.add(Menu.NONE, ON_TOP_MODE, Menu.NONE, R.string.board_top);
		submenu.add(Menu.NONE, DIGEST_MODE, Menu.NONE, R.string.board_digest);
		submenu.add(Menu.NONE, MARK_MODE, Menu.NONE, R.string.board_mark);

		MenuItem subMenuItem = submenu.getItem();
		subMenuItem.setIcon(R.drawable.switcher_inverse);
		subMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS
				| MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		return super.onCreateOptionsMenu(menu);
	}

	private List<Topic> getTopicList(int mode) {
		switch (mode + 1) {
		case CLASSIC_MODE:
			return cList;
		case FORUM_MODE:
			return fList;
		case ON_TOP_MODE:
			return tList;
		case DIGEST_MODE:
			return dList;
		case MARK_MODE:
			return mList;
		case THREAD_MODE:
			return thList;
		default:
			return null;
		}
	}

	private void setParams(int mode) {
		this.mode = mode;
		switch (mode + 1) {

		case CLASSIC_MODE:
			start = cStart;
			headPosition = cHPosition;
			break;
		case FORUM_MODE:
			start = fStart;
			headPosition = fHPosition;
			break;
		case ON_TOP_MODE:
			start = tStart;
			headPosition = tHPosition;
			break;
		case DIGEST_MODE:
			start = dStart;
			headPosition = dHPosition;
			break;
		case MARK_MODE:
			start = mStart;
			headPosition = mHPosition;
			break;
		case THREAD_MODE:
			start = th_Start;
			headPosition = thHPosition;
			break;
		}
	}

	private void saveParams(int mode, int value, int xPosition) {
		// here,mode should be the current mode
		switch (mode + 1) {
		case CLASSIC_MODE:
			cStart = value;
			cHPosition = xPosition;
			break;
		case FORUM_MODE:
			fStart = value;
			fHPosition = xPosition;
			break;
		case ON_TOP_MODE:
			tStart = value;
			tHPosition = xPosition;
			break;
		case DIGEST_MODE:
			dStart = value;
			dHPosition = xPosition;
			break;
		case MARK_MODE:
			mStart = value;
			mHPosition = xPosition;
			break;
		case THREAD_MODE:
			th_Start = value;
			thHPosition = xPosition;
			break;
		}
	}

	private void changeView(int newMode) {
		if (newMode == this.mode) {
			Toast.makeText(this, "您当前正在此模式下", Toast.LENGTH_SHORT).show();
			return;
		}
		List<Topic> nowList = getTopicList(this.mode);// get current topList
		nowList.clear();// clear it
		nowList.addAll(topicList);// pass the topicList to nowList
		List<Topic> newList = getTopicList(newMode);
		topicList.clear();
		topicList.addAll(newList);
		saveParams(this.mode, start, headPosition);
		setParams(newMode);
		this.mode = newMode;
		Log.i(TAG, "start is " + start + ",position is " + headPosition);
		if (0 == topicList.size()) {

			isFirstLoad = true;
			doRetrieve();
		} else {
			draw();
		}
	}

	public boolean getResult(TaskResult result) {
		moreBtn.setVisibility(View.VISIBLE);
		progressbar.setVisibility(View.GONE);
		if (TaskResult.Failed == result) {
			Toast.makeText(this, errorCause, Toast.LENGTH_SHORT).show();
			return false;
		}
		if (null == result || TaskResult.NO_DATA == result) {
			Toast.makeText(this, "没有啦", Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}

	private Topic getContextItemTopic(int position) {
		if (position >= 1 && position <= myAdapter.getCount()) {
			return (Topic) myAdapter.getItem(position - 1);
		}
		return null;
	}

	private void draw() {
		myAdapter.refresh(topicList);
	}

	private void goTop() {
		allTopics.setSelection(headPosition);
	}

	@Override
	protected void onDestroy() {
		if (null != doRetrieveTask
				&& doRetrieveTask.getStatus() == GenericTask.Status.RUNNING) {
			doRetrieveTask.cancel(true);
		}
		super.onDestroy();
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	private class RetrieveTopicTask extends GenericTask {
		@Override
		protected TaskResult _doInBackground(String... params) {
			List<Topic> newList;
			try {
				newList = BBSOperator.getInstance().getTopicList(params[0]);
			} catch (HttpException e) {
				e.printStackTrace();
				errorCause = e.getMessage();
				return TaskResult.Failed;
			}
			if (isFirstLoad) {
				topicList = newList;
				headPosition = 1;
			} else {
				headPosition = topicList.size();
				topicList.addAll(newList);
			}
			if (newList.size() < LOADNUM) {
				hasMoreData = false;
			} else {
				hasMoreData = true;
			}
			isFirstLoad = false;
			start = topicList.size();
			if (topicList.size() == 0) {
				return TaskResult.NO_DATA;
			}
			return TaskResult.OK;
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

	@Override
	public void onLoadMoreData() {
		int lastItem = allTopics.getLastItemIndex();
		Log.i(TAG,
				"lastItem is " + lastItem + "getCount is "
						+ myAdapter.getCount());
		if (hasMoreData && lastItem == myAdapter.getCount()) {
			doLoadMore();
		}
	}
}
