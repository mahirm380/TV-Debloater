package tv.dumbtv.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** ADB-only configuration bridge. The manifest requires DUMP, which the shell has and apps do not. */
public final class Configure extends BroadcastReceiver {
    static final String ACTION = "tv.dumbtv.home.CONFIGURE";
    private static final Set<String> ALLOWED = new HashSet<>(Arrays.asList(
            "weather", "weather.name", "weather.latitude", "weather.longitude",
            "weather.timezone", "weather.unit",
            "shortcut.netflix", "shortcut.netflix.scan",
            "shortcut.prime", "shortcut.prime.scan",
            "shortcut.freely", "shortcut.freely.scan"));

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION.equals(intent.getAction())) {
            setResultCode(2);
            return;
        }
        String key = intent.getStringExtra("key");
        if (key == null || !ALLOWED.contains(key)) {
            setResultCode(3);
            return;
        }
        String value = intent.getBooleanExtra("clear", false)
                ? null : intent.getStringExtra("value");
        if (value == null && !intent.getBooleanExtra("clear", false)) {
            setResultCode(4);
            return;
        }
        Config.get(context).set(key, value);
        setResultCode(1);
    }
}
