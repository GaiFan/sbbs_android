package com.gfan.sbbs.ui.main;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.AdapterView;
import android.widget.Toast;

import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.gfan.sbbs.bean.Attachment;
import com.gfan.sbbs.file.utils.FileUtils;
import com.gfan.sbbs.othercomponent.SBBSConstants;
import com.gfan.sbbs.ui.Abstract.BaseActivity;
import com.gfan.sbbs.ui.Adapter.AttachmentAdapter;
import com.gfan.sbbs.utils.StringUtils;
import com.gfan.sbbs.utils.images.ImageUtils;

public class FileUploadActivity extends BaseActivity implements OnItemClickListener {
	
	private GridView gridView;
	private AttachmentAdapter attAdapter;
	private File mImageFile;
	private Uri mImageUri;
	
	private static final int MENU_ADD_PHOTO = Menu.FIRST;
	private static final int MENU_ADD_PIC = Menu.FIRST + 1;
	
	private static final int REQUEST_IMAGE_CAPTURE = 0;
	private static final int REQUEST_PHOTO_LIBRARY = 1;
	
	private static final String TAG = FileUploadActivity.class.getName();

	private List<Attachment> fileList;
	private int toBeDelAttPos;
	@Override
	protected void processUnLogin() {
		return;
	}

	@Override
	protected void setup() {
		return;
	}
	
	@Override
	protected void _onCreate(Bundle savedInstanceState) {
		super._onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		ActionBarSherlock actionBar = ActionBarSherlock.wrap(this);
		actionBar
				.setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
		setContentView(R.layout.file_upload);
		fileList = new ArrayList<Attachment>();
		initLayout();
	}
	private void initLayout(){
		gridView = (GridView) findViewById(R.id.image_table_view);
		attAdapter = new AttachmentAdapter(this);
		gridView.setAdapter(attAdapter);
		gridView.setOnItemClickListener(this);
		if(!FileUtils.getInstance().isEmpty()){
			fileList = FileUtils.getInstance().toArrayList();
			refresh(fileList);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_ADD_PHOTO, Menu.NONE, "add photo")
				.setIcon(R.drawable.toolbar_camera)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(Menu.NONE, MENU_ADD_PIC, Menu.NONE, "add pic")
				.setIcon(R.drawable.toolbar_media)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:{
			finish();
			Log.i(TAG, "FileUploadActivity finished,start WritePost");
			break;
		}

		case MENU_ADD_PHOTO:
			openImageCaptureMenu();
			break;
		case MENU_ADD_PIC:
			openPhotoLibraryMenu();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	protected void openImageCaptureMenu() {
		try {
			
			String filename = getPhotoFileName(new Date());
			mImageFile = new File(StringUtils.getBasePath(), filename);
			mImageUri = Uri.fromFile(mImageFile);
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
			startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
	}

	protected void openPhotoLibraryMenu() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		startActivityForResult(intent, REQUEST_PHOTO_LIBRARY);
	}
	
	private String getPhotoFileName(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddKms",Locale.CHINESE);
		return dateFormat.format(date) + ".jpg";
	}

	/**
	 * compress images before upload
	 * @param url
	 */
	
	private String compressImages(String url){
		if(url.startsWith("file")){
			url = url.substring(7);
		}
		return ImageUtils.compressImages(url, SBBSConstants.IMAGE_QUALITY, this);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		String photoDir = "";
		if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
//			Bitmap bitmap = null;
//			if(null != data.getData()){
//				mImageUri = data.getData();
//				photoDir = mImageUri.getPath();
//			}else{
//				bitmap = (Bitmap) data.getExtras().get("data");
//				ByteArrayOutputStream baos = new ByteArrayOutputStream();
//				try {
//					File file = new File(StringUtils.getCacheFilePath(),getPhotoFileName(new Date()));
//					BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
//					bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//					bos.write(baos.toByteArray(), 0, 1024);
//					bos.flush();
//					bos.close();
//					photoDir = file.getPath();
//				} catch (FileNotFoundException e) {
//					e.printStackTrace();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//				if(null!= bitmap && !bitmap.isRecycled()){
//					bitmap.recycle();
//					bitmap = null;
//				}
//			}
//			Log.i(TAG, "photo path is "+photoDir);
			Intent intent = new Intent(this, WritePost.class);
			addToFileList(intent, mImageUri);

		} else if (requestCode == REQUEST_PHOTO_LIBRARY
				&& resultCode == RESULT_OK) {
			if(null != data.getData()){
				mImageUri = data.getData();
			}else{
				Log.e(TAG, "data.getData==null");
			}
			Intent intent = new Intent(this, WritePost.class);
			addToFileList(intent, mImageUri);
		}
	}
	
	private void addToFileList(Intent intent, Uri uri) {
		Log.i(TAG, "scheme is "+mImageUri.getScheme());
		Attachment att = new Attachment();
		if (uri.getScheme().equals("content")) {// stored on the sdcard
			att.setUrl(getRealPathFromURI(uri));
		} else {
			att.setUrl("file://"+uri.getPath());
			att.setFileName(mImageFile.getName());
		}
		/**
		 * parse file like this
		 * "/mnt/sdcard/Universal Image Loader @#&=+-_.,!()~'%20.png"
		 */

		/**TODO
		 * check if the photo need to compress
		 */
		String tempUrl = compressImages(att.getUrl());
		att.setUrl(tempUrl);
		FileUtils.getInstance().addToAttUrl(att.getUrl());
		fileList.add(att);
		refresh(fileList);
	}
	
	private String getRealPathFromURI(Uri contentUri) {
		String[] proj = { MediaColumns.DATA };
		@SuppressWarnings("deprecation")
		Cursor cursor = managedQuery(contentUri, proj, null, null, null);
		int column_index = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
		cursor.moveToFirst();
		String path = "file://"+cursor.getString(column_index);
		Log.i(TAG, "path is " + path);
		
		return path;
	}

	private void refresh(List<Attachment> fileList){
		attAdapter.refresh(fileList);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private void removeAttachment(){
		Attachment toBeRemovedAtt = (Attachment) attAdapter.getItem(toBeDelAttPos);
		fileList.remove(toBeRemovedAtt);
		refresh(fileList);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
		toBeDelAttPos = position;
		startActionMode(new AnActionModeOfEpicProportions());
	}
	
	private final class AnActionModeOfEpicProportions implements ActionMode.Callback {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			menu.add("remove").setIcon(R.drawable.ic_menu_delete_inverse).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			removeAttachment();
			FileUtils.getInstance().removeFromUrl(attAdapter.getAttachmentUrl(toBeDelAttPos));
			mode.finish();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			// TODO Auto-generated method stub
			
		}
		
	}

}
