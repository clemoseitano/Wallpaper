package app.freetime.wallpaper;

import android.os.Environment;

/**
 * Created by Realme on 19/03/2017.
 */
public class Constants {
    public static final String UPDATE_MESSAGE = "app.freetime.wallpaper.wallpaper_update";
    public static final String SD_CARD = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final String DEFAULT_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Wallpapers";
    public static final String LAST_FILE = "app.freetime_wallpaper_last_file";
    public static final String FILE_TYPES = "jpg jpeg png";
    public static final String PREF_WALLPAPER_DIRS = "app.freetime_wallpaper_dir";
    public static final String NEXT_EVENT = "app.freetime_wallpaper_next";
    public static final String PREF_IMAGE_NUMBER = "app.freetime_wallpaper_image";
}
