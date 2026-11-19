package com.idrees.emberhome;

import android.service.dreams.DreamService;
import android.os.Environment;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.VideoView;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Color;
import android.graphics.Path;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * EmberHome Aerial dream: plays user-provided MP4 scenes when available,
 * otherwise rotates user-provided landscape images. If no media is installed,
 * a lightweight vector landscape is shown as a safe offline fallback.
 *
 * Place media in Movies/EmberHome/Aerial on devices that expose shared storage.
 */
public class AerialDreamService extends DreamService {
    private final Handler handler = new Handler();
    private final List<File> videos = new ArrayList<File>();
    private final List<File> images = new ArrayList<File>();
    private int imageIndex = 0;
    private VideoView videoView;
    private ImageView imageView;
    private Bitmap currentBitmap;
    private SceneView sceneView;
    private Runnable imageRotation;
    private boolean detached;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);
        detached = false;

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);
        root.setFocusable(true);
        root.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });

        scanMedia();
        if (!videos.isEmpty() && !DeviceProfile.isLowRamDevice(this)) {
            playVideo(root, 0);
        } else if (!images.isEmpty()) {
            showImages(root);
        } else {
            sceneView = new SceneView(this);
            root.addView(sceneView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    private File mediaFolder() {
        return new File(new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), "EmberHome"), "Aerial");
    }

    private void scanMedia() {
        videos.clear();
        images.clear();
        File folder = mediaFolder();
        if (!folder.exists() || !folder.isDirectory()) return;
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (!file.isFile()) continue;
            String name = file.getName().toLowerCase();
            if (name.endsWith(".mp4") || name.endsWith(".m4v") || name.endsWith(".3gp")) {
                videos.add(file);
            } else if (name.endsWith(".jpg") || name.endsWith(".jpeg")
                    || name.endsWith(".png") || name.endsWith(".webp")) {
                images.add(file);
            }
        }
    }

    private void playVideo(final FrameLayout root, final int index) {
        if (detached || videos.isEmpty()) return;
        if (videoView != null) {
            try { videoView.stopPlayback(); } catch (Exception ignored) { }
            root.removeView(videoView);
        }
        videoView = new VideoView(this);
        root.addView(videoView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        videoView.setVideoPath(videos.get(index % videos.size()).getAbsolutePath());
        videoView.setOnCompletionListener(mp -> playVideo(root, (index + 1) % videos.size()));
        videoView.setOnErrorListener((mp, what, extra) -> {
            root.removeView(videoView);
            videoView = null;
            if (!images.isEmpty()) showImages(root);
            else {
                sceneView = new SceneView(AerialDreamService.this);
                root.addView(sceneView, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            return true;
        });
        videoView.setOnPreparedListener(mp -> {
            if (!detached) {
                mp.setLooping(false);
                videoView.start();
            }
        });
    }

    private void showImages(final FrameLayout root) {
        imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(imageView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        showNextImage();
        imageRotation = new Runnable() {
            @Override public void run() {
                if (detached) return;
                showNextImage();
                handler.postDelayed(this, 25000L);
            }
        };
        handler.postDelayed(imageRotation, 25000L);
    }

    private void showNextImage() {
        if (imageView == null || images.isEmpty()) return;
        File file = images.get(imageIndex++ % images.size());
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        int targetW = imageView.getWidth() > 0 ? imageView.getWidth() : 1920;
        int targetH = imageView.getHeight() > 0 ? imageView.getHeight() : 1080;
        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= targetW
                && bounds.outHeight / (sample * 2) >= targetH) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            if (currentBitmap != null && currentBitmap != bitmap && !currentBitmap.isRecycled())
                currentBitmap.recycle();
            currentBitmap = bitmap;
        }
    }

    @Override
    public void onDetachedFromWindow() {
        detached = true;
        if (imageRotation != null) handler.removeCallbacks(imageRotation);
        handler.removeCallbacksAndMessages(null);
        if (videoView != null) {
            try { videoView.stopPlayback(); } catch (Exception ignored) { }
        }
        if (currentBitmap != null && !currentBitmap.isRecycled()) currentBitmap.recycle();
        currentBitmap = null;
        super.onDetachedFromWindow();
    }

    /** Lightweight no-network scene if no local media is present. */
    private static class SceneView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        SceneView(Context context) { super(context); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            paint.setShader(new LinearGradient(0, 0, 0, h,
                    new int[]{0xFF102B4E, 0xFFDA8A62, 0xFF172C38},
                    null, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
            paint.setColor(0xFFFFD39A);
            c.drawCircle(w * .76f, h * .30f, Math.min(w, h) * .075f, paint);

            path.reset();
            path.moveTo(0, h * .70f);
            path.lineTo(w * .20f, h * .42f);
            path.lineTo(w * .39f, h * .68f);
            path.lineTo(w * .61f, h * .36f);
            path.lineTo(w * .86f, h * .69f);
            path.lineTo(w, h * .52f);
            path.lineTo(w, h);
            path.lineTo(0, h);
            path.close();
            paint.setColor(0xFF263E4A);
            c.drawPath(path, paint);

            path.reset();
            path.moveTo(0, h * .78f);
            path.quadTo(w * .45f, h * .65f, w, h * .82f);
            path.lineTo(w, h);
            path.lineTo(0, h);
            path.close();
            paint.setColor(0xFF0D2835);
            c.drawPath(path, paint);

            paint.setColor(0xBFFFFFFF);
            paint.setTextSize(Math.max(14f, h * .018f));
            c.drawText("EMBERHOME  •  AERIAL", w * .055f, h * .92f, paint);
        }
    }
}
