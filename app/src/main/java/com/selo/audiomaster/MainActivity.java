package com.selo.audiomaster;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_AUDIO = 41;
    private static final int REQUEST_MIC = 42;
    private final int bg = 0xFF101116, panel = 0xFF1B1D25, gold = 0xFFD8B66F, white = 0xFFF4F0E7, muted = 0xFFAAA6A0;
    private MediaPlayer player;
    private MediaRecorder recorder;
    private Uri selectedUri;
    private File recordingFile;
    private TextView fileLabel, statusLabel, speedLabel, volumeLabel, recordButton;
    private float speed = 1.0f, volume = 1.0f;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        buildUi();
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setPadding(0, 8, 0, 8); return t;
    }
    private Button button(String label) {
        Button b = new Button(this); b.setText(label); b.setTextColor(white); b.setAllCaps(false);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF34313A));
        return b;
    }
    private LinearLayout card(LinearLayout root) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(18, 14, 18, 14);
        box.setBackgroundTintList(android.content.res.ColorStateList.valueOf(panel));
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(panel); shape.setCornerRadius(24); shape.setStroke(1, 0xFF38343A); box.setBackground(shape);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 10, 0, 10); root.addView(box, lp); return box;
    }
    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(20, 16, 20, 24); root.setBackgroundColor(bg);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this); scroll.addView(root); setContentView(scroll);
        TextView brand = text("S   SELO AUDIO MASTER AI", 19, gold); brand.setGravity(Gravity.CENTER_VERTICAL); root.addView(brand);
        root.addView(text("استوديو الصوت المحمول", 14, muted));
        LinearLayout fileCard = card(root);
        fileCard.addView(text("ملف الصوت", 18, white));
        fileLabel = text("لم يتم اختيار ملف صوت بعد", 13, muted); fileCard.addView(fileLabel);
        Button choose = button("اختيار ملف صوت من الهاتف"); fileCard.addView(choose);
        choose.setOnClickListener(v -> { Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("audio/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i, PICK_AUDIO); });
        Button play = button("تشغيل / إيقاف"); fileCard.addView(play); play.setOnClickListener(v -> togglePlayback());
        LinearLayout controls = card(root);
        controls.addView(text("التحكم أثناء التشغيل", 18, white));
        speedLabel = text("سرعة التشغيل: 1.00×", 13, gold); controls.addView(speedLabel);
        SeekBar speedBar = new SeekBar(this); speedBar.setMax(150); speedBar.setProgress(50); controls.addView(speedBar);
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int p, boolean user) { speed = 0.5f + p / 100f; speedLabel.setText(String.format(Locale.US, "سرعة التشغيل: %.2f×", speed)); applyPlayback(); }
            public void onStartTrackingTouch(SeekBar b) {} public void onStopTrackingTouch(SeekBar b) {}
        });
        volumeLabel = text("مستوى الصوت: 100%", 13, gold); controls.addView(volumeLabel);
        SeekBar volumeBar = new SeekBar(this); volumeBar.setMax(100); volumeBar.setProgress(100); controls.addView(volumeBar);
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int p, boolean user) { volume = p / 100f; volumeLabel.setText("مستوى الصوت: " + p + "%"); if (player != null) player.setVolume(volume, volume); }
            public void onStartTrackingTouch(SeekBar b) {} public void onStopTrackingTouch(SeekBar b) {}
        });
        LinearLayout rec = card(root);
        rec.addView(text("التسجيل الصوتي", 18, white));
        rec.addView(text("سجّل تعليقك الصوتي واحفظه كملف M4A على الهاتف.", 12, muted));
        recordButton = text("● بدء التسجيل", 16, gold); recordButton.setGravity(Gravity.CENTER); rec.addView(recordButton);
        recordButton.setOnClickListener(v -> toggleRecording());
        Button share = button("مشاركة / تصدير آخر تسجيل"); rec.addView(share); share.setOnClickListener(v -> shareRecording());
        LinearLayout info = card(root);
        info.addView(text("ملاحظات النسخة الأولى", 16, gold));
        info.addView(text("هذه نسخة Android تأسيسية: استيراد وتشغيل الصوت، التحكم في السرعة ومستوى الصوت أثناء التشغيل، والتسجيل والتصدير. مؤثرات الاستوديو المتقدمة، فصل طبقة الصوت عن السرعة، ومحرك تحويل النص إلى كلام الحقيقي ستُضاف في مراحل لاحقة بعد دمجها واختبارها.", 12, muted));
        statusLabel = text("جاهز للعمل على الهاتف", 12, gold); root.addView(statusLabel);
    }
    private void togglePlayback() {
        if (selectedUri == null) { toast("اختار ملف صوت الأول"); return; }
        try {
            if (player != null && player.isPlaying()) { player.pause(); statusLabel.setText("تم إيقاف التشغيل مؤقتًا"); return; }
            if (player == null) {
                player = new MediaPlayer(); player.setDataSource(this, selectedUri);
                player.setOnPreparedListener(mp -> { mp.setVolume(volume, volume); applyPlayback(); mp.start(); statusLabel.setText("يتم تشغيل الملف"); });
                player.setOnCompletionListener(mp -> statusLabel.setText("انتهى تشغيل الملف"));
                player.prepareAsync();
            } else { applyPlayback(); player.start(); statusLabel.setText("يتم تشغيل الملف"); }
        } catch (Exception e) { toast("تعذر تشغيل الملف: " + e.getMessage()); }
    }
    private void applyPlayback() {
        if (player != null && Build.VERSION.SDK_INT >= 23) try { PlaybackParams p = player.getPlaybackParams(); p.setSpeed(speed); player.setPlaybackParams(p); player.setVolume(volume, volume); } catch (Exception ignored) {}
    }
    private void toggleRecording() {
        if (recorder != null) { stopRecording(); return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC); return; }
        startRecording();
    }
    private void startRecording() {
        try {
            File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC); if (dir == null) dir = getFilesDir();
            recordingFile = new File(dir, "SELO_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".m4a");
            if (Build.VERSION.SDK_INT >= 31) recorder = new MediaRecorder(this); else recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC); recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); recorder.setAudioEncodingBitRate(192000); recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(recordingFile.getAbsolutePath()); recorder.prepare(); recorder.start();
            recordButton.setText("■ إيقاف التسجيل"); statusLabel.setText("جاري التسجيل...");
        } catch (Exception e) { recorder = null; toast("تعذر بدء التسجيل: " + e.getMessage()); }
    }
    private void stopRecording() {
        try { recorder.stop(); toast("تم حفظ التسجيل على الهاتف"); statusLabel.setText("تم حفظ: " + recordingFile.getName()); }
        catch (Exception e) { toast("التسجيل قصير جدًا أو لم يكتمل"); if (recordingFile != null) recordingFile.delete(); }
        finally { recorder.release(); recorder = null; recordButton.setText("● بدء التسجيل"); }
    }
    private void shareRecording() {
        if (recordingFile == null || !recordingFile.exists()) { toast("سجّل صوتًا أولًا"); return; }
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, recordingFile.getName());
        values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "audio/mp4");
        values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_MUSIC + "/SELO Audio Master");
        try {
            Uri dest = getContentResolver().insert(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
            if (dest != null) try (java.io.OutputStream os = getContentResolver().openOutputStream(dest); java.io.InputStream is = new java.io.FileInputStream(recordingFile)) { byte[] buf = new byte[8192]; int n; while ((n = is.read(buf)) > 0) os.write(buf, 0, n); }
            toast("تم تصدير التسجيل إلى مجلد الموسيقى");
        } catch (Exception e) { toast("تعذر تصدير الملف: " + e.getMessage()); }
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AUDIO && resultCode == RESULT_OK && data != null) {
            selectedUri = data.getData(); if (selectedUri != null) {
                getContentResolver().takePersistableUriPermission(selectedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (player != null) { player.release(); player = null; }
                String name = "ملف صوت محدد";
                try (android.database.Cursor c = getContentResolver().query(selectedUri, null, null, null, null)) { if (c != null && c.moveToFirst()) { int ix = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME); if (ix >= 0) name = c.getString(ix); } } catch (Exception ignored) {}
                fileLabel.setText(name); statusLabel.setText("تم اختيار الملف");
            }
        }
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQUEST_MIC && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startRecording(); else if (requestCode == REQUEST_MIC) toast("يلزم السماح بالميكروفون للتسجيل");
    }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    @Override protected void onDestroy() {
        if (player != null) { player.release(); player = null; }
        if (recorder != null) { try { recorder.stop(); } catch (Exception ignored) {} recorder.release(); recorder = null; }
        super.onDestroy();
    }
}
