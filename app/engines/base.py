from abc import ABC,abstractmethod
class Engine(ABC):
 name="base";label="Base engine";description="";mode="tts"
 def info(self):
  return {"id":self.name,"name":self.label,"description":self.description,"mode":self.mode,"status":"preview" if self.mode=="preview" else "available"}
 @abstractmethod
 def synthesize(self,text:str,voice:str,settings:dict)->tuple[bytes,str]: ...
