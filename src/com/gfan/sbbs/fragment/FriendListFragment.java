package com.gfan.sbbs.fragment;

import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.gfan.sbbs.bean.User;
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
import com.gfan.sbbs.ui.Adapter.FriendsListAdapter;
import com.gfan.sbbs.ui.base.BaseViewModel;
import com.gfan.sbbs.ui.base.HomeViewModel;
import com.gfan.sbbs.ui.main.R;
import com.gfan.sbbs.utils.MyListView;
import com.umeng.analytics.MobclickAgent;

public class FriendListFragment extends SherlockFragment implements
		BaseViewModel.OnViewModelChangObserver {

	private LayoutInflater mInflater;
	private View mLayout;
	private MyListView friendListView;
	private List<User> friendList;
	private FriendsListAdapter myAdapter;
	private OnOpenActivityFragmentListener mOnOpenActivityListener;
	private HomeViewModel mHomeViewModel;

	private GenericTask mRetrieveTask, mDelFriendsTask;
	private boolean isLoaded = false, isLogined;
	private String friendsUrl, errorCause;

	private static final int MENU_VIEWPROFILE = 0;
	private static final int MENU_MAIL = 1;
	private static final int MENU_DELETE = 2;
	private int positionSelected;
	@SuppressWarnings("unused")
	private ActionMode actionMode;

	private static final String TAG = "FriendListFragment";
	private TaskListener mRetrieveTaskListener = new TaskAdapter() {

		private ProgressDialog pdialog;

		@Override
		public String getName() {
			return "mRetrieveTaskListener";
		}

		@Override
		public void onPreExecute(GenericTask task) {
			super.onPreExecute(task);
			pdialog = new ProgressDialog(getSherlockActivity());
			pdialog.setMessage(getResources().getString(R.string.loading));
			pdialog.show();
			pdialog.setCancelable(false);
		}

		@Override
		public void onPostExecute(GenericTask task, TaskResult result) {
			super.onPostExecute(task, result);
			pdialog.cancel();
			friendListView.onRefreshComplete();

			processResult(result);
		}
	};

	private TaskListener mDelTaskListener = new TaskAdapter() {

		@Override
		public String getName() {
			return "mDelTaskListener";
		}

		@Override
		public void onPostExecute(GenericTask task, TaskResult result) {
			super.onPostExecute(task, result);
			processDelResult(result);
		}

	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		setRetainInstance(true);
		Log.i(TAG, "OnCreate");

		isLogined = MyApplication.getInstance().checkLogin();
		friendsUrl = SBBSConstants.FRIENDS_URL;
		if (isLogined) {
			friendsUrl = friendsUrl
					.concat("?token=" + MyApplication.getInstance().getToken());
		}
	}

	
	@Override
	public void onPause() {
		super.onPause();
		MobclickAgent.onPageEnd("FriendListFragment");
	}


	@Override
	public void onResume() {
		super.onResume();
		MobclickAgent.onPageStart("FriendListFragment");
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Activity parentActivity = getSherlockActivity();
		if (parentActivity instanceof OnOpenActivityFragmentListener) {
			mOnOpenActivityListener = (OnOpenActivityFragmentListener) parentActivity;
		}
		initArgs();
		initEvents();
		if (null != mHomeViewModel.getCurrentTab()
				&& ActivityFragmentTargets.TAB_FRIENDS.equals(mHomeViewModel
						.getCurrentTab())) {
			if (!isLogined) {
				processUnLogin();
			} else {
				doRetrieve();
			}	
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mInflater = inflater;
		mLayout = mInflater.inflate(R.layout.list_without_header, null);
		mHomeViewModel = ((MyApplication) getSherlockActivity()
				.getApplication()).getmHomeViewModel();
		mHomeViewModel.registerViewModelChangeObserver(this);
		return mLayout;
	}

	@Override
	public void onDestroy() {
		if (null != mRetrieveTask
				&& mRetrieveTask.getStatus() == GenericTask.Status.RUNNING) {
			mRetrieveTask.cancel(true);
		}
		super.onDestroy();
	}

	@Override
	public void onViewModelChange(BaseViewModel viewModel,
			String changedPropertyName, Object... params) {
		Log.i(TAG, TAG + "-->currentTab is " + mHomeViewModel.getCurrentTab());
		if (HomeViewModel.CURRENTTAB_PROPERTY_NAME.equals(changedPropertyName)) {
			if (isLogined) {
				if (!isLoaded
						&& mHomeViewModel.getCurrentTab().equals(
								ActivityFragmentTargets.TAB_FRIENDS)) {
					doRetrieve();
				} else if (isLoaded
						&& mHomeViewModel.getCurrentTab().equals(
								ActivityFragmentTargets.TAB_FRIENDS)) {
					draw();
				}
			} else {
				processUnLogin();
			}
		}
	}

	private void doRetrieve() {
		if (null != mRetrieveTask
				&& GenericTask.Status.RUNNING == mRetrieveTask.getStatus()) {
			return;
		}
		mRetrieveTask = new RetrieveFriendListTask();
		mRetrieveTask.setListener(mRetrieveTaskListener);
		mRetrieveTask.execute(friendsUrl);
	}

	private void initArgs() {
		friendListView = (MyListView) mLayout.findViewById(R.id.my_list);
		myAdapter = new FriendsListAdapter(mInflater);
		friendListView.setAdapter(myAdapter);
	}

	private void initEvents() {
		friendListView.setonRefreshListener(new MyListView.OnRefreshListener() {

			@Override
			public void onRefresh() {
				doRetrieve();
			}
		});
		friendListView
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1,
							int position, long id) {
						positionSelected = position;
						actionMode = getSherlockActivity().startActionMode(
								new AnActionModeOfEpicProportions());
					}
				});
	}

	private void draw() {
		myAdapter.refresh(friendList);
		getSherlockActivity().setTitle(R.string.menu_friends);
	}

	private void goTop() {
		friendListView.setSelection(1);
	}

	private User getContextItemUser(int position) {
		if (position >= 1 && position <= myAdapter.getCount()) {
			return (User) myAdapter.getItem(position - 1);
		}
		return null;
	}

	private void processUnLogin() {
		Toast.makeText(getSherlockActivity(), R.string.login_indicate,
				Toast.LENGTH_SHORT).show();
	}

	private void processResult(TaskResult result) {
		if (TaskResult.Failed == result) {
			Toast.makeText(getSherlockActivity(), errorCause,
					Toast.LENGTH_SHORT).show();
			return;
		} else if (TaskResult.NO_DATA == result) {
			Toast.makeText(getSherlockActivity(), R.string.friends_no_data,
					Toast.LENGTH_SHORT).show();
			return;
		}
		isLoaded = true;
		draw();
		goTop();
	}

	private void delFriends(User user) {
		String id = user.getId();
		String url = "http://bbs.seu.edu.cn/api/friends/delete.json?id=" + id+"&token="+MyApplication.getInstance().getToken();
		mDelFriendsTask = new DeleteFriendsTask();
		mDelFriendsTask.setListener(mDelTaskListener);
		mDelFriendsTask.execute(url);
	}

	private void processDelResult(TaskResult result) {
		if (TaskResult.IO_ERROR == result || TaskResult.Failed == result) {
			Toast.makeText(getSherlockActivity(), R.string.del_friends_failed,
					Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(getSherlockActivity(), R.string.del_friends_success,
					Toast.LENGTH_SHORT).show();
		}
	}

	private class DeleteFriendsTask extends GenericTask {
		private boolean result;

		@Override
		protected TaskResult _doInBackground(String... params) {
			result = BBSOperator.getInstance().getBoolean(params[0]);
			if (!result) {
				return TaskResult.Failed;
			}
			return TaskResult.OK;
		}

	}

	private class RetrieveFriendListTask extends GenericTask {

		@Override
		protected TaskResult _doInBackground(String... params) {
			try {
				friendList = BBSOperator.getInstance().getFriends(params[0]);
			} catch (HttpException e) {
				e.printStackTrace();
				errorCause = e.getMessage();
				return TaskResult.Failed;
			}
			if (null == friendList || 0 == friendList.size()) {
				return TaskResult.NO_DATA;
			}

			return TaskResult.OK;
		}
	}

	private final class AnActionModeOfEpicProportions implements
			ActionMode.Callback {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {

			menu.add(Menu.NONE, MENU_VIEWPROFILE, Menu.NONE, R.string.friends_view_profile)
					.setIcon(R.drawable.ic_menu_profile_inverse)
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
			menu.add(Menu.NONE, MENU_MAIL, Menu.NONE, R.string.friends_mail)
					.setIcon(R.drawable.ic_compose_inverse)
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
			menu.add(Menu.NONE, MENU_DELETE, Menu.NONE, R.string.friends_delete)
					.setIcon(R.drawable.ic_menu_delete_inverse)
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			User user = getContextItemUser(positionSelected);
			Bundle bundle = new Bundle();
			switch (item.getItemId()) {
			case MENU_MAIL:

				bundle.putString("reciever", user.getId());
				mOnOpenActivityListener.onOpenActivityOrFragment(
						ActivityFragmentTargets.NEW_MAIL, bundle);
				break;
			case MENU_VIEWPROFILE:
				bundle.putString("userID", user.getId());
				mOnOpenActivityListener.onOpenActivityOrFragment(
						ActivityFragmentTargets.USER, bundle);
				break;
			case MENU_DELETE:
				delFriends(user);
				break;
			}
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			// TODO Auto-generated method stub

		}

	}

}