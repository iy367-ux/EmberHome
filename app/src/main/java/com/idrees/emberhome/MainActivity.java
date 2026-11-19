package com.idrees.emberhome;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.method.TransformationMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {

    private static final int BG = 0xFF0A0A0A, AMBER = 0xFFFFB454, DIM = 0xFF8A867D,
            SOFT = 0xFFBDB9B0, FG = 0xFFF2EFE9, LINE = 0xFF262626, CARD = 0xFF141414,
            CARD_SEL = 0xFF1D1D1D;
    private static final String[] STYLES = {"Poster", "Rows", "Spotlight", "Grid"};
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private static class AppEntry {
        String label, pkg;
        ComponentName component;
        Drawable icon;
    }

    private FrameLayout root;
    private final List<AppEntry> apps = new ArrayList<AppEntry>();
    private final List<AppEntry> allApps = new ArrayList<AppEntry>();
    private final List<TextView> wxViews = new ArrayList<TextView>();
    private final List<Animator> anims = new ArrayList<Animator>();
    private SharedPreferences prefs;
    private float density;
    private int style;
    private Runnable populator;
    private View firstTile, lastSelected;
    private BaseAdapter listAdapter;
    private AbsListView listView;
    private Dialog settingsDlg;
    private boolean editMode = false;
    private int editingIndex = -1;
    private String focusPkg = null;
    private int wallpaperIndex = 0;
    private int gridColumns = 6;
    private static final int MEDIA_PERMISSION_REQUEST = 731;

    private static final String[] WALLPAPER_CATEGORIES = {
            "Gradients", "Sunsets", "Nature", "Cities", "Plain colours"
    };
    private static final String[] WALLPAPER_NAMES = {
            "Amber Horizon", "Midnight Blue", "Violet Fade",
            "Golden Dusk", "Afterglow", "Desert Sunset",
            "Forest Night", "Ocean Deep", "Moss & Stone",
            "City Lights", "Neon District", "Midnight Skyline",
            "Pure Black", "Deep Blue", "Charcoal"
    };

    // weather: 0 loading, 1 ok, 2 failed
    private int wxState = 0;
    private int wxTempC, wxTempF, wxHiC, wxLoC, wxHiF, wxLoF;
    private String wxDesc = "", wxPlace = "";
    private long wxFetched = 0;

    private final BroadcastReceiver pkgReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { loadApps(); }
    };

    // ================================================================ lifecycle

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        density = getResources().getDisplayMetrics().density;
        prefs = getSharedPreferences("ember", MODE_PRIVATE);
        wallpaperIndex = prefs.getInt("wallpaper", 0);
        enableImmersiveMode();
        style = prefs.getInt("style", 0);
        if (style < 0 || style >= STYLES.length) style = 0;
        root = new FrameLayout(this);
        Drawable customWallpaper = loadCustomWallpaper();
        root.setBackgroundDrawable(customWallpaper != null ? customWallpaper : wallpaperDrawable(wallpaperIndex));
        setContentView(root);
        String cached = prefs.getString("wx", null);
        if (cached != null) parseWeather(cached);
        buildUi();
        loadApps();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        registerReceiver(pkgReceiver, f);
        if (System.currentTimeMillis() - wxFetched > 15 * 60 * 1000L) fetchWeather();
    }

    @Override
    protected void onStop() {
        super.onStop();
        try { unregisterReceiver(pkgReceiver); } catch (Exception ignored) { }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (settingsDlg != null && settingsDlg.isShowing()) settingsDlg.dismiss();
        focusFirst();
    }

    @Override
    public void onBackPressed() {
        if (editMode) {
            finishEditMode();
            return;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enableImmersiveMode();
    }

    private void enableImmersiveMode() {
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            getWindow().getDecorView().setSystemUiVisibility(flags);
        } else {
            getWindow().setFlags(1024, 1024);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (editMode && event.getAction() == KeyEvent.ACTION_DOWN) {
            int key = event.getKeyCode();
            if (key == KeyEvent.KEYCODE_BACK) {
                finishEditMode();
                return true;
            }
            if (key == KeyEvent.KEYCODE_MENU || key == 176 /* KEYCODE_SETTINGS */) {
                if (editingIndex >= 0 && editingIndex < apps.size()) {
                    menuFor(apps.get(editingIndex));
                }
                return true;
            }
            int delta = 0;
            if (style == 3) {
                if (key == KeyEvent.KEYCODE_DPAD_LEFT) delta = -1;
                else if (key == KeyEvent.KEYCODE_DPAD_RIGHT) delta = 1;
                else if (key == KeyEvent.KEYCODE_DPAD_UP) delta = -gridColumns;
                else if (key == KeyEvent.KEYCODE_DPAD_DOWN) delta = gridColumns;
            } else {
                if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_UP) delta = -1;
                else if (key == KeyEvent.KEYCODE_DPAD_RIGHT || key == KeyEvent.KEYCODE_DPAD_DOWN) delta = 1;
            }
            if (delta != 0) {
                moveApp(editingIndex, editingIndex + delta);
                return true;
            }
            if (key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_DPAD_CENTER) {
                finishEditMode();
                return true;
            }
            if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_DPAD_LEFT
                    || key == KeyEvent.KEYCODE_DPAD_RIGHT || key == KeyEvent.KEYCODE_DPAD_UP
                    || key == KeyEvent.KEYCODE_DPAD_DOWN) return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == 176 /* KEYCODE_SETTINGS */) {
            showSettings();
            return true;
        }
        return super.onKeyDown(keyCode, e);
    }

    // ================================================================ helpers

    private int dp(float v) { return (int) (v * density + 0.5f); }

    private TextView tv(String text, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        return t;
    }

    private TextClock clock(String fmt, int sp, int color, boolean light) {
        TextClock c = new TextClock(this);
        c.setFormat24Hour(fmt);
        c.setFormat12Hour(fmt);
        c.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        c.setTextColor(color);
        c.setIncludeFontPadding(false);
        if (light) c.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        return c;
    }

    private static class Upper implements TransformationMethod {
        @Override public CharSequence getTransformation(CharSequence s, View v) {
            return s == null ? null : s.toString().toUpperCase();
        }
        @Override public void onFocusChanged(View v, CharSequence s, boolean f, int d, Rect r) { }
    }

    private TextClock dateView(int sp) {
        TextClock d = clock("EEEE d MMMM", sp, AMBER, false);
        d.setTransformationMethod(new Upper());
        d.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return d;
    }

    private TextView wx(int kind, int sp, int color) {
        TextView t = tv("", sp, color);
        t.setTag(Integer.valueOf(kind));
        wxViews.add(t);
        return t;
    }

    private LinearLayout box(int orientation) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(orientation);
        return l;
    }

    private boolean showAppLabels() { return prefs.getBoolean("show_labels", true); }
    private boolean motionEnabled() { return prefs.getBoolean("motion", true); }
    private float iconScale() {
        int size = prefs.getInt("icon_size", 1);
        return size == 0 ? 0.82f : (size == 2 ? 1.18f : 1f);
    }
    private int scaledIcon(int base) { return dp(Math.max(28, (int) (base * iconScale() + 0.5f))); }

    private void enter(View v, int idx) {
        if (!motionEnabled()) {
            v.setAlpha(1f);
            v.setTranslationY(0f);
            return;
        }
        v.setAlpha(0f);
        v.setTranslationY(dp(22));
        v.animate().alpha(1f).translationY(0f).setStartDelay(idx * 90L).setDuration(520)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    private void scale(View v, float s) {
        if (!motionEnabled()) {
            v.setScaleX(s);
            v.setScaleY(s);
        } else {
            v.animate().scaleX(s).scaleY(s).setDuration(170).start();
        }
    }

    // ---- flat dark drawables ----
    private GradientDrawable flat(boolean strong, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(strong ? CARD_SEL : CARD);
        g.setCornerRadius(radius);
        g.setStroke(strong ? dp(2) : dp(1), strong ? AMBER : LINE);
        return g;
    }

    private StateListDrawable stateFlat(float radius, boolean transparentIdle) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, flat(true, radius));
        s.addState(new int[]{android.R.attr.state_selected}, flat(true, radius));
        s.addState(new int[]{android.R.attr.state_focused}, flat(true, radius));
        s.addState(new int[]{}, transparentIdle ? new ColorDrawable(0) : flat(false, radius));
        return s;
    }

    private TextView chip(String text, final Runnable action) {
        TextView t = tv(text, 15, FG);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(22), dp(10), dp(22), dp(10));
        t.setFocusable(true);
        t.setClickable(true);
        t.setBackground(stateFlat(dp(30), false));
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { action.run(); }
        });
        return t;
    }

    private TextView settingsChip() {
        return chip("Settings", new Runnable() { @Override public void run() { showSettings(); } });
    }

    // ================================================================ UI per style

    private void buildUi() {
        for (Animator a : anims) a.cancel();
        anims.clear();
        root.removeAllViews();
        wxViews.clear();
        firstTile = null;
        lastSelected = null;
        populator = null;
        listView = null;
        listAdapter = null;
        switch (style) {
            case 1: buildRows(); break;
            case 2: buildSpotlight(); break;
            case 3: buildGrid(); break;
            default: buildPoster();
        }
        updateWx();
        populate();
    }

    private void populate() {
        if (populator != null) populator.run();
        focusFirst();
    }

    private void focusFirst() {
        root.post(new Runnable() {
            @Override public void run() {
                if (focusPkg != null) {
                    View tile = root.findViewWithTag("ember:" + focusPkg);
                    if (tile != null && tile.requestFocus()) return;
                    int index = indexOfPackage(focusPkg);
                    if (index >= 0 && listView != null) {
                        listView.setSelection(index);
                        listView.requestFocus();
                        return;
                    }
                }
                if (firstTile != null) firstTile.requestFocus();
                else if (listView != null) listView.requestFocus();
            }
        });
    }

    private int indexOfPackage(String pkg) {
        for (int i = 0; i < apps.size(); i++) {
            if (apps.get(i).pkg.equals(pkg)) return i;
        }
        return -1;
    }

    private void beginEditMode(AppEntry app) {
        int index = apps.indexOf(app);
        if (index < 0) return;
        editMode = true;
        editingIndex = index;
        focusPkg = app.pkg;
        Toast.makeText(this, "EDIT MODE: " + app.label
                + "  •  arrows to move  •  OK to finish  •  Menu for options",
                Toast.LENGTH_LONG).show();
    }

    private void finishEditMode() {
        editMode = false;
        editingIndex = -1;
        populate();
    }

    private void saveAppOrder() {
        StringBuilder order = new StringBuilder();
        for (AppEntry app : apps) {
            if (order.length() > 0) order.append("|");
            order.append(app.pkg);
        }
        prefs.edit().putString("app_order", order.toString()).apply();
    }

    private void applySavedAppOrder(List<AppEntry> list) {
        final String saved = prefs.getString("app_order", "");
        if (saved.length() == 0) return;
        final String[] order = saved.split("\\|");
        Collections.sort(list, new Comparator<AppEntry>() {
            @Override public int compare(AppEntry a, AppEntry b) {
                int pa = java.util.Arrays.asList(order).indexOf(a.pkg);
                int pb = java.util.Arrays.asList(order).indexOf(b.pkg);
                if (pa < 0) pa = Integer.MAX_VALUE;
                if (pb < 0) pb = Integer.MAX_VALUE;
                return pa == pb ? a.label.compareToIgnoreCase(b.label) : (pa < pb ? -1 : 1);
            }
        });
    }

    private void moveApp(int from, int to) {
        if (from < 0 || to < 0 || from >= apps.size() || to >= apps.size() || from == to) return;
        AppEntry moving = apps.remove(from);
        apps.add(to, moving);
        editingIndex = to;
        focusPkg = moving.pkg;
        saveAppOrder();
        populate();
    }

    // ---------------------------------------------------------------- A: Poster

    private void buildPoster() {
        TextClock giant = clock("HH:mm", 250, 0xFF171717, false);
        giant.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        FrameLayout.LayoutParams gl = new FrameLayout.LayoutParams(WRAP, WRAP);
        gl.leftMargin = -dp(24);
        gl.topMargin = -dp(60);
        root.addView(giant, gl);
        ObjectAnimator drift = ObjectAnimator.ofFloat(giant, "translationY", 0f, dp(18));
        drift.setDuration(9000);
        drift.setRepeatMode(ObjectAnimator.REVERSE);
        drift.setRepeatCount(ObjectAnimator.INFINITE);
        drift.setInterpolator(new AccelerateDecelerateInterpolator());
        drift.start();
        anims.add(drift);

        LinearLayout main = box(LinearLayout.HORIZONTAL);
        main.setPadding(dp(56), dp(40), dp(56), dp(44));
        root.addView(main, new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout left = box(LinearLayout.VERTICAL);
        left.setGravity(Gravity.BOTTOM);
        main.addView(left, new LinearLayout.LayoutParams(0, MATCH, 1f));
        left.addView(dateView(15));
        TextView w0 = wx(0, 28, FG);
        LinearLayout.LayoutParams l0 = new LinearLayout.LayoutParams(WRAP, WRAP);
        l0.topMargin = dp(8);
        left.addView(w0, l0);
        left.addView(wx(1, 15, DIM));
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(WRAP, WRAP);
        cl.topMargin = dp(20);
        left.addView(settingsChip(), cl);
        enter(left, 0);

        ListView lv = new ListView(this);
        lv.setDivider(null);
        lv.setSelector(new ColorDrawable(0));
        lv.setVerticalScrollBarEnabled(false);
        lv.setVerticalFadingEdgeEnabled(false);
        lv.setClipToPadding(false);
        lv.setPadding(0, dp(30), 0, dp(30));
        final PosterAdapter ad = new PosterAdapter();
        lv.setAdapter(ad);
        listView = lv;
        listAdapter = ad;
        main.addView(lv, new LinearLayout.LayoutParams(dp(320), MATCH));
        enter(lv, 2);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) { launch(apps.get(pos)); }
        });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> p, View v, int pos, long id) {
                beginEditMode(apps.get(pos));
                return true;
            }
        });
        lv.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (lastSelected != null && lastSelected != v) pick(lastSelected, false);
                if (v != null) { pick(v, true); lastSelected = v; }
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });
        lv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) { if (lastSelected != null) pick(lastSelected, has); }
        });
        populator = new Runnable() { @Override public void run() { ad.notifyDataSetChanged(); } };
    }

    private void pick(View row, boolean on) {
        Object tag = row.getTag();
        if (!(tag instanceof View[])) return;
        View[] t = (View[]) tag;
        ((TextView) t[0]).setTextColor(on ? FG : SOFT);
        row.animate().translationX(on ? -dp(14) : 0).setDuration(170).start();
        t[1].animate().scaleX(on ? 1.18f : 1f).scaleY(on ? 1.18f : 1f).setDuration(170).start();
    }

    private class PosterAdapter extends BaseAdapter {
        @Override public int getCount() { return apps.size(); }
        @Override public Object getItem(int p) { return apps.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int pos, View convert, ViewGroup parent) {
            LinearLayout row;
            TextView label;
            ImageView icon;
            if (convert == null) {
                row = box(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                row.setPadding(dp(8), dp(4), dp(8), dp(4));
                row.setLayoutParams(new AbsListView.LayoutParams(MATCH, dp(60)));
                label = tv("", 19, SOFT);
                label.setSingleLine(true);
                label.setEllipsize(TextUtils.TruncateAt.END);
                icon = new ImageView(MainActivity.this);
                row.addView(label, new LinearLayout.LayoutParams(0, WRAP, 1f));
                label.setGravity(Gravity.RIGHT);
                LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(scaledIcon(44), scaledIcon(44));
                il.leftMargin = dp(14);
                row.addView(icon, il);
                row.setTag(new View[]{label, icon});
            } else {
                row = (LinearLayout) convert;
                View[] t = (View[]) row.getTag();
                label = (TextView) t[0];
                icon = (ImageView) t[1];
            }
            row.animate().cancel();
            icon.animate().cancel();
            row.setTranslationX(0);
            icon.setScaleX(1f);
            icon.setScaleY(1f);
            label.setTextColor(SOFT);
            AppEntry e = apps.get(pos);
            row.setContentDescription("ember:" + e.pkg);
            label.setText(e.label);
            label.setVisibility(showAppLabels() ? View.VISIBLE : View.GONE);
            icon.setImageDrawable(e.icon);
            return row;
        }
    }

    // ---------------------------------------------------------------- B: Rows

    private void buildRows() {
        LinearLayout col = box(LinearLayout.VERTICAL);
        col.setPadding(dp(48), dp(32), dp(48), dp(20));
        root.addView(col, new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout head = box(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        col.addView(head, new LinearLayout.LayoutParams(MATCH, WRAP));
        LinearLayout hl = box(LinearLayout.VERTICAL);
        head.addView(hl, new LinearLayout.LayoutParams(0, WRAP, 1f));
        hl.addView(dateView(14));
        hl.addView(wx(0, 18, FG));
        hl.addView(wx(1, 13, DIM));
        TextClock c = clock("HH:mm", 40, FG, true);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(WRAP, WRAP);
        cp.rightMargin = dp(24);
        head.addView(c, cp);
        head.addView(settingsChip());
        enter(head, 0);

        TextView favLabel = tv("Favorites", 14, DIM);
        LinearLayout.LayoutParams fl = new LinearLayout.LayoutParams(WRAP, WRAP);
        fl.topMargin = dp(26);
        col.addView(favLabel, fl);
        final LinearLayout favRow = box(LinearLayout.HORIZONTAL);
        col.addView(rowScroll(favRow), new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView allLabel = tv("All apps", 14, DIM);
        LinearLayout.LayoutParams al = new LinearLayout.LayoutParams(WRAP, WRAP);
        al.topMargin = dp(10);
        col.addView(allLabel, al);
        final LinearLayout allRow = box(LinearLayout.HORIZONTAL);
        col.addView(rowScroll(allRow), new LinearLayout.LayoutParams(MATCH, WRAP));
        enter(favLabel, 1);
        enter(favRow, 2);
        enter(allLabel, 3);
        enter(allRow, 4);

        populator = new Runnable() {
            @Override public void run() {
                favRow.removeAllViews();
                allRow.removeAllViews();
                firstTile = null;
                Set<String> favs = favSet();
                int n = 0;
                for (AppEntry e : apps) {
                    if (favs.contains(e.pkg)) {
                        View t = makeTile(e, 200, 112, 52, true, 1.08f, null, false);
                        favRow.addView(t);
                        if (firstTile == null) firstTile = t;
                        n++;
                    }
                }
                if (n == 0) {
                    TextView hint = tv("Use Menu while editing an app to manage favorites", 15, DIM);
                    hint.setPadding(0, dp(40), 0, dp(40));
                    favRow.addView(hint);
                }
                for (AppEntry e : apps) {
                    View t = makeTile(e, 104, 104, 48, true, 1.1f, null, false);
                    allRow.addView(t);
                    if (firstTile == null) firstTile = t;
                }
            }
        };
    }

    private HorizontalScrollView rowScroll(LinearLayout row) {
        HorizontalScrollView s = new HorizontalScrollView(this);
        s.setHorizontalScrollBarEnabled(false);
        s.setClipToPadding(false);
        s.setClipChildren(false);
        s.setPadding(dp(8), dp(14), dp(8), dp(14));
        row.setClipChildren(false);
        row.setClipToPadding(false);
        s.addView(row, new FrameLayout.LayoutParams(WRAP, WRAP));
        return s;
    }

    /** One focusable tile; used by Rows and Spotlight. */
    private View makeTile(final AppEntry e, int w, int h, int iconDp, boolean label,
                          final float focusScale, final TextView bigName, boolean focusOnlyBg) {
        LinearLayout t = box(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(8), dp(8), dp(8), dp(8));
        t.setFocusable(true);
        t.setClickable(true);
        t.setBackground(stateFlat(dp(18), focusOnlyBg));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(w), dp(h));
        lp.rightMargin = dp(focusOnlyBg ? 20 : 12);
        t.setLayoutParams(lp);
        ImageView ic = new ImageView(this);
        ic.setImageDrawable(e.icon);
        t.addView(ic, new LinearLayout.LayoutParams(scaledIcon(iconDp), scaledIcon(iconDp)));
        if (label && showAppLabels()) {
            TextView l = tv(e.label, 13, FG);
            l.setSingleLine(true);
            l.setEllipsize(TextUtils.TruncateAt.END);
            l.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(MATCH, WRAP);
            ll.topMargin = dp(6);
            t.addView(l, ll);
        }
        t.setTag("ember:" + e.pkg);
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (editMode) finishEditMode();
                else launch(e);
            }
        });
        t.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                beginEditMode(e);
                return true;
            }
        });
        t.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) {
                scale(v, has ? focusScale : 1f);
                if (has && bigName != null) bigName.setText(e.label);
            }
        });
        return t;
    }

    // ---------------------------------------------------------------- C: Spotlight

    private void buildSpotlight() {
        LinearLayout col = box(LinearLayout.VERTICAL);
        col.setPadding(dp(56), dp(36), dp(56), dp(24));
        root.addView(col, new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout top = box(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        col.addView(top, new LinearLayout.LayoutParams(MATCH, WRAP));
        top.addView(clock("HH:mm", 56, FG, true), new LinearLayout.LayoutParams(0, WRAP, 1f));
        LinearLayout right = box(LinearLayout.VERTICAL);
        right.setGravity(Gravity.RIGHT);
        right.addView(dateView(14));
        TextView w0 = wx(0, 18, FG);
        w0.setGravity(Gravity.RIGHT);
        right.addView(w0);
        TextView w1 = wx(1, 13, DIM);
        w1.setGravity(Gravity.RIGHT);
        right.addView(w1);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(WRAP, WRAP);
        rp.rightMargin = dp(24);
        top.addView(right, rp);
        top.addView(settingsChip());
        enter(top, 0);

        LinearLayout mid = box(LinearLayout.VERTICAL);
        mid.setGravity(Gravity.CENTER_VERTICAL);
        col.addView(mid, new LinearLayout.LayoutParams(MATCH, 0, 1f));
        final TextView big = tv("", 60, FG);
        big.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        big.setSingleLine(true);
        big.setEllipsize(TextUtils.TruncateAt.END);
        mid.addView(big);
        mid.addView(tv("OK to open     Hold OK to rearrange     Menu for options", 14, AMBER));
        enter(mid, 2);

        final LinearLayout row = box(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        HorizontalScrollView dock = rowScroll(row);
        dock.setPadding(dp(30), dp(34), dp(30), dp(34));
        col.addView(dock, new LinearLayout.LayoutParams(MATCH, WRAP));
        enter(dock, 3);

        populator = new Runnable() {
            @Override public void run() {
                row.removeAllViews();
                firstTile = null;
                for (AppEntry e : apps) {
                    View t = makeTile(e, 84, 84, 60, false, 1.35f, big, true);
                    row.addView(t);
                    if (firstTile == null) firstTile = t;
                }
                big.setText(apps.isEmpty() ? "" : apps.get(0).label);
            }
        };
    }

    // ---------------------------------------------------------------- D: Grid

    private void buildGrid() {
        LinearLayout col = box(LinearLayout.VERTICAL);
        col.setPadding(dp(48), dp(32), dp(48), 0);
        root.addView(col, new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout head = box(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        col.addView(head, new LinearLayout.LayoutParams(MATCH, WRAP));
        head.addView(clock("HH:mm", 48, FG, true), new LinearLayout.LayoutParams(0, WRAP, 1f));
        LinearLayout right = box(LinearLayout.VERTICAL);
        right.setGravity(Gravity.RIGHT);
        right.addView(dateView(14));
        TextView w0 = wx(0, 18, FG);
        w0.setGravity(Gravity.RIGHT);
        right.addView(w0);
        TextView w1 = wx(1, 13, DIM);
        w1.setGravity(Gravity.RIGHT);
        right.addView(w1);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(WRAP, WRAP);
        rp.rightMargin = dp(24);
        head.addView(right, rp);
        head.addView(settingsChip());
        enter(head, 0);

        GridView g = new GridView(this);
        gridColumns = DeviceProfile.gridColumns(this);
        g.setNumColumns(gridColumns);
        g.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        g.setHorizontalSpacing(dp(14));
        g.setVerticalSpacing(dp(14));
        g.setSelector(new ColorDrawable(0));
        g.setVerticalScrollBarEnabled(false);
        g.setClipToPadding(false);
        g.setClipChildren(false);
        g.setPadding(dp(10), dp(24), dp(10), dp(24));
        final GridAdapter ad = new GridAdapter();
        g.setAdapter(ad);
        listView = g;
        listAdapter = ad;
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(MATCH, 0, 1f);
        gp.topMargin = dp(10);
        col.addView(g, gp);
        enter(g, 2);

        g.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) { launch(apps.get(pos)); }
        });
        g.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> p, View v, int pos, long id) {
                beginEditMode(apps.get(pos));
                return true;
            }
        });
        g.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (lastSelected != null && lastSelected != v) scale(lastSelected, 1f);
                if (v != null) { scale(v, 1.08f); lastSelected = v; }
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });
        g.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) { if (lastSelected != null) scale(lastSelected, has ? 1.08f : 1f); }
        });
        populator = new Runnable() { @Override public void run() { ad.notifyDataSetChanged(); } };
    }

    private class GridAdapter extends BaseAdapter {
        @Override public int getCount() { return apps.size(); }
        @Override public Object getItem(int p) { return apps.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int pos, View convert, ViewGroup parent) {
            LinearLayout tile;
            ImageView icon;
            TextView label;
            if (convert == null) {
                tile = box(LinearLayout.VERTICAL);
                tile.setGravity(Gravity.CENTER);
                tile.setPadding(dp(8), dp(10), dp(8), dp(10));
                tile.setBackground(stateFlat(dp(18), false));
                tile.setLayoutParams(new AbsListView.LayoutParams(MATCH, dp(112)));
                icon = new ImageView(MainActivity.this);
                tile.addView(icon, new LinearLayout.LayoutParams(scaledIcon(56), scaledIcon(56)));
                label = tv("", 14, FG);
                label.setSingleLine(true);
                label.setEllipsize(TextUtils.TruncateAt.END);
                label.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
                lp.topMargin = dp(8);
                label.setVisibility(showAppLabels() ? View.VISIBLE : View.GONE);
                tile.addView(label, lp);
                tile.setTag(new View[]{icon, label});
            } else {
                tile = (LinearLayout) convert;
                View[] t = (View[]) tile.getTag();
                icon = (ImageView) t[0];
                label = (TextView) t[1];
            }
            tile.animate().cancel();
            tile.setScaleX(1f);
            tile.setScaleY(1f);
            AppEntry e = apps.get(pos);
            tile.setContentDescription("ember:" + e.pkg);
            icon.setImageDrawable(e.icon);
            label.setText(e.label);
            label.setVisibility(showAppLabels() ? View.VISIBLE : View.GONE);
            return tile;
        }
    }

    private Set<String> hiddenApps() {
        return new HashSet<String>(prefs.getStringSet("hidden_apps", new HashSet<String>()));
    }

    private void showAppManager() {
        if (allApps.isEmpty()) {
            Toast.makeText(this, "No installed apps found yet", Toast.LENGTH_SHORT).show();
            return;
        }
        final String[] labels = new String[allApps.size()];
        boolean[] checked = new boolean[allApps.size()];
        final Set<String> hidden = hiddenApps();
        for (int i = 0; i < allApps.size(); i++) {
            AppEntry app = allApps.get(i);
            labels[i] = app.label;
            checked[i] = !hidden.contains(app.pkg);
        }
        AlertDialog dialog = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                .setTitle("Show apps on home")
                .setMultiChoiceItems(labels, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override public void onClick(DialogInterface d, int which, boolean isChecked) {
                        String pkg = allApps.get(which).pkg;
                        Set<String> current = hiddenApps();
                        if (isChecked) current.remove(pkg);
                        else current.add(pkg);
                        prefs.edit().putStringSet("hidden_apps", current).apply();
                    }
                })
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) { loadApps(); }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss(DialogInterface d) { loadApps(); }
        });
        dialog.show();
    }

    private GradientDrawable wallpaperDrawable(int index) {
        int[][] palettes = {
                {0xFF071B33, 0xFF4C6A92}, {0xFF101A35, 0xFF244D73}, {0xFF24133F, 0xFF7B4C9A},
                {0xFF311B2B, 0xFFF29A58}, {0xFF54233D, 0xFFEA6A57}, {0xFF3B1E2D, 0xFFDA8B45},
                {0xFF061F1B, 0xFF27665A}, {0xFF071D2B, 0xFF145A72}, {0xFF202A20, 0xFF65734A},
                {0xFF080F24, 0xFF34466D}, {0xFF170D2A, 0xFF563B86}, {0xFF090D18, 0xFF303B50},
                {0xFF000000, 0xFF000000}, {0xFF06152E, 0xFF0B2B55}, {0xFF101010, 0xFF292929}
        };
        int safe = index < 0 || index >= palettes.length ? 0 : index;
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, palettes[safe]);
        drawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        return drawable;
    }

    private static final int PHOTO_WALLPAPER_REQUEST = 732;

    private void choosePhotoWallpaper() {
        try {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            pick.setType("image/*");
            pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(pick, PHOTO_WALLPAPER_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "This device has no compatible image picker.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PHOTO_WALLPAPER_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) { }
            prefs.edit().putString("custom_wallpaper_uri", uri.toString()).apply();
            Drawable drawable = loadCustomWallpaper();
            if (drawable != null && root != null) {
                root.setBackgroundDrawable(drawable);
                Toast.makeText(this, "Photo wallpaper applied.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Could not load that image. Try another picture.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private Drawable loadCustomWallpaper() {
        String value = prefs.getString("custom_wallpaper_uri", null);
        if (value == null || value.length() == 0) return null;
        java.io.InputStream stream = null;
        try {
            Uri uri = Uri.parse(value);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            stream = getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(stream, null, bounds);
            if (stream != null) stream.close();
            stream = null;
            int targetW = Math.max(1, getResources().getDisplayMetrics().widthPixels);
            int targetH = Math.max(1, getResources().getDisplayMetrics().heightPixels);
            int sample = 1;
            while (bounds.outWidth / (sample * 2) >= targetW
                    && bounds.outHeight / (sample * 2) >= targetH) sample *= 2;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            stream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(stream, null, options);
            if (bitmap == null) return null;
            BitmapDrawable drawable = new BitmapDrawable(getResources(), bitmap);
            drawable.setGravity(Gravity.FILL);
            return drawable;
        } catch (Exception e) {
            return null;
        } finally {
            if (stream != null) try { stream.close(); } catch (Exception ignored) { }
        }
    }

    private void applyThemePreset() {
        int next = (prefs.getInt("theme_preset", 0) + 1) % 3;
        int[] wallpapers = {3, 7, 2};
        prefs.edit().putInt("theme_preset", next)
                .putInt("wallpaper", wallpapers[next])
                .remove("custom_wallpaper_uri").apply();
        wallpaperIndex = wallpapers[next];
        if (root != null) root.setBackgroundDrawable(wallpaperDrawable(wallpaperIndex));
    }

    private String themePresetLabel() {
        int theme = prefs.getInt("theme_preset", 0);
        return "Theme preset: " + (theme == 0 ? "Ember" : (theme == 1 ? "Ocean" : "Violet"));
    }

    private void applyWallpaper() {
        wallpaperIndex = Math.max(0, Math.min(WALLPAPER_NAMES.length - 1, wallpaperIndex));
        prefs.edit().putInt("wallpaper", wallpaperIndex).remove("custom_wallpaper_uri").apply();
        if (root != null) root.setBackgroundDrawable(wallpaperDrawable(wallpaperIndex));
    }

    private void showWallpaperGallery() {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Holo_NoActionBar_Fullscreen);
        final FrameLayout stage = new FrameLayout(this);
        final LinearLayout content = box(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(40), dp(30), dp(40), dp(30));
        stage.addView(content, new FrameLayout.LayoutParams(MATCH, MATCH));

        final TextView category = tv("", 16, AMBER);
        category.setGravity(Gravity.CENTER);
        final TextView name = tv("", 34, FG);
        name.setGravity(Gravity.CENTER);
        final TextView hint = tv("LEFT / RIGHT: browse     UP / DOWN: category     OK: apply     BACK: close",
                13, SOFT);
        hint.setGravity(Gravity.CENTER);
        content.addView(category, new LinearLayout.LayoutParams(MATCH, WRAP));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(MATCH, 0, 1f);
        content.addView(name, nameParams);
        content.addView(hint, new LinearLayout.LayoutParams(MATCH, WRAP));

        final Runnable refresh = new Runnable() {
            @Override public void run() {
                int cat = wallpaperIndex / 3;
                category.setText(WALLPAPER_CATEGORIES[cat] + "  •  " + (wallpaperIndex % 3 + 1) + " / 3");
                name.setText(WALLPAPER_NAMES[wallpaperIndex]);
                stage.setBackgroundDrawable(wallpaperDrawable(wallpaperIndex));
            }
        };
        refresh.run();
        dialog.setContentView(stage);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override public boolean onKey(DialogInterface d, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
                int categoryIndex = wallpaperIndex / 3;
                int within = wallpaperIndex % 3;
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    wallpaperIndex = categoryIndex * 3 + (within + 2) % 3;
                    refresh.run();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    wallpaperIndex = categoryIndex * 3 + (within + 1) % 3;
                    refresh.run();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    int next = (categoryIndex + (keyCode == KeyEvent.KEYCODE_DPAD_UP ? 4 : 1)) % 5;
                    wallpaperIndex = next * 3 + within;
                    refresh.run();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    applyWallpaper();
                    dialog.dismiss();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss();
                    return true;
                }
                return true;
            }
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(MATCH, MATCH);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }
        stage.setFocusableInTouchMode(true);
        stage.requestFocus();
    }

    private String iconSizeLabel() {
        int size = prefs.getInt("icon_size", 1);
        return "Icon size: " + (size == 0 ? "Small" : (size == 2 ? "Large" : "Medium"));
    }

    private void startAerialSetup() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            ArrayList<String> missing = new ArrayList<String>();
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                if (checkSelfPermission("android.permission.READ_MEDIA_VIDEO") != PackageManager.PERMISSION_GRANTED)
                    missing.add("android.permission.READ_MEDIA_VIDEO");
                if (checkSelfPermission("android.permission.READ_MEDIA_IMAGES") != PackageManager.PERMISSION_GRANTED)
                    missing.add("android.permission.READ_MEDIA_IMAGES");
                if (android.os.Build.VERSION.SDK_INT >= 34
                        && checkSelfPermission("android.permission.READ_MEDIA_VISUAL_USER_SELECTED") != PackageManager.PERMISSION_GRANTED)
                    missing.add("android.permission.READ_MEDIA_VISUAL_USER_SELECTED");
            } else if (checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
                missing.add("android.permission.READ_EXTERNAL_STORAGE");
            }
            if (!missing.isEmpty()) {
                requestPermissions(missing.toArray(new String[missing.size()]), MEDIA_PERMISSION_REQUEST);
                return;
            }
        }
        openDreamSettings();
    }

    private void openDreamSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS));
            Toast.makeText(this, "Choose EmberHome Aerial. Media folder: Movies/EmberHome/Aerial",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "This device does not expose Android screensaver settings. Fire TV support may vary.",
                    Toast.LENGTH_LONG).show();
            openSystemSettings();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MEDIA_PERMISSION_REQUEST) {
            boolean granted = false;
            for (int result : grantResults) if (result == PackageManager.PERMISSION_GRANTED) granted = true;
            if (granted || grantResults.length == 0) openDreamSettings();
            else Toast.makeText(this, "Allow media access to use your local Aerial videos and pictures.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showDeviceInfo() {
        String maker = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
        String kind = DeviceProfile.isAmazonFireTv() ? "Amazon Fire TV detected" : "Android device";
        String ram = DeviceProfile.isLowRamDevice(this) ? "Low-RAM profile" : "Standard memory profile";
        String info = maker + "\\n" + kind + "\\n" + ram
                + "\\nAndroid API: " + android.os.Build.VERSION.SDK_INT
                + "\\nResponsive grid columns: " + DeviceProfile.gridColumns(this)
                + "\\nAerial media folder: Movies/EmberHome/Aerial";
        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                .setTitle("Device compatibility")
                .setMessage(info)
                .setPositiveButton("OK", null)
                .show();
    }

    // ================================================================ settings

    private void showSettings() {
        if (settingsDlg != null && settingsDlg.isShowing()) return;
        final Dialog d = new Dialog(this, android.R.style.Theme_Holo_Dialog_NoActionBar);
        settingsDlg = d;
        LinearLayout p = box(LinearLayout.VERTICAL);
        p.setPadding(dp(32), dp(26), dp(32), dp(28));
        GradientDrawable pb = new GradientDrawable();
        pb.setColor(0xFF111111);
        pb.setCornerRadius(dp(20));
        pb.setStroke(dp(1), LINE);
        p.setBackground(pb);

        p.addView(tv("Settings", 24, FG));
        p.addView(sectionLabel("Customisation mode"));
        final boolean advanced = prefs.getBoolean("advanced_settings", false);
        p.addView(chip(advanced ? "Advanced control panel  •  ON" : "Simple settings  •  ON", new Runnable() {
            @Override public void run() {
                prefs.edit().putBoolean("advanced_settings", !advanced).apply();
                d.dismiss();
                showSettings();
            }
        }));
        p.addView(sectionLabel("Style"));
        LinearLayout styleRow = box(LinearLayout.HORIZONTAL);
        TextView selected = null;
        for (int i = 0; i < STYLES.length; i++) {
            final int idx = i;
            TextView c = chip(STYLES[i], new Runnable() {
                @Override public void run() {
                    d.dismiss();
                    if (idx != style) {
                        style = idx;
                        prefs.edit().putInt("style", idx).apply();
                        buildUi();
                    }
                }
            });
            if (i == style) {
                GradientDrawable on = new GradientDrawable();
                on.setColor(AMBER);
                on.setCornerRadius(dp(30));
                c.setBackground(on);
                c.setTextColor(0xFF0A0A0A);
                selected = c;
            }
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(WRAP, WRAP);
            cp.rightMargin = dp(10);
            styleRow.addView(c, cp);
        }
        p.addView(styleRow);

        p.addView(sectionLabel("Personalisation"));
        LinearLayout personal = box(LinearLayout.HORIZONTAL);
        TextView wallpaperButton = chip("Wallpapers", new Runnable() {
            @Override public void run() { d.dismiss(); showWallpaperGallery(); }
        });
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(WRAP, WRAP);
        wp.rightMargin = dp(10);
        personal.addView(wallpaperButton, wp);
        personal.addView(chip("Manage apps", new Runnable() {
            @Override public void run() { d.dismiss(); showAppManager(); }
        }));
        personal.addView(chip("My photo", new Runnable() {
            @Override public void run() { d.dismiss(); choosePhotoWallpaper(); }
        }));
        p.addView(personal);

        p.addView(sectionLabel("Aerial screensaver"));
        p.addView(chip("Set up Aerial screensaver", new Runnable() {
            @Override public void run() { d.dismiss(); startAerialSetup(); }
        }));

        if (advanced) {
            p.addView(sectionLabel("Advanced appearance"));
            LinearLayout appearance = box(LinearLayout.HORIZONTAL);
            appearance.addView(chip(showAppLabels() ? "App labels: On" : "App labels: Off", new Runnable() {
                @Override public void run() {
                    prefs.edit().putBoolean("show_labels", !showAppLabels()).apply();
                    d.dismiss(); buildUi(); showSettings();
                }
            }));
            TextView iconSize = chip(iconSizeLabel(), new Runnable() {
                @Override public void run() {
                    int next = (prefs.getInt("icon_size", 1) + 1) % 3;
                    prefs.edit().putInt("icon_size", next).apply();
                    d.dismiss(); buildUi(); showSettings();
                }
            });
            LinearLayout.LayoutParams sizeParams = new LinearLayout.LayoutParams(WRAP, WRAP);
            sizeParams.leftMargin = dp(8);
            appearance.addView(iconSize, sizeParams);
            p.addView(appearance);
            p.addView(chip(themePresetLabel(), new Runnable() {
                @Override public void run() {
                    applyThemePreset();
                    d.dismiss(); showSettings();
                }
            }));

            p.addView(chip(motionEnabled() ? "Motion effects: On" : "Motion effects: Off", new Runnable() {
                @Override public void run() {
                    prefs.edit().putBoolean("motion", !motionEnabled()).apply();
                    d.dismiss(); buildUi(); showSettings();
                }
            }));
            p.addView(chip("Device compatibility info", new Runnable() {
                @Override public void run() { showDeviceInfo(); }
            }));
        }

        p.addView(sectionLabel("Weather"));
        LinearLayout wxRow = box(LinearLayout.HORIZONTAL);
        wxRow.setGravity(Gravity.CENTER_VERTICAL);
        final TextView cityValue = tv(cityLabel(), 16, SOFT);
        LinearLayout.LayoutParams cvp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        wxRow.addView(cityValue, cvp);
        final TextView units = chip(useF() ? "Fahrenheit" : "Celsius", new Runnable() { @Override public void run() { } });
        units.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                prefs.edit().putString("units", useF() ? "C" : "F").apply();
                ((TextView) v).setText(useF() ? "Fahrenheit" : "Celsius");
                updateWx();
            }
        });
        TextView change = chip("Change city", new Runnable() {
            @Override public void run() { askCity(cityValue); }
        });
        LinearLayout.LayoutParams chp = new LinearLayout.LayoutParams(WRAP, WRAP);
        chp.rightMargin = dp(10);
        wxRow.addView(change, chp);
        wxRow.addView(units);
        p.addView(wxRow);

        p.addView(sectionLabel("More"));
        LinearLayout more = box(LinearLayout.HORIZONTAL);
        TextView sys = chip("TV settings", new Runnable() {
            @Override public void run() { d.dismiss(); openSystemSettings(); }
        });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(WRAP, WRAP);
        sp.rightMargin = dp(10);
        more.addView(sys, sp);
        more.addView(chip("Close", new Runnable() { @Override public void run() { d.dismiss(); } }));
        p.addView(more);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(p);
        d.setContentView(scroll);
        if (d.getWindow() != null) {
            d.getWindow().setLayout(dp(700), WRAP);
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        d.show();
        if (selected != null) selected.requestFocus();
    }

    private TextView sectionLabel(String s) {
        TextView t = tv(s, 13, DIM);
        t.setPadding(0, dp(22), 0, dp(10));
        return t;
    }

    private boolean useF() { return "F".equals(prefs.getString("units", "C")); }

    private String cityLabel() {
        String c = prefs.getString("city", "");
        return c.length() == 0 ? "Automatic (from your internet connection)" : c;
    }

    private void askCity(final TextView valueView) {
        final EditText in = new EditText(this);
        in.setSingleLine(true);
        in.setHint("City, e.g. Nairobi (empty = automatic)");
        in.setText(prefs.getString("city", ""));
        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                .setTitle("Weather city")
                .setView(in)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface di, int w) {
                        prefs.edit().putString("city", in.getText().toString().trim()).apply();
                        valueView.setText(cityLabel());
                        wxState = 0;
                        updateWx();
                        fetchWeather();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ================================================================ weather (wttr.in)

    private String wxMain() {
        if (wxState == 1) return (useF() ? wxTempF : wxTempC) + "\u00b0   " + wxDesc;
        return wxState == 2 ? "Weather unavailable" : "Loading weather\u2026";
    }

    private String wxDetail() {
        if (wxState != 1) return "";
        String s = "H " + (useF() ? wxHiF : wxHiC) + "\u00b0    L " + (useF() ? wxLoF : wxLoC) + "\u00b0";
        if (wxPlace.length() > 0) s += "    \u00b7    " + wxPlace;
        return s;
    }

    private void updateWx() {
        for (TextView t : wxViews) {
            int kind = ((Integer) t.getTag()).intValue();
            t.setText(kind == 0 ? wxMain() : wxDetail());
        }
    }

    private boolean parseWeather(String s) {
        try {
            JSONObject o = new JSONObject(s);
            JSONObject c = o.getJSONArray("current_condition").getJSONObject(0);
            JSONObject w = o.getJSONArray("weather").getJSONObject(0);
            wxTempC = Integer.parseInt(c.getString("temp_C"));
            wxTempF = Integer.parseInt(c.getString("temp_F"));
            wxDesc = c.getJSONArray("weatherDesc").getJSONObject(0).getString("value").trim();
            wxHiC = Integer.parseInt(w.getString("maxtempC"));
            wxLoC = Integer.parseInt(w.getString("mintempC"));
            wxHiF = Integer.parseInt(w.getString("maxtempF"));
            wxLoF = Integer.parseInt(w.getString("mintempF"));
            wxPlace = "";
            try {
                JSONArray na = o.getJSONArray("nearest_area");
                wxPlace = na.getJSONObject(0).getJSONArray("areaName").getJSONObject(0).getString("value").trim();
            } catch (Exception ignored) { }
            wxState = 1;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String httpGet(String u) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(12000);
        c.setRequestProperty("User-Agent", "EmberHome/1.0");
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        } finally {
            c.disconnect();
        }
    }

    private void fetchWeather() {
        final String city = prefs.getString("city", "");
        wxFetched = System.currentTimeMillis();
        new AsyncTask<Void, Void, String>() {
            @Override protected String doInBackground(Void... v) {
                String path;
                try { path = city.length() == 0 ? "" : URLEncoder.encode(city, "UTF-8").replace("+", "%20"); }
                catch (Exception e) { path = ""; }
                String[] urls = {"http://wttr.in/" + path + "?format=j1", "https://wttr.in/" + path + "?format=j1"};
                for (String u : urls) {
                    try {
                        String s = httpGet(u).trim();
                        if (s.startsWith("{")) return s;
                    } catch (Exception ignored) { }
                }
                return null;
            }
            @Override protected void onPostExecute(String s) {
                if (s != null && parseWeather(s)) prefs.edit().putString("wx", s).apply();
                else if (wxState != 1) wxState = 2;
                if (wxState == 2) wxFetched = 0;
                updateWx();
            }
        }.execute();
    }

    // ================================================================ apps

    private void loadApps() {
        new AsyncTask<Void, Void, List<AppEntry>>() {
            @Override protected List<AppEntry> doInBackground(Void... v) { return queryApps(); }
            @Override protected void onPostExecute(List<AppEntry> r) {
                allApps.clear();
                allApps.addAll(r);
                applySavedAppOrder(allApps);
                Set<String> hidden = hiddenApps();
                apps.clear();
                for (AppEntry app : allApps) {
                    if (!hidden.contains(app.pkg)) apps.add(app);
                }
                if (editingIndex >= apps.size()) {
                    editingIndex = -1;
                    editMode = false;
                }
                populate();
            }
        }.execute();
    }

    private List<AppEntry> queryApps() {
        PackageManager pm = getPackageManager();
        Map<String, AppEntry> map = new LinkedHashMap<String, AppEntry>();
        String[] cats = {Intent.CATEGORY_LAUNCHER, "android.intent.category.LEANBACK_LAUNCHER"};
        for (String cat : cats) {
            Intent q = new Intent(Intent.ACTION_MAIN);
            q.addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                if (ri.activityInfo == null) continue;
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName())) continue;
                ComponentName cn = new ComponentName(pkg, ri.activityInfo.name);
                String key = cn.flattenToString();
                if (map.containsKey(key)) continue;
                AppEntry e = new AppEntry();
                e.component = cn;
                e.pkg = pkg;
                CharSequence l = ri.loadLabel(pm);
                e.label = l != null ? l.toString() : pkg;
                e.icon = ri.loadIcon(pm);
                map.put(key, e);
            }
        }
        List<AppEntry> out = new ArrayList<AppEntry>(map.values());
        Collections.sort(out, new Comparator<AppEntry>() {
            @Override public int compare(AppEntry a, AppEntry b) { return a.label.compareToIgnoreCase(b.label); }
        });
        return out;
    }

    private void launch(AppEntry e) {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setComponent(e.component);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try { startActivity(i); } catch (ActivityNotFoundException ex) { loadApps(); }
        catch (SecurityException ex) { }
    }

    private Set<String> favSet() {
        return new HashSet<String>(prefs.getStringSet("favs", new HashSet<String>()));
    }

    private void menuFor(final AppEntry e) {
        final boolean fav = favSet().contains(e.pkg);
        String[] items = {fav ? "Remove from favorites" : "Add to favorites", "App info / uninstall"};
        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                .setTitle(e.label)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface di, int which) {
                        if (which == 0) {
                            Set<String> s = favSet();
                            if (fav) s.remove(e.pkg); else s.add(e.pkg);
                            prefs.edit().putStringSet("favs", s).apply();
                            if (style == 1) populate();
                        } else {
                            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", e.pkg, null));
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            try { startActivity(i); } catch (Exception ignored) { }
                        }
                    }
                }).show();
    }

    private void openSystemSettings() {
        Intent i = new Intent(Settings.ACTION_SETTINGS);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(i); } catch (Exception ignored) { }
    }
}
