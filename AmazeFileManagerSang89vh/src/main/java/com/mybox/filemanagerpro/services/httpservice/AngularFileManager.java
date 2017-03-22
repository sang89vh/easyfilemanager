package com.mybox.filemanagerpro.services.httpservice;

import android.content.Context;
import android.util.Log;

import com.mybox.filemanagerpro.R;
import com.mybox.filemanagerpro.exceptions.RootNotPermittedException;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * https://github.com/joni2back/angular-filemanager/blob/master/API.md
 * @author Admin
 *
 */
public class AngularFileManager{
	private String TAG="AngularFileManager";
	private Context mContext;
	private JSONObject errorMessage;
	public AngularFileManager(Context context){
		this.mContext=context;
		 errorMessage = new JSONObject();
		try {

			String error = "Access denied to remove file";
			JSONObject data = new JSONObject();
			data.put("success", false);
			data.put("error", error);

			errorMessage.put("result", data);

		}catch (JSONException e){
			Log.e(TAG,e.getMessage(),e);
		}
	}
/*
	Listing (URL: fileManagerConfig.listUrl, Method: POST)

	JSON Request content

	{
	    "action": "list",
	    "path": "/public_html"
	}
	JSON Response

	{ "result": [ 
	    {
	        "name": "magento",
	        "rights": "drwxr-xr-x",
	        "size": "4096",
	        "date": "2016-03-03 15:31:40",
	        "type": "dir"
	    }, {
	        "name": "index.php",
	        "rights": "-rw-r--r--",
	        "size": "549923",
	        "date": "2016-03-03 15:31:40",
	        "type": "file"
	    }
	]}
	
	*/
	protected JSONObject listing(JSONObject para) throws JSONException {
		String path = getRealPath(para.getString("path"));


		JSONObject result = new JSONObject();
		JSONArray data = new JSONArray();
		
		result.put("result", data);
		
		return result;
	}	
	
	/*
	Rename (URL: fileManagerConfig.renameUrl, Method: POST)

	JSON Request content

	{
	    "action": "rename",
	    "item": "/public_html/index.php",
	    "newItemPath": "/public_html/index2.php"
	}
	JSON Response

	{ "result": { "success": true, "error": null } }
	
	*/
	
	protected JSONObject rename(JSONObject para) throws JSONException, RootNotPermittedException {
		String item = getRealPath(para.getString("item"));
		File source = new File(item);
		
		String newItemPath = getRealPath(para.getString("newItemPath"));
		File target = new File(newItemPath);
		
		FileUtil.renameFolder(source, target, mContext);
		
		JSONObject result = new JSONObject();
		String error=null;
		JSONObject data = new JSONObject();
		data.put("success", true);
		data.put("error", error);
		
		result.put("result", data);
		
		return result;
	}	
	/*
	Move (URL: fileManagerConfig.moveUrl, Method: POST)

	JSON Request content

	{
	    "action": "move",
	    "items": ["/public_html/libs", "/public_html/config.php"],
	    "newPath": "/public_html/includes"
	}
	JSON Response

	{ "result": { "success": true, "error": null } }
	
	*/
	
