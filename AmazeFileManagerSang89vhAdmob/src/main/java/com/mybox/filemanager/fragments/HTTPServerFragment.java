package com.mybox.filemanager.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mybox.filemanager.R;
import com.mybox.filemanager.activities.MainActivity;
import com.mybox.filemanager.services.httpservice.HTTPService;
import com.mybox.filemanager.utils.theme.AppTheme;

/**
 * Created by sang89vh@gmail.com on 04-03-2017.
 */
public class HTTPServerFragment extends Fragment {
    private String TAG ="HTTPServerFragment";
    TextView statusText, warningText, httpAddrText, passwordText;
    Button httpBtn;
    private MainActivity mainActivity;
    private View rootView;
    private SharedPreferences preferences;
    private Switch isActivePasswordSwich;

    private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                warningText.setText("");
            } else {
                stopServer();
                statusText.setText(getResources().getString(R.string.http_status_not_running));
                warningText.setText(getResources().getString(R.string.http_no_wifi));
                httpAddrText.setText("");
                httpBtn.setText(getResources().getString(R.string.start_http));
            }
        }
    };
    private BroadcastReceiver httpReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == HTTPService.ACTION_STARTED) {
                statusText.setText(getResources().getString(R.string.http_status_running));
                warningText.setText("");
                httpAddrText.setText(getHTTPAddressString());
                httpBtn.setText(getResources().getString(R.string.stop_http));
            } else if (action == HTTPService.ACTION_FAILEDTOSTART) {
                statusText.setText(getResources().getString(R.string.http_status_not_running));
                warningText.setText("Oops! Something went wrong");
                httpAddrText.setText("");
                httpBtn.setText(getResources().getString(R.string.start_http));
            } else if (action == HTTPService.ACTION_STOPPED) {
                statusText.setText(getResources().getString(R.string.http_status_not_running));
                httpAddrText.setText("");
                httpBtn.setText(getResources().getString(R.string.start_http));
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mainActivity = (MainActivity) getActivity();

        preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        switch (item.getItemId()) {
            case R.id.choose_http_port:
                int currentFtpPort = HTTPService.getDefaultPortFromPreferences(preferences);

                new MaterialDialog.Builder(getActivity())
                        .input(getString(R.string.http_port_edit_menu_title), Integer.toString(currentFtpPort), true, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

                            }
                        })
                        .inputType(InputType.TYPE_CLASS_NUMBER)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                EditText editText = dialog.getInputEditText();
                                if (editText != null) {
                                    String name = editText.getText().toString();

                                    int portNumber = Integer.parseInt(name);
                                    if (portNumber < 1024) {
                                        Toast.makeText(getActivity(), R.string.http_port_change_error_invalid, Toast.LENGTH_SHORT)
                                             .show();
                                    } else {
                                        HTTPService.changeHTTPServerPort(preferences, portNumber);
                                        Toast.makeText(getActivity(), R.string.http_port_change_success, Toast.LENGTH_SHORT)
                                             .show();
                                    }
                                }
                            }
                        })
                        .positiveText("CHANGE")
                        .negativeText(R.string.cancel)
                        .build()
                        .show();

                return true;

            case R.id.choose_http_password:
                if (!HTTPService.isRunning()) {
                    String currentHttpPassword = getHTTPPassword(preferences);

                    new MaterialDialog.Builder(getActivity())
                            .input(getString(R.string.http_password_edit_menu_title), currentHttpPassword, true, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

                                }
                            })
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    EditText editText = dialog.getInputEditText();
                                    if (editText != null) {
                                        String name = editText.getText().toString();

                                        setHTTPPassword(preferences, name);
                                        Toast.makeText(getActivity(), R.string.http_password_change_success, Toast.LENGTH_SHORT)
                                                .show();


                                    }
                                }
                            })
                            .positiveText("CHANGE")
                            .negativeText(R.string.cancel)
                            .build()
                            .show();
                }else {
                    Toast.makeText(getActivity(), R.string.http_password_change_error, Toast.LENGTH_LONG)
                            .show();
                }
                return true;
        }

        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mainActivity.getMenuInflater().inflate(R.menu.http_server_menu, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_http, container, false);
        statusText =(TextView) rootView.findViewById(R.id.statusText);
        passwordText =(TextView) rootView.findViewById(R.id.passwordText);
        passwordText.setText(getResources().getString(R.string.text_message_password_http) +" "+  getHTTPPassword(preferences));
        isActivePasswordSwich =(Switch) rootView.findViewById(R.id.is_active_password);
        isActivePasswordSwich.setChecked(getActiveHTTPPassword(preferences));
        isActivePasswordSwich.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (!HTTPService.isRunning()) {
                    setActiveHTTPPassword(preferences, isChecked);
                }else{
                    Toast.makeText(getActivity(), R.string.http_password_change_error, Toast.LENGTH_LONG)
                            .show();
                    isActivePasswordSwich.setChecked(getActiveHTTPPassword(preferences));
                }
            }
        });

        warningText = (TextView) rootView.findViewById(R.id.warningText);
        httpAddrText = (TextView) rootView.findViewById(R.id.httpAddressText);
        httpBtn = (Button) rootView.findViewById(R.id.startStopButton);

        ImageView httpImage = (ImageView)rootView.findViewById(R.id.http_image);

        //light theme
        if (mainActivity.getAppTheme().equals(AppTheme.LIGHT)) {
            httpImage.setImageResource(R.drawable.ic_http_light);
        } else {
            //dark
            httpImage.setImageResource(R.drawable.ic_http_dark);
        }

        httpBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!HTTPService.isRunning()) {
                    if (HTTPService.isConnectedToWifi(getContext()))
                        startServer();
                    else
                        warningText.setText(getResources().getString(R.string.http_no_wifi));
                } else {
                    stopServer();
                }
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
        mainActivity.setActionBarTitle(getResources().getString(R.string.http));
        mainActivity.floatingActionButton.hideMenuButton(true);
        mainActivity.buttonBarFrame.setVisibility(View.GONE);
        mainActivity.supportInvalidateOptionsMenu();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Sends a broadcast to start http server
     */
    private void startServer() {
        Intent intent = new Intent(HTTPService.ACTION_START_HTTPSERVER);
        //send password here "password = intent.getStringExtra("password");"
        //intent.putExtra("password",getHTTPPassword(preferences));
        //intent.putExtra("isPasswordProtected",getActiveHTTPPassword(preferences));
        getContext().sendBroadcast(intent);
    }

    /**
     * Sends a broadcast to stop http server
     */
    private void stopServer() {
        getContext().sendBroadcast(new Intent(HTTPService.ACTION_STOP_HTTPSERVER));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStatus();
        IntentFilter wifiFilter = new IntentFilter();
        wifiFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        getContext().registerReceiver(mWifiReceiver, wifiFilter);
        IntentFilter httpFilter = new IntentFilter();
        httpFilter.addAction(HTTPService.ACTION_STARTED);
        httpFilter.addAction(HTTPService.ACTION_STOPPED);
        httpFilter.addAction(HTTPService.ACTION_CHANGE_PASSWORD);
        httpFilter.addAction(HTTPService.ACTION_FAILEDTOSTART);
        getContext().registerReceiver(httpReceiver, httpFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getContext().unregisterReceiver(mWifiReceiver);
        getContext().unregisterReceiver(httpReceiver);
    }

    /**
     * Update UI widgets based on connection status
     */
    private void updateStatus() {
        if (HTTPService.isRunning()) {
            statusText.setText(getResources().getString(R.string.http_status_running));
            httpBtn.setText(getResources().getString(R.string.stop_http));
            httpAddrText.setText(getHTTPAddressString());
            isActivePasswordSwich.setChecked(getActiveHTTPPassword(preferences));
            passwordText.setText(getResources().getString(R.string.text_message_password_http) +" "+  getHTTPPassword(preferences));
        } else {
            statusText.setText(getResources().getString(R.string.http_status_not_running));
            httpBtn.setText(getResources().getString(R.string.start_http));
        }
    }
    private String PASSWORD_PREFERENCE_KEY ="HTTP_PASSWORD";
    private  String getHTTPPassword(SharedPreferences preferences) {
        try {
            return preferences.getString(PASSWORD_PREFERENCE_KEY, getResources().getString(R.string.http_default_password));
        } catch (ClassCastException ex) {
            Log.e(TAG, "Default password preference is not an string. Resetting to default.");
            setHTTPPassword(preferences, getResources().getString(R.string.http_default_password));
            return getResources().getString(R.string.http_default_password);
        }
    }

    private void setHTTPPassword(SharedPreferences preferences, String password){
        preferences.edit()
                .putString(PASSWORD_PREFERENCE_KEY, password)
                .apply();

        passwordText.setText(getResources().getString(R.string.text_message_password_http) +" "+  getHTTPPassword(preferences));

        Intent intent = new Intent(HTTPService.ACTION_CHANGE_PASSWORD);
        //send password here "password = intent.getStringExtra("password");"
        intent.putExtra("password",getHTTPPassword(preferences));
        intent.putExtra("isPasswordProtected",getActiveHTTPPassword(preferences));
        getContext().sendBroadcast(intent);

    }

    private String ACTIVE_PASSWORD_PREFERENCE_KEY ="ACTIVE_HTTP_PASSWORD";
    private  Boolean getActiveHTTPPassword(SharedPreferences preferences) {
        try {
            return preferences.getBoolean(ACTIVE_PASSWORD_PREFERENCE_KEY, false);
        } catch (ClassCastException ex) {
            Log.e(TAG, "Default active password preference is not an string. Resetting to default.");
            setActiveHTTPPassword(preferences, false);
            return false;
        }
    }

    private void setActiveHTTPPassword(SharedPreferences preferences, boolean isActivePassword){


        preferences.edit()
                .putBoolean(ACTIVE_PASSWORD_PREFERENCE_KEY, isActivePassword)
                .apply();


    }
    /**
     * @return address at which server is running
     */
    private String getHTTPAddressString() {
        return "http://" + HTTPService.getLocalInetAddress(getContext()).getHostAddress() + ":" + HTTPService.getPort();
    }
}
