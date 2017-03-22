package com.mybox.filemanagerpro.services.httpservice;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.mybox.filemanagerpro.R;
import com.mybox.filemanagerpro.activities.BaseActivity;
import com.mybox.filemanagerpro.activities.MainActivity;
import com.mybox.filemanagerpro.exceptions.RootNotPermittedException;
import com.mybox.filemanagerpro.filesystem.BaseFile;
import com.mybox.filemanagerpro.filesystem.HFile;
import com.mybox.filemanagerpro.filesystem.RootHelper;
import com.mybox.filemanagerpro.fragments.SearchAsyncHelper;
import com.mybox.filemanagerpro.ui.Layoutelements;
import com.mybox.filemanagerpro.utils.AppConfig;
import com.mybox.filemanagerpro.utils.DataUtils;
import com.mybox.filemanagerpro.utils.OpenMode;
import com.mybox.filemanagerpro.utils.provider.UtilitiesProviderInterface;

import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoFileUpload;
import fi.iki.elonen.NanoHTTPD;

/**
 * Created by jack on 2/25/17.
 */

public class HTTPServer extends NanoHTTPD {
    private String password;
    private boolean isPasswordProtected = false;
    private AngularFileManager angularFileManager;
    private static String TAG = "HTTPServer";
    private JSONArray LIST_ELEMENTS;
    private UtilitiesProviderInterface utilsProvider;
    private HTTPServer mCallbacks;
    private MainActivity mainActivity;
    private AppConfig appConfig;
    private String mPath, mInput;
    public SearchAsyncHelper.SearchTask mSearchTask;
    private OpenMode mOpenMode;
    OpenMode openmode = OpenMode.FILE;
    private boolean mRootMode, isRegexEnabled, isMatchesEnabled;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private StringBuffer mMainHtml, mLoginHtml;

    public static final String KEY_PATH = "path";
    public static final String KEY_INPUT = "input";
    public static final String KEY_OPEN_MODE = "open_mode";
    public static final String KEY_ROOT_MODE = "root_mode";
    public static final String KEY_REGEX = "regex";
    public static final String KEY_REGEX_MATCHES = "matches";

    private Context mContext;


    private NanoFileUpload uploader;

