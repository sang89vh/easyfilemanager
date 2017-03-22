package com.mybox.filemanager.services.httpservice;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mybox.filemanager.R;
import com.mybox.filemanager.filesystem.BaseFile;
import com.mybox.filemanager.utils.GenericCopyUtil;
import com.mybox.filemanager.utils.PreferenceUtils;
import com.mybox.filemanager.utils.ServiceWatcherUtil;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class ZipUtils {
    String TAG ="ZipUtils";
    long totalBytes = 0l;
    String mZipPath;
    ArrayList<BaseFile> baseFiles;
    Context mContext;


    public ZipUtils(Context context, String path, JSONArray items) {
        mContext=context;
        baseFiles = new ArrayList<BaseFile>();

        for (int i = 0; i < items.length(); i++) {

            String object = null;
            try {
                object = getRealPath((String) items.getString(i));
            } catch (JSONException e) {
                Log.e(TAG,e.getMessage(),e);
            }

            BaseFile bf = new BaseFile(object);

            baseFiles.add(bf);
        }


        File zipFile = new File(path);
        mZipPath = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getString(PreferenceUtils.KEY_PATH_COMPRESS, path);
        if (!mZipPath.equals(path)) {
            mZipPath.concat(mZipPath.endsWith("/") ? (zipFile.getName()) : ("/" + zipFile.getName()));
        }

        if (!zipFile.exists()) {
            try {
                zipFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }


    }

    private long getTotalBytes(ArrayList<BaseFile> baseFiles) {
        long totalBytes = 0l;
        for (BaseFile f1 : baseFiles) {
            if (f1.isDirectory()) {
                totalBytes += f1.folderSize();
            } else {
                totalBytes += f1.length();
            }
        }
        return totalBytes;
    }


    ZipOutputStream zos;



    public ArrayList<File> toFileArray(ArrayList<BaseFile> a) {
        ArrayList<File> b = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            b.add(new File(a.get(i).getPath()));
        }
        return b;
    }



    public void execute() {

        OutputStream out;
        File zipDirectory = new File(mZipPath);

        try {
            out = FileUtil.getOutputStream(zipDirectory, mContext, totalBytes);
            zos = new ZipOutputStream(new BufferedOutputStream(out));

            ArrayList<File> fa = toFileArray(baseFiles);
            for (File file : fa) {
                compressFile(file, "");
            }
        } catch (Exception e) {
        } finally {

            try {
                zos.flush();
                zos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void compressFile(File file, String path) throws IOException, NullPointerException {

        if (!file.isDirectory()) {


            byte[] buf = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
            int len;
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
            zos.putNextEntry(new ZipEntry(path + "/" + file.getName()));
            while ((len = in.read(buf)) > 0) {

                zos.write(buf, 0, len);
                ServiceWatcherUtil.POSITION += len;
            }
            in.close();
            return;
        }
        if (file.list() == null) {
            return;
        }
        for (File currentFile : file.listFiles()) {

            compressFile(currentFile, path + File.separator + file.getName());

        }
    }

    public  String getRealPath(String path){
        String realPath = path.replaceAll(
                "/" + mContext.getResources().getString(R.string.storage)+"01"
                        + "|" + "/" + mContext.getResources().getString(R.string.storage)+"02"
                        + "|" + "/" + mContext.getResources().getString(R.string.images)
                        + "|" + "/" + mContext.getResources().getString(R.string.videos)
                        + "|" + "/" + mContext.getResources().getString(R.string.audio)
                        + "|" + "/" + mContext.getResources().getString(R.string.documents)
                , "");
        if(path.contains(mContext.getResources().getString(R.string.storage)+"01")){
            realPath = "/storage/emulated/0"+realPath;
        }else if(path.contains(mContext.getResources().getString(R.string.storage)+"02")){
            realPath = "/storage/emulated/legacy"+realPath;
        }

        return realPath;
    }


}
