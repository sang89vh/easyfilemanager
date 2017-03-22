package com.mybox.filemanagerpro.utils.provider;

import com.mybox.filemanagerpro.utils.Futils;
import com.mybox.filemanagerpro.utils.color.ColorPreference;
import com.mybox.filemanagerpro.utils.theme.AppTheme;
import com.mybox.filemanagerpro.utils.theme.AppThemeManagerInterface;

/**
 * Created by Rémi Piotaix <remi.piotaix@gmail.com> on 2016-10-17.
 */
public interface UtilitiesProviderInterface {
    Futils getFutils();

    ColorPreference getColorPreference();

    AppTheme getAppTheme();

    AppThemeManagerInterface getThemeManager();
}