	protected JSONObject move(JSONObject para) throws JSONException {
		JSONArray items = para.getJSONArray("items");
		String newPath = getRealPath(para.getString("newPath"));


		
		boolean b=true;
		String error=null;
		for (int i = 0; i < items.length(); i++) {

			String object = getRealPath((String) items.getString(i));
				File target=new File(newPath +File.separator+ FilenameUtils.getName(object));
	            File source=new File(object);
	            if(!source.renameTo(target)){
	            	b=false;
	            	error = "Oops! Something went wrong";
	            }
	        }
		
		
		JSONObject result = new JSONObject();
		
		JSONObject data = new JSONObject();
		data.put("success", b);
		data.put("error", error);
		
		result.put("result", data);
		
		return result;
	}	
	/*
	Copy (URL: fileManagerConfig.copyUrl, Method: POST)

	JSON Request content

	{
	    "action": "copy",
	    "items": ["/public_html/index.php", "/public_html/config.php"],
	    "newPath": "/includes",
	    "singleFilename": "renamed.php" <-- (only present in single selection copy)
	}
	JSON Response

	{ "result": { "success": true, "error": null } }
	
	
	*/
	protected JSONObject copy(JSONObject para) throws JSONException {
		JSONArray items = para.getJSONArray("items");
		String newPath = getRealPath(para.getString("newPath"));
		String singleFilename = para.getString("singleFilename");
		

		for (int i = 0; i < items.length(); i++) {

			String object = getRealPath((String) items.getString(i));
			File target=new File(newPath+File.separator+ FilenameUtils.getName(object));
			File source = new File(object);
			FileUtil.copyFile(source, target, mContext);
		}
		
		JSONObject result = new JSONObject();
		
		String error=null;
		JSONObject data = new JSONObject();
		data.put("success", true);
		data.put("error", error);
		
		result.put("result", data);
		
		return result;
	}
	/*
	Remove (URL: fileManagerConfig.removeUrl, Method: POST)

	JSON Request content

	{
	    "action": "remove",
	    "items": ["/public_html/index.php"],
	}
	JSON Response

	{ "result": { "success": true, "error": null } }
	
	
	*/
	protected JSONObject remove(JSONObject para) throws JSONException {
		JSONArray items = para.getJSONArray("items");

		for (int i = 0; i < items.length(); i++) {

			String object = getRealPath((String) items.getString(i));
			File file = new File(object);
			FileUtil.deleteFile(file, mContext);
		}
		JSONObject result = new JSONObject();
		
		String error=null;
		JSONObject data = new JSONObject();
		data.put("success", true);
		data.put("error", error);
		
		result.put("result", data);
		
		return result;
	}
	/*
	Edit file (URL: fileManagerConfig.editUrl, Method: POST)

	JSON Request content

	{
	    "action": "edit",
	    "item": "/public_html/index.php",
	    "content": "<?php echo random(); ?>"
	}
	JSON Response

	{ "result": { "success": true, "error": null } }
	
	
	*/
	protected JSONObject edit(JSONObject para) throws JSONException {
		String item = getRealPath(para.getString("item"));
		String content = para.getString("content");
		
		String error=null;
		Boolean success = true;
		try  {
			BufferedWriter bw = new BufferedWriter(new FileWriter(item));

			bw.write(content);

			// no need to close it.
		    bw.close();

		} catch (IOException e) {

			error = e.getMessage();

		}

		
		JSONObject result = new JSONObject();
		
		JSONObject data = new JSONObject();
		data.put("success", success);
		data.put("error", error);
		
		result.put("result", data);
		
		return result;
	}
	/*
	Get content of a file (URL: fileManagerConfig.getContentUrl, Method: POST)

	JSON Request content

	{
	    "action": "getContent",
	    "item": "/public_html/index.php"
	}
	JSON Response

	{ "result": "<?php echo random(); ?>" }
	
	*/
	protected JSONObject getContent(JSONObject para) throws JSONException {
		String item = getRealPath(para.getString("item"));
		
		JSONObject result = new JSONObject();
		
		JSONObject data = new JSONObject();
		
		result.put("result", data);
		
		return result;
	}
	/*
	Create folder (URL: fileManagerConfig.createFolderUrl, Method: POST)

	JSON Request content

	{
	    "action": "createFolder",
	    "newPath": "/public_html/new-folder"
	}
	JSON Response

	{ "result": { "success": true, "error": null } }
	
	
	*/
	protected JSONObject createFolder(JSONObject para) throws JSONException {
		String newPath = getRealPath(para.getString("newPath"));
		
		File file = new File(newPath);
		FileUtil.mkdir(file, mContext);
		
		JSONObject result = new JSONObject();
		
		
		String error=null;
		JSONObject data = new JSONObject();
		data.put("success", true);
		data.put("error", error);
		
		result.put("result", data);
		
		return result;
	}
	/*
	Set permissions (URL: fileManagerConfig.permissionsUrl, Method: POST)

	JSON Request content

	{
	    "action": "changePermissions",
	    "items": ["/public_html/root", "/public_html/index.php"],
	    "permsCode": "653",
	    "perms": "rw-r-x-wx",
	    "recursive": true
	}
	JSON Response

	{ "result": { "success": true, "error": null } }
	
	*/
	protected JSONObject changePermissions(JSONObject para) throws JSONException {
		JSONArray items = para.getJSONArray("items");
		String perms = para.getString("perms");
		String permsCode = para.getString("permsCode");
		Boolean recursive = para.getBoolean("recursive");


		Boolean success = true;
		String error=null;
		for (int i = 0; i < items.length(); i++) {
			
			String object = getRealPath((String) items.getString(i));
			error = changePermission(permsCode,object);
			if(error != null){
				success = false;
			}
		}
		JSONObject result = new JSONObject();

		JSONObject data = new JSONObject();
		data.put("success", success);
		data.put("error", error);
		
		result.put("result", data);
		
		return result;
	}
	/*
	Compress file (URL: fileManagerConfig.compressUrl, Method: POST)

	JSON Request content

	{
	    "action": "compress",
	    "items": ["/public_html/photos", "/public_html/docs"],
	    "destination": "/public_html/backups",
	    "compressedFilename": "random-files.zip"
	}}
	JSON Response

	{ "result": { "success": true, "error": null } }
	
	*/
	protected JSONObject compress(JSONObject para) throws JSONException {
		JSONArray items = para.getJSONArray("items");
		String destination = getRealPath(para.getString("destination"));
		String compressedFilename = para.getString("compressedFilename");
		
		ZipUtils zipUtils = new ZipUtils(mContext,destination+"/"+compressedFilename+".zip",items);
		zipUtils.execute();
		JSONObject result = new JSONObject();
		
		String error=null;
		JSONObject data = new JSONObject();
		data.put("success", true);
		data.put("error", error);
		
		result.put("result", data);
		
		return result;
	}
	/*
	Extract file (URL: fileManagerConfig.extractUrl, Method: POST)

	JSON Request content

	{
	    "action": "extract",
	    "destination": "/public_html/extracted-files",
	    "item": "/public_html/compressed.zip"
	}
	JSON Response

	{ "result": { "success": true, "error": null } }
	
	*/
	protected JSONObject extract(JSONObject para) throws JSONException {
		String destination = getRealPath(para.getString("destination"));
		String item = getRealPath(para.getString("item"));
		ExtractUtils eu=new ExtractUtils(item, destination,mContext);
		eu.doInBackground();
		
		JSONObject result = new JSONObject();
		
		String error=null;
		JSONObject data = new JSONObject();
		data.put("success", true);
		data.put("error", error);
		
		result.put("result", data);
		
		return result;
	}


