package com.winlator.xenvironment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.winlator.core.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class ImageFs {
    public static final String USER = "xuser";
    public static final String HOME_PATH = "/home/"+USER;
    public static final String CACHE_PATH = HOME_PATH+"/.cache";
    public static final String CONFIG_PATH = HOME_PATH+"/.config";
    public static final String WINEPREFIX = HOME_PATH+"/.wine";
    private final File rootDir;
    public String winePath;
    public String home_path;
    public String cache_path;
    public String config_path;
    public String wineprefix;

    private ImageFs(File rootDir) {
        this.rootDir = rootDir;
        winePath = rootDir + "/opt/wine";
        home_path = rootDir + HOME_PATH;
        cache_path = rootDir + CACHE_PATH;
        config_path = rootDir + CONFIG_PATH;
        wineprefix = rootDir + WINEPREFIX;
    }

    public static ImageFs find(Context context) {
        return new ImageFs(new File(context.getFilesDir(), "imagefs"));
    }

    public File getRootDir() {
        return rootDir;
    }

    public boolean isValid() {
        return rootDir.isDirectory() && getImgVersionFile().exists();
    }

    public int getVersion() {
        File imgVersionFile = getImgVersionFile();
        return imgVersionFile.exists() ? Integer.parseInt(FileUtils.readLines(imgVersionFile).get(0)) : 0;
    }

    public String getFormattedVersion() {
        return String.format(Locale.ENGLISH, "%.1f", (float)getVersion());
    }

    public void createImgVersionFile(int version) {
        getConfigDir().mkdirs();
        File file = getImgVersionFile();
        try {
            file.createNewFile();
            FileUtils.writeString(file, String.valueOf(version));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getWinePath() {
        return winePath;
    }

    public void setWinePath(String winePath) {
        this.winePath = FileUtils.toRelativePath(rootDir.getPath(), winePath);
    }

    public File getConfigDir() {
        return new File(rootDir, ".winlator");
    }

    public File getImgVersionFile() {
        return new File(getConfigDir(), ".img_version");
    }

    public File getInstalledWineDir() {
        return new File(rootDir, "/opt/installed-wine");
    }

    public File getTmpDir() {
        return new File(rootDir, "/tmp");
    }

    public File getGlibc32Dir() {
        return new File(rootDir, "/usr/lib/arm-linux-gnueabihf");
    }

    public File getGlibc64Dir() {
        return new File(rootDir, "/usr/lib");
    }

    public File getLib32Dir() {
        return new File(rootDir, "/usr/lib/arm-linux-gnueabihf");
    }

    public File getLib64Dir() {
        return new File(rootDir, "/usr/lib");
    }

    @NonNull
    @Override
    public String toString() {
        return rootDir.getPath();
    }
}
