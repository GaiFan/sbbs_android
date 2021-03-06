package com.gfan.sbbs.fragment;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gfan.sbbs.bean.Topic;
import com.gfan.sbbs.db.TopicDAO;
import com.gfan.sbbs.db2.TopicTable;
import com.gfan.sbbs.http.HttpException;
import com.gfan.sbbs.othercomponent.ActivityFragmentTargets;
import com.gfan.sbbs.othercomponent.BBSOperator;
import com.gfan.sbbs.othercomponent.MyApplication;
import com.gfan.sbbs.othercomponent.OnOpenActivityFragmentListener;
import com.gfan.sbbs.othercomponent.SBBSConstants;
import com.gfan.sbbs.task.GenericTask;
import com.gfan.sbbs.task.TaskAdapter;
import com.gfan.sbbs.task.TaskListener;
import com.gfan.sbbs.task2.TaskResult;
import com.gfan.sbbs.ui.Adapter.TopTenAdapter;
import com.gfan.sbbs.ui.base.BaseViewModel;
import com.gfan.sbbs.ui.base.HomeViewModel;
import com.gfan.sbbs.ui.main.R;
import com.gfan.sbbs.utils.MyListView;
import com.umeng.analytics.MobclickAgent;

public class HotFragment extends SherlockFragment implements
		BaseViewModel.OnViewModelChangObserver, OnNavigationListener {

	private View mLayout;
	private LayoutInflater mInflater;
	private OnOpenActivityFragmentListener mOnOpenActivityFragmentListener;
	private MyListView hotListView;
	private List<Topic> hotList, siteHotList;
	private List<List<Topic>> hotSectionListGroup;
	private int selectedItem, type;
	private TopTenAdapter myAdapter;
	private GenericTask mRetrieveTask;
	private String hotUrl, hotSectionUrl, errorCause;
	private boolean forceLoad = false, isLoaded = false;
	private HomeViewModel mHomeViewModel;

	private static final String TAG = "HotFragment";
	private static final int MENU_REFRESH = 0;
	private TaskListener mRetrieveHotTaskListener = new TaskAdapter() {
		ProgressDialog pdialog;

		@Override
		public String getName() {
			return "mRetrieveHotTaskListener";
		}

		@Override
		public void onPreExecute(GenericTask task) {
			super.onPreExecute(task);
			pdialog = new ProgressDialog(getSherlockActivity());
			pdialog.setMessage(getResources().getString(R.string.loading));
			pdialog.show();
		}

		@Override
		public void onPostExecute(GenericTask task, TaskResult result) {
			super.onPostExecute(task, result);
			pdialog.cancel();
			isLoaded = true;
			hotListView.onRefreshComplete();
			processResult(result);
		}

	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		setRetainInstance(true);
		hotList = new ArrayList<Topic>();
		hotSectionListGroup = new ArrayList<List<Topic>>();
		for (int i = 0; i < 13; i++) {
			List<Topic> hotSectionList = new ArrayList<Topic>();
			hotSectionListGroup.add(hotSectionList);
		}
		siteHotList = new ArrayList<Topic>();
		hotList = siteHotList;
		selectedItem = 0;
		type = TopicTable.TYPE_HOT;

		Log.i(TAG, "OnCreate");
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		menu.add(0, MENU_REFRESH, 0, "refresh").setIcon(R.drawable.ic_refresh)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	
	
	@Override
	public void onPause() {
		super.onPause();
		MobclickAgent.onPageEnd("HotFragment");
	}

	@Override
	public void onResume() {
		super.onResume();
		MobclickAgent.onPageStart("HotFragment");
		draw();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Activity parentActivity = getSherlockActivity();
		if (parentActivity instanceof OnOpenActivityFragmentListener) {
			mOnOpenActivityFragmentListener = (OnOpenActivityFragmentListener) parentActivity;
		}
		Context context = getSherlockActivity().getSupportActionBar()
				.getThemedContext();
		ArrayAdapter<CharSequence> list = ArrayAdapter.createFromResource(
				context, R.array.hot, R.layout.sherlock_spinner_item);
		list.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
//		getSherlockActivity().getSupportActionBar().setNavigationMode(
//				ActionBar.NAVIGATION_MODE_LIST);

		getSherlockActivity().getSupportActionBar().setListNavigationCallbacks(
				list, this);

		initArgs();
		initEvent();
		Log.i(TAG, TAG + "-->initFinished.type is " + type + ",selectedItem is "
				+ selectedItem);
		if (null != mHomeViewModel.getCurrentTab()
				&& mHomeViewModel.getCurrentTab().equals(
						ActivityFragmentTargets.TAB_HOT)) {
			getSherlockActivity().getSupportActionBar().setNavigationMode(
					ActionBar.NAVIGATION_MODE_LIST);
			doRetrieve();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mInflater = inflater;
		mLayout = inflater.inflate(R.layout.list_without_header, null);
		MyApplication application = (MyApplication) getActivity()
				.getApplication();
		mHomeViewModel = application.getmHomeViewModel();
		mHomeViewModel.registerViewModelChangeObserver(this);
		Log.i(TAG, "onCreateView");
		return mLayout;
	}

	private void initArgs() {
		hotListView = (MyListView) mLayout.findViewById(R.id.my_list);
		myAdapter = new TopTenAdapter(mInflater);
		hotListView.setAdapter(myAdapter);
		hotUrl = SBBSConstants.HOTURL;
		hotSectionUrl = SBBSConstants.HOT_SECTIONS;
		if (MyApplication.getInstance().checkLogin()) {
			hotUrl = hotUrl.concat("?token=" + MyApplication.getInstance().getToken());
			hotSectionUrl = hotSectionUrl.concat("?token="
					+ MyApplication.getInstance().getToken());
		}
	}

	private void initEvent() {
		hotListView.setonRefreshListener(new MyListView.OnRefreshListener() {

			@Override
			public void onRefresh() {
				forceLoad = true;
				doRetrieve();
			}
		});
		hotListView
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1,
							int position, long id) {
						Topic topic = getContextItemTopic(position);
						Bundle bundle = new Bundle();
						bundle.putInt("id", topic.getId());
						Log.i("TopTen", "choose " + position);
						bundle.putString("title", topic.getTitle());
						bundle.putString("boardID", topic.getBoardName());
						if (null != mOnOpenActivityFragmentListener) {
							mOnOpenActivityFragmentListener
									.onOpenActivityOrFragment(
											ActivityFragmentTargets.ON_TOPIC,
											bundle);
						}
					}
				});
	}

	private void doRetrieve() {
		if (null != mRetrieveTask
				&& GenericTask.Status.RUNNING == mRetrieveTask.getStatus()) {
			return;
		}
		mRetrieveTask = new RetrievetHotTask();
		mRetrieveTask.setListener(mRetrieveHotTaskListener);
		if (0 == selectedItem) {
			mRetrieveTask.execute(hotUrl);
		} else {
			String url;
			if (MyApplication.getInstance().checkLogin()) {
				url = hotSectionUrl.concat("&section=" + type);
			} else {
				url = hotSectionUrl.concat("?section=" + type);
			}
			mRetrieveTask.execute(url);
		}
		Log.i(TAG, TAG + "-->doRetrieve");
	}

	private void draw() {
		myAdapter.refresh(hotList);
	}

	private void goTop() {
		hotListView.setSelection(1);
	}

	private Topic getContextItemTopic(int position) {
		if (position >= 1 && position <= myAdapter.getCount()) {
			return (Topic) myAdapter.getItem(position - 1);
		}
		return null;
	}

	private void processResult(TaskResult result) {
		if (TaskResult.Failed == result) {
			Toast.makeText(MyApplication.mContext, errorCause,
					Toast.LENGTH_SHORT).show();
			return;
		}
		if (result == TaskResult.NO_DATA) {
			Toast.makeText(MyApplication.mContext, R.string.hot_no_data,
					Toast.LENGTH_SHORT).show();
			return;
		}
		forceLoad = false;
		draw();
		goTop();

	}

	private List<Topic> getList(int selectType) {
		if (TopicTable.TYPE_HOT == selectType) {
			return siteHotList;
		} else {
			return hotSectionListGroup.get(selectType);
		}
	}

	private void changeView(int newType) {
		if (type == newType) {
			return;
		}
		List<Topic> newList = getList(newType);
		List<Topic> nowList = getList(type);
		nowList.clear();
		nowList.addAll(hotList);
		hotList = newList;
		type = newType;
		if (hotList.size() == 0) {
			doRetrieve();
		} else {
			draw();
		}
	}

	@Override
	public void onDestroy() {
		if (null != mRetrieveTask
				&& mRetrieveTask.getStatus() == GenericTask.Status.RUNNING) {
			mRetrieveTask.cancel(true);
		}
		super.onDestroy();
	}

	private class RetrievetHotTask extends GenericTask {
		@Override
		protected TaskResult _doInBackground(String... params) {
			TopicDAO topicDAO = new TopicDAO(MyApplication.mContext);
			List<Topic> list = topicDAO.fetchTopics(type);
			if (list.size() != 0 && !forceLoad) {
				hotList = list;
				Log.i(TAG, "get topics from database");
			} else {
				// hotList = SBBSSupport.getTopicListApi(arg0[0]);
				try {
					hotList = BBSOperator.getInstance().getTopicList(params[0]);
				} catch (HttpException e) {
					e.printStackTrace();
					errorCause = e.getMessage();
					return TaskResult.Failed;
				}
				topicDAO.deleteList(type);
				long id = topicDAO.insertTopic(hotList, type);
				Log.i(TAG, "force load,id is " + id);
			}
			if (null == hotList || hotList.size() == 0) {
				return TaskResult.NO_DATA;
			}
			return TaskResult.OK;
		}
	}

	@Override
	public void onViewModelChange(BaseViewModel viewModel,
			String changedPropertyName, Object... params) {
		if (HomeViewModel.CURRENTTAB_PROPERTY_NAME.equals(changedPropertyName)) {
			if (!isLoaded
					&& mHomeViewModel.getCurrentTab().equals(
							ActivityFragmentTargets.TAB_HOT)) {
				doRetrieve();
			} else if (isLoaded
					&& mHomeViewModel.getCurrentTab().equals(
							ActivityFragmentTargets.TAB_HOT)) {
				draw();
				Log.i(TAG, "HotFragment draw");
			}
		}
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		selectedItem = itemPosition;
		int newType = 0;
		if (selectedItem == 0) {
			newType = TopicTable.TYPE_HOT;
		} else {
			newType = selectedItem - 1;
		}
		changeView(newType);
		return true;
	}

}
