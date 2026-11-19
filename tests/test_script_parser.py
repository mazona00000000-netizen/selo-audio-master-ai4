from app.script_parser import parse_script
def test_paragraphs_and_cues():
 d=parse_script("[حزين] أول سطر.\n\n[غاضب] تاني سطر.")
 assert len(d)==2 and d[0]["segments"][0]["profile"]["key"]=="sad" and d[1]["segments"][0]["profile"]["key"]=="angry"
def test_inline_cue_splits_segments():
 d=parse_script("[هادي] بداية [غاضب] نهاية")
 assert len(d[0]["segments"])==2 and d[0]["segments"][1]["text"]=="نهاية"
def test_pause_only():
 d=parse_script("[وقفة]")
 assert d[0]["segments"][0]["pause_only"] is True
