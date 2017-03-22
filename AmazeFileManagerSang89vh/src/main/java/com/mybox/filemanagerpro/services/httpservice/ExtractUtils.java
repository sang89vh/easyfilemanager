package com.mybox.filemanagerpro.services.httpservice;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;
import com.mybox.filemanagerpro.R;
import com.mybox.filemanagerpro.utils.AppConfig;
import com.mybox.filemanagerpro.utils.GenericCopyUtil;
import com.mybox.filemanagerpro.utils.ServiceWatcherUtil;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
public class ExtractUtils {
                                                                        // to initiate chart in process viewer fragment
    private ArrayList<String> entries=new ArrayList<String>();           // names of entries to be extracted
    private String epath;
    private String file;
    private Context cd;
    long totalBytes = 0l;


    public  ExtractUtils(String file,String extractPath,Context cd) {

        if (extractPath != null) {
            // a custom dynamic path to extract files to
            epath = extractPath;
        } else {

            epath = PreferenceManager.getDefaultSharedPreferences(cd).getString("extractpath", file);
        }
        
        this.file = file;
        
        this.cd = cd;
    }

    /**
     * Method calculates zip file size to initiate progress
     * Supporting local file extraction progress for now
     * @param filePath
     * @return
     */
    private long getTotalSize(String filePath) {

        return new File(filePath).length();
    }

   



        private void createDir(File dir) {
            FileUtil.mkdir(dir, cd);
        }

        /**
         * Method extracts {@link ZipEntry} from {@link ZipFile}
         * @param zipFile zip file from which entries are to be extracted
         * @param entry zip entry that is to be extracted
         * @param outputDir output directory
         * @throws Exception
         */
        private void unzipEntry(ZipFile zipFile, ZipEntry entry, String outputDir)
                throws Exception {

            if (entry.isDirectory()) {
                // zip entry is a directory, return after creating new directory
                createDir(new File(outputDir, entry.getName()));
                return;
            }

            final File outputFile = new File(outputDir, entry.getName());

            if (!outputFile.getParentFile().exists()) {
                // creating directory if not already exists

                createDir(outputFile.getParentFile());
            }

            BufferedInputStream inputStream = new BufferedInputStream(
                    zipFile.getInputStream(entry));
            BufferedOutputStream outputStream = new BufferedOutputStream(
                    FileUtil.getOutputStream(outputFile, cd, 0));
            try {
                int len;
                byte buf[] = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
                while ((len = inputStream.read(buf)) > 0) {

                    outputStream.write(buf, 0, len);
                    ServiceWatcherUtil.POSITION+=len;
                }
            } finally {
                outputStream.close();
                inputStream.close();
            }
        }

        private void unzipRAREntry(Archive zipFile, FileHeader entry, String outputDir)
                throws Exception {
            String name = entry.getFileNameString();
            name=name.replaceAll("\\\\","/");
            if (entry.isDirectory()) {
                createDir(new File(outputDir, name));
                return;
            }
            File outputFile = new File(outputDir, name);
            if (!outputFile.getParentFile().exists()) {
                createDir(outputFile.getParentFile());
            }
            //	Log.i("Amaze", "Extracting: " + entry);
            BufferedInputStream inputStream = new BufferedInputStream(
                    zipFile.getInputStream(entry));
            BufferedOutputStream outputStream = new BufferedOutputStream(
                    FileUtil.getOutputStream(outputFile, cd, entry.getFullUnpackSize()));
            try {
                int len;
                byte buf[] = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
                while ((len = inputStream.read(buf)) > 0) {

                    outputStream.write(buf, 0, len);
                    ServiceWatcherUtil.POSITION+=len;
                }
            } catch (Exception e) {

                throw new Exception();
            } finally {
                outputStream.close();
                inputStream.close();
            }
        }

        private void unzipTAREntry(TarArchiveInputStream zipFileStream, TarArchiveEntry entry,
                                   String outputDir) throws Exception {
            String name=entry.getName();
            if (entry.isDirectory()) {
                createDir(new File(outputDir, name));
                return;
            }
            File outputFile = new File(outputDir, name);
            if (!outputFile.getParentFile().exists()) {
                createDir(outputFile.getParentFile());
            }

            BufferedOutputStream outputStream = new BufferedOutputStream(
                    FileUtil.getOutputStream(outputFile,cd,entry.getRealSize()));
            try {
                int len;
                byte buf[] = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
                while ((len = zipFileStream.read(buf)) > 0) {

                    outputStream.write(buf, 0, len);
                    ServiceWatcherUtil.POSITION += len;
                }
            } catch (Exception e) {

                throw new Exception();
            } finally {
                outputStream.close();
            }
        }

        /**
         * Helper method to initiate extraction of zip/jar files.
         * @param archive the file pointing to archive
         * @param destinationPath the where to extract
         * @param entryNamesList names of files to be extracted from the archive
         * @return
         */
        private boolean extract(File archive, String destinationPath,
                               ArrayList<String> entryNamesList) {

            ArrayList<ZipEntry> entry1=new ArrayList<ZipEntry>();
            try {
                ZipFile zipfile = new ZipFile(archive);

                // iterating archive elements to find file names that are to be extracted
                for (Enumeration e = zipfile.entries(); e.hasMoreElements(); ) {

                    ZipEntry zipEntry = (ZipEntry) e.nextElement();

                    for (String entry : entryNamesList) {

                        if (zipEntry.getName().contains(entry)) {
                            // header to be extracted is atleast the entry path (may be more, when it is a directory)
                            entry1.add(zipEntry);
                        }
                    }
                }

                // get the total size of elements to be extracted
                for (ZipEntry entry:entry1) {
                    totalBytes+=entry.getSize();
                }

                // setting total bytes calculated from zip entries


                int i = 0;
                for(ZipEntry entry:entry1) {


                        unzipEntry(zipfile, entry, destinationPath);

                }

                return true;
            } catch (Exception e) {
                Log.e("amaze", "Error while extracting file " + archive, e);
                AppConfig.toast(cd, cd.getString(R.string.error));
                return false;
            }

        }

