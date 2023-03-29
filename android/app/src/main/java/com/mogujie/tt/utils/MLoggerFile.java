//package com.hmi.beic62.pc.utils;
package com.mogujie.tt.utils;

import android.content.Context;
import android.os.Environment;

import com.hmi.beic62.pc.constants.ConstantsPreference;
import com.hmi.beic62.pc.constants.ConstantsThis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 往sd卡写入日志文件
 */

public final class MLoggerFile {
    private final static int LOG_INFO = 0;
    private final static int LOG_WARNING = 1;
    private final static int LOG_ERROR = 2;
    private static File mFile;
    private static FileOutputStream mOutputStream;
    private static Context mContext;

    /**
     * @param toFile 为true写入文件，为false写入控制台
     * @return
     */
    public static boolean Init(Context context, boolean toFile)
    {
        mContext=context;
        Date curDate = new Date(System.currentTimeMillis());
//        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestr = formatter.format(curDate);
//        String logFilename = String.format("/data/tombstones/HmiLogs/pcHmi%s.log", timestr);
//        String logFilePath = String.format("/data/tombstones/hmiLogs");
//        String logFilePath = String.format("/sdcard/mtklog/mobilelog/mobilesclog/hLogs");
        String logFilePath = String.format("/android/mteam/Logs");
//        String logFilename = String.format("/pcHmi%s.log", timestr);
        String logFilename = String.format("hmi%s.log", timestr);

//        File apkPathFile = new File(Environment.getExternalStorageDirectory(),logFilePath);
//        File apkPathFile = new File(logFilePath,logFilename);
        File apkPathFile = new File(logFilePath);
        if (!apkPathFile.exists()) {
            apkPathFile.mkdirs();
        }
        mFile = new File(apkPathFile, logFilename);

        MPreferenceUtils.setPrefString(mContext, ConstantsPreference.P_CURRENT_LOGFILENAME, logFilename);



//        mFile = new File(Environment.getExternalStorageDirectory(), logFilename);
//        mFile = new File(logFilename);
//        try {
//            mFile.createNewFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false;
//        }

        try {
            mOutputStream = new FileOutputStream(mFile);
//            mOutputStream = new FileOutputStream(apkPathFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static boolean Uninit()
    {
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            }catch (IOException e){
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    public static boolean LogInfo(String... str)
    {
        return Log(LOG_INFO, str);
    }

    public static boolean i(String... str)
    {
        return Log(LOG_INFO, str);
    }

    public static boolean LogWarning(String... str)
    {
        return Log(LOG_WARNING, str);
    }

    public static boolean LogError(String... str)
    {
        return Log(LOG_ERROR, str);
    }

    private static boolean Log(int level, String... str)
    {
        if (mOutputStream == null || str.length == 0)
            return false;

        String levelStr;
        if (level == LOG_INFO)
            levelStr = "[INFO]";
        else if (level == LOG_WARNING)
            levelStr = "[WARNING]";
        else if (level == LOG_ERROR)
            levelStr = "[ERROR]";
        else
            levelStr = "[ERROR]";

        //前面加上日期和级别信息
        Date curDate = new Date(System.currentTimeMillis());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String timeStr = formatter.format(curDate);
        String prefix = String.format("[%s]%s", timeStr, levelStr);

        StackTraceElement[] trace = new Throwable().getStackTrace();
        // 下标为0的元素是上一行语句的信息, 下标为1的才是调用printLine的地方的信息
        StackTraceElement tmp = trace[1];

        //String lineNoStr = String.format("[%s:%d]", tmp.getFileName(), tmp.getLineNumber());
        String threadIDStr = String.format("[ThreadID %d]", Thread.currentThread().getId());
        StringBuilder builder = new StringBuilder();
        //builder.append(lineNoStr);
//        builder.append(prefix);
//        builder.append(threadIDStr);
        builder.append(levelStr);
        for (int i = 0; i < str.length; ++i)
        {
            builder.append(str[i]);
        }
        builder.append("\n");
        String actualLogToWrite = builder.toString();

        byte[] bt = actualLogToWrite.getBytes();
        if (bt == null)
            return false;

        try {
            mOutputStream.write(bt, 0, bt.length);
            mOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }finally {
//            try {
//                if (mOutputStream != null) {
//                    mOutputStream.close();
//                }
//            } catch (Exception e2) {
//            }
        }

        return true;
    }

}
