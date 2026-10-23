package tv.dumbtv.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.tv.TvInputManager;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

import java.util.Locale;

/** A card drawn in one pass: banner, or icon plus name, or an input name; focus ring on top. */
final class Tile extends View {
    private static final float FOCUS_SCALE = 1.06f;

    final Item item;
    final String kind;
    private final CharSequence title;
    private final CharSequence displayTitle;
    private final int cardW, cardH;
    private final boolean app;
    private final float radius, stroke;
    private final RectF card = new RectF();
    private final Path clip = new Path();
    private final Path arrow = new Path();
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyph = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint inner = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint brand = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private boolean moving;

    Tile(Context c, Item item, String kind, CharSequence title, int cardW, int cardH,
            int accent) {
        super(c);
        this.item = item;
        this.kind = kind;
        this.title = title;
        this.displayTitle = title;
        this.cardW = cardW;
        this.cardH = cardH;
        app = Catalog.APPS.equals(kind);
        radius = Ui.dp(c, app ? 18 : 14);
        stroke = Ui.dp(c, 3f);

        fill.setColor(app ? Ui.SURFACE : 0xD91A1D24);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(stroke);
        ring.setColor(accent);
        dot.setColor(0xFF34C759);
        glyph.setColor(Ui.TEXT);
        glyph.setStyle(Paint.Style.STROKE);
        glyph.setStrokeWidth(Ui.dp(c, 2.2f));
        glyph.setStrokeCap(Paint.Cap.ROUND);
        glyph.setStrokeJoin(Paint.Join.ROUND);
        inner.setColor(Ui.TEXT);
        inner.setTypeface(Ui.MEDIUM);
        inner.setTextAlign(Paint.Align.CENTER);
        inner.setTextSize(Ui.dp(c, 11));
        brand.setTypeface(Ui.MEDIUM);
        brand.setTextAlign(Paint.Align.CENTER);

        setFocusable(true);
        setClickable(true);
        setLongClickable(true);
        setContentDescription(title);
    }

    void setMoving(boolean on) {
        moving = on;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(cardW, cardH);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        card.set(0, 0, cardW, cardH);
        clip.reset();
        clip.addRoundRect(card, radius, radius, Path.Direction.CW);
        setPivotX(cardW / 2f);
        setPivotY(cardH / 2f);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previous) {
        super.onFocusChanged(focused, direction, previous);
        float s = focused ? FOCUS_SCALE : 1f;
        animate().scaleX(s).scaleY(s).translationZ(focused ? Ui.dp(getContext(), 12) : 0)
                .setDuration(140).start();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable banner = item.banner, icon = item.icon;
        canvas.save();
        canvas.clipPath(clip);
        canvas.drawRect(card, fill);
        if (app) drawCompactApp(canvas, banner, icon);
        else drawCompactInput(canvas);
        if (moving) canvas.drawColor(0x99000000);
        canvas.restore();

        if (item.state == TvInputManager.INPUT_STATE_CONNECTED) {
            float r = Ui.dp(getContext(), 3.5f);
            canvas.drawCircle(cardW - r * 3, r * 3, r, dot);
        }
        if (isFocused() || moving) {
            float half = stroke / 2;
            ring.setPathEffect(moving ? new DashPathEffect(new float[] {stroke * 3, stroke * 2}, 0) : null);
            canvas.drawRoundRect(half, half, cardW - half, cardH - half, radius - half, radius - half, ring);
        }
        if (moving) drawArrows(canvas);
    }

    /** The TV banner becomes the tile itself; fallback icons remain fully contained. */
    private void drawCompactApp(Canvas canvas, Drawable banner, Drawable icon) {
        String name = displayTitle.toString().toLowerCase(Locale.ROOT);
        if (name.contains("netflix")) {
            canvas.drawColor(0xFF080808);
            brand.setColor(0xFFE50914);
            brand.setTextSize(cardH * 0.62f);
            float baseline = cardH / 2f - (brand.ascent() + brand.descent()) / 2f;
            canvas.drawText("N", cardW / 2f, baseline, brand);
            return;
        }
        if (name.contains("live tv") || name.equals("hd+")) {
            canvas.drawColor(0xFF0A84FF);
            float cx = cardW / 2f, cy = cardH * 0.46f;
            float w = cardW * 0.38f, h = cardH * 0.36f;
            glyph.setColor(0xFFFFFFFF);
            RectF tv = new RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
            canvas.drawRoundRect(tv, Ui.dp(getContext(), 3), Ui.dp(getContext(), 3), glyph);
            canvas.drawLine(cx, tv.bottom, cx, tv.bottom + cardH * 0.10f, glyph);
            canvas.drawLine(cx - w * 0.18f, tv.bottom + cardH * 0.10f,
                    cx + w * 0.18f, tv.bottom + cardH * 0.10f, glyph);
            return;
        }
        if ("com.amazon.venezia".equals(item.id)) {
            canvas.drawColor(0xFFF59E0B);
            glyph.setColor(0xFFFFFFFF);
            float cx = cardW / 2f, top = cardH * 0.29f;
            float w = cardH * 0.48f, h = cardH * 0.43f;
            RectF bag = new RectF(cx - w / 2, top, cx + w / 2, top + h);
            canvas.drawRoundRect(bag, Ui.dp(getContext(), 3), Ui.dp(getContext(), 3), glyph);
            RectF handle = new RectF(cx - w * 0.22f, top - h * 0.33f,
                    cx + w * 0.22f, top + h * 0.20f);
            canvas.drawArc(handle, 190, 160, false, glyph);
            brand.setColor(0xFFFFFFFF);
            brand.setTextSize(cardH * 0.28f);
            float baseline = top + h * 0.68f - (brand.ascent() + brand.descent()) / 2f;
            canvas.drawText("a", cx, baseline, brand);
            return;
        }
        if (banner != null) {
            int iw = Math.max(1, banner.getIntrinsicWidth());
            int ih = Math.max(1, banner.getIntrinsicHeight());
            float scale = Math.min(cardW / (float) iw, cardH / (float) ih);
            int w = Math.max(1, Math.round(iw * scale));
            int h = Math.max(1, Math.round(ih * scale));
            int left = (cardW - w) / 2, top = (cardH - h) / 2;
            banner.setBounds(left, top, left + w, top + h);
            banner.draw(canvas);
            return;
        }
        Drawable art = icon;
        if (art != null) {
            int maxW = Math.round(cardW * 0.76f);
            int maxH = Math.round(cardH * 0.76f);
            int iw = Math.max(1, art.getIntrinsicWidth());
            int ih = Math.max(1, art.getIntrinsicHeight());
            float scale = Math.min(maxW / (float) iw, maxH / (float) ih);
            int w = Math.max(1, Math.round(iw * scale));
            int h = Math.max(1, Math.round(ih * scale));
            int left = (cardW - w) / 2;
            int top = (cardH - h) / 2;
            art.setBounds(left, top, left + w, top + h);
            art.draw(canvas);
        }
    }

