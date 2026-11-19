"""A clearly-labelled sine-wave preview; never presented as speech synthesis."""
import io,wave
import numpy as np
from .base import Engine
class DemoEngine(Engine):
 name="demo";label="نغمة اختبار";description="اختبار تشغيل الصوت فقط — لا يحوّل النص إلى كلام.";mode="preview"
 def synthesize(self,text,voice,settings):
  sr=24000;seconds=1.2;freq=float(settings.get("pitch",1.0))*220
  t=np.arange(int(sr*seconds),dtype=np.float32)/sr
  signal=(.18*np.sin(2*np.pi*freq*t)).astype(np.float32);pcm=(np.clip(signal,-1,1)*32767).astype("<i2")
  out=io.BytesIO()
  with wave.open(out,"wb") as f:f.setnchannels(1);f.setsampwidth(2);f.setframerate(sr);f.writeframes(pcm.tobytes())
  return out.getvalue(),"audio/wav"
