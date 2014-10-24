package com.gfan.sbbs.file.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.gfan.sbbs.bean.Attachment;
import com.gfan.sbbs.utils.StringUtils;

public class FileUtils {

	private BlockingQueue<String> attUrlList;
	private static FileUtils fileUtils;
	private static final int TIMEOUT = 5;

	private FileUtils() {
		attUrlList = new ArrayBlockingQueue<String>(20);
	}

	public static FileUtils getInstance() {
		if (null == fileUtils) {
			fileUtils = new FileUtils();
		}
		return fileUtils;
	}

	public boolean  addToAttUrl(String url) {
		if (!attUrlList.contains(url)) {
			return attUrlList.add(url);
		}
		return false;
	}

	public String takeFromAttUrl() throws InterruptedException {
		return attUrlList.poll(TIMEOUT, TimeUnit.SECONDS);
	}

	public void removeFromUrl(String url) {
		if (attUrlList.contains(url)) {
			attUrlList.remove(url);
		}
	}

	public void cleanQueues() {
		attUrlList.clear();
	}

	public boolean isEmpty() {
		Object obj = attUrlList.peek();
		if (null == obj) {
			return true;
		} else {
			return false;
		}
	}
	
	public List<Attachment> toArrayList(){
		List<Attachment> attachmentList = new ArrayList<Attachment>();
		Iterator<String> iterator = attUrlList.iterator();
		String url;
		while(iterator.hasNext()){
			url = iterator.next();
			attachmentList.add(new Attachment().setUrl(url));
		}
		return attachmentList;
	}
	/**
	 * output debug info to sdcard
	 * @param string
	 * @throws IOException 
	 */
	public void writeToFile(String str) throws IOException{
		File file = new File(StringUtils.getBasePath()+"debug.txt");
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		long filelength = file.length();
		raf.seek(filelength);
		raf.writeBytes(str);
		raf.close();
	}
}
