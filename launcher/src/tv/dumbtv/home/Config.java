package tv.dumbtv.home;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * All settings live in one plain {@code key=value} file in the app's external files dir,
 * so {@code adb pull}/{@code adb push} is the whole export/import story and a hand edit is
 * picked up on the next return to the home screen.
 */
final class Config {
    static final String FILE = "home.conf";

    static final class Choice {
        final String key;
        final int title;
        final String[] values;
        final int[] names;
        final String fallback;

        Choice(String key, int title, String fallback, String[] values, int[] names) {
            this.key = key;
            this.title = title;
            this.fallback = fallback;
            this.values = values;
            this.names = names;
        }

        String name(Context context, String value) {
            int i = Arrays.asList(values).indexOf(value);
            return i < 0 ? value : context.getString(names[i]);
        }
    }

    static final Choice ACCENT = new Choice("accent", R.string.accent_color, "white",
            new String[] {"white", "blue", "cyan", "green", "amber", "red", "pink", "violet"},
            new int[] {R.string.color_white, R.string.color_blue, R.string.color_cyan,
                    R.string.color_green, R.string.color_amber, R.string.color_red,
                    R.string.color_pink, R.string.color_violet});
    static final Choice TILES = new Choice("tiles", R.string.tile_size, "large",
            new String[] {"small", "medium", "large"},
            new int[] {R.string.small, R.string.medium, R.string.large});
    static final Choice CLOCK = new Choice("clock", R.string.clock, "12h",
            new String[] {"auto", "24h", "12h"},
            new int[] {R.string.system, R.string.hour_24, R.string.hour_12});
    static final Choice INPUTS = new Choice("inputs", R.string.inputs_row, "on",
            new String[] {"on", "off"},
            new int[] {R.string.shown, R.string.hidden});
    static final Choice SWITCH = new Choice("switch", R.string.open_inputs_with, "auto",
            new String[] {"auto", "tv", "home"},
            new int[] {R.string.automatic, R.string.tv_app, R.string.dumbtv_home});
    static final Choice BACKGROUND = new Choice("background", R.string.background, "image",
            new String[] {"black", "graphite", "midnight", "image", "adaptive", "adaptive-tint"},
            new int[] {R.string.black, R.string.graphite, R.string.midnight,
                    R.string.wallpaper, R.string.adaptive_solid, R.string.adaptive_wallpaper});
    static final Choice WALLPAPER = new Choice("wallpaper", R.string.wallpaper, "aurora",
            new String[] {"aurora", "alpine", "coast", "custom"},
            new int[] {R.string.aurora_bay, R.string.alpine_aurora,
                    R.string.golden_coast, R.string.my_photos});
    static final Choice MOTION = new Choice("motion", R.string.wallpaper_motion, "still",
            new String[] {"still", "live"},
            new int[] {R.string.still, R.string.live_slow_pan});
    static final Choice DIM = new Choice("dim", R.string.image_dim, "20",
            new String[] {"0", "20", "40", "60", "80"},
            new int[] {R.string.dim_0, R.string.dim_20, R.string.dim_40,
                    R.string.dim_60, R.string.dim_80});

    static final Choice ROTATE = new Choice("rotate", R.string.wallpaper_rotation, "off",
            new String[] {"off", "visit", "hourly", "daily", "daytime"},
            new int[] {R.string.off, R.string.every_return_home, R.string.every_hour,
                    R.string.every_day, R.string.by_time_of_day});

    static final Choice[] CHOICES = {ACCENT, TILES, INPUTS, SWITCH, BACKGROUND, WALLPAPER,
            MOTION, DIM, ROTATE};

    /** Folder of wallpapers, next to home.conf, that Background › Image file rotates through. */
    static final String WALLPAPERS = "wallpapers";
    private static final String OFFSET = "wallpaper.offset";

    private static final int[] ACCENTS = {
        0xFFFFFFFF, 0xFF3D8BFF, 0xFF2BC4E8, 0xFF34C759, 0xFFFFB340, 0xFFFF5147, 0xFFFF5C9D, 0xFFA07CFF,
    };
    private static final int[] BACKGROUNDS = {0xFF000000, 0xFF141518, 0xFF0B1220, 0xFF000000, 0xFF141518, 0xFF141518};

    private static Config instance;

    final File dir;
    private final File file;
    private final Map<String, String> values = new LinkedHashMap<>();
    private long signature = Long.MIN_VALUE;
    private int version;
    private long visits;

    static Config get(Context c) {
        if (instance == null) instance = new Config(c.getApplicationContext());
        return instance;
    }

    private Config(Context c) {
        File external = c.getExternalFilesDir(null);
        dir = external != null ? external : c.getFilesDir();
        file = new File(dir, FILE);
        refresh();
    }

    /** Changes every time a setting changes, from the UI or from the file on disk. */
    int version() {
        return version;
    }

    void refresh() {
        long sig = file.lastModified() * 31 + file.length();
        if (sig == signature) return;
        signature = sig;
        values.clear();
        if (file.isFile()) read();
        version++;
    }

    private void read() {
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            for (String line; (line = r.readLine()) != null; ) {
                line = line.trim();
                int eq = line.indexOf('=');
                if (line.startsWith("#") || eq <= 0) continue;
                values.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        } catch (IOException e) {
            Log.w("dumbtv", "could not read " + file, e);
        }
    }

