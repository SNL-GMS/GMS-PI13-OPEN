import os

def which(program):
    """
    Search PATH for a given program name.
    """
    for path in os.environ["PATH"].split(os.pathsep):
        fpath = os.path.join(path, program)
        if os.path.exists(fpath) and os.path.isfile(fpath) and os.access(fpath, os.X_OK):
            return fpath

    return None
