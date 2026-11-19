package com.idrees.emberhome;

import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.FrameLayout;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * KitKat-compatible port of the Thiago OS launcher experience.
 * Deliberately uses the Android View/Canvas APIs available on API 19;
 * no Compose, AndroidX, DataStore, profile installer or network requests.
 */
public class ThiagoActivity extends Activity {
    private ThiagoView ui;
    private final ArrayList<AppItem> apps = new ArrayList<AppItem>();
    private final HashSet<String> favourites = new HashSet<String>();
    private final ArrayList<String> appOrder = new ArrayList<String>();
    private final HashMap<String, Bitmap> icons = new HashMap<String, Bitmap>();
    private SharedPreferences prefs;
    private int page = 0;             // 0 home, 1 settings
    private int zone = 0;             // 0 nav, 1 featured actions, 2 favourites, 3 all apps, 4 settings
    private int nav = 0, selected = 0, grid = 0, setting = 0, menuIndex = 0, ccIndex = 0, featuredAction = 0;
    private boolean drawer = false, control = false, context = false;
    private AppItem contextApp;
    private boolean longHandled = false;
    private int volumePct = 50, brightnessPct = 50;
    private String toast = "";
    private long toastUntil = 0;

    private static final String[] SETTINGS = {
        "Network & internet", "Display & picture", "Sound", "Bluetooth & devices",
        "Cast & screen mirroring", "Inputs / HDMI source", "Apps", "Date & time",
        "Language", "Android settings", "About EmberHome"
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(0x00000400 | 0x00000100 | 0x00000200);
        prefs = getSharedPreferences("ember_thiago", MODE_PRIVATE);
        Set<String> saved = prefs.getStringSet("favourites", null);
        if (saved != null) favourites.addAll(saved);
        String savedOrder = prefs.getString("order", "");
        if (savedOrder.length() > 0) appOrder.addAll(Arrays.asList(savedOrder.split("\n")));
        zone = 2;
        ui = new ThiagoView(this);
        setContentView(ui);
        refreshApps();
    }

    @Override protected void onResume() {
        super.onResume();
        if (ui != null) { refreshApps(); ui.invalidate(); }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        page = 0; drawer = false; control = false; context = false; zone = 2;
        if (ui != null) ui.invalidate();
    }