        private boolean extract(File archive, String destinationPath) {

            try {
                ArrayList<ZipEntry> arrayList=new ArrayList<ZipEntry>();
                ZipFile zipfile = new ZipFile(archive);
                for (Enumeration e = zipfile.entries(); e.hasMoreElements();) {

                    // adding all the elements to be extracted to an array list
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    arrayList.add(entry);
                }

                for(ZipEntry entry:arrayList) {
                    // calculating size of compressed items
                    totalBytes += entry.getSize();
                }



                for (ZipEntry entry : arrayList) {

                        unzipEntry(zipfile, entry, destinationPath);

                }

                return true;
            } catch (Exception e) {
                Log.e("amaze", "Error while extracting file " + archive, e);
                AppConfig.toast(cd, cd.getString(R.string.error));
                return false;
            }

        }

        private boolean extractTar(File archive, String destinationPath) {

            try {

                ArrayList<TarArchiveEntry> archiveEntries=new ArrayList<TarArchiveEntry>();

                TarArchiveInputStream inputStream;

                if(archive.getName().endsWith(".tar"))
                    inputStream =new TarArchiveInputStream(new BufferedInputStream(new FileInputStream(archive)));
                else inputStream = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(archive)));

                TarArchiveEntry tarArchiveEntry=inputStream.getNextTarEntry();

                while(tarArchiveEntry != null){
                    archiveEntries.add(tarArchiveEntry);
                    tarArchiveEntry=inputStream.getNextTarEntry();
                }

                for(TarArchiveEntry entry : archiveEntries) {
                    totalBytes += entry.getSize();
                }



                for(TarArchiveEntry entry : archiveEntries){


                        unzipTAREntry(inputStream, entry, destinationPath);
                }

                // operating finished
                inputStream.close();

                return true;
            } catch (Exception e) {
                Log.e("amaze", "Error while extracting file " + archive, e);
                AppConfig.toast(cd, cd.getString(R.string.error));
                return false;
            }

        }

        private boolean extractRar(File archive, String destinationPath) {

            try {
                ArrayList<FileHeader> arrayList=new ArrayList<FileHeader>();
                Archive zipFile = new Archive(archive);
                FileHeader fh = zipFile.nextFileHeader();

                while(fh != null){
                    arrayList.add(fh);
                    fh=zipFile.nextFileHeader();

                }

                for (FileHeader header:arrayList) {
                    totalBytes += header.getFullUnpackSize();
                }



                for (FileHeader header:arrayList){

                        unzipRAREntry(zipFile, header, destinationPath);
                }

                return true;
            } catch (Exception e) {
                Log.e("amaze", "Error while extracting file " + archive, e);
                AppConfig.toast(cd, cd.getString(R.string.error));
                return false;
            }
        }

        private boolean extractRar(File archive, String destinationPath, ArrayList<String> entries) {

            try {

                Archive rarFile = new Archive(archive);

                ArrayList<FileHeader> arrayList=new ArrayList<FileHeader>();

                // iterating archive elements to find file names that are to be extracted
                for (FileHeader header : rarFile.getFileHeaders()) {
                    for (String entry : entries) {

                        if (header.getFileNameString().contains(entry)) {
                            // header to be extracted is atleast the entry path (may be more, when it is a directory)
                            arrayList.add(header);
                        }
                    }
                }

                // get the total size of elements to be extracted
                for (FileHeader entry : arrayList) {
                    totalBytes += entry.getFullUnpackSize();
                }

                int i = 0;
                for(FileHeader entry : arrayList) {

                        unzipRAREntry(rarFile, entry, destinationPath);
                }

                return true;
            } catch (Exception e) {

                Log.e("amaze", "Error while extracting file " + archive, e);
                AppConfig.toast(cd, cd.getString(R.string.error));
                return false;
            }
        }

        protected void doInBackground() {

            File f = new File(file);

            String path;
            if(epath.equals(file)) {

                // custom extraction path not set, extract at default path
                path=f.getParent()+"/"+f.getName().substring(0, f.getName().lastIndexOf("."));
            } else {

                if(epath.endsWith("/")) {
                    path=epath + f.getName().substring(0, f.getName().lastIndexOf("."));
                } else {
                    path=epath + "/" + f.getName().substring(0, f.getName().lastIndexOf("."));
                }
            }

            if(entries!=null && entries.size()!=0) {
                if (f.getName().toLowerCase().endsWith(".zip"))
                    extract(f, path, entries);
                else if (f.getName().toLowerCase().endsWith(".rar"))
                    extractRar(f, path, entries);
            } else if(f.getName().toLowerCase().endsWith(".zip") || f.getName().toLowerCase().endsWith(".jar") || f.getName().toLowerCase().endsWith(".apk"))
                extract(f, path);
            else if(f.getName().toLowerCase().endsWith(".rar"))
                extractRar(f, path);
            else if(f.getName().toLowerCase().endsWith(".tar") || f.getName().toLowerCase().endsWith(".tar.gz"))
                extractTar(f, path);
            Log.i("Amaze", "Almost Completed");
        }

       
}
