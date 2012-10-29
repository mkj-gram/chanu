/**
 * 
 */
package com.chanapps.four.data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import android.content.Intent;
import android.database.DatabaseUtils.InsertHelper;
import android.util.Log;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class ChanPostService extends ChanThreadService {
	private static final String TAG = ChanPostService.class.getName();
	
	public ChanPostService() {
		super("Post");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		String boardName = intent.getStringExtra("board");
		int threadNumber = intent.getIntExtra("thread", 0);

		BufferedReader in = null;
		ChanDatabaseHelper h = new ChanDatabaseHelper(getBaseContext());
		InsertHelper ih = new InsertHelper(h.getWritableDatabase(), ChanDatabaseHelper.POST_TABLE);
		
		try {
			Set<Integer> ids = getListOfIds(h, boardName);
			prepareColumnIndexes(ih);
			
			URL chanApi = new URL("http://api.4chan.org/" + boardName + "/res/" + threadNumber + ".json");
            Log.i(TAG, "Calling API " + chanApi + " ...");
            URLConnection tc = chanApi.openConnection();
            Log.i(TAG, "Opened API " + chanApi + " response length=" + tc.getContentLength());
	        in = new BufferedReader(new InputStreamReader(tc.getInputStream()));
	        Gson gson = new GsonBuilder().create();
	        
			JsonReader reader = new JsonReader(in);
			reader.setLenient(true);
			reader.beginObject();
			reader.nextName(); // "posts"
			reader.beginArray();
			while (reader.hasNext()) {
				ChanThread thread = gson.fromJson(reader, ChanThread.class);
				thread.board = boardName;
				boolean postExists = !ids.contains(thread.no);
            	Log.i(TAG, thread.toString() + ", existed = " + postExists);
            	addPost(ih, thread, postExists);
			}
		} catch (Exception e) {
			Log.e(TAG, "Error parsing Chan post json. " + e.getMessage(), e);
		} finally {
			if (ih != null) {
				ih.close();
			}
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception e) {
				Log.e(TAG, "Error closing reader", e);
			}
		}
	}
}
