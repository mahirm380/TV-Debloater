package tv.dumbtv.home;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** A useful front end for Fire OS's always-on, permission-protected AirPlay receiver. */
public final class AirPlay extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        String name = Settings.Global.getString(getContentResolver(), "device_name");
        if (name == null || name.trim().isEmpty()) name = "this TV";

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setPadding(Ui.dp(this, 96), Ui.dp(this, 48), Ui.dp(this, 96), Ui.dp(this, 48));
        page.setBackgroundColor(Config.get(this).background());

        TextView title = text("AirPlay is ready", 42, Color.WHITE);
        TextView steps = text("On your iPhone, iPad or Mac:\n\n"
                + "1. Open Control Centre.\n"
                + "2. Choose Screen Mirroring.\n"
                + "3. Select " + name + ".", 25, 0xFFE6E6E6);
        TextView hint = text("Press Back to return to DumbTV", 18, 0xFFAAAAAA);

        page.addView(title, params(0, Ui.dp(this, 36)));
        page.addView(steps, params(1, 0));
        page.addView(hint, params(0, 0));
        setContentView(page);
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);
        view.setLineSpacing(0, 1.18f);
        return view;
    }

    private static LinearLayout.LayoutParams params(int weight, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                weight == 0 ? ViewGroup.LayoutParams.WRAP_CONTENT : 0,
                weight);
        p.bottomMargin = bottom;
        return p;
    }
}
