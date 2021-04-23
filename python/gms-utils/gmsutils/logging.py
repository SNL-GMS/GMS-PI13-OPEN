import logging

logger = logging.getLogger(__name__)

class tc: #NOSONAR - name is already pythonic
    BOLD = '\033[1m'
    GRAY = '\033[90m'
    RED = '\033[31m'
    GREEN = '\033[32m'
    YELLOW = '\033[33m'
    MAGENTA = '\033[35m'
    CYAN = '\033[36m'
    ENDC = '\033[0m'
    
    
def print_color(termcolor, message, bold=False, loglevel=logging.INFO):
    """
    Prints a message with the specified termcolor
    :param termcolor: Color to print the message in
    :param message: Message string to print
    :param bold: Bold text
    :param loglevel: logging level
    """

    print_str = f'{termcolor}{message}{tc.ENDC}'
    if bold:
        print_str = f'{tc.BOLD}{print_str}'

    # for info level just print
    if loglevel == logging.INFO:
        print(print_str)
    else:
        logger.log(loglevel, print_str)

def print_info(message, bold=False):
    """
    Print an info message, optionally in bold
    :param message: Message string to print
    :param bold: Bold text
    """
    print_str = f'{message}{tc.ENDC}'
    if bold:
        print_str = f'{tc.BOLD}{print_str}'
    
    logger.info(print_str)
        
def print_warning(message):
    """
    Print a warning message in bold yellow.
    :param message: Message string to print
    """
    print_color(tc.YELLOW, message, bold=True, loglevel=logging.WARNING)


def print_error(message):
    """
    Print an error message in bold red.
    :param message: Message string to print
    """
    print_color(tc.RED, message, bold=True, loglevel=logging.ERROR)

