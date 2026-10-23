package tv.dumbtv.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.widget.TextView;

final class Ui {
    static final int TEXT = 0xFFF2F2F3;
    static final int MUTED = 0x99FFFFFF;
    static final int FAINT = 0x66FFFFFF;
    static final int SURFACE = 0xD91A1D24;
    static final int DIALOG = android.R.style.Theme_DeviceDefault_Dialog_Alert;
    static final Typeface LIGHT = Typeface.create("sans-serif-light", Typeface.NORMAL);
    static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);

    private Ui() {}

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** Black on light accents, white on dark ones. */
    static int ink(int color) {
        return Color.luminance(color) > 0.5f ? 0xFF000000 : 0xFFFFFFFF;
    }

    static TextView text(Context c, float sp, int color, Typeface face) {
        TextView v = new TextView(c);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTextColor(color);
        v.setTypeface(face);
        v.setIncludeFontPadding(false);
        return v;
    }

    static GradientDrawable round(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    /** Translucent wallpaper glass with the warm hairline used by both home-screen docks. */
    static GradientDrawable glass(Context c) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {0xA64A536A, 0x9430394F, 0xA64A4055});
        d.setCornerRadius(dp(c, 28));
        d.setStroke(dp(c, 1), 0xD9FF806F);
        return d;
    }

    /** Accent fill when focused, a faint fill otherwise. */
    static void focusStyle(TextView v, int accent, int idle, float radius) {
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[] {android.R.attr.state_focused}, round(accent, radius));
        bg.addState(new int[0], round(idle, radius));
        v.setBackground(bg);
        v.setTextColor(new ColorStateList(
                new int[][] {{android.R.attr.state_focused}, {}},
                new int[] {ink(accent), 0xD9FFFFFF}));
    }
}
