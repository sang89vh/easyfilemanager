package com.mybox.filemanagerpro.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.mybox.filemanagerpro.activities.MainActivity;
import com.mybox.filemanagerpro.utils.theme.AppTheme;

/**
 * Created by vishal on 18/1/17.
 *
 * Class sets text color based on current theme, without explicit method call in app lifecycle
 */

public class ThemedTextView extends TextView {

    public ThemedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (((MainActivity) context).getAppTheme().equals(AppTheme.LIGHT)) {
            setTextColor(getResources().getColor(android.R.color.black));
        } else if (((MainActivity) context).getAppTheme().equals(AppTheme.DARK)) {
            setTextColor(getResources().getColor(android.R.color.white));
        }
    }
}