    public HTTPServer(int port, Context context) {
        super(port);
        mContext = context;

        appConfig = (AppConfig) mContext.getApplicationContext();
        mainActivity = appConfig.getMainActivity();

        utilsProvider = mainActivity;
        mCallbacks = this;
        uploader = new NanoFileUpload(new DiskFileItemFactory());

        angularFileManager = new AngularFileManager(mContext);

        mMainHtml = new StringBuffer("<html lang=\"en\" data-ng-app=\"FileManagerApp\">\n \n<header> \n");
        mMainHtml.append("<meta name=\"viewport\" content=\"initial-scale=1.0, user-scalable=no\"> \n");
        mMainHtml.append("<meta charset=\"utf-8\"> \n");
        mMainHtml.append("<title>Easy File Manager</title> \n");
        mMainHtml.append("<script src=\"/angular_min.js\"></script> \n");
        mMainHtml.append("<script src=\"/angular_translate_min.js\"></script> \n");
        mMainHtml.append("<script src=\"/ng_file_upload_min.js\"></script> \n");
        mMainHtml.append("<script src=\"/jquery_min.js\"></script> \n");
        mMainHtml.append("<script src=\"/bootstrap_min.js\"></script> \n");
        mMainHtml.append("<link rel=\"stylesheet\" href=\"/bootstrap_min.css\" /> \n");

        mMainHtml.append("<link rel=\"stylesheet\" href=\"/angular_filemanager_min.css\"> \n");
        mMainHtml.append("<script src=\"/angular_filemanager_min.js\"></script> \n");
        mMainHtml.append("</header> \n");
        mMainHtml.append("<script type=\"text/javascript\">\n");
        mMainHtml.append("    angular.module('FileManagerApp').config(['fileManagerConfigProvider', function (config) {\n");
        mMainHtml.append("      var defaults = config.$get();\n");
        mMainHtml.append("      config.set({\n");
        mMainHtml.append("        appName: 'angular-filemanager',\n");
        mMainHtml.append("        pickCallback: function(item) {\n");
        mMainHtml.append("          var msg = 'Picked %s \"%s\" for external use'\n");
        mMainHtml.append("            .replace('%s', item.type)\n");
        mMainHtml.append("            .replace('%s', item.fullPath());\n");
        mMainHtml.append("          window.alert(msg);\n");
        mMainHtml.append("        },\n");
        mMainHtml.append("\n");
        mMainHtml.append("        allowedActions: angular.extend(defaults.allowedActions, {\n");
        mMainHtml.append("          pickFiles: false,\n");
        mMainHtml.append("          pickFolders: false,\n");
        mMainHtml.append("          changePermissions: false,\n");
        mMainHtml.append("        }),\n");
        mMainHtml.append("      });\n");
        mMainHtml.append("    }]);\n");
        mMainHtml.append("$(document).ready(function(){\n");
        mMainHtml.append("\tsetTimeout(function(){$(\".breadcrumb li\")[0].remove();},100);\n");
        mMainHtml.append("})\n");
        mMainHtml.append("  </script>\n");
        mMainHtml.append("<body class=\"ng-cloak\">\n");
        mMainHtml.append("<div class=\"col-md-12\" >\n");
        mMainHtml.append("<angular-filemanager></angular-filemanager> \n");
        mMainHtml.append("<div class=\"container\"><a href=\"https://play.google.com/store/apps/developer?id=MyboxTeam\">Free Download File Manager on Google Play Store:Click here</a></div> \n");
        mMainHtml.append("</div> \n");
        mMainHtml.append("</body> \n</html>");


        mLoginHtml = new StringBuffer();
        mLoginHtml.append("<!DOCTYPE html>\n");
        mLoginHtml.append("<html lang=\"en\">\n");
        mLoginHtml.append("  <head>\n");
        mLoginHtml.append("    <meta charset=\"utf-8\">\n");
        mLoginHtml.append("    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n");
        mLoginHtml.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        mLoginHtml.append("    <meta name=\"description\" content=\"\">\n");
        mLoginHtml.append("    <meta name=\"author\" content=\"\">\n");
        mLoginHtml.append("    <title>Easy wifi file manager</title>\n");
        mLoginHtml.append("    <link href=\"/bootstrap_min.css\" rel=\"stylesheet\">\n");
//        mLoginHtml.append("    <script src=\"/bootstrap_min.js\"></script>\n");
        mLoginHtml.append("  </head>\n");
        mLoginHtml.append("\n");
        mLoginHtml.append("  <body>\n");
        mLoginHtml.append("    <div class=\"container\">\n");
        mLoginHtml.append("    <div class=\"col-md-4 col-sm-12\">\n");
        mLoginHtml.append("    </div>\n");
        mLoginHtml.append("    <div class=\"col-md-4 col-sm-12\">\n");
        mLoginHtml.append("      <form class=\"form-signin\" action=\"/\" method=\"POST\">\n");
        mLoginHtml.append("        <h2 class=\"form-signin-heading\">Please sign in</h2>\n");
        mLoginHtml.append("        <label for=\"inputPassword\" class=\"sr-only\">Password</label>\n");
        mLoginHtml.append("        <input name=\"password\" type=\"password\" id=\"inputPassword\" class=\"form-control\" placeholder=\"Password\" required>\n");
        mLoginHtml.append("        <button class=\"btn btn-lg btn-primary btn-block\" type=\"submit\">Sign in</button>\n");
        mLoginHtml.append("      </form>\n");
        mLoginHtml.append("    </div>\n");
        mLoginHtml.append("    <div class=\"col-md-4 col-sm-12\">\n");
        mLoginHtml.append("    </div>\n");
        mLoginHtml.append("    </div> \n");
        mLoginHtml.append("  </body>\n");
        mLoginHtml.append("</html>");

    }


    private static final Random RANDOM = new Random();
    private static final int TOKEN_SIZE = 24;
    private static final char[] HEX = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final String TOKEN_COOKIE = "__SESSION_ID__";
    private static HashMap<String, String> sessions = new HashMap<>();

    private String genSessionToken() {
        StringBuilder sb = new StringBuilder(TOKEN_SIZE);
        for (int i = 0; i < TOKEN_SIZE; i++) {
            sb.append(HEX[RANDOM.nextInt(HEX.length)]);
        }
        return sb.toString();
    }

    private String newSessionToken() {
        String token;
        do {
            token = genSessionToken();
        } while (sessions.containsKey(token));
        return token;
    }

    public synchronized String findSessionCreate(CookieHandler cookies) {
        String token = cookies.read(TOKEN_COOKIE);
        if (token == null) {
            token = newSessionToken();
            cookies.set(TOKEN_COOKIE, token, 10);
        }
        if (!sessions.containsKey(token)) {
            sessions.put(token, token);
        }
        return sessions.get(token);
    }

    public String findSession(CookieHandler cookies) {
        String token = cookies.read(TOKEN_COOKIE);
        if (token == null) {
            return null;
        }else{
            cookies.delete(TOKEN_COOKIE);
        }

        return sessions.get(token);
    }

    public void destroySession(String token, CookieHandler cookies) {
        sessions.remove(token);
        cookies.delete(TOKEN_COOKIE);
    }

    public static final String MULTIPART = "multipart/";

