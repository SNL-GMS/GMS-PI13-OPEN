import os
import subprocess

from gmsutils.logging import print_warning


def run_command(command, chdir=None, print_output=True, stdin=None, print_command=False):
    """
    Execute the specified command in an optionally specified directory
    and return when the command execution is completed.

    Option to disable printing of stdout and stderr

    Option to pass std_in to the process
    
    Returns the return code, stdout, and stderr of the command.
    """
    if print_command:
        print(f'Running command: {command}')

    cwd = None
    if chdir is not None:
        try:
            cwd = os.getcwd()
        except:
            # just continue on if we can't get the cwd
            pass

        os.chdir(chdir)

    cmd = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, stdin=subprocess.PIPE)
    out, err = cmd.communicate(input=stdin)
    out = out.decode()
    err = err.decode()

    if chdir is not None and cwd is not None:
        os.chdir(cwd)

    if print_output:
        print(f'{out}')
        if len(err) > 0:
            print_warning(f'{err}')

    return cmd.returncode, out, err
