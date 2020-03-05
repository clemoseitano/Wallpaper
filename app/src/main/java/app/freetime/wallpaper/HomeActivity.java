package app.freetime.wallpaper;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import app.freetime.fileexplorer.FileAdapter;
import app.freetime.fileexplorer.FileDialog;

public class HomeActivity extends AppCompatActivity implements FileDialog.FileListener {

    private static final int PERMISSION = 2344;

    private void updateStatus() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            Window window = getWindow();
            if (Build.VERSION.SDK_INT >= 21) {
                actionBar.setElevation(0);
            }

            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
            }
            actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorPrimary)));
            setTitle(getResources().getString(R.string.app_name));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        updateStatus();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickWallpapers();
            }
        });

        ViewPager viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int hasStoragePermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            List<String> permissions = new ArrayList<>();
            if (hasStoragePermission != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (!permissions.isEmpty())
                requestPermissions(permissions.toArray(new String[permissions.size()]), PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION:
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                        Toast.makeText(this, "Permission denied: " + permissions[i] + ". This may cause " +
                                "the app to behave abnormally", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, Settings.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFileReturned(int requestcode, File file) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor ed = prefs.edit();
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File f, String s) {
                        return !f.isDirectory() && Constants.FILE_TYPES.contains(FileAdapter.getFileExtension(f.getName()).toLowerCase());
                    }
                });
                if (files != null) {
                    if (files.length >= 1) {
                        ed.putString(Constants.PREF_WALLPAPER_DIRS, file.getAbsolutePath());
                        ed.apply();
                    }
                }
            } else if (!file.isDirectory()) {
                Bitmap b = BitmapFactory.decodeFile(file.getAbsolutePath());
                WallpaperManager myWallpaperManager = WallpaperManager
                        .getInstance(getApplicationContext());
                try {
                    myWallpaperManager.setBitmap(b);
                    b.recycle();
                } catch (IOException e) {
                    Toast.makeText(this, "Failed to set wallpaper", Toast.LENGTH_SHORT).show();
                }
                ed.putString(Constants.PREF_WALLPAPER_DIRS, file.getAbsolutePath());
                ed.apply();
            }
            Toast.makeText(HomeActivity.this, "Wallpaper is being updated",
                    Toast.LENGTH_LONG).show();

            ed.putString(Constants.LAST_FILE, file.getParent());
            ed.putInt(Constants.PREF_IMAGE_NUMBER, 1);
            ed.apply();
            startService(new Intent(this, WallpaperService.class));
            Intent i = new Intent(Constants.UPDATE_MESSAGE);
            sendBroadcast(i);
        }

    }

    private void copyFile(String absolutePath, String defaultDir) {
        InputStream in;
        OutputStream out;
        try {
            File dir = new File(defaultDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            in = new FileInputStream(new File(absolutePath));
            out = new FileOutputStream(new File(defaultDir, "img_" + Long.toString(System.currentTimeMillis())));
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "File: " + absolutePath + " not found", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void pickWallpapers() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        FileDialog f = new FileDialog();
        Bundle arguments = new Bundle();
        arguments.putInt(FileDialog.ARG_REQUEST_CODE, 3456);
        f.setMode(FileDialog.Mode.PICK_FILE);
        f.setTitle("Pick File");
        f.setTempDirectory(prefs.getString(Constants.LAST_FILE,
                Environment.getExternalStorageDirectory().getAbsolutePath()));
        f.setArguments(arguments);
        f.show(getSupportFragmentManager(), "FileDialog");
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(new Fragment(), ("All").toUpperCase());
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFrag(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }


}
