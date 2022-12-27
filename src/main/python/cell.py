import z5py
from pathlib import Path
import os
import numpy as np

class Cell:
    def __init__(self, name, path, color, linestyle='-'):
        self.name = name
        self.path = path
        self.color = color
        self.linestyle = linestyle

    def read_volume(self, volume_name):
        if volume_name is None:
            return None
        f = z5py.File(str(self.path))
        project_name = Path(self.path).name.rstrip('.n5')
        name = 'volumes' + os.path.sep + project_name + '_' + volume_name
        return np.array(f[name])