	/*
	Download multiples files in ZIP/TAR (URL: fileManagerConfig.downloadFileUrl, Method: GET)

	JSON Request content

	{
	    "action": "downloadMultiple",
	    "items": ["/public_html/image1.jpg", "/public_html/image2.jpg"],
	    "toFilename": "multiple-items.zip"
	}}
	Response

	-File content
	Errors / Exceptions

	Any backend error should be with an error 500 HTTP code.

	Btw, you can also report errors with a 200 response both using this json structure

	{ "result": {
	    "success": false,
	    "error": "Access denied to remove file"
	}}
	
	*/
	protected JSONObject downloadMultiple(JSONObject para) throws JSONException {
		JSONArray items = para.getJSONArray("items");
		String toFilename = para.getString("toFilename");
		
		JSONObject result = new JSONObject();
		
		String error="Access denied to remove file";
		JSONObject data = new JSONObject();
		data.put("success", false);
		data.put("error", error);
		
		result.put("result", data);
		
		return result;
	}

	protected JSONObject error(){

		return errorMessage;
	}
	public  String getRealPath(String path){
		String realPath = path.replaceAll(
				"/" + mContext.getResources().getString(R.string.storage)+"01"
						+ "|" + "/" + mContext.getResources().getString(R.string.storage)+"02"
						+ "|" + "/" + mContext.getResources().getString(R.string.images)
						+ "|" + "/" + mContext.getResources().getString(R.string.videos)
						+ "|" + "/" + mContext.getResources().getString(R.string.audio)
						+ "|" + "/" + mContext.getResources().getString(R.string.extstorage)
						+ "|" + "/" + mContext.getResources().getString(R.string.documents)
				, "");
		if(path.contains(mContext.getResources().getString(R.string.storage)+"01")){
			realPath = "/storage/emulated/0"+realPath;
		}else if(path.contains(mContext.getResources().getString(R.string.storage)+"02")){
			realPath = "/storage/emulated/legacy"+realPath;
		}else if(path.contains(mContext.getResources().getString(R.string.extstorage))){
			realPath = "/storage/sdcard1"+realPath;
		}

		return realPath;
	}

	public  String changePermission(String pers,String path){
		Process process = null;
		DataOutputStream dataOutputStream = null;

		try {
			process = Runtime.getRuntime().exec("su");
			dataOutputStream = new DataOutputStream(process.getOutputStream());
			dataOutputStream.writeBytes("chmod "+pers+" "+path+"\n");
			dataOutputStream.writeBytes("exit\n");
			dataOutputStream.flush();
			process.waitFor();
		} catch (Exception e) {

			return e.getMessage();
		} finally {
			try {
				if (dataOutputStream != null) {
					dataOutputStream.close();
				}
				process.destroy();
			} catch (Exception e) {
				return e.getMessage();
			}
		}
		return null;
	}





}