    private void drawCompactInput(Canvas canvas) {
        String name = title.toString().toLowerCase(Locale.ROOT);
        float cy = cardH / 2f;
        float symbolW = Ui.dp(getContext(), 28), gap = Ui.dp(getContext(), 6);
        float room = cardW - symbolW - gap - Ui.dp(getContext(), 14);
        CharSequence s = TextUtils.ellipsize(displayTitle, inner, room, TextUtils.TruncateAt.END);
        float textW = inner.measureText(s.toString());
        float start = (cardW - symbolW - gap - textW) / 2f;
        float cx = start + symbolW / 2f;
        if (name.contains("hdmi")) drawHdmi(canvas, cx, cy);
        else if (name.contains("composite") || name.contains("av")) drawComposite(canvas, cx, cy);
        else drawController(canvas, cx, cy);
        float x = start + symbolW + gap;
        float baseline = cy - (inner.ascent() + inner.descent()) / 2f;
        inner.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(s, 0, s.length(), x, baseline, inner);
        inner.setTextAlign(Paint.Align.CENTER);
    }

    private void drawHdmi(Canvas canvas, float cx, float cy) {
        float w = cardW * 0.23f, h = cardH * 0.20f;
        Path p = new Path();
        p.moveTo(cx - w / 2, cy - h / 2);
        p.lineTo(cx + w / 2, cy - h / 2);
        p.lineTo(cx + w * 0.40f, cy + h / 2);
        p.lineTo(cx - w * 0.40f, cy + h / 2);
        p.close();
        canvas.drawPath(p, glyph);
        canvas.drawLine(cx - w * 0.25f, cy, cx + w * 0.25f, cy, glyph);
    }

    private void drawComposite(Canvas canvas, float cx, float cy) {
        float r = cardW * 0.03f, gap = r * 2.5f;
        int[] colors = {0xFFFFD52E, 0xFFFF453A, 0xFFF2F2F3};
        for (int i = 0; i < 3; i++) {
            glyph.setColor(colors[i]);
            canvas.drawCircle(cx + (i - 1) * gap, cy, r, glyph);
        }
        glyph.setColor(Ui.TEXT);
    }

    private void drawController(Canvas canvas, float cx, float cy) {
        float w = cardW * 0.22f, h = cardH * 0.32f;
        RectF body = new RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
        canvas.drawRoundRect(body, h * 0.45f, h * 0.45f, glyph);
        canvas.drawLine(cx - w * 0.28f, cy, cx - w * 0.08f, cy, glyph);
        canvas.drawLine(cx - w * 0.18f, cy - h * 0.28f, cx - w * 0.18f, cy + h * 0.28f, glyph);
        canvas.drawCircle(cx + w * 0.19f, cy - h * 0.12f, glyph.getStrokeWidth(), glyph);
        canvas.drawCircle(cx + w * 0.29f, cy + h * 0.12f, glyph.getStrokeWidth(), glyph);
    }

    private void drawArrows(Canvas canvas) {
        float h = cardH / 6f, w = h * 0.7f, mid = cardH / 2f, pad = h * 0.8f;
        inner.setStyle(Paint.Style.FILL);
        arrow.reset();
        arrow.moveTo(pad, mid);
        arrow.lineTo(pad + w, mid - h / 2);
        arrow.lineTo(pad + w, mid + h / 2);
        arrow.close();
        arrow.moveTo(cardW - pad, mid);
        arrow.lineTo(cardW - pad - w, mid - h / 2);
        arrow.lineTo(cardW - pad - w, mid + h / 2);
        arrow.close();
        canvas.drawPath(arrow, inner);
    }
}
