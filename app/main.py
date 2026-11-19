from pathlib import Path
from fastapi import FastAPI,HTTPException
from fastapi.responses import FileResponse,Response
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel,Field
from .script_parser import parse_script
from .engines import get_engine,list_engines
ROOT=Path(__file__).resolve().parent.parent
app=FastAPI(title="SELO AUDIO MASTER AI",version="0.1.0")
app.mount("/static",StaticFiles(directory=ROOT/"static"),name="static")
class ParseRequest(BaseModel):
 script:str=Field(default="",max_length=100000)
class PreviewRequest(BaseModel):
 engine:str="demo";text:str=Field(default="اختبار",max_length=5000);voice:str="preview";settings:dict=Field(default_factory=dict)
@app.get("/")
def home():return FileResponse(ROOT/"static"/"index.html")
@app.get("/api/health")
def health():return {"ok":True,"app":"SELO AUDIO MASTER AI","version":"0.1.0"}
@app.get("/api/engines")
def engines():return {"engines":list_engines()}
@app.post("/api/parse")
def parse(req:ParseRequest):
 paragraphs=parse_script(req.script)
 return {"paragraphs":paragraphs,"segments":sum(p["segment_count"] for p in paragraphs),"unknown_cues":[s["cue"] for p in paragraphs for s in p["segments"] if s["profile"]["unknown"]]}
@app.post("/api/preview")
def preview(req:PreviewRequest):
 try:
  data,mime=get_engine(req.engine).synthesize(req.text,req.voice,req.settings)
  return Response(content=data,media_type=mime,headers={"Content-Disposition":'inline; filename="selo-preview.wav"'})
 except KeyError as e:raise HTTPException(status_code=404,detail=str(e))
 except Exception as e:raise HTTPException(status_code=500,detail=f"Preview failed: {type(e).__name__}: {e}")
