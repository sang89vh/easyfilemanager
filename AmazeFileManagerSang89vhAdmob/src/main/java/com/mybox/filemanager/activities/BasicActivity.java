package com.mybox.filemanager.activities;

import android.support.v7.app.AppCompatActivity;

import com.mybox.filemanager.utils.AppConfig;
import com.mybox.filemanager.utils.Futils;
import com.mybox.filemanager.utils.color.ColorPreference;
import com.mybox.filemanager.utils.provider.UtilitiesProviderInterface;
import com.mybox.filemanager.utils.theme.AppTheme;
import com.mybox.filemanager.utils.theme.AppThemeManagerInterface;

/**
 * Created by rpiotaix on 17/10/16.
 */
public class BasicActivity extends AppCompatActivity implements UtilitiesProviderInterface {
    private boolean initialized = false;
    private UtilitiesProviderInterface utilsProvider;

    private void initialize() {
        utilsProvider = getAppConfig().getUtilsProvider();

        initialized = true;
    }

    protected AppConfig getAppConfig() {
        return (AppConfig) getApplication();
    }

    @Override
    public Futils getFutils() {
        if (!initialized)
            initialize();

        return utilsProvider.getFutils();
    }

    public ColorPreference getColorPreference() {
        if (!initialized)
            initialize();

        return utilsProvider.getColorPreference();
    }

    @Override
    public AppTheme getAppTheme() {
        if (!initialized)
            initialize();

        return utilsProvider.getAppTheme();
    }

    @Override
    public AppThemeManagerInterface getThemeManager() {
        if (!initialized)
            initialize();

        return utilsProvider.getThemeManager();

    }
}
