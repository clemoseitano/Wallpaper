package app.freetime.wallpaper;

import android.app.Service;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import app.freetime.fileexplorer.FileAdapter;

public class WallpaperService extends Service {
    public static final String LOCK_NAME_STATIC = "app.freetime.wallpaper.wakelock.Static";
    private static WallpaperService js = new WallpaperService();
    private static PowerManager.WakeLock lockStatic = null;
    private SharedPreferences prefs;
    private BroadcastReceiver wallpaperChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            runWallpaper();
        }
    };

    public static WallpaperService getInstance() {
        return js;
    }

    public static void acquireStaticLock(Context context) {
        getLock(context).acquire();
    }

    synchronized private static PowerManager.WakeLock getLock(Context context) {
        if (lockStatic == null) {
            PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    LOCK_NAME_STATIC);
            lockStatic.setReferenceCounted(true);
        }
        return (lockStatic);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        js = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        getLock(this).acquire();

        registerReceiver(wallpaperChangeReceiver, new IntentFilter(Constants.UPDATE_MESSAGE));

        if (prefs.getString(Constants.PREF_WALLPAPER_DIRS, null) != null)
            runWallpaper();
        PhoneUnlockedReceiver receiver = new PhoneUnlockedReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(wallpaperChangeReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        getLock(this).release();
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void runWallpaper() {
        int position = prefs.getInt(Constants.PREF_IMAGE_NUMBER, 0);
        File file = new File(prefs.getString(Constants.PREF_WALLPAPER_DIRS, Constants.DEFAULT_DIR));
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] fileList = file.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File f, String s) {
                        return !f.isDirectory() && Constants.FILE_TYPES.contains(FileAdapter.getFileExtension(f.getName()).toLowerCase());
                    }
                });
                if (fileList != null) {
                    if (fileList.length >= 1) {
                        if (!fileList[position % fileList.length].isDirectory()) {
                            String f = fileList[position % fileList.length].getAbsolutePath();
                            Bitmap b = BitmapFactory.decodeFile(f);
                            WallpaperManager myWallpaperManager = WallpaperManager
                                    .getInstance(getApplicationContext());
                            try {
                                myWallpaperManager.setBitmap(b);
                                b.recycle();
                            } catch (IOException e) {
                                Toast.makeText(WallpaperService.this, "Failed to set wallpaper", Toast.LENGTH_SHORT).show();
                            }
                        }

                    }
                }
            } else if (!file.isDirectory()) {
                return;
            }
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(Constants.LAST_FILE, file.getParent());
            ed.putInt(Constants.PREF_IMAGE_NUMBER, ++position);
            ed.apply();
        }
        Long nextEvent = prefs.getLong(Constants.NEXT_EVENT, 5000);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runWallpaper();
            }
        }, (nextEvent));


    }

    class PhoneUnlockedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                Log.d("TAG", "Phone unlocked");
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Log.d("TAG", "Phone locked");
            }
        }
    }
}
