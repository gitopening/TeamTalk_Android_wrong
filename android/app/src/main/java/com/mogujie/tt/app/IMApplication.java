package com.mogujie.tt.app;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import com.mogujie.tt.imservice.service.IMService;
import com.mogujie.tt.utils.ImageLoaderUtil;
import com.mogujie.tt.utils.Logger;
import com.mogujie.tt.utils.MLoggerFile;


public class IMApplication extends Application {

	private Logger logger = Logger.getLogger(IMApplication.class);
	private Context mContext;


	/**
	 * @param args
	 */
	public static void main(String[] args) {
	}

	@Override
	public void onCreate() {
		super.onCreate();
		logger.i("Application starts");
		mContext = this;
		MLoggerFile.Init(mContext,true);

		startIMService();
		ImageLoaderUtil.initImageLoaderConfig(getApplicationContext());
	}

	private void startIMService() {
		logger.i("start IMService");
		Intent intent = new Intent();
		intent.setClass(this, IMService.class);
		startService(intent);
	}

    public static boolean gifRunning = true;//gif是否运行
}
