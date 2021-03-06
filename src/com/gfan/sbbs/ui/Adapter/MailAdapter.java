package com.gfan.sbbs.ui.Adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gfan.sbbs.bean.Mail;
import com.gfan.sbbs.othercomponent.MyApplication;
import com.gfan.sbbs.othercomponent.Preferences;
import com.gfan.sbbs.ui.main.R;

public class MailAdapter extends BaseAdapter {

	private Context context;
	private List<Mail> mailList;
	private LayoutInflater mInflater;
	
	public MailAdapter(Context context){
		this.context = context;
		this.mailList = new ArrayList<Mail>();
	}
	
	public MailAdapter(LayoutInflater mInflater){
		this.mailList = new ArrayList<Mail>();
		this.mInflater = mInflater;
		this.context = MyApplication.getInstance().getActivity();
	}
	public void setList(List<Mail>list){
		this.mailList = list;
	}
	
	@Override
	public int getCount() {
		return mailList.size();
	}

	@Override
	public Object getItem(int position) {
		return mailList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}
	public void refresh(List<Mail> list){
		this.mailList = list;
		notifyDataSetChanged();
	}
	public void refresh(){
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		Mail mail;
		if(convertView == null){
			if(null == mInflater){
			mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);}
			convertView =  mInflater.inflate(R.layout.mail_item, null);
			holder = new ViewHolder();
			holder.authorInfo = (TextView) convertView.findViewById(R.id.mail_authorinfo);
			holder.titleInfo = (TextView) convertView.findViewById(R.id.mail_titleinfo);
			convertView.setTag(holder);
		}else{
			holder = (ViewHolder) convertView.getTag();
		}
		mail = mailList.get(position);
		holder.authorInfo.setText(MyApplication.getInstance().getActivity().getResources().getString(R.string.mail_from)+ mail.getFrom());
		holder.titleInfo.setText(mail.getTitle());
		if(mail.isUnRead()){
			convertView.setBackgroundColor(0xffFFFFFF);
//			holder.titleInfo.getPaint().setFakeBoldText(true);
		}else{
			convertView.setBackgroundColor(0xffEEEEEE);
//			holder.titleInfo.getPaint().setFakeBoldText(false);
		}
		String fontSize = MyApplication.getInstance().getmPreference().getString(Preferences.FONT_SIZE_ADJUST, "Normal");
		if(fontSize.equals(Preferences.FONT_SIZE_NORMAL)){
			holder.authorInfo.setTextAppearance(MyApplication.getInstance().getActivity(), R.style.AuthorText_Normal);
			holder.titleInfo.setTextAppearance(MyApplication.getInstance().getActivity(), R.style.TitleText_Normal);
		}else if(fontSize.equals(Preferences.FONT_SIZE_LARGE)){
			holder.authorInfo.setTextAppearance(MyApplication.getInstance().getActivity(),R.style.AuthorText_Large);
			holder.titleInfo.setTextAppearance(MyApplication.getInstance().getActivity(), R.style.TitleText_Large);
		}else{
			holder.authorInfo.setTextAppearance(MyApplication.getInstance().getActivity(), R.style.AuthorText_Small);
			holder.titleInfo.setTextAppearance(MyApplication.getInstance().getActivity(), R.style.TitleText_Small);
		}
		
		return convertView;
	}

	private static class ViewHolder{
		TextView authorInfo;
		TextView titleInfo;
	}
}
