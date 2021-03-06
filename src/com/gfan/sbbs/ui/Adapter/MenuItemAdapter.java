package com.gfan.sbbs.ui.Adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gfan.sbbs.menu.MenuItem;
import com.gfan.sbbs.ui.main.R;

public class MenuItemAdapter extends BaseAdapter {
	private List<MenuItem> mList;
	private LayoutInflater mInflater;

	public MenuItemAdapter(Context mComtext){
		mInflater = LayoutInflater.from(mComtext);
		mList = new ArrayList<MenuItem>();
	}
	@Override
	public int getCount() {
		return mList.size();
	}

	@Override
	public Object getItem(int position) {
		return mList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}
	
	public void draw(List<MenuItem> list){
		this.mList = list;
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if(null == convertView){
			convertView = mInflater.inflate(R.layout.menu_item, null);
			holder = new ViewHolder();
			holder.text = (TextView) convertView.findViewById(R.id.rbm_item_text);
			holder.icon = (ImageView) convertView.findViewById(R.id.rbm_item_icon);
			convertView.setTag(holder);
		}else{
			holder = (ViewHolder) convertView.getTag();
		}
		holder.icon.setImageResource(mList.get(position).getIcon());
		holder.text.setText(mList.get(position).getTitle());
		return convertView;
	}

	class ViewHolder {
		TextView text;
		ImageView icon;
	}
}
