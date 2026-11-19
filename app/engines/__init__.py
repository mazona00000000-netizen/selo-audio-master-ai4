"""Engine registry. New providers implement the Engine contract here."""
from .base import Engine
from .demo import DemoEngine
_REGISTRY={e.name:e for e in (DemoEngine(),)}
def list_engines(): return [e.info() for e in _REGISTRY.values()]
def get_engine(name:str)->Engine:
 if name not in _REGISTRY: raise KeyError(f"Unknown engine: {name}")
 return _REGISTRY[name]
