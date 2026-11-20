package com.selo.audiomaster;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
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
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
  private final int bg=0xFF101116, panel=0xFF1D1F28, gold=0xFFE0BD75, white=0xFFF7F3EA, muted=0xFFB8B2A8;
  private EditText script;
  private Spinner voicePicker;
  private SeekBar speed;
  private TextView speedLabel,status;
  private Button generate,play,export;
  private TextToSpeech tts;
  private final ArrayList<Voice> arabicVoices=new ArrayList<>();
  private File wavFile;
  private MediaPlayer player;
  private final Handler handler=new Handler(Looper.getMainLooper());

  @Override public void onCreate(Bundle b){
    super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);build();initTts();
  }
  private TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,8,0,8);return t;}
  private GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(22);d.setStroke(1,0xFF393640);return d;}
  private LinearLayout card(LinearLayout root){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(18,14,18,14);c.setBackground(shape(panel));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,10,0,10);root.addView(c,p);return c;}
  private Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(white);b.setAllCaps(false);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF403A32));return b;}
  private EditText edit(String hint,boolean multi){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(0xFF89858A);e.setTextColor(white);e.setTextSize(15);e.setPadding(14,12,14,12);e.setBackground(shape(0xFF15171D));if(multi){e.setGravity(Gravity.TOP|Gravity.RIGHT);e.setMinLines(5);e.setMaxLines(14);}return e;}
  private void build(){
    LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(18,14,18,26);root.setBackgroundColor(bg);
    ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
    root.addView(label("SELO  VOICE ACTOR AI",22,gold));
    root.addView(label("تحويل النص إلى صوت — بدون مفتاح API أو رسوم",14,muted));
    LinearLayout textCard=card(root);textCard.addView(label("النص المراد أداؤه",18,white));script=edit("اكتب النص العربي هنا...",true);textCard.addView(script);
    LinearLayout voiceCard=card(root);voiceCard.addView(label("الصوت العربي المتاح على هاتفك",16,white));
    voicePicker=new Spinner(this);voicePicker.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"جاري فحص أصوات الهاتف..."}));voiceCard.addView(voicePicker);
    TextView note=label("يعتمد الصوت على محرك النطق المثبّت في جهازك. إذا لم يظهر صوت عربي طبيعي، نزّل صوتًا عربيًا من إعدادات تحويل النص إلى كلام في الهاتف. لا يُرسل النص إلى خدمة API من التطبيق.",11,muted);voiceCard.addView(note);
    Button settings=btn("إعدادات تنزيل الأصوات");voiceCard.addView(settings);settings.setOnClickListener(v->{try{startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA));}catch(Exception e){status.setText("افتح إعدادات الهاتف ثم إمكانية الوصول ← تحويل النص إلى كلام.");}});
    LinearLayout speedCard=card(root);speedCard.addView(label("سرعة الإلقاء",16,white));speedLabel=label("1.00×",12,gold);speedCard.addView(speedLabel);speed=new SeekBar(this);speed.setMax(80);speed.setProgress(40);speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean u){float rate=.6f+p/100f;speedLabel.setText(String.format(Locale.US,"%.2f×",rate));}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});speedCard.addView(speed);
    LinearLayout actions=card(root);generate=btn("✦  إنشاء التعليق الصوتي");actions.addView(generate);play=btn("▶  تشغيل النتيجة");play.setEnabled(false);actions.addView(play);export=btn("حفظ WAV في الهاتف");export.setEnabled(false);actions.addView(export);
    status=label("جاري تشغيل محرك الصوت المحلي...",13,gold);root.addView(status);
    generate.setOnClickListener(v->generateSpeech());play.setOnClickListener(v->playResult());export.setOnClickListener(v->exportWav());
  }
  private void initTts(){
    tts=new TextToSpeech(this,result->{
      if(result!=TextToSpeech.SUCCESS){status.setText("محرك النطق غير متاح على الهاتف.");generate.setEnabled(false);return;}
      int language=tts.setLanguage(new Locale("ar"));
      Set<Voice> all=tts.getVoices();
      if(all!=null){for(Voice voice:all)if(voice.getLocale()!=null&&"ar".equals(voice.getLocale().getLanguage()))arabicVoices.add(voice);}
      Collections.sort(arabicVoices,(a,b)->{if(a.isNetworkConnectionRequired()!=b.isNetworkConnectionRequired())return a.isNetworkConnectionRequired()?1:-1;return Integer.compare(b.getQuality(),a.getQuality());});
      if(!arabicVoices.isEmpty()){
        ArrayList<String> names=new ArrayList<>();for(Voice voice:arabicVoices)names.add(voice.getName()+(voice.isNetworkConnectionRequired()?" · يحتاج إنترنت":" · يعمل دون إنترنت"));
        voicePicker.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));
        voicePicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int position,long id){if(tts!=null&&position<arabicVoices.size())tts.setVoice(arabicVoices.get(position));}public void onNothingSelected(AdapterView<?> p){}});
      }
      if(language==TextToSpeech.LANG_MISSING_DATA||language==TextToSpeech.LANG_NOT_SUPPORTED){
        status.setText(arabicVoices.isEmpty()?"ما فيه صوت عربي مثبت. استخدم زر إعدادات تنزيل الأصوات ثم جرّب مرة ثانية.":"تم العثور على أصوات عربية. اختر الصوت واضغط إنشاء.");
      }else status.setText(arabicVoices.isEmpty()?"محرك الهاتف جاهز؛ ما لقيت صوتًا عربيًا. نزّل صوتًا من إعدادات النطق.":"جاهز — اختر صوتًا عربيًا واكتب النص.");
      tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
        @Override public void onStart(String id){handler.post(()->status.setText("جاري إنشاء الصوت..."));}
        @Override public void onDone(String id){handler.post(()->{play.setEnabled(true);export.setEnabled(true);generate.setEnabled(true);status.setText("تم إنشاء ملف الصوت على الهاتف ✓");});}
        @Override public void onError(String id){handler.post(()->{generate.setEnabled(true);status.setText("تعذر إنشاء الصوت. تحقق من تثبيت صوت عربي في إعدادات الهاتف.");});}
      });
    });
  }
  private void generateSpeech(){
    String text=script.getText().toString().trim();
    if(text.isEmpty()){status.setText("اكتب النص الذي تريد تحويله إلى صوت.");return;}
    if(tts==null){status.setText("محرك الصوت لم يجهز بعد.");return;}
    File dir=getExternalFilesDir(Environment.DIRECTORY_MUSIC);if(dir==null)dir=getFilesDir();if(!dir.exists()&&!dir.mkdirs()){status.setText("تعذر تجهيز مجلد الصوت.");return;}
    wavFile=new File(dir,"SELO_Voice_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+".wav");
    generate.setEnabled(false);play.setEnabled(false);export.setEnabled(false);
    float rate=.6f+speed.getProgress()/100f;tts.setSpeechRate(rate);
    String id="selo_"+System.currentTimeMillis();
    int result=tts.synthesizeToFile(text,new Bundle(),wavFile,id);
    if(result!=TextToSpeech.SUCCESS){generate.setEnabled(true);status.setText("محرك الهاتف رفض النص. جرّب نصًا أقصر أو صوتًا عربيًا آخر.");}
  }
  private void playResult(){try{if(wavFile==null||!wavFile.exists())return;if(player!=null){player.release();player=null;}player=new MediaPlayer();player.setDataSource(wavFile.getAbsolutePath());player.setOnPreparedListener(mp->{mp.start();status.setText("يتم تشغيل الأداء الصوتي...");});player.setOnCompletionListener(mp->status.setText("انتهى تشغيل الأداء الصوتي."));player.prepareAsync();}catch(Exception e){status.setText("تعذر تشغيل الملف: "+e.getMessage());}}
  private void exportWav(){if(wavFile==null||!wavFile.exists())return;try{ContentValues v=new ContentValues();v.put(MediaStore.MediaColumns.DISPLAY_NAME,wavFile.getName());v.put(MediaStore.MediaColumns.MIME_TYPE,"audio/wav");v.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_MUSIC+"/SELO Voice Actor");Uri uri=getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,v);if(uri==null)throw new Exception("تعذر إنشاء الملف");try(InputStream in=new FileInputStream(wavFile);OutputStream out=getContentResolver().openOutputStream(uri)){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);}status.setText("تم الحفظ في Music/SELO Voice Actor ✓");}catch(Exception e){status.setText("تعذر الحفظ: "+e.getMessage());}}
  @Override protected void onDestroy(){if(player!=null){player.release();player=null;}if(tts!=null){tts.stop();tts.shutdown();tts=null;}super.onDestroy();}
}
