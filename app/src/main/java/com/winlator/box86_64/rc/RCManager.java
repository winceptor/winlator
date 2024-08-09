package com.winlator.box86_64.rc;

import android.content.Context;
import android.content.res.AssetManager;
import android.media.MediaScannerConnection;
import android.os.Environment;

import com.winlator.core.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class RCManager {
    private LinkedList<RCFile> rcfiles;
    private final Context context;
    private int maxRCFileId;
    private boolean rcfilesLoaded = false;

    public RCManager(Context context) {
        this.context = context;
    }

    public RCFile duplicateRCFile(RCFile source) {
        String newName;
        for (int i = 1; ; i++) {
            newName = source.getName() + " (" + i + ")";
            boolean found = false;
            for (RCFile rcfile : rcfiles) {
                if (rcfile.getName().equals(newName)) {
                    found = true;
                    break;
                }
            }
            if (!found) break;
        }

        int newId = ++maxRCFileId;
        File newFile = RCFile.getRCFile(context, newId);

        try {
            JSONObject data = new JSONObject(FileUtils.readString(RCFile.getRCFile(context, source.id)));
            data.put("id", newId);
            data.put("name", newName);
            FileUtils.writeString(newFile, data.toString());
        } catch (JSONException e) {
        }

        RCFile rcfile = loadRCFile(context, newFile);
        rcfiles.add(rcfile);
        return rcfile;
    }

    public static File getRCFilesDir(Context context) {
        File rcfilesDir = new File(context.getFilesDir(), "rcfiles");
        if (!rcfilesDir.isDirectory()) rcfilesDir.mkdir();
        return rcfilesDir;
    }

    public List<RCFile> getRCFiles() {
        if (!rcfilesLoaded) loadRCFiles();
        return rcfiles;
    }

    private void copyAssetRCFilesIfNeeded() {
        File rcfilesDir = RCManager.getRCFilesDir(context);
        if (FileUtils.isEmpty(rcfilesDir)) {
            FileUtils.copy(context, "box86_64/rcfiles", rcfilesDir);
            return;
        }

        File[] files = rcfilesDir.listFiles();
        if (files == null) return;

        try {
            AssetManager assetManager = context.getAssets();
            String[] assetFiles = assetManager.list("box86_64/rcfiles");
            for (String assetFile : assetFiles) {
                String assetPath = "box86_64/rcfiles/" + assetFile;
                RCFile originRCFile = loadRCFile(context, FileUtils.readAssetsFile(context, assetPath));

                File targetFile = null;
                for (File file : files) {
                    RCFile targetRCFile = loadRCFile(context, file);
                    if (originRCFile.id == targetRCFile.id && originRCFile.getName().equals(targetRCFile.getName())) {
                        targetFile = file;
                        break;
                    }
                }

                if (targetFile != null) {
                    FileUtils.copy(context, assetPath, targetFile);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String loadJSONFromAsset(Context context, String fileName) throws IOException {
        String l;
        AssetManager assetManager = context.getAssets();
        InputStream is = assetManager.open(fileName);

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        while ((l = reader.readLine()) != null) {
            sb.append(l);
        }

        reader.close();
        return sb.toString();
    }

    public RCFile createRCFile(String name) {
        RCFile rcfile = new RCFile(context, ++maxRCFileId);
        rcfile.setName(name);
        rcfile.save();
        rcfiles.add(rcfile);
        return rcfile;
    }

    public void loadRCFiles() {
        File rcfilesDir = RCManager.getRCFilesDir(context);
        copyAssetRCFilesIfNeeded();

        LinkedList<RCFile> rcfiles = new LinkedList<>();
        File[] files = rcfilesDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getPath().endsWith(".rcp")) {
                    RCFile rcfile = loadRCFile(context, file);
                    rcfiles.add(rcfile);
                    maxRCFileId = Math.max(maxRCFileId, rcfile.id);
                }
            }
        }

        Collections.sort(rcfiles);
        this.rcfiles = rcfiles;
        rcfilesLoaded = true;
    }

    public static RCFile loadRCFile(Context context, File file) {
        if (file.exists() && file.isFile()) return loadRCFile(context, FileUtils.readString(file));
        return null;
    }

    public static RCFile loadRCFile(Context context, String json) {
        try {
            return loadRCFile(context, new JSONObject(json));
        } catch (JSONException e) {
            return null;
        }
    }

    public static RCFile loadRCFile(Context context, JSONObject obj) {
        try {
            JSONObject rcfileJSONObject = obj;
            int rcfileId = rcfileJSONObject.getInt("id");
            String rcfileName = rcfileJSONObject.getString("name");
            LinkedList<RCGroup> groups = new LinkedList<>();
            JSONArray groupsJSONArray = rcfileJSONObject.getJSONArray("groups");

            for (int i = 0; i < groupsJSONArray.length(); i++) {
                JSONObject groupJSONObject = groupsJSONArray.getJSONObject(i);
                String groupName = groupJSONObject.getString("name");
                String groupDesc = groupJSONObject.getString("desc");
                boolean groupEnabled = groupJSONObject.getBoolean("enabled");
                LinkedList<RCItem> items = new LinkedList<>();
                JSONArray itemsJSONArray = groupJSONObject.getJSONArray("items");

                for (int j = 0; j < itemsJSONArray.length(); j++) {
                    JSONObject itemJSONObject = itemsJSONArray.getJSONObject(j);
                    String processName = itemJSONObject.getString("processName");
                    String itemDesc = itemJSONObject.getString("desc");
                    TreeMap<String, String> map = new TreeMap<>();

                    JSONObject varsJSONObject = itemJSONObject.getJSONObject("vars");
                    Iterator<String> keys = varsJSONObject.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        String value = varsJSONObject.getString(key);
                        map.put(key, value);
                    }

                    RCItem item = new RCItem(processName, itemDesc, map);
                    items.add(item);
                }
                RCGroup group = new RCGroup(groupName, groupDesc, groupEnabled, items);
                groups.add(group);
            }

            RCFile rcfile = new RCFile(context, rcfileId);
            rcfile.setName(rcfileName);
            for (RCGroup group : groups)
                rcfile.getGroups().add(group);

            return rcfile;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public RCFile getRcfile(int id) {
        for (RCFile rcfile : getRCFiles()) if (rcfile.id == id) return rcfile;
        return null;
    }

    public File exportRCFile(RCFile rcfile) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File destination = new File(downloadsDir, "Winlator/rcfiles/" + rcfile.getName() + ".rcp");
        FileUtils.copy(RCFile.getRCFile(context, rcfile.id), destination);
        MediaScannerConnection.scanFile(context, new String[]{destination.getAbsolutePath()}, null, null);
        return destination.isFile() ? destination : null;
    }

    public void saveAllRCFiles() {
        if (rcfiles != null) {
            for (RCFile rcfile : rcfiles)
                rcfile.save();
        }
    }

    public void removeRCFile(RCFile rcfile) {
        File file = RCFile.getRCFile(context, rcfile.id);
        if (file.isFile() && file.delete()) rcfiles.remove(rcfile);
    }
}