    public static final boolean isMultipartContent(RequestContext ctx) {
        String contentType = ctx.getContentType();
        if (contentType == null) {
            return false;
        }
        if (contentType.toLowerCase(Locale.ENGLISH).startsWith(MULTIPART)) {
            return true;
        }
        return false;
    }

    @Override
    public Response serve(IHTTPSession session) {

        CookieHandler cookieHandler = session.getCookies();

        /*
	Upload file (URL: fileManagerConfig.uploadUrl, Method: POST, Content-Type: multipart/form-data)

	Http post request payload

	------WebKitFormBoundaryqBnbHc6RKfXVAf9j
	Content-Disposition: form-data; name="destination"
	/

	------WebKitFormBoundaryqBnbHc6RKfXVAf9j
	Content-Disposition: form-data; name="file-0"; filename="github.txt"
	Content-Type: text/plain
	JSON Response

	{ "result": { "success": true, "error": null } }
	Unlimited file items to upload, each item will be enumerated as file-0, file-1, etc.

	For example, you may retrieve the file in PHP using:

	$destination = $_POST['destination'];
	$_FILES['file-0'] or foreach($_FILES)
	Download / Preview file (URL: fileManagerConfig.downloadMultipleUrl, Method: GET)

	Http query params

	[fileManagerConfig.downloadFileUrl]?action=download&path=/public_html/image.jpg
	Response

	-File content

	------

	*/


        String uri = session.getUri();
        Log.d(TAG, uri);

       if ("/".equals(uri)) {

           if (Method.POST.equals(session.getMethod())) {

               Map<String, String> files = new HashMap<String, String>();
               try {
                   session.parseBody(files);
               } catch (IOException ioe) {
                   Log.e(TAG, ioe.getMessage(), ioe);

                   return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
               } catch (ResponseException re) {
                   Log.e(TAG, re.getMessage(), re);
                   return newFixedLengthResponse(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
               }

               String mypass = session.getParms().get("password");

               if (password.equals(mypass)) {
                   findSessionCreate(session.getCookies());
                   return newFixedLengthResponse(mMainHtml.toString());
               }else{
                   return newFixedLengthResponse("oop!Wrong password <a href=\"\\\">Back to login page</a>");
               }
           }

            if (isPasswordProtected){
                if(findSession(session.getCookies()) != null) {
                    return newFixedLengthResponse(mMainHtml.toString());
                } else {
                    return newFixedLengthResponse(mLoginHtml.toString());
                }
            }else {
                return newFixedLengthResponse(mMainHtml.toString());
            }

        } else if (uri.contains("js")) {
            InputStream fis;
            try {
                fis = mContext.getResources().getAssets().open(uri.substring(1, uri.length()));
            } catch (IOException e) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", angularFileManager.error().toString());
            }
            return newChunkedResponse(Response.Status.OK, "application/javascript", fis);

        } else if (uri.equals("/favicon.ico")) {
            InputStream fis;
            try {
                fis = mContext.getResources().getAssets().open(uri.substring(1, uri.length()));
            } catch (IOException e) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", angularFileManager.error().toString());
            }
            return newChunkedResponse(Response.Status.OK, "image/x-icon", fis);

        } else if (uri.contains("css")) {
            InputStream fis;
            try {
                fis = mContext.getResources().getAssets().open(uri.substring(1, uri.length()));
            } catch (IOException e) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", angularFileManager.error().toString());
            }
            return newChunkedResponse(Response.Status.OK, "text/css", fis);
        } else if (uri.contains("woff2")) {
            InputStream fis;
            try {
                fis = mContext.getResources().getAssets().open(uri.substring(1, uri.length()).replace("/", "_"));
            } catch (IOException e) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", angularFileManager.error().toString());
            }
            return newChunkedResponse(Response.Status.OK, "font/woff2", fis);
        } else if (uri.contains("woff")) {

            InputStream fis;
            try {
                fis = mContext.getResources().getAssets().open(uri.substring(1, uri.length()).replace("/", "_"));
            } catch (IOException e) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", angularFileManager.error().toString());
            }
            return newChunkedResponse(Response.Status.OK, "font/woff", fis);

        } else if (uri.equals("/bridges/php/handler.php")) {
            JSONObject para = null;
            String action = null;


            Map<String, String> files = new HashMap<String, String>();
            Method method = session.getMethod();

            Boolean isUpload = isMultipartContent(new NanoFileUpload.NanoHttpdContext(session));
            String destination = null;
            List<String> filesUpload = new ArrayList<>();

            if (Method.PUT.equals(method) || Method.POST.equals(method)) {
                try {
                    session.parseBody(files);
                } catch (IOException ioe) {
                    Log.e(TAG, ioe.getMessage(), ioe);

                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
                } catch (ResponseException re) {
                    Log.e(TAG, re.getMessage(), re);
                    return newFixedLengthResponse(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
                }

                if (isUpload) {

                    int k = 0;
                    while (session.getParms().get("file-" + k) != null) {
                        filesUpload.add(session.getParms().get("file-" + k));
                        k++;
                    }


                    destination = angularFileManager.getRealPath(session.getParms().get("destination"));

                } else {

                    try {
                        para = new JSONObject(files.get("postData"));
                        action = para.getString("action");


                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            } else {

                Map<String, String> getPara = session.getParms();

                action = getPara.get("action");
            }

            if (isUpload && !"/".equals(destination)) {

                try {
                    boolean b = true;
                    String error = null;
                    for (int i = 0; i < filesUpload.size(); i++) {

                        String fileName = filesUpload.get(i);
                        File target = new File(destination + File.separator + fileName);
                        File source = new File(files.get("file-" + i));

                        FileUtil.copyFile(source, target, mContext);

                    }


                    JSONObject result = new JSONObject();

                    JSONObject data = new JSONObject();
                    data.put("success", b);
                    data.put("error", error);

                    result.put("result", data);
                    return newFixedLengthResponse(Response.Status.OK, "application/json", result.toString());
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                }


            } else if (isUpload && !"/".equals(destination)) {
                JSONObject result = new JSONObject();
                try {
                    JSONObject data = new JSONObject();
                    data.put("success", false);
                    data.put("error", "Pls, choice other directory");
                    result.put("result", data);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
                return newFixedLengthResponse(Response.Status.OK, "application/json", result.toString());

            }


            if ("list".equals(action)) {
                String path = null;
                try {

                    path = para.getString("path");

                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                }

                String realPath = angularFileManager.getRealPath(path);

                if ("/".equals(path)) {

                    List<String> dirs = mainActivity.getListDrawer();

                    return newFixedLengthResponse(Response.Status.OK, "application/json", listDirToJson(dirs).toString());
                } else if (("/" + mContext.getResources().getString(R.string.storage) + "01").equals(path)) {
                    openmode = OpenMode.FILE;
                    ArrayList<Layoutelements> dirs = getAllSubDir("/storage/emulated/0");
                    return newFixedLengthResponse(Response.Status.OK, "application/json", listLayoutelementsToJsonStorage(dirs).toString());


                } else if (("/" + mContext.getResources().getString(R.string.storage) + "02").equals(path)) {
                    openmode = OpenMode.FILE;
                    ArrayList<Layoutelements> dirs = getAllSubDir("/storage/emulated/legacy");
                    return newFixedLengthResponse(Response.Status.OK, "application/json", listLayoutelementsToJsonStorage(dirs).toString());

                } else if (("/" + mContext.getResources().getString(R.string.images)).equals(path)) {
                    openmode = OpenMode.CUSTOM;
                    ArrayList<Layoutelements> dirs = getAllSubDir("0");
                    return newFixedLengthResponse(Response.Status.OK, "application/json", listLayoutelementsToJson(dirs).toString());


                } else if (("/" + mContext.getResources().getString(R.string.videos)).equals(path)) {
                    openmode = OpenMode.CUSTOM;
                    ArrayList<Layoutelements> dirs = getAllSubDir("1");
                    return newFixedLengthResponse(Response.Status.OK, "application/json", listLayoutelementsToJson(dirs).toString());


                } else if (("/" + mContext.getResources().getString(R.string.audio)).equals(path)) {
                    openmode = OpenMode.CUSTOM;
                    ArrayList<Layoutelements> dirs = getAllSubDir("2");
                    return newFixedLengthResponse(Response.Status.OK, "application/json", listLayoutelementsToJson(dirs).toString());


                } else if (("/" + mContext.getResources().getString(R.string.documents)).equals(path)) {
                    openmode = OpenMode.CUSTOM;
                    ArrayList<Layoutelements> dirs = getAllSubDir("3");
                    return newFixedLengthResponse(Response.Status.OK, "application/json", listLayoutelementsToJson(dirs).toString());


                } else if (("/" + mContext.getResources().getString(R.string.extstorage)).equals(path)) {
                    openmode = OpenMode.FILE;
                    ArrayList<Layoutelements> dirs = getAllSubDir("/storage/sdcard1");
                    return newFixedLengthResponse(Response.Status.OK, "application/json", listLayoutelementsToJson(dirs).toString());


                }
                else {
                    openmode = OpenMode.FILE;
                    ArrayList<Layoutelements> dirs = getAllSubDir(realPath);
                    return newFixedLengthResponse(Response.Status.OK, "application/json", listLayoutelementsToJson(dirs).toString());
                }

            } else if ("download".equals(action)) {

                String path = session.getParms().get("path");
                String realPath = angularFileManager.getRealPath(path);
                Log.d(TAG, "real path:" + realPath);

                InputStream is = null;
                File f = null;
                try {

                    f = new File(realPath);
                    is = new FileInputStream(f);

                } catch (FileNotFoundException e) {
                    Log.e(TAG, e.getMessage(), e);

                    return newFixedLengthResponse(Response.Status.OK, "application/json", angularFileManager.error().toString());
                }
                String extension = MimeTypeMap.getFileExtensionFromUrl(realPath);

                String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

                NanoHTTPD.Response res = newChunkedResponse(Response.Status.OK, type, is);
                res.addHeader("Content-Disposition", "attachment; filename=\"" + "MyboxTeam_" + f.getName() + "\"");
                return res;


            } else if ("getContent".equals(action)) {
                String item = null;
                try {
                    item = para.getString("item");
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
                String realPath = angularFileManager.getRealPath(item);
                Log.d(TAG, "real path:" + realPath);

                InputStream is = null;
                try {

                    File f = new File(realPath);
                    is = new FileInputStream(f);

                } catch (FileNotFoundException e) {
                    Log.e(TAG, e.getMessage(), e);
                }

                String content = convertStreamToString(is);

                return newFixedLengthResponse(Response.Status.OK, "application/json", stringContentToJson(content).toString());

            } else if ("rename".equals(action)) {

                JSONObject result = null;
                try {

                    result = angularFileManager.rename(para);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Error");
                } catch (RootNotPermittedException e) {
                    Log.e(TAG, e.getMessage(), e);
                    return newFixedLengthResponse(Response.Status.OK, "application/json", angularFileManager.error().toString());
                }

                return newFixedLengthResponse(Response.Status.OK, "application/json", result.toString());

            } else if ("move".equals(action)) {

                JSONObject result = null;
                try {

                    result = angularFileManager.move(para);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Error");
                }

                return newFixedLengthResponse(Response.Status.OK, "application/json", result.toString());

            } else if ("copy".equals(action)) {

                JSONObject result = null;
                try {

                    result = angularFileManager.copy(para);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                    return newFixedLengthResponse(Response.Status.OK, "application/json", angularFileManager.error().toString());
                }

                return newFixedLengthResponse(Response.Status.OK, "application/json", result.toString());

            } else if ("remove".equals(action)) {

                JSONObject result = null;
                try {

                    result = angularFileManager.remove(para);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Error");
                }

                return newFixedLengthResponse(Response.Status.OK, "application/json", result.toString());

            } else if ("edit".equals(action)) {

                JSONObject result = null;
                try {

                    result = angularFileManager.edit(para);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Error");
                }

                return newFixedLengthResponse(Response.Status.OK, "application/json", result.toString());

            } else if ("createFolder".equals(action)) {

                JSONObject result = null;
                try {

                    result = angularFileManager.createFolder(para);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Error");
                }

                return newFixedLengthResponse(Response.Status.OK, "application/json", result.toString());

            } else if ("changePermissions".equals(action)) {

                JSONObject result = null;
                try {

                    result = angularFileManager.changePermissions(para);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Error");
                }

                return newFixedLengthResponse(Response.Status.OK, "application/json", result.toString());

            } else if ("compress".equals(action)) {

                JSONObject result = null;
                try {

                    result = angularFileManager.compress(para);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Error");
                }

                return newFixedLengthResponse(Response.Status.OK, "application/json", result.toString());

            } else if ("extract".equals(action)) {

                JSONObject result = null;
                try {

                    result = angularFileManager.extract(para);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Error");
                }

                return newFixedLengthResponse(Response.Status.OK, "application/json", result.toString());

            } else {
                return newFixedLengthResponse(Response.Status.OK, "application/json", listDirToJson(null).toString());
            }

        } else {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Error");
        }

    }

    private ArrayList<Layoutelements> getAllSubDir(String path) {
        mPath = path;

        // params comes from the execute() call: params[0] is the url.
        ArrayList<Layoutelements> list = null;

        switch (openmode) {

            case CUSTOM:
                ArrayList<BaseFile> arrayList = null;
                switch (Integer.parseInt(path)) {
                    case 0:
                        arrayList = (listImages());
                        path = "0";
                        break;
                    case 1:
                        arrayList = (listVideos());
                        path = "1";
                        break;
                    case 2:
                        arrayList = (listaudio());
                        path = "2";
                        break;
                    case 3:
                        arrayList = (listDocs());
                        path = "3";
                        break;

                }
                try {
                    if (arrayList != null)
                        list = addToSubfolder(arrayList);
                    else return new ArrayList<>();
                } catch (Exception e) {
                }
                break;

            default:
                // we're neither in OTG not in SMB, load the list based on root/general filesystem
                try {
                    ArrayList<BaseFile> arrayList1;
                    arrayList1 = RootHelper.getFilesList(path, BaseActivity.rootMode, false,
                            new RootHelper.GetModeCallBack() {
                                @Override
                                public void getMode(OpenMode mode) {
                                    openmode = mode;
                                }
                            });
                    list = addToSubfolder(arrayList1);

                } catch (RootNotPermittedException e) {
                    AppConfig.toast(mContext, mContext.getString(R.string.rootfailure));
                    return null;
                }
                break;
        }


        return list;
    }

    private JSONObject stringContentToJson(String content) {
        JSONObject result = new JSONObject();

        try {

            result.put("result", content);

        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return result;
    }

    private JSONObject listDirToJson(List<String> dirs) {
        JSONObject result = new JSONObject();
        JSONArray data = new JSONArray();
        for (String dir : dirs) {
            try {

                JSONObject jsonDir = new JSONObject();
                jsonDir.put("name", dir);
                jsonDir.put("rights", "drwxr-xr-x");
                jsonDir.put("size", "0");
                jsonDir.put("date", "2016-03-03 15:31:40");
                jsonDir.put("type", "dir");

                data.put(jsonDir);

            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        try {

            result.put("result", data);

        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return result;
    }

    private JSONObject listLayoutelementsToJson(ArrayList<Layoutelements> dirs) {
        JSONObject result = new JSONObject();
        JSONArray data = new JSONArray();
        for (Layoutelements dir : dirs) {
            try {

                JSONObject jsonDir = new JSONObject();
                jsonDir.put("name", dir.getDesc().replace(mPath + "/", ""));

                String rights = dir.getPermissions();
                String pa = "---------";
                if (rights.length() < 9) {
                    rights = rights + pa.substring(0, 9 - rights.length());
                }
                jsonDir.put("rights", "-" + rights);
                jsonDir.put("size", String.valueOf(dir.getlongSize()));
                Date d = new Date(dir.getDate1());
                jsonDir.put("date", dateFormat.format(d));
                jsonDir.put("type", dir.isDirectory() ? "dir" : "file");

                data.put(jsonDir);

            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        try {

            result.put("result", data);

        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return result;
    }

    private JSONObject listLayoutelementsToJsonStorage(ArrayList<Layoutelements> dirs) {
        JSONObject result = new JSONObject();
        JSONArray data = new JSONArray();
        for (Layoutelements dir : dirs) {
            try {

                JSONObject jsonDir = new JSONObject();
                jsonDir.put("name", dir.getDesc().replaceAll("/storage/emulated/legacy/|/storage/emulated/0/", ""));

                String rights = dir.getPermissions();
                String pa = "---------";
                if (rights.length() < 9) {
                    rights = rights + pa.substring(0, 9 - rights.length());
                }
                jsonDir.put("rights", "-" + rights);
                jsonDir.put("size", String.valueOf(dir.getlongSize()));
                Date d = new Date(dir.getDate1());
                jsonDir.put("date", dateFormat.format(d));
                jsonDir.put("type", dir.isDirectory() ? "dir" : "file");

                data.put(jsonDir);

            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        try {

            result.put("result", data);

        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return result;
    }

    public class SearchTask extends AsyncTask<String, BaseFile, Void> {

        @Override
        protected void onPreExecute() {

            /*
            * Note that we need to check if the callbacks are null in each
            * method in case they are invoked after the Activity's and
            * Fragment's onDestroy() method have been called.
             */
            if (mCallbacks != null) {

                mCallbacks.onPreExecute();
            }
        }

        // mCallbacks not checked for null because of possibility of
        // race conditions b/w worker thread main thread
        @Override
        protected Void doInBackground(String... params) {
            Log.d(TAG, "doInBackground");
            String path = params[0];

            Log.d(TAG, path);

            HFile file = new HFile(mOpenMode, path);
            file.generateMode(null);
            if (file.isSmb()) return null;

            // level 1
            // if regex or not
            if (!isRegexEnabled) {
                search(file, mInput);
            } else {

                // compile the regular expression in the input
                Pattern pattern = Pattern.compile(bashRegexToJava(mInput));
                // level 2
                if (!isMatchesEnabled) {
                    searchRegExFind(file, pattern);
                } else {
                    searchRegExMatch(file, pattern);
                }
            }
            return null;
        }

        @Override
        public void onPostExecute(Void c) {
            mCallbacks.onPostExecute();

        }

        @Override
        protected void onCancelled() {
            onCancelled();
        }

        @Override
        public void onProgressUpdate(BaseFile... val) {
            Log.d(TAG, "isCancelled:" + isCancelled());
            if (!isCancelled()) {
                onProgressUpdate(val[0]);
            }
        }

        /**
         * Recursively search for occurrences of a given text in file names and publish the result
         *
         * @param file  the current path
         * @param query the searched text
         */
        private void search(HFile file, String query) {

            if (file.isDirectory()) {
                ArrayList<BaseFile> f = file.listFiles(mRootMode);
                // do you have permission to read this directory?
                if (!isCancelled())
                    for (BaseFile x : f) {
                        if (!isCancelled()) {
                            if (x.isDirectory()) {
                                if (x.getName().toLowerCase()
                                        .contains(query.toLowerCase())) {
                                    publishProgress(x);
                                }
                                if (!isCancelled()) search(x, query);

                            } else {
                                if (x.getName().toLowerCase()
                                        .contains(query.toLowerCase())) {
                                    publishProgress(x);
                                }
                            }
                        } else return;
                    }
                else return;
            } else {
                System.out
                        .println(file.getPath() + "Permission Denied");
            }
        }

        /**
         * Recursively find a java regex pattern {@link Pattern} in the file names and publish the result
         *
         * @param file    the current file
         * @param pattern the compiled java regex
         */
        private void searchRegExFind(HFile file, Pattern pattern) {

            if (file.isDirectory()) {
                ArrayList<BaseFile> f = file.listFiles(mRootMode);

                if (!isCancelled())
                    for (BaseFile x : f) {
                        if (!isCancelled()) {
                            if (x.isDirectory()) {
                                if (pattern.matcher(x.getName()).find()) publishProgress(x);
                                if (!isCancelled()) searchRegExFind(x, pattern);

                            } else {
                                if (pattern.matcher(x.getName()).find()) {
                                    publishProgress(x);
                                }
                            }
                        } else return;
                    }
                else return;
            } else {
                System.out
                        .println(file.getPath() + "Permission Denied");
            }
        }

        /**
         * Recursively match a java regex pattern {@link Pattern} with the file names and publish the result
         *
         * @param file    the current file
         * @param pattern the compiled java regex
         */
        private void searchRegExMatch(HFile file, Pattern pattern) {

            if (file.isDirectory()) {
                ArrayList<BaseFile> f = file.listFiles(mRootMode);

                if (!isCancelled())
                    for (BaseFile x : f) {
                        if (!isCancelled()) {
                            if (x.isDirectory()) {
                                if (pattern.matcher(x.getName()).matches()) publishProgress(x);
                                if (!isCancelled()) searchRegExMatch(x, pattern);

                            } else {
                                if (pattern.matcher(x.getName()).matches()) {
                                    publishProgress(x);
                                }
                            }
                        } else return;
                    }
                else return;
            } else {
                System.out
                        .println(file.getPath() + "Permission Denied");
            }
        }

        /**
         * method converts bash style regular expression to java. See {@link Pattern}
         *
         * @param originalString
         * @return converted string
         */
        private String bashRegexToJava(String originalString) {
            StringBuilder stringBuilder = new StringBuilder();

            for (int i = 0; i < originalString.length(); i++) {
                switch (originalString.charAt(i) + "") {
                    case "*":
                        stringBuilder.append("\\w*");
                        break;
                    case "?":
                        stringBuilder.append("\\w");
                        break;
                    default:
                        stringBuilder.append(originalString.charAt(i));
                        break;
                }
            }

            Log.d(getClass().getSimpleName(), stringBuilder.toString());
            return stringBuilder.toString();
        }
    }

    public void onPreExecute() {
        Log.d(TAG, "onPreExecute search");
    }


    public void onPostExecute() {
        Log.d(TAG, "onPostExecute search complete");
    }


    public void onProgressUpdate(BaseFile val) {
        Log.d(TAG, "onProgressUpdate search");
        addTo(val);
    }


    public void onCancelled() {

        Log.d(TAG, "onCancelled search");
    }

    // method to add search result entry to the LIST_ELEMENT arrayList
    private void addTo(BaseFile mFile) {
        File f = new File(mFile.getPath());
        String size = "";
        String type = "";
        if (!DataUtils.hiddenfiles.contains(mFile.getPath())) {
            if (mFile.isDirectory()) {
                size = "";
                type = "dir";
                try {
                    JSONObject jsonDir = new JSONObject();
                    jsonDir.put("name", f.getPath());
                    jsonDir.put("rights", mFile.getPermisson());
                    jsonDir.put("size", size);
                    jsonDir.put("date", mFile.getDate() + "");
                    jsonDir.put("type", type);
                    LIST_ELEMENTS.put(jsonDir);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                }

            } else {
                type = "file";
                long longSize = 0;
                try {
                    if (mFile.getSize() != -1) {
                        longSize = Long.valueOf(mFile.getSize());
                        size = Formatter.formatFileSize(mContext, longSize);
                    } else {
                        size = "";
                        longSize = 0;
                    }

                } catch (NumberFormatException e) {
                    Log.e(TAG, e.getMessage(), e);
                }

            }
        }

        Log.d(TAG, LIST_ELEMENTS.toString());

    }

    public Layoutelements newElement(BitmapDrawable i, String d, String permissions, String symlink, String size, long longSize, boolean directorybool, boolean b, String date) {
        Layoutelements item = new Layoutelements(i, new File(d).getName(), d, permissions, symlink, size, longSize, b, date, directorybool);
        return item;
    }

    private ArrayList<Layoutelements> addToSubfolder(ArrayList<BaseFile> mFile) {
        ArrayList<Layoutelements> a = new ArrayList<Layoutelements>();
        for (int i = 0; i < mFile.size(); i++) {
            BaseFile ele = mFile.get(i);
            File f = new File(ele.getPath());
            Log.d(TAG, "================");
            Log.d(TAG, ele.getPath());
            Log.d(TAG, f.getAbsolutePath());
            Log.d(TAG, f.getPath());

            String size = "";
            if (!DataUtils.hiddenfiles.contains(ele.getPath())) {
                if (ele.isDirectory()) {
                    size = "";
                    Layoutelements layoutelements = utilsProvider.getFutils().newElement(null,
                            f.getPath(), ele.getPermisson(), ele.getLink(), size, 0, true, false,
                            ele.getDate() + "");
                    layoutelements.setMode(ele.getMode());
                    a.add(layoutelements);
                } else {
                    long longSize = 0;
                    try {
                        if (ele.getSize() != -1) {
                            longSize = Long.valueOf(ele.getSize());
                            size = Formatter.formatFileSize(mContext, longSize);
                        } else {
                            size = "";
                            longSize = 0;
                        }
                    } catch (NumberFormatException e) {
                        //e.printStackTrace();
                    }
                    try {
                        Layoutelements layoutelements = utilsProvider.getFutils().newElement(null, f.getPath(), ele.getPermisson(),
                                ele.getLink(), size, longSize, false, false, ele.getDate() + "");
                        layoutelements.setMode(ele.getMode());
                        a.add(layoutelements);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return a;
    }

    ArrayList<BaseFile> listaudio() {
        String currentPath = mPath;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String[] projection = {
                MediaStore.Audio.Media.DATA
        };

        Cursor cursor = mContext.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);

        ArrayList<BaseFile> songs = new ArrayList<>();
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                BaseFile strings = RootHelper.generateBaseFile(new File(path), false);
                if (strings != null) songs.add(strings);
            } while (cursor.moveToNext() && mPath.equals(currentPath));
        }
        cursor.close();
        return songs;
    }

    ArrayList<BaseFile> listImages() {
        String currentPath = mPath;
        ArrayList<BaseFile> songs = new ArrayList<>();
        final String[] projection = {MediaStore.Images.Media.DATA};
        final Cursor cursor = mContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null);
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                BaseFile strings = RootHelper.generateBaseFile(new File(path), false);
                if (strings != null) songs.add(strings);
            } while (cursor.moveToNext() && mPath.equals(currentPath));
        }
        cursor.close();
        return songs;
    }

    ArrayList<BaseFile> listVideos() {
        String currentPath = mPath;
        ArrayList<BaseFile> songs = new ArrayList<>();
        final String[] projection = {MediaStore.Images.Media.DATA};
        final Cursor cursor = mContext.getContentResolver().query(MediaStore.Video.Media
                        .EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null);
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                BaseFile strings = RootHelper.generateBaseFile(new File(path), false);
                if (strings != null) songs.add(strings);
            } while (cursor.moveToNext() && mPath.equals(currentPath));
        }
        cursor.close();
        return songs;
    }


    ArrayList<BaseFile> listDocs() {
        String currentPath = mPath;
        ArrayList<BaseFile> songs = new ArrayList<>();
        final String[] projection = {MediaStore.Files.FileColumns.DATA};
        Cursor cursor = mContext.getContentResolver().query(MediaStore.Files
                        .getContentUri("external"), projection,
                null,
                null, null);
        String[] types = new String[]{".pdf", ".xml", ".html", ".asm", ".text/x-asm", ".def", ".in", ".rc",
                ".list", ".log", ".pl", ".prop", ".properties", ".rc",
                ".doc", ".docx", ".msg", ".odt", ".pages", ".rtf", ".txt", ".wpd", ".wps"};
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                if (path != null && contains(types, path)) {
                    BaseFile strings = RootHelper.generateBaseFile(new File(path), false);
                    if (strings != null) songs.add(strings);
                }
            } while (cursor.moveToNext() && mPath.equals(currentPath));
        }
        cursor.close();
        return songs;
    }


    boolean contains(String[] types, String path) {
        for (String string : types) {
            if (path.endsWith(string)) return true;
        }
        return false;
    }


    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isPasswordProtected() {
        return isPasswordProtected;
    }

    public void setPasswordProtected(boolean passwordProtected) {
        isPasswordProtected = passwordProtected;
    }


    @Override
    public void stop() {
        super.stop();

        //reset session
        sessions = new  HashMap<>();
    }
}