    private void save() {
        File tmp = new File(dir, FILE + ".tmp");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
            w.write("# dumbtv home. Edit on the TV (Options) or here; unknown keys are kept.\n");
            for (Map.Entry<String, String> e : values.entrySet()) {
                w.write(e.getKey() + "=" + e.getValue() + "\n");
            }
        } catch (IOException e) {
            Log.w("dumbtv", "could not write " + tmp, e);
            return;
        }
        if (!tmp.renameTo(file)) Log.w("dumbtv", "could not replace " + file);
        signature = file.lastModified() * 31 + file.length();
    }

    String get(String key, String fallback) {
        String v = values.get(key);
        return v == null || v.isEmpty() ? fallback : v;
    }

    String get(Choice c) {
        String v = values.get(c.key);
        return v != null && Arrays.asList(c.values).contains(v) ? v : c.fallback;
    }

    void set(String key, String value) {
        if (value == null || value.isEmpty()) values.remove(key);
        else values.put(key, value);
        version++;
        save();
    }

    List<String> list(String key) {
        List<String> out = new ArrayList<>();
        for (String s : get(key, "").split(",")) {
            if (!s.trim().isEmpty()) out.add(s.trim());
        }
        return out;
    }

    void setList(String key, List<String> items) {
        // String.join is Android 8+; TextUtils.join keeps Fire OS 6 (Android 7.1) working.
        set(key, TextUtils.join(",", items));
    }

    void removePrefix(String... prefixes) {
        for (Iterator<String> it = values.keySet().iterator(); it.hasNext(); ) {
            String k = it.next();
            for (String p : prefixes) {
                if (k.startsWith(p)) {
                    it.remove();
                    break;
                }
            }
        }
        version++;
        save();
    }

    int accent() {
        String v = values.get(ACCENT.key);
        if (v != null && v.startsWith("#")) {
            try {
                return Color.parseColor(v) | 0xFF000000;
            } catch (IllegalArgumentException ignored) {
                // falls through to the named palette
            }
        }
        return ACCENTS[Arrays.asList(ACCENT.values).indexOf(get(ACCENT))];
    }

    int background() {
        return BACKGROUNDS[Arrays.asList(BACKGROUND.values).indexOf(get(BACKGROUND))];
    }

    /** Counts returns to the home screen, for the "visit" rotation. */
    void visited() {
        visits++;
    }

    /** Steps the rotation forward by one, so the next wallpaper shows now. */
    void nextWallpaper() {
        set(OFFSET, String.valueOf(offset() + 1));
    }

    private int offset() {
        try {
            return Integer.parseInt(get(OFFSET, "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 0 morning (6-13 h), 1 afternoon (13-20 h), 2 night. */
    static int period() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return h >= 6 && h < 13 ? 0 : h >= 13 && h < 20 ? 1 : 2;
    }

    private static final String[][] PERIOD_WORDS = {
        {"morning", "day"},
        {"afternoon", "evening"},
        {"night", "midnight"},
    };

    /**
     * Files whose name carries a word for {@code period}; empty when others are tagged but not
     * this one, null when no file is tagged at all (then the period just indexes the pool).
     */
    private static List<File> forPeriod(List<File> pool, int period) {
        List<File> fit = new ArrayList<>();
        boolean tagged = false;
        for (File f : pool) {
            // Whole words only, so "sunday.jpg" or "holiday.jpg" is not a morning picture.
            List<String> words = Arrays.asList(f.getName().toLowerCase(Locale.ROOT).split("[^a-z]+"));
            for (int p = 0; p < PERIOD_WORDS.length; p++) {
                for (String w : PERIOD_WORDS[p]) {
                    if (words.contains(w)) {
                        tagged = true;
                        if (p == period && !fit.contains(f)) fit.add(f);
                    }
                }
            }
        }
        return tagged ? fit : null;
    }

    List<File> wallpapers() {
        List<File> out = new ArrayList<>();
        File[] files = new File(dir, WALLPAPERS).listFiles();
        if (files == null) return out;
        for (File f : files) {
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (f.isFile() && (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")
                    || n.endsWith(".webp"))) {
                out.add(f);
            }
        }
        Collections.sort(out);
        return out;
    }

    File wallpaper() {
        List<File> pool = wallpapers();
        if (!pool.isEmpty()) {
            long now = System.currentTimeMillis();
            long step;
            String mode = get(ROTATE);
            if ("daytime".equals(mode)) {
                int period = period();
                List<File> fit = forPeriod(pool, period);
                if (fit == null) return pool.get(period % pool.size());
                if (!fit.isEmpty()) pool = fit;
                mode = "visit";
            }
            switch (mode) {
                case "visit": step = visits; break;
                case "hourly": step = now / 3_600_000L; break;
                case "daily": step = (now + TimeZone.getDefault().getOffset(now)) / 86_400_000L; break;
                default: step = 0;
            }
            int n = pool.size();
            return pool.get((int) (((step + offset()) % n + n) % n));
        }
        for (String name : new String[] {"wallpaper.jpg", "wallpaper.jpeg", "wallpaper.png", "wallpaper.webp"}) {
            File f = new File(dir, name);
            if (f.isFile()) return f;
        }
        return null;
    }
}
