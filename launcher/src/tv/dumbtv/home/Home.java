package tv.dumbtv.home;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class Home extends Activity {
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ExecutorService colorIo = Executors.newSingleThreadExecutor();
    private final List<TextView> chips = new ArrayList<>();
    private Config cfg;
    private TvInputManager tv;
    private int applied = -1, appsGeneration;
    private boolean resumed, landed;
    private FrameLayout root;
    private ImageView wall;
    private View shade;
    private View tint;
    private View heroSpace;
    private TextView clock, date, greeting, weatherTemp, weatherDetail;
    private Row apps, inputs;
    private Tile moving;
    private String wallKey;
    private String backgroundMode;
    private ValueAnimator colorAnimation;
    private ValueAnimator wallpaperAnimation;
    private int displayedColor, targetColor;
    private boolean tintWallpaper;
    private boolean weatherLoading;
    private long weatherLastAttempt;

    private final BroadcastReceiver packages = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            Uri data = i.getData();
            loadApps(data == null ? null : data.getSchemeSpecificPart());
        }
    };

    private final BroadcastReceiver time = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            tick();
        }
    };

    private final TvInputManager.TvInputCallback inputWatch = new TvInputManager.TvInputCallback() {
        @Override
        public void onInputStateChanged(String id, int state) {
            loadInputs();
        }

        @Override
        public void onInputAdded(String id) {
            loadInputs();
        }

        @Override
        public void onInputRemoved(String id) {
            loadInputs();
        }

        @Override
        public void onInputUpdated(String id) {
            loadInputs();
        }
    };

    private final class Row {
        final String kind;
        final TextView focusName;
        final FrameLayout panel;
        final HorizontalScrollView scroll;
        final LinearLayout strip;
        int tileWidth;
        int tileGap;
        int visibleTiles = 1;
        List<Item> all = new ArrayList<>();

        Row(String kind, LinearLayout parent) {
            this.kind = kind;
            boolean appRow = Catalog.APPS.equals(kind);
            int edge = dp(108), inset = dp(14);
            focusName = Ui.text(Home.this, 17, Ui.TEXT, Ui.MEDIUM);
            focusName.setSingleLine(true);
            focusName.setVisibility(appRow ? View.VISIBLE : View.GONE);
            if (appRow) {
                LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(MATCH, dp(28));
                nameLp.leftMargin = edge + dp(20);
                nameLp.rightMargin = edge;
                parent.addView(focusName, nameLp);
            }
            strip = new LinearLayout(Home.this);
            strip.setClipChildren(false);
            strip.setClipToPadding(false);
            strip.setGravity(Gravity.CENTER_HORIZONTAL);
            strip.setMinimumWidth(getResources().getDisplayMetrics().widthPixels - 2 * edge);
            strip.setPadding(inset, appRow ? dp(8) : dp(5), inset, appRow ? dp(8) : dp(5));
            scroll = new HorizontalScrollView(Home.this) {
                /** Moves only in complete tile-width steps, so neither edge shows a clipped card. */
                @Override
                protected int computeScrollDeltaToGetChildRectOnScreen(Rect r) {
                    View focused = getCurrentFocus();
                    if (focused == null || focused.getParent() != strip || tileWidth <= 0) return 0;
                    int index = strip.indexOfChild(focused);
                    int left = focused.getLeft();
                    int right = focused.getRight();
                    int current = getScrollX();
                    int viewport = getWidth();
                    if (left >= current && right <= current + viewport) return 0;
                    int first = right > current + viewport
                            ? index - visibleTiles + 1 : index;
                    first = Math.max(0, Math.min(first,
                            Math.max(0, strip.getChildCount() - visibleTiles)));
                    int range = Math.max(0, strip.getWidth() - viewport);
                    int target = Math.max(0, Math.min(range, first * (tileWidth + tileGap)));
                    return target - current;
                }
            };
            scroll.setFocusable(false);
            scroll.setHorizontalScrollBarEnabled(false);
            scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            scroll.setClipChildren(true);
            scroll.setClipToPadding(true);
            scroll.setPadding(0, 0, 0, 0);
            scroll.addView(strip, new FrameLayout.LayoutParams(WRAP, WRAP));

            panel = new FrameLayout(Home.this);
            // The insets leave room for the focus scale, so clipping here keeps the next card
            // inside the glass instead of drawing through its rounded edge.
            panel.setClipChildren(true);
            panel.setClipToPadding(true);
            panel.setBackground(Ui.glass(Home.this));
            panel.addView(scroll, new FrameLayout.LayoutParams(MATCH, WRAP));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
            lp.leftMargin = edge;
            lp.rightMargin = edge;
            lp.bottomMargin = appRow ? dp(8) : 0;
            parent.addView(panel, lp);
        }

        ViewGroup tiles() {
            return strip;
        }

        void setVisible(boolean on) {
            panel.setVisibility(on ? View.VISIBLE : View.GONE);
            scroll.setVisibility(on ? View.VISIBLE : View.GONE);
            if (Catalog.APPS.equals(kind)) focusName.setVisibility(on ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Future<List<Item>> catalog = io.submit(() -> Catalog.apps(this));
        cfg = Config.get(this);
        tv = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        setContentView(build());

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        registerReceiver(packages, f);
        Keeper.start(this);
        if (tv != null) tv.registerCallback(inputWatch, ui);
        firstApps(catalog);
        loadInputs();
    }

    /**
     * Waits for the app list and loads the art of the tiles that fit on screen, so the first
     * frame is complete. After a cold start that frame is what people see first: loading in the
     * background drew the Inputs row alone, focused HDMI 1 for ~0.3 s, then popped the apps and
     * their banners in.
     */
    private void firstApps(Future<List<Item>> catalog) {
        try {
            apps.all = catalog.get();
        } catch (ExecutionException e) {
            loadApps(null);
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            loadApps(null);
            return;
        }
        int fit = getResources().getDisplayMetrics().widthPixels / (cardSize(true)[0] + dp(18)) + 1;
        for (Item it : Catalog.visible(cfg, Catalog.APPS, apps.all)) {
            if (fit-- <= 0) break;
            Catalog.art(getPackageManager(), it);
            it.artRequested = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        Guard.arrived(this);
        cfg.refresh();
        cfg.visited();
        if (cfg.version() != applied) {
            weatherLastAttempt = 0;
            apply();
        }
        else background();
        IntentFilter f = new IntentFilter(Intent.ACTION_TIME_TICK);
        f.addAction(Intent.ACTION_TIME_CHANGED);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(time, f);
        tick();
        weather();
    }

    @Override
    public void onEnterAnimationComplete() {
        Guard.shown();
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        stopColorAnimation();
        stopWallpaperAnimation();
        targetColor = 0;
        unregisterReceiver(time);
        if (moving != null) stopMoving();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(packages);
        if (tv != null) tv.unregisterCallback(inputWatch);
        io.shutdownNow();
        colorIo.shutdownNow();
        super.onDestroy();
    }

    /**
     * Android 9 keeps a hidden window's GPU context (about 35 MB on this TV) unless memory is
     * critical. Dropping it while an app plays costs one context rebuild on the way back.
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level < TRIM_MEMORY_UI_HIDDEN || level >= TRIM_MEMORY_COMPLETE) return;
        try {
            Class<?> wmg = Class.forName("android.view.WindowManagerGlobal");
            Object global = wmg.getMethod("getInstance").invoke(null);
            wmg.getMethod("trimMemory", int.class).invoke(global, TRIM_MEMORY_COMPLETE);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // not reachable on this build: the context simply stays
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (moving != null) stopMoving();
        if (resumed) focusFirst();
    }

    @Override
    public void onBackPressed() {
        focusFirst();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (moving == null) return super.dispatchKeyEvent(e);
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            switch (e.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    shift(-1);
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    shift(1);
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    break;
                default:
                    stopMoving();
            }
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_MENU) {
            View f = getCurrentFocus();
            if (f instanceof Tile) menu((Tile) f);
            else startActivity(new Intent(this, Options.class));
            return true;
        }
        return super.onKeyDown(code, e);
    }

    /**
     * The scroll position for a focused rect {@code r}: the earliest of the top, the apps row or
     * the inputs row that still shows the row holding {@code r} whole.
     */
    private int rowTarget(ScrollView page, Rect r) {
        View content = page.getChildAt(0);
        List<int[]> rows = rowBlocks(content);
        int[] at = null;
        for (int[] b : rows) {
            if (r.centerY() >= b[0]) at = b;
        }
        if (at == null) return 0;
        int range = Math.max(0, content.getHeight() - page.getHeight());
        return Math.max(0, Math.min(range, stopFor(at, rows, page.getHeight())));
    }

    /** Panel top and bottom of each visible dock, in {@code content}'s coordinates. */
    private List<int[]> rowBlocks(View content) {
        List<int[]> rows = new ArrayList<>();
        for (Row row : new Row[] {apps, inputs}) {
            if (row.scroll.getVisibility() == View.VISIBLE) {
                rows.add(new int[] {topIn(row.panel, content),
                        topIn(row.panel, content) + row.panel.getHeight()});
            }
        }
        return rows;
    }

    /** The earliest of the top or a row title that shows the whole of {@code at}. */
    private static int stopFor(int[] at, List<int[]> rows, int height) {
        if (height >= at[1]) return 0;
        for (int[] b : rows) {
            if (b[0] <= at[0] && b[0] + height >= at[1]) return b[0];
        }
        return at[0];
    }

    /**
     * When the page is taller than the screen, pads its bottom just enough that every row's stop
     * is reachable. Otherwise the last stop is clamped and the clock above it shows half cut.
     */
    private void fitBottom(ScrollView page) {
        View content = page.getChildAt(0);
        if (content == null || page.getHeight() == 0) return;
        int base = dp(32), height = page.getHeight();
        int natural = content.getHeight() - content.getPaddingBottom() + base;
        int want = base;
        if (natural > height) {
            int last = 0;
            List<int[]> rows = rowBlocks(content);
            for (int[] b : rows) last = Math.max(last, stopFor(b, rows, height));
            want = base + Math.max(0, last + height - natural);
        }
        if (content.getPaddingBottom() != want) {
            content.setPadding(content.getPaddingLeft(), content.getPaddingTop(), content.getPaddingRight(), want);
        }
    }

    /** {@code v}'s top edge in {@code content}'s coordinates. */
    private static int topIn(View v, View content) {
        int y = 0;
        while (v != null && v != content) {
            y += v.getTop();
            v = (View) v.getParent();
        }
        return y;
    }

    private View build() {
        root = new FrameLayout(this);
        wall = new ImageView(this);
        wall.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(wall, new FrameLayout.LayoutParams(MATCH, MATCH));
        shade = new View(this);
        root.addView(shade, new FrameLayout.LayoutParams(MATCH, MATCH));
        tint = new View(this);
        root.addView(tint, new FrameLayout.LayoutParams(MATCH, MATCH));

        ScrollView scroll = new ScrollView(this) {
            /** Scrolls in whole rows: the focused row shows complete, and a row starts at the top edge. */
            @Override
            protected int computeScrollDeltaToGetChildRectOnScreen(Rect r) {
                return rowTarget(this, r) - getScrollY();
            }

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                super.onLayout(changed, l, t, r, b);
                fitBottom(this);
            }

            /**
             * Up and down only move the focus; scrolling follows it. Left to ScrollView, a press
             * with nothing focusable that way scrolls half a screen and leaves rows cut.
             */
            @Override
            public boolean executeKeyEvent(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.KEYCODE_DPAD_UP:
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                    case KeyEvent.KEYCODE_PAGE_UP:
                    case KeyEvent.KEYCODE_PAGE_DOWN:
                    case KeyEvent.KEYCODE_SPACE:
                        return false;
                    default:
                        return super.executeKeyEvent(e);
                }
            }
        };
        scroll.setFocusable(false);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setClipChildren(false);
        root.addView(scroll, new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout column = new LinearLayout(this) {
            @Override
            public View focusSearch(View focused, int direction) {
                if (direction == View.FOCUS_DOWN && chips.contains(focused)) {
                    View below = nearest(apps.tiles(), focused);
                    if (below != null) return below;
                }
                return super.focusSearch(focused, direction);
            }
        };
        column.setOrientation(LinearLayout.VERTICAL);
        column.setClipChildren(false);
        column.setClipToPadding(false);
        column.setPadding(0, dp(24), 0, dp(8));
        scroll.addView(column, new FrameLayout.LayoutParams(MATCH, WRAP));

        FrameLayout head = new FrameLayout(this);
        head.setClipChildren(false);
        head.setClipToPadding(false);
        head.setPadding(dp(56), 0, dp(64), 0);

        LinearLayout when = new LinearLayout(this);
        when.setOrientation(LinearLayout.VERTICAL);
        clock = Ui.text(this, 44, Ui.TEXT, Ui.LIGHT);
        when.addView(clock);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.HORIZONTAL);
        details.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        details.setPadding(dp(1), dp(6), 0, 0);
        date = Ui.text(this, 13, Ui.MUTED, Ui.MEDIUM);
        weatherTemp = Ui.text(this, 13, Ui.TEXT, Ui.MEDIUM);
        weatherTemp.setText(" · --°");
        weatherDetail = Ui.text(this, 13, Ui.MUTED, Ui.MEDIUM);
        weatherDetail.setText(" " + Weather.place(cfg));
        details.addView(date);
        details.addView(weatherTemp);
        details.addView(weatherDetail);
        when.addView(details);
        FrameLayout.LayoutParams whenParams = new FrameLayout.LayoutParams(dp(390), WRAP,
                Gravity.LEFT | Gravity.TOP);
        head.addView(when, whenParams);

        greeting = Ui.text(this, 18, Ui.TEXT, Ui.MEDIUM);
        greeting.setGravity(Gravity.LEFT);
        greeting.setPadding(dp(1), dp(9), 0, 0);
        when.addView(greeting);

        LinearLayout actions = new LinearLayout(this);
        actions.setClipChildren(false);
        actions.setClipToPadding(false);
        actions.setGravity(Gravity.RIGHT | Gravity.TOP);
        actions.addView(chip("Options", v -> startActivity(new Intent(this, Options.class))));
        actions.addView(chip("Settings", v -> openSettings()));
        FrameLayout.LayoutParams actionParams = new FrameLayout.LayoutParams(WRAP, WRAP,
                Gravity.RIGHT | Gravity.TOP);
        actionParams.rightMargin = dp(8);
        head.addView(actions, actionParams);
        column.addView(head, new LinearLayout.LayoutParams(MATCH, dp(122)));

        heroSpace = new View(this);
        column.addView(heroSpace, new LinearLayout.LayoutParams(MATCH, dp(201)));
        apps = new Row(Catalog.APPS, column);
        inputs = new Row(Catalog.INPUTS, column);
        return root;
    }

    /**
     * The child of {@code strip} horizontally closest to {@code from}, restricted to its top
     * visual row so focus remains aligned with the compact dock.
     */
    private static View nearest(ViewGroup strip, View from) {
        int n = strip.getChildCount();
        if (n == 0) return null;
        int[] at = new int[2];
        from.getLocationOnScreen(at);
        int x = at[0] + from.getWidth() / 2;
        int[] ys = new int[n];
        int topY = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            strip.getChildAt(i).getLocationOnScreen(at);
            ys[i] = at[1];
            topY = Math.min(topY, at[1]);
        }
        View best = null;
        int bestGap = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            if (ys[i] != topY) continue;
            View c = strip.getChildAt(i);
            c.getLocationOnScreen(at);
            int gap = Math.abs(at[0] + c.getWidth() / 2 - x);
            if (gap < bestGap) {
                bestGap = gap;
                best = c;
            }
        }
        return best;
    }

    private TextView chip(String label, View.OnClickListener click) {
        TextView v = Ui.text(this, 14, Ui.TEXT, Ui.MEDIUM);
        v.setText(label);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(18), dp(9), dp(18), dp(9));
        v.setFocusable(true);
        v.setClickable(true);
        v.setOnClickListener(click);
        v.setOnFocusChangeListener((view, has) -> {
            view.animate().scaleX(has ? 1.06f : 1f).scaleY(has ? 1.06f : 1f).setDuration(110).start();
            if (has) focusBackground(null);
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
        lp.leftMargin = dp(10);
        lp.topMargin = dp(6);
        v.setLayoutParams(lp);
        chips.add(v);
        return v;
    }

    private void apply() {
        applied = cfg.version();
        int accent = cfg.accent();
        for (TextView c : chips) Ui.focusStyle(c, accent, 0x20FFFFFF, dp(20));
        heroSpace.getLayoutParams().height = dp(201);
        heroSpace.requestLayout();
        render(apps);
        render(inputs);
        background();
        tick();
    }

    /** Rebuilds the app list, keeping loaded art for every package except {@code changed}. */
    private void loadApps(String changed) {
        int generation = ++appsGeneration;
        io.execute(() -> {
            List<Item> list = Catalog.apps(this);
            ui.post(() -> {
                if (generation != appsGeneration || isDestroyed()) return;
                for (Item fresh : list) {
                    for (Item old : apps.all) {
                        if (old.id.equals(fresh.id) && !old.id.equals(changed) && old.artRequested) {
                            fresh.banner = old.banner;
                            fresh.icon = old.icon;
                            fresh.artRequested = true;
                            fresh.backgroundColor = old.backgroundColor;
                        }
                    }
                }
                apps.all = list;
                render(apps);
            });
        });
    }

    private void loadInputs() {
        inputs.all = Catalog.inputs(this, tv);
        render(inputs);
    }

    private void render(Row row) {
        if (moving != null && moving.kind.equals(row.kind)) stopMoving();
        View focused = getCurrentFocus();
        String keep = focused instanceof Tile && ((Tile) focused).kind.equals(row.kind)
                ? ((Tile) focused).item.id : null;
        ViewGroup target = row.strip;
        row.strip.removeAllViews();

        boolean enabled = !Catalog.INPUTS.equals(row.kind) || "on".equals(cfg.get(Config.INPUTS));
        List<Item> shown = enabled ? Catalog.visible(cfg, row.kind, row.all) : new ArrayList<>();
        boolean appRow = Catalog.APPS.equals(row.kind);
        int[] size = cardSize(appRow);
        int w = size[0], h = size[1];
        int gap = rowGap(w);
        row.tileWidth = w;
        row.tileGap = gap;
        row.visibleTiles = rowVisibleTiles(w, gap);
        int accent = cfg.accent();
        int index = 0, restore = -1;
        for (Item it : shown) {
            CharSequence label = Catalog.label(cfg, it);
            Tile t = new Tile(this, it, row.kind, label, w, h, accent);
            t.setOnClickListener(v -> open((Tile) v));
            t.setOnFocusChangeListener((v, has) -> {
                Tile tile = (Tile) v;
                if (has) {
                    focusBackground(tile.item);
                    if (Catalog.APPS.equals(tile.kind)) apps.focusName.setText(label);
                } else if (Catalog.APPS.equals(tile.kind)) {
                    apps.focusName.setText("");
                }
            });
            t.setOnLongClickListener(v -> {
                menu((Tile) v);
                return true;
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
            if (index < shown.size() - 1) lp.rightMargin = gap;
            target.addView(t, lp);
            if (it.id.equals(keep)) restore = index;
            index++;
            if (Catalog.APPS.equals(row.kind) && !it.artRequested) loadArt(it);
        }
        row.setVisible(!shown.isEmpty());
        if (restore >= 0) {
            target.getChildAt(restore).requestFocus();
        } else if (keep != null && target.getChildCount() > 0) {
            target.getChildAt(0).requestFocus();
        } else if (getCurrentFocus() == null || !landed) {
            focusFirst();
        }
    }

    private void loadArt(Item it) {
        it.artRequested = true;
        io.execute(() -> {
            Catalog.art(getPackageManager(), it);
            ui.post(() -> {
                ViewGroup tiles = apps.tiles();
                for (int i = 0; i < tiles.getChildCount(); i++) tiles.getChildAt(i).invalidate();
                if (focusedItem() == it) focusBackground(it);
            });
        });
    }

    private Item focusedItem() {
        View focused = getCurrentFocus();
        return focused instanceof Tile ? ((Tile) focused).item : null;
    }

    private void focusBackground(Item it) {
        String mode = cfg.get(Config.BACKGROUND);
        if (!resumed || !"adaptive".equals(mode) && !"adaptive-tint".equals(mode)) return;
        if ("adaptive-tint".equals(mode) && !tintWallpaper) return;
        if (it != null && it.info != null && it.backgroundColor == 0 && !it.colorRequested
                && (it.banner != null || it.icon != null)) {
            it.colorRequested = true;
            colorIo.execute(() -> {
                int color = FocusColor.from(it.banner != null ? it.banner : it.icon);
                ui.post(() -> {
                    it.backgroundColor = color;
                    if (focusedItem() == it) focusBackground(it);
                });
            });
        }
        int color = it == null || it.backgroundColor == 0 ? FocusColor.FALLBACK : it.backgroundColor;
        if ("adaptive-tint".equals(mode)) color = FocusColor.tint(color);
        if (color == targetColor) return;
        targetColor = color;
        stopColorAnimation();
        if (Settings.Global.getFloat(getContentResolver(), "animator_duration_scale", 1f) == 0f) {
            setAdaptiveColor(color);
            return;
        }
        colorAnimation = ValueAnimator.ofArgb(displayedColor, color);
        colorAnimation.setDuration(250);
        colorAnimation.addUpdateListener(a -> setAdaptiveColor((Integer) a.getAnimatedValue()));
        colorAnimation.start();
    }

    private void setAdaptiveColor(int color) {
        displayedColor = color;
        if ("adaptive-tint".equals(backgroundMode)) tint.setBackgroundColor(color);
        else root.setBackgroundColor(color);
    }

    private void stopColorAnimation() {
        if (colorAnimation != null) {
            colorAnimation.cancel();
            colorAnimation = null;
        }
    }

    private int[] cardSize(boolean appRow) {
        if (!appRow) return new int[] {dp(120), dp(44)};
        switch (cfg.get(Config.TILES)) {
            case "small":
                return new int[] {dp(92), dp(52)};
            case "large":
                return new int[] {dp(104), dp(59)};
            default:
                return new int[] {dp(98), dp(55)};
        }
    }

    /** Spaces a whole number of row cards across the dock, leaving no clipped next card. */
    private int rowGap(int cardWidth) {
        int inner = rowInnerWidth();
        int minimum = dp(18);
        int cards = Math.max(1, (inner + minimum) / (cardWidth + minimum));
        return cards <= 1 ? minimum : Math.max(minimum, (inner - cards * cardWidth) / (cards - 1));
    }

    private int rowVisibleTiles(int cardWidth, int gap) {
        return Math.max(1, (rowInnerWidth() + gap) / (cardWidth + gap));
    }

    private int rowInnerWidth() {
        return getResources().getDisplayMetrics().widthPixels - 2 * dp(108) - 2 * dp(14);
    }

    private void focusFirst() {
        apps.scroll.scrollTo(0, 0);
        inputs.scroll.scrollTo(0, 0);
        View first = apps.tiles().getChildAt(0);
        if (first == null) first = inputs.tiles().getChildAt(0);
        landed = apps.tiles().getChildCount() > 0;
        if (first == null) first = chips.get(0);
        first.requestFocus();
    }

    private void tick() {
        Date now = new Date();
        String marker = DateFormat.format("a", now).toString().toLowerCase(Locale.getDefault());
        SpannableString half = new SpannableString(" " + marker);
        half.setSpan(new RelativeSizeSpan(0.36f), 0, half.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        clock.setText(DateFormat.format("h:mm", now));
        clock.append(half);
        Locale locale = Locale.getDefault();
        date.setText(DateFormat.format(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), now));
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        greeting.setText(hour < 5 ? "Good night" : hour < 12 ? "Good morning"
                : hour < 18 ? "Good afternoon" : hour < 22 ? "Good evening" : "Good night");
        weather();
        // hourly and time-of-day rotations change while the screen stays open; a no-op otherwise
        background();
    }

    private void weather() {
        if ("off".equals(cfg.get("weather", "on"))) {
            weatherTemp.setVisibility(View.GONE);
            weatherDetail.setVisibility(View.GONE);
            return;
        }
        weatherTemp.setVisibility(View.VISIBLE);
        weatherDetail.setVisibility(View.VISIBLE);
        Weather.Reading cached = Weather.cached(this, cfg);
        if (cached != null) showWeather(cached);
        long now = System.currentTimeMillis();
        if (weatherLoading || cached != null && now - cached.time < Weather.REFRESH_MS
                || now - weatherLastAttempt < 5 * 60 * 1000L) return;
        weatherLoading = true;
        weatherLastAttempt = now;
        io.execute(() -> {
            Weather.Reading fresh = null;
            try {
                fresh = Weather.fetch(cfg);
                Weather.save(this, cfg, fresh);
            } catch (java.io.IOException ignored) {
                // Keep the last reading when the TV or weather service is temporarily offline.
            }
            Weather.Reading result = fresh;
            ui.post(() -> {
                weatherLoading = false;
                if (isDestroyed()) return;
                if (result != null) showWeather(result);
                else if (Weather.cached(this, cfg) == null) {
                    weatherDetail.setText(" " + Weather.place(cfg) + " · unavailable");
                }
            });
        });
    }

    private void showWeather(Weather.Reading reading) {
        weatherTemp.setText(" · " + Math.round(reading.temperature) + Weather.unitSymbol(cfg));
        weatherDetail.setText(" " + Weather.place(cfg) + " · " + reading.condition());
    }

    private void background() {
        String mode = cfg.get(Config.BACKGROUND);
        boolean image = "image".equals(mode) || "adaptive-tint".equals(mode);
        String selected = cfg.get(Config.WALLPAPER);
        boolean custom = "custom".equals(selected);
        File file = image && custom ? cfg.wallpaper() : null;
        boolean builtIn = image && (!custom || file == null);
        if (builtIn && custom) selected = "aurora";
        boolean hasWallpaper = file != null || builtIn;
        tintWallpaper = "adaptive-tint".equals(mode) && hasWallpaper;
        if ("adaptive".equals(mode)) {
            if (!mode.equals(backgroundMode)) {
                stopColorAnimation();
                targetColor = 0;
                root.setBackgroundColor(FocusColor.FALLBACK);
                displayedColor = FocusColor.FALLBACK;
                tint.setBackgroundColor(Color.TRANSPARENT);
            }
            backgroundMode = mode;
            focusBackground(focusedItem());
        } else if ("adaptive-tint".equals(mode)) {
            root.setBackgroundColor(hasWallpaper ? Color.BLACK : cfg.background());
            if (!mode.equals(backgroundMode) || !hasWallpaper) {
                stopColorAnimation();
                targetColor = 0;
                tint.setBackgroundColor(Color.TRANSPARENT);
                displayedColor = Color.TRANSPARENT;
            }
            backgroundMode = mode;
            if (tintWallpaper) focusBackground(focusedItem());
        } else {
            stopColorAnimation();
            targetColor = 0;
            backgroundMode = mode;
            tint.setBackgroundColor(Color.TRANSPARENT);
            root.setBackgroundColor(cfg.background());
        }
        setWallpaperMotion(hasWallpaper && "live".equals(cfg.get(Config.MOTION)));
        int dim = Integer.parseInt(cfg.get(Config.DIM));
        if (hasWallpaper) {
            int alpha = dim * 255 / 100;
            shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[] {Color.argb(alpha / 2, 0, 0, 0), Color.argb(alpha, 0, 0, 0),
                            Color.argb(Math.min(255, alpha + 46), 0, 0, 0)}));
        } else {
            shade.setBackgroundColor(Color.TRANSPARENT);
        }
        String key = builtIn ? "resource:" + selected
                : file == null ? null : file.getPath() + ":" + file.lastModified();
        if (key == null ? wallKey == null : key.equals(wallKey)) return;
        wallKey = key;
        if (builtIn) {
            switch (selected) {
                case "alpine":
                    wall.setImageResource(R.drawable.alpine_aurora);
                    break;
                case "coast":
                    wall.setImageResource(R.drawable.golden_coast);
                    break;
                default:
                    wall.setImageResource(R.drawable.cinematic_night);
                    break;
            }
            return;
        }
        if (file == null) {
            wall.setImageDrawable(null);
            return;
        }
        io.execute(() -> {
            Bitmap b = decode(file, getResources().getDisplayMetrics().widthPixels,
                    getResources().getDisplayMetrics().heightPixels);
            ui.post(() -> {
                if (key.equals(wallKey)) wall.setImageBitmap(b);
            });
        });
    }

    /** A restrained Ken Burns movement makes a still wallpaper feel live without distracting. */
    private void setWallpaperMotion(boolean live) {
        if (!live || Settings.Global.getFloat(getContentResolver(),
                "animator_duration_scale", 1f) == 0f) {
            stopWallpaperAnimation();
            return;
        }
        if (wallpaperAnimation != null) return;
        wallpaperAnimation = ValueAnimator.ofFloat(0f, 1f);
        wallpaperAnimation.setDuration(24000);
        wallpaperAnimation.setRepeatCount(ValueAnimator.INFINITE);
        wallpaperAnimation.setRepeatMode(ValueAnimator.REVERSE);
        wallpaperAnimation.addUpdateListener(a -> {
            float p = (Float) a.getAnimatedValue();
            float scale = 1.045f + 0.035f * p;
            wall.setScaleX(scale);
            wall.setScaleY(scale);
            wall.setTranslationX(dp(10) * (p - 0.5f));
            wall.setTranslationY(dp(5) * (0.5f - p));
        });
        wallpaperAnimation.start();
    }

    private void stopWallpaperAnimation() {
        if (wallpaperAnimation != null) {
            wallpaperAnimation.cancel();
            wallpaperAnimation = null;
        }
        if (wall != null) {
            wall.setScaleX(1f);
            wall.setScaleY(1f);
            wall.setTranslationX(0f);
            wall.setTranslationY(0f);
        }
    }

    private static Bitmap decode(File f, int w, int h) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), o);
        o.inSampleSize = 1;
        while (o.outWidth / (o.inSampleSize * 2) >= w && o.outHeight / (o.inSampleSize * 2) >= h) {
            o.inSampleSize *= 2;
        }
        o.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(f.getPath(), o);
    }

    /**
     * Fire OS answers the generic settings action with a stub that reopens Amazon's home, which
     * Guard then replaces with ours. The TV settings entry point opens the real settings screen.
     */
    private void openSettings() {
        Intent tvSettings = new Intent(Intent.ACTION_MAIN)
                .addCategory("android.intent.category.LEANBACK_SETTINGS");
        boolean found = tvSettings.resolveActivity(getPackageManager()) != null;
        launch(found ? tvSettings : new Intent(Settings.ACTION_SETTINGS), null);
    }

    private void open(Tile t) {
        if (Catalog.APPS.equals(t.kind)) {
            Intent app = new Intent(t.item.intent);
            // Use Fire TV's dedicated Appstore activity instead of the newer launcher-embedded
            // grid. It offers the same catalogue while allowing HOME to return here normally.
            if ("com.amazon.venezia".equals(t.item.id)) {
                app = new Intent("com.amazon.venezia.ade.FEATURED_LAUNCH")
                        .setPackage("com.amazon.venezia");
            }
            launch(app, t.item.id);
            return;
        }
        Intent own = Input.intent(this, cfg, t.item, Catalog.label(cfg, t.item));
        // The built-in screen goes on top of this one in the same task, so BACK returns here and
        // the HOME takeover's singleTask relaunch clears it. NEW_TASK | RESET_TASK_IF_NEEDED on an
        // activity of the task that is already in front only brings that task forward and starts
        // nothing (ActivityStarter.complyActivityFlags), so it is started plainly.
        if (own != null) startActivity(own);
        else launch(new Intent(t.item.intent), null);
    }

    private void launch(Intent i, String pkg) {
        if (Guard.AMAZON.equals(pkg)) Guard.pause();
        try {
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "Could not open that", Toast.LENGTH_SHORT).show();
        }
    }

    private void menu(Tile t) {
        boolean app = Catalog.APPS.equals(t.kind);
        String[] actions = app
                ? new String[] {"Move", "Rename", "Hide", "App info"}
                : new String[] {"Move", "Rename", "Hide"};
        new AlertDialog.Builder(this, Ui.DIALOG)
                .setTitle(Catalog.label(cfg, t.item))
                .setItems(actions, (d, which) -> {
                    switch (which) {
                        case 0:
                            if (t.getParent() == null) break;
                            moving = t;
                            t.setMoving(true);
                            break;
                        case 1:
                            rename(t);
                            break;
                        case 2:
                            Catalog.setShown(cfg, t.kind, t.item, false);
                            render(app ? apps : inputs);
                            Toast.makeText(this, "Hidden. Bring it back from Options.", Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            launch(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", t.item.id, null)), null);
                    }
                })
                .show();
    }

    private void rename(Tile t) {
        EditText field = new EditText(this);
        field.setSingleLine();
        field.setText(Catalog.label(cfg, t.item));
        field.setSelectAllOnFocus(true);
        FrameLayout box = new FrameLayout(this);
        box.setPadding(dp(24), dp(8), dp(24), 0);
        box.addView(field);
        String key = "name." + t.item.id;
        AlertDialog d = new AlertDialog.Builder(this, Ui.DIALOG)
                .setTitle("Rename")
                .setView(box)
                .setPositiveButton("Save", (dialog, w) -> {
                    String name = field.getText().toString().trim();
                    cfg.set(key, name.equals(t.item.label.toString()) ? null : name);
                    apply();
                })
                .setNeutralButton("Original", (dialog, w) -> {
                    cfg.set(key, null);
                    apply();
                })
                .setNegativeButton("Cancel", null)
                .create();
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        d.show();
    }

    private void shift(int by) {
        ViewGroup strip = (ViewGroup) moving.getParent();
        int from = strip.indexOfChild(moving), to = from + by;
        if (to < 0 || to >= strip.getChildCount()) return;
        ViewGroup.LayoutParams lp = moving.getLayoutParams();
        strip.removeViewAt(from);
        strip.addView(moving, to, lp);
        moving.requestFocus();
    }

    private void stopMoving() {
        Tile t = moving;
        moving = null;
        t.setMoving(false);
        ViewGroup strip = (ViewGroup) t.getParent();
        if (strip == null) return;
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < strip.getChildCount(); i++) ids.add(((Tile) strip.getChildAt(i)).item.id);
        Catalog.saveOrder(cfg, t.kind, ids);
        applied = cfg.version();
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }
}