    private void refreshApps() {
        PackageManager pm = getPackageManager();
        LinkedHashMap<String, AppItem> found = new LinkedHashMap<String, AppItem>();
        String[] cats = { Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER };
        for (String cat : cats) {
            Intent query = new Intent(Intent.ACTION_MAIN);
            query.addCategory(cat);
            List<ResolveInfo> list = pm.queryIntentActivities(query, 0);
            for (ResolveInfo ri : list) {
                if (ri.activityInfo == null) continue;
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || found.containsKey(pkg)) continue;
                if (!ri.activityInfo.enabled || !ri.activityInfo.applicationInfo.enabled) continue;
                AppItem item = new AppItem();
                item.pkg = pkg;
                item.cls = ri.activityInfo.name;
                item.label = String.valueOf(ri.loadLabel(pm));
                item.tv = Intent.CATEGORY_LEANBACK_LAUNCHER.equals(cat);
                item.icon = ri.loadIcon(pm);
                found.put(pkg, item);
            }
        }
        apps.clear();
        apps.addAll(found.values());
        Collections.sort(apps, new Comparator<AppItem>() {
            public int compare(AppItem a, AppItem b) {
                boolean af = favourites.contains(a.pkg), bf = favourites.contains(b.pkg);
                if (af != bf) return af ? -1 : 1;
                int ai = appOrder.indexOf(a.pkg), bi = appOrder.indexOf(b.pkg);
                if (ai >= 0 || bi >= 0) { if (ai < 0) return 1; if (bi < 0) return -1; if (ai != bi) return ai - bi; }
                return Collator.getInstance().compare(a.label, b.label);
            }
        });
        if (selected >= apps.size()) selected = Math.max(0, apps.size() - 1);
        if (grid >= apps.size()) grid = Math.max(0, apps.size() - 1);
        if (ui != null) ui.invalidate();
    }

    private void launch(AppItem a) {
        if (a == null) return;
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(a.tv ? Intent.CATEGORY_LEANBACK_LAUNCHER : Intent.CATEGORY_LAUNCHER);
            i.setComponent(new ComponentName(a.pkg, a.cls));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
        } catch (Exception e) {
            try {
                Intent i = getPackageManager().getLaunchIntentForPackage(a.pkg);
                if (i != null) startActivity(i); else showToast("Unable to open " + a.label);
            } catch (Exception ignored) { showToast("Unable to open " + a.label); }
        }
    }

    private void toggleFavourite(AppItem a) {
        if (a == null) return;
        if (favourites.contains(a.pkg)) favourites.remove(a.pkg); else favourites.add(a.pkg);
        prefs.edit().putStringSet("favourites", new HashSet<String>(favourites)).commit();
        String pkg = a.pkg;
        refreshApps();
        for (int i=0;i<apps.size();i++) if (apps.get(i).pkg.equals(pkg)) { selected=i; grid=i; break; }
        showToast(favourites.contains(pkg) ? "Added to favourites" : "Removed from favourites");
    }

    private void moveApp(AppItem a, int dir) {
        if (a == null) return;
        ArrayList<AppItem> group = new ArrayList<AppItem>();
        for (AppItem item : apps) if (favourites.contains(item.pkg) == favourites.contains(a.pkg)) group.add(item);
        int at = group.indexOf(a), next = at + dir;
        if (at < 0 || next < 0 || next >= group.size()) { showToast("Can't move further"); return; }
        AppItem other = group.get(next);
        int x = apps.indexOf(a), y = apps.indexOf(other);
        Collections.swap(apps, x, y);
        appOrder.clear();
        for (AppItem item : apps) appOrder.add(item.pkg);
        StringBuilder order = new StringBuilder();
        for (AppItem item : apps) order.append(item.pkg).append("\n");
        prefs.edit().putString("order", order.toString()).commit();
        showToast("App order changed");
        ui.invalidate();
    }

    private void showToast(String text) {
        toast = text; toastUntil = System.currentTimeMillis() + 1800;
        if (ui != null) ui.invalidate();
    }

    private void openContext(AppItem a) {
        if (a == null) return;
        contextApp = a; context = true; menuIndex = 0; ui.invalidate();
    }

    private void openSettingsIntent(String action) {
        try { startActivity(new Intent(action)); }
        catch (Exception e) { try { startActivity(new Intent(Settings.ACTION_SETTINGS)); } catch (Exception ignored) { showToast("Setting unavailable on this TV"); } }
    }

    private void settingAction(int index) {
        switch(index) {
            case 0: openSettingsIntent(Settings.ACTION_WIFI_SETTINGS); break;
            case 1: openSettingsIntent(Settings.ACTION_DISPLAY_SETTINGS); break;
            case 2: openSettingsIntent(Settings.ACTION_SOUND_SETTINGS); break;
            case 3: openSettingsIntent(Settings.ACTION_BLUETOOTH_SETTINGS); break;
            case 4: openSettingsIntent("android.settings.CAST_SETTINGS"); break;
            case 5:
                try { startActivity(new Intent("com.android.tv.action.VIEW_INPUTS")); }
                catch(Exception e) { openSettingsIntent(Settings.ACTION_SETTINGS); }
                break;
            case 6: openSettingsIntent(Settings.ACTION_APPLICATION_SETTINGS); break;
            case 7: openSettingsIntent(Settings.ACTION_DATE_SETTINGS); break;
            case 8: showToast("Language follows your TV's system language"); break;
            case 9: openSettingsIntent(Settings.ACTION_SETTINGS); break;
            default: showToast("EmberHome · Thiago OS-style KitKat edition"); break;
        }
    }

    private void changeVolume(int delta) {
        AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int value = Math.max(0, Math.min(max, am.getStreamVolume(AudioManager.STREAM_MUSIC) + delta));
        am.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0);
        volumePct = max == 0 ? 0 : value * 100 / max;
        ui.invalidate();
    }

    private void changeBrightness(int delta) {
        brightnessPct = Math.max(5, Math.min(100, brightnessPct + delta));
        try { Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightnessPct * 255 / 100);
        } catch (Exception e) { showToast("Brightness control unavailable"); }
        ui.invalidate();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        boolean ok = k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_NUMPAD_ENTER || k == KeyEvent.KEYCODE_BUTTON_A;
        if (ok && e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() > 0 && !longHandled) {
            longHandled = true;
            if (control) { control = false; }
            else if (context) { context = false; }
            else if (page == 0) openContext(drawer ? appAt(grid) : appAt(selected));
            ui.invalidate();
            return true;
        }
        if (ok && e.getAction() == KeyEvent.ACTION_UP) {
            if (!longHandled) handleKey("ok");
            longHandled = false; ui.invalidate(); return true;
        }
        if (k == KeyEvent.KEYCODE_BACK || k == KeyEvent.KEYCODE_ESCAPE) {
            if (e.getAction() == KeyEvent.ACTION_UP) { handleKey("back"); if (ui != null) ui.invalidate(); }
            return true;
        }
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            switch(k) {
                case KeyEvent.KEYCODE_DPAD_UP: handleKey("up"); return true;
                case KeyEvent.KEYCODE_DPAD_DOWN: handleKey("down"); return true;
                case KeyEvent.KEYCODE_DPAD_LEFT: handleKey("left"); return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT: handleKey("right"); return true;
                case KeyEvent.KEYCODE_MENU: openContext(drawer ? appAt(grid) : appAt(selected)); return true;
                case KeyEvent.KEYCODE_VOLUME_UP: changeVolume(1); return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN: changeVolume(-1); return true;
            }
        }
        return super.dispatchKeyEvent(e);
    }

    private AppItem appAt(int i) { return i >= 0 && i < apps.size() ? apps.get(i) : null; }

    private void handleKey(String key) {
        if (context) {
            if ("up".equals(key)) menuIndex = Math.max(0, menuIndex-1);
            else if ("down".equals(key)) menuIndex = Math.min(3, menuIndex+1);
            else if ("back".equals(key)) context = false;
            else if ("ok".equals(key)) {
                AppItem a = contextApp;
                if (menuIndex == 0) toggleFavourite(a);
                else if (menuIndex == 1) moveApp(a, -1);
                else if (menuIndex == 2) moveApp(a, 1);
                else if (menuIndex == 3 && a != null) {
                    try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + a.pkg))); }
                    catch(Exception ignored) { showToast("App info unavailable"); }
                }
                context = false;
            }
            return;
        }
        if (control) {
            if ("back".equals(key) || "up".equals(key)) control = false;
            else if ("left".equals(key)) { if (ccIndex == 6) changeVolume(-5); else if (ccIndex == 7) changeBrightness(-5); else ccIndex = Math.max(0,ccIndex-1); }
            else if ("right".equals(key)) { if (ccIndex == 6) changeVolume(5); else if (ccIndex == 7) changeBrightness(5); else ccIndex = Math.min(7,ccIndex+1); }
            else if ("down".equals(key)) ccIndex = Math.min(7,ccIndex+2);
            else if ("ok".equals(key)) { if(ccIndex==0) settingAction(0); else if(ccIndex==1) settingAction(3); else if(ccIndex==2) settingAction(4); else if(ccIndex==3) settingAction(5); else if(ccIndex==4) settingAction(9); else if(ccIndex==5) {control=false;page=1;zone=4;} }
            return;
        }
        if (page == 1) {
            if ("up".equals(key)) setting = Math.max(0, setting-1);
            else if ("down".equals(key)) setting = Math.min(SETTINGS.length-1, setting+1);
            else if ("ok".equals(key) || "right".equals(key)) settingAction(setting);
            else if ("back".equals(key) || "left".equals(key)) { page=0; zone=2; }
            return;
        }
        if (drawer) {
            if ("left".equals(key)) grid = Math.max(0,grid-1);
            else if ("right".equals(key)) grid = Math.min(apps.size()-1,grid+1);
            else if ("up".equals(key)) { if (grid < 8) { drawer=false; zone=2; selected=Math.min(7,grid); } else grid=Math.max(0,grid-8); }
            else if ("down".equals(key)) grid = Math.min(apps.size()-1,grid+8);
            else if ("ok".equals(key)) launch(appAt(grid));
            else if ("back".equals(key)) drawer=false;
            return;
        }
        if ("back".equals(key)) { if(control) control=false; else if(page==1) page=0; else zone=2; }
        else if ("left".equals(key)) {
            if (zone==0) nav=Math.max(0,nav-1);
            else if (zone==1) zone=0;
            else if (zone==2) selected=Math.max(0,selected-1);
        } else if ("right".equals(key)) {
            if (zone==0) { if(nav<2) nav++; else {control=true;ccIndex=0;} }
            else if (zone==1) zone=1;
            else if (zone==2) selected=Math.min(Math.min(7,apps.size()-1),selected+1);
        } else if ("up".equals(key)) {
            if (zone==2) zone=1; else if(zone==1) zone=0; else if(zone==0) zone=0;
        } else if ("down".equals(key)) {
            if (zone==0) { if(nav==2) {page=1;setting=0;} else zone=1; }
            else if(zone==1 || zone==2) { drawer=true;grid=Math.min(selected,Math.max(0,apps.size()-1)); }
        } else if ("ok".equals(key)) {
            if (zone==0) {
                if(nav==0) { page=0;drawer=false;zone=2; }
                else if(nav==1) {
                    try { Intent i=getPackageManager().getLaunchIntentForPackage("com.android.vending"); if(i!=null)startActivity(i);else showToast("Play Store not found"); }
                    catch(Exception ex){showToast("Play Store not found");}
                } else { page=1;setting=0; }
            } else if (zone==1) { if(featuredAction==0) launch(appAt(selected)); else toggleFavourite(appAt(selected)); }
            else if (zone==2) launch(appAt(selected));
        }
    }

    private class AppItem {
        String pkg, cls, label;
        boolean tv;
        Drawable icon;
    }

    private class ThiagoView extends View {
        private final Paint p = new Paint(3);
        private final Paint text = new Paint(3);
        private final RectF r = new RectF();
        private final int ink = Color.rgb(14,18,32), muted = Color.rgb(74,82,104), accent = Color.rgb(79,91,213);
        private final int[] tileColors = {0xff5b7cfa,0xffef8d61,0xff47b99b,0xffa879d7,0xffdf6d8a,0xffd2a84d};
        private float sx, sy;
        ThiagoView(Context c) { super(c); setFocusable(true); setFocusableInTouchMode(true); setLayerType(View.LAYER_TYPE_HARDWARE, null); }
        private void box(Canvas c,float x,float y,float w,float h,float rad,int color) {
            p.setColor(color); p.setStyle(Paint.Style.FILL); c.drawRoundRect(x,y,x+w,y+h,rad,rad,p);
        }
        private void stroke(Canvas c,float x,float y,float w,float h,float rad,int color,float sw) {
            p.setColor(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(sw); c.drawRoundRect(x,y,x+w,y+h,rad,rad,p); p.setStyle(Paint.Style.FILL);
        }
        private void txt(Canvas c,String s,float x,float y,float size,int color,boolean bold) {
            text.setColor(color); text.setTextSize(size); text.setTypeface(bold?Typeface.create("sans-serif",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL)); text.setTextAlign(Paint.Align.LEFT); text.setStyle(Paint.Style.FILL); c.drawText(s,x,y,text);
        }
        private void center(Canvas c,String s,float x,float y,float size,int color,boolean bold) {
            text.setColor(color); text.setTextSize(size); text.setTypeface(bold?Typeface.create("sans-serif",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL)); text.setTextAlign(Paint.Align.CENTER); c.drawText(s,x,y,text); text.setTextAlign(Paint.Align.LEFT);
        }
        private void pill(Canvas c,float x,float y,float w,float h,boolean focus) {
            box(c,x,y,w,h,26,0x99ffffff); stroke(c,x,y,w,h,26,focus?accent:0x88ffffff,focus?3:1);
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            sx=getWidth()/1280f; sy=getHeight()/720f;
            canvas.save(); canvas.scale(sx,sy);
            wallpaper(canvas);
            if(page==1) drawSettings(canvas); else if(drawer) drawDrawer(canvas); else drawHome(canvas);
            drawTop(canvas);
            if(control) drawControl(canvas);
            if(context) drawContext(canvas);
            if(System.currentTimeMillis()<toastUntil) drawToast(canvas);
            canvas.restore();
        }
        private void wallpaper(Canvas c) {
            p.setShader(new LinearGradient(0,0,1280,720, new int[]{0xffe9e6ff,0xffddebff,0xffe4f7d2,0xffc9f06a},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,1280,720,p); p.setShader(null);
            p.setShader(new RadialGradient(180,120,390,new int[]{0x99b9a8ff,0x00b9a8ff},null,Shader.TileMode.CLAMP)); c.drawRect(0,0,600,520,p); p.setShader(null);
            p.setShader(new RadialGradient(1090,180,430,new int[]{0x88b6f04a,0x00b6f04a},null,Shader.TileMode.CLAMP)); c.drawRect(650,0,1280,620,p); p.setShader(null);
            p.setShader(new RadialGradient(650,690,430,new int[]{0x777fe3ff,0x007fe3ff},null,Shader.TileMode.CLAMP)); c.drawRect(200,250,1100,720,p); p.setShader(null);
            p.setColor(0x44ffffff); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2);
            Path path=new Path(); path.moveTo(-40,570);path.cubicTo(260,410,520,680,830,480);path.cubicTo(1050,340,1180,260,1330,290);c.drawPath(path,p);
            path.reset();path.moveTo(400,760);path.cubicTo(530,450,800,230,1110,160);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
        }
        private void drawTop(Canvas c) {
            txt(c,"Ember",52,61,31,ink,true); txt(c,"HOME",153,61,13,accent,true);
            box(c,470,23,342,55,28,0x99ffffff); stroke(c,470,23,342,55,28,0x99ffffff,1);
            String[] n={"Home","Store","Settings"};
            for(int i=0;i<3;i++){float x=480+i*108; if(nav==i && page==0) box(c,x,29,101,43,23,0xddffffff); center(c,n[i],x+50,57,16,nav==i?ink:muted,true);}
            pill(c,930,23,300,55,control);
            boolean wifi=false;
            try { WifiManager wm=(WifiManager)getSystemService(WIFI_SERVICE); wifi=wm.isWifiEnabled(); } catch(Exception ignored){}
            center(c,(wifi?"●":"○")+"  "+new SimpleDateFormat("EEE d MMM",Locale.getDefault()).format(new Date())+"     "+new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()),1080,57,15,ink,true);
        }
        private void drawHome(Canvas c) {
            AppItem a=appAt(selected);
            txt(c,"YOUR TV. YOUR APPS.",70,132,13,accent,true);
            if(a!=null) {
                drawIcon(c,a,70,164,170);
                txt(c,a.label,275,210,43,ink,true);
                txt(c,category(a),278,244,17,muted,false);
                String desc=description(a);
                txt(c,desc,278,281,17,muted,false);
                box(c,278,316,140,48,25,featuredAction==0 && zone==1?accent:ink); center(c,"OPEN",348,346,16,Color.WHITE,true);
                box(c,432,316,165,48,25,featuredAction==1 && zone==1?0xddffffff:0xbaffffff); stroke(c,432,316,165,48,25,featuredAction==1 && zone==1?accent:0x99ffffff,featuredAction==1 && zone==1?3:1);
                center(c,favourites.contains(a.pkg)?"★ FAVOURITE":"☆ FAVOURITE",514,346,14,ink,true);
            } else {
                txt(c,"Welcome to EmberHome",70,220,42,ink,true);
                txt(c,"Install apps to see them here.",70,260,18,muted,false);
            }
            txt(c,"FAVOURITES",70,420,17,ink,true);
            txt(c,"Your apps, in your order",205,420,13,muted,false);
            for(int i=0;i<Math.min(8,apps.size());i++) {
                float x=70+i*145;
                AppItem item=apps.get(i);
                drawIcon(c,item,x,445,76);
                center(c,ellipsize(item.label,16),x+38,546,13,ink,true);
                if(i==selected && zone==2) stroke(c,x-6,439,88,114,18,accent,3);
            }
            if(apps.isEmpty()) txt(c,"No launchable apps found",70,500,18,muted,false);
            
        }
        private String ellipsize(String s,int n){return s.length()>n?s.substring(0,n-1)+"…":s;}
        private String category(AppItem a) {
            String pkg=a.pkg.toLowerCase(Locale.US);
            if(pkg.contains("youtube")||pkg.contains("netflix")||pkg.contains("disney")||pkg.contains("prime")||pkg.contains("video"))return "Video & entertainment";
            if(pkg.contains("spotify")||pkg.contains("music"))return "Music";
            if(pkg.contains("game"))return "Games";
            if(pkg.contains("settings"))return "System";
            return "Installed application";
        }
        private String description(AppItem a) { return "Open "+a.label+" on your TV."; }
        private void drawDrawer(Canvas c) {
            txt(c,"ALL APPS",70,130,30,ink,true);
            txt(c,"Every app installed on this TV",70,160,15,muted,false);
            int cols=8, start=grid/cols;
            for(int i=0;i<apps.size();i++) {
                int row=i/cols, col=i%cols;
                float x=74+col*148, y=195+row*150;
                if(y>650) continue;
                drawIcon(c,apps.get(i),x,y,76);
                center(c,ellipsize(apps.get(i).label,17),x+38,y+102,13,ink,true);
                if(i==grid) { stroke(c,x-7,y-7,90,120,17,accent,4); box(c,x-3,y+112,82,3,2,accent); }
            }
            if(apps.isEmpty()) txt(c,"No launchable apps found",70,250,18,muted,false);
            txt(c,"Hold OK for favourites, ordering and app information",70,684,12,muted,false);
        }
        private void drawSettings(Canvas c) {
            txt(c,"SETTINGS",70,130,32,ink,true);
            txt(c,"Your TV, configured your way",70,160,15,muted,false);
            box(c,70,188,340,475,28,0x99ffffff);
            for(int i=0;i<SETTINGS.length;i++) {
                float y=205+i*40;
                if(i==setting) box(c,82,y-25,316,36,18,0xddffffff);
                txt(c,SETTINGS[i],102,y,15,i==setting?ink:muted,i==setting);
            }
            box(c,440,188,770,475,28,0x99ffffff);
            txt(c,SETTINGS[setting],485,252,27,ink,true);
            txt(c,settingDescription(setting),485,295,17,muted,false);
            box(c,485,340,260,52,26,0xddffffff);
            center(c,"OPEN SETTINGS",615,373,15,ink,true);
            txt(c,"These shortcuts open the TV's own Android settings.",485,440,14,muted,false);
            txt(c,"EmberHome does not modify or disable system services.",485,466,14,muted,false);
        }
        private String settingDescription(int i) {
            String[] d={"Choose a Wi-Fi network and manage connections.","Open picture, display and screen options.","Manage sound output and volume.","Pair Bluetooth remotes, speakers and accessories.","Open the TV's cast receiver or mirroring settings.","Open the TV's source selector when the firmware provides one.","View installed apps, permissions and storage.","Change the date, time and time zone.","The launcher follows the language configured on Android.","Open the complete Android settings application.","EmberHome · lightweight, ad-free and offline."};
            return d[i];
        }
        private void drawControl(Canvas c) {
            box(c,760,105,455,550,34,0xeef5f7ff); stroke(c,760,105,455,550,34,0xccffffff,2);
            txt(c,"CONTROL CENTRE",800,150,18,ink,true);
            String[] names={"Wi-Fi","Bluetooth","Cast","Source","Settings","Apps"};
            String[] sub={wifiState(),"Open settings","Open cast settings","TV inputs","All settings","Manage apps"};
            for(int i=0;i<6;i++) {
                int col=i%2,row=i/2; float x=795+col*198,y=180+row*112;
                box(c,x,y,178,92,20,0xbaffffff);
                if(i==ccIndex) stroke(c,x,y,178,92,20,accent,3);
                txt(c,names[i],x+17,y+35,16,ink,true);
                txt(c,sub[i],x+17,y+62,12,muted,false);
            }
            txt(c,"VOLUME",800,535,13,muted,true); box(c,900,516,250,18,9,0x55ffffff); box(c,900,516,250*volumePct/100f,18,9,accent);
            txt(c,"BRIGHTNESS",800,592,13,muted,true); box(c,900,573,250,18,9,0x55ffffff); box(c,900,573,250*brightnessPct/100f,18,9,0xff8cae48);
            if(ccIndex>=6) stroke(c,790,506+(ccIndex-6)*57,365,42,16,accent,3);
        }
        private String wifiState() { try { return ((WifiManager)getSystemService(WIFI_SERVICE)).isWifiEnabled()?"On":"Off"; } catch(Exception e){return "Unknown";} }
        private void drawContext(Canvas c) {
            box(c,390,205,500,315,30,0xeeffffff); stroke(c,390,205,500,315,30,0xccffffff,2);
            txt(c,"APP OPTIONS",430,250,16,muted,true);
            String[] opts={favourites.contains(contextApp.pkg)?"Remove from favourites":"Add to favourites","Move left","Move right","App info"};
            for(int i=0;i<4;i++) { float y=286+i*53; if(i==menuIndex) box(c,414,y-28,450,44,22,0xfff0f1fb); txt(c,opts[i],445,y,17,ink,i==menuIndex); }
        }
        private void drawToast(Canvas c) {
            box(c,430,650,420,48,24,0xee141826); center(c,toast,640,680,15,Color.WHITE,true);
        }
        private void drawIcon(Canvas c,AppItem a,float x,float y,float size) {
            box(c,x,y,size,size,Math.min(18,size*.23f),0x22ffffff);
            Bitmap b=iconBitmap(a);
            if(b!=null) { p.setAlpha(255); c.drawBitmap(b,null,new RectF(x,y,x+size,y+size),p); }
            else { box(c,x,y,size,size,16,tileColors[Math.abs(a.pkg.hashCode())%tileColors.length]); center(c,a.label.substring(0,1).toUpperCase(Locale.US),x+size/2,y+size*.64f,size*.48f,Color.WHITE,true); }
        }
        private Bitmap iconBitmap(AppItem a) {
            Bitmap b=icons.get(a.pkg); if(b!=null)return b;
            try {
                int n=128; b=Bitmap.createBitmap(n,n,Bitmap.Config.ARGB_8888);
                Canvas c=new Canvas(b); a.icon.setBounds(8,8,n-8,n-8); a.icon.draw(c);
                icons.put(a.pkg,b); return b;
            } catch(Exception e){return null;}
        }
    }
}