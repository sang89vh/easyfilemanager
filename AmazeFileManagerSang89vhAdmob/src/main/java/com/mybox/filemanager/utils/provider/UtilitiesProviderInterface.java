package com.mybox.filemanager.utils.provider;

import com.mybox.filemanager.utils.Futils;
import com.mybox.filemanager.utils.color.ColorPreference;
import com.mybox.filemanager.utils.theme.AppTheme;
import com.mybox.filemanager.utils.theme.AppThemeManagerInterface;

/**
 * Created by Rémi Piotaix <remi.piotaix@gmail.com> on 2016-10-17.
 */
public interface UtilitiesProviderInterface {
    Futils getFutils();

    ColorPreference getColorPreference();

    AppTheme getAppTheme();

    AppThemeManagerInterface getThemeManager();
}
