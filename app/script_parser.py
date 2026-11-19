"""Parse Arabic performance cues and split scripts into paragraphs/segments."""
from __future__ import annotations
import re, math
CUE_SPLIT = re.compile(r"(\[[^\]]*\])")
RULES = [
("sad","حزين",("حزين","حزن","بكاء","sad","crying"),.88,.84),
("angry","غاضب",("غضب","غاضب","عصبي","angry"),1.10,1.20),
("happy","فرحان",("فرحان","سعيد","مبسوط","متحمس","happy","excited"),1.08,1.05),
("fear","خايف",("خايف","خوف","رعب","مرعوب","scared","fear"),1.05,.85),
("suspense","تشويق",("تشويق","غموض","ترقب","suspense","mysterious"),.90,.82),
("calm","هادي",("هادي","هدوء","رايق","calm","gentle"),.95,.90),
("whisper","همس",("همس","وشوشة","whisper"),.90,.38),
("low","صوت واطي",("واطي","منخفض","خافت","low voice"),.95,.60),
("loud","صوت عالي",("عالي","صراخ","بيصرخ","loud","shout"),1.04,1.30),
("slow","بطيء",("بطيء","ببطء","slow"),.85,1.0),
("fast","سريع",("سريع","بسرعة","fast"),1.18,1.0),
("pause","وقفة",("وقفة","صمت","سكوت","pause","silence"),1.0,1.0)]
def profile(cue:str)->dict:
 c=cue.strip().lower(); matches=[r for r in RULES if any(w.lower() in c for w in r[2])]
 if not matches:return {"key":"neutral","label":"عادي" if not c else cue,"speed":1.0,"volume":1.0,"unknown":bool(c)}
 return {"key":matches[0][0],"label":" + ".join(r[1] for r in matches),"speed":round(max(.6,min(1.5,math.prod(r[3] for r in matches))),2),"volume":round(max(.2,min(1.6,math.prod(r[4] for r in matches))),2),"unknown":False}
def parse_script(script:str)->list[dict]:
 paragraphs=[]; current=None
 for line_no,raw in enumerate(script.splitlines(),1):
  line=raw.strip()
  if not line or line.startswith("#") or line.startswith("//"):
   if not line: current=None
   continue
  pieces=[]; pending=[]
  for chunk in CUE_SPLIT.split(line):
   if not chunk:continue
   if chunk.startswith("[") and chunk.endswith("]"):pending.append(chunk[1:-1].strip());continue
   txt=" ".join(chunk.replace("*","").replace("_"," ").split())
   if txt:
    cue=" ".join(x for x in pending if x);pieces.append({"text":txt,"line":line_no,"cue":cue,"profile":profile(cue)});pending=[]
  if pending:
   cue=" ".join(x for x in pending if x);pieces.append({"text":"","line":line_no,"cue":cue,"profile":profile(cue),"pause_only":True})
  if not pieces:continue
  if current is None:current={"index":len(paragraphs),"segments":[]};paragraphs.append(current)
  current["segments"].extend(pieces)
 for p in paragraphs:
  p["snippet"]=next((s["text"][:64] for s in p["segments"] if s["text"]),"وقفة")
  p["segment_count"]=len(p["segments"]);p["voiceslot"]=p["index"]+1
 return paragraphs
