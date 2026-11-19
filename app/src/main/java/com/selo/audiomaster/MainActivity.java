package com.selo.audiomaster;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.content.*;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Environment;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
  private final int bg=0xFF101116, panel=0xFF1D1F28, gold=0xFFE0BD75, white=0xFFF7F3EA, muted=0xFFB8B2A8;
  private EditText apiKey, script, style;
  private Spinner voice, mood;
  private Button generate, play, export;
  private TextView status;
  private byte[] wavBytes;
  private File wavFile;
  private MediaPlayer player;
  private final Handler handler=new Handler(Looper.getMainLooper());

  @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg); build();}
  private TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,8,0,8);return t;}
  private GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(22);d.setStroke(1,0xFF393640);return d;}
  private LinearLayout card(LinearLayout root){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(18,14,18,14);c.setBackground(shape(panel));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,10,0,10);root.addView(c,p);return c;}
  private Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(white);b.setAllCaps(false);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF403A32));return b;}
  private EditText edit(String hint,boolean multi){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(0xFF89858A);e.setTextColor(white);e.setTextSize(15);e.setPadding(14,12,14,12);e.setBackground(shape(0xFF15171D));if(multi){e.setGravity(Gravity.TOP|Gravity.RIGHT);e.setMinLines(5);e.setMaxLines(14);}return e;}
  private void build(){
    LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(18,14,18,26);root.setBackgroundColor(bg);
    ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
    root.addView(label("SELO  VOICE ACTOR AI",22,gold));
    root.addView(label("استوديو الأداء الصوتي بالذكاء الاصطناعي",14,muted));
    LinearLayout keyCard=card(root);keyCard.addView(label("مفتاح Gemini API",16,white));apiKey=edit("الصق مفتاح Google AI Studio هنا",false);apiKey.setSingleLine(true);apiKey.setInputType(129);keyCard.addView(apiKey);
    TextView note=label("المفتاح يُستخدم للاتصال بمحرك توليد الصوت ولا يتم حفظه داخل التطبيق.",11,muted);keyCard.addView(note);
    LinearLayout textCard=card(root);textCard.addView(label("النص المراد أداؤه",18,white));script=edit("اكتب القصة أو التعليق الصوتي هنا...",true);textCard.addView(script);
    LinearLayout voiceCard=card(root);voiceCard.addView(label("اختيار خامة المؤدي",17,white));
    voice=new Spinner(this);String[] voices={"Charon — صوت رجالي إخباري","Algenib — صوت رجالي خشن","Orus — صوت رجالي حازم","Rasalgethi — صوت رجالي معلوماتي","Enceladus — صوت رجالي هوائي","Kore — صوت نسائي حازم","Aoede — صوت نسائي ناعم","Sulafat — صوت دافئ","Schedar — أداء متزن","Fenrir — أداء قوي"};voice.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,voices));voiceCard.addView(voice);
    voiceCard.addView(label("أسلوب الإلقاء والانفعال",16,white));mood=new Spinner(this);String[] moods={"درامي سينمائي عميق","حزين ومؤثر مع إحساس واضح","خوف وتوتر وهمس","غضب وانفعال قوي","تشويق وغموض وتصاعد تدريجي","هادئ ودافئ كراوي بودكاست","إخباري واضح وواثق","فرح وحماس طبيعي","بكاء وانكسار عاطفي","ساخر وخفيف الظل"};mood.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,moods));voiceCard.addView(mood);
    voiceCard.addView(label("توجيه إضافي للأداء (اختياري)",15,white));style=edit("مثال: باللهجة المصرية، مخارج واضحة، سرعة متوسطة، وقفات طبيعية...",true);style.setMinLines(2);style.setMaxLines(4);voiceCard.addView(style);
    LinearLayout action=card(root);generate=btn("✦  توليد الأداء الصوتي");action.addView(generate);play=btn("▶  تشغيل النتيجة");play.setEnabled(false);action.addView(play);export=btn("حفظ WAV في الهاتف");export.setEnabled(false);action.addView(export);
    status=label("جاهز — اكتب النص وأدخل مفتاح Gemini API لبدء التوليد.",13,gold);root.addView(status);
    generate.setOnClickListener(v->generateSpeech());play.setOnClickListener(v->playResult());export.setOnClickListener(v->exportWav());
  }
  private String voiceId(){String s=(String)voice.getSelectedItem();return s.substring(0,s.indexOf(" —"));}
  private String selectedStyle(){String s=(String)mood.getSelectedItem();String extra=style.getText().toString().trim();return "Perform in Egyptian Arabic. "+s+". Natural human voice acting, expressive but believable, clear pronunciation, consistent pacing. "+extra;}
  private void generateSpeech(){
    final String key=apiKey.getText().toString().trim(), text=script.getText().toString().trim();
    if(key.isEmpty()){status.setText("أدخل مفتاح Gemini API أولًا.");return;}
    if(text.isEmpty()){status.setText("اكتب النص الذي تريد تحويله إلى صوت.");return;}
    generate.setEnabled(false);play.setEnabled(false);export.setEnabled(false);status.setText("جاري توليد الأداء الصوتي من Gemini 3.8 Flash TTS...");
    new Thread(()->{
      HttpURLConnection conn=null;
      try{
        JSONObject annotation=new JSONObject();annotation.put("type","speech_metadata");annotation.put("style",selectedStyle());
        JSONObject part=new JSONObject();part.put("type","text");part.put("text",text);part.put("annotations",new JSONArray().put(annotation));
        JSONObject input=new JSONObject();input.put("type","user_input");input.put("content",new JSONArray().put(part));
        JSONObject speech=new JSONObject();speech.put("speech_config",new JSONArray().put(new JSONObject().put("voice",voiceId())));
        JSONObject body=new JSONObject();body.put("model","gemini-3.8-flash-tts");body.put("input",new JSONArray().put(input));body.put("response_format",new JSONObject().put("type","audio"));body.put("generation_config",speech);
        URL url=new URL("https://generativelanguage.googleapis.com/v1beta/interactions");conn=(HttpURLConnection)url.openConnection();conn.setRequestMethod("POST");conn.setConnectTimeout(30000);conn.setReadTimeout(180000);conn.setDoOutput(true);conn.setRequestProperty("x-goog-api-key",key);conn.setRequestProperty("Content-Type","application/json; charset=UTF-8");
        try(OutputStream os=conn.getOutputStream()){os.write(body.toString().getBytes("UTF-8"));}
        int code=conn.getResponseCode();InputStream is=(code>=200&&code<300)?conn.getInputStream():conn.getErrorStream();String response=readAll(is);
        if(code<200||code>=300)throw new Exception("Gemini API ("+code+"): "+extractError(response));
        JSONObject json=new JSONObject(response);JSONArray steps=json.optJSONArray("steps");String audio=null;
        if(steps!=null)for(int i=steps.length()-1;i>=0&&audio==null;i--){JSONObject st=steps.optJSONObject(i);if(st==null)continue;JSONArray content=st.optJSONArray("content");if(content!=null)for(int j=0;j<content.length();j++){JSONObject c=content.optJSONObject(j);if(c!=null&&"audio".equals(c.optString("type"))){audio=c.optString("data",null);if(audio!=null)break;}}}
        if(audio==null)throw new Exception("لم يرجع المحرك ملفًا صوتيًا. راجع إعدادات الحساب أو جرّب مرة أخرى.");
        byte[] bytes=android.util.Base64.decode(audio,android.util.Base64.DEFAULT);
        File dir=getExternalFilesDir(Environment.DIRECTORY_MUSIC);if(dir==null)dir=getFilesDir();if(!dir.exists())dir.mkdirs();
        File out=new File(dir,"SELO_Voice_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+".wav");
        try(FileOutputStream fos=new FileOutputStream(out)){fos.write(bytes);}
        wavBytes=bytes;wavFile=out;
        handler.post(()->{status.setText("تم توليد الأداء بنجاح ✓  "+out.getName());play.setEnabled(true);export.setEnabled(true);generate.setEnabled(true);});
      }catch(Exception e){String msg=e.getMessage()==null?"حدث خطأ غير معروف":e.getMessage();handler.post(()->{status.setText("تعذر التوليد: "+msg);generate.setEnabled(true);});}
      finally{if(conn!=null)conn.disconnect();}
    }).start();
  }
  private String readAll(InputStream in)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");}
  private String extractError(String s){try{JSONObject j=new JSONObject(s);return j.optJSONObject("error")!=null?j.getJSONObject("error").optString("message",s):s;}catch(Exception e){return s;}}
  private void playResult(){try{if(wavFile==null||!wavFile.exists())return;if(player!=null){player.release();player=null;}player=new MediaPlayer();player.setDataSource(wavFile.getAbsolutePath());player.setOnPreparedListener(mp->{mp.start();status.setText("يتم تشغيل الأداء الصوتي...");});player.setOnCompletionListener(mp->status.setText("انتهى تشغيل الأداء الصوتي."));player.prepareAsync();}catch(Exception e){status.setText("تعذر تشغيل الملف: "+e.getMessage());}}
  private void exportWav(){if(wavFile==null||!wavFile.exists())return;try{ContentValues v=new ContentValues();v.put(MediaStore.MediaColumns.DISPLAY_NAME,wavFile.getName());v.put(MediaStore.MediaColumns.MIME_TYPE,"audio/wav");v.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_MUSIC+"/SELO Voice Actor");Uri uri=getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,v);if(uri==null)throw new Exception("تعذر إنشاء الملف");try(InputStream in=new FileInputStream(wavFile);OutputStream out=getContentResolver().openOutputStream(uri)){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);}status.setText("تم الحفظ في Music/SELO Voice Actor ✓");}catch(Exception e){status.setText("تعذر الحفظ: "+e.getMessage());}}
  @Override protected void onDestroy(){if(player!=null){player.release();player=null;}super.onDestroy();}
}