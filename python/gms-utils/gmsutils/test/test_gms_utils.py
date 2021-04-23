#!/usr/bin/env python3

import os
import sys
import logging
import contextlib

from unittest import TestCase
from unittest.mock import patch
from io import StringIO 

from gmsutils import tc
from gmsutils import print_info
from gmsutils import print_warning
from gmsutils import print_error
from gmsutils import print_color
from gmsutils import which
from gmsutils import run_command

class GmsUtilsTests(TestCase):
    
    def setUp(self):
        self.log_name = 'gmsutils.logging'
    
    def tearDown(self):
        pass
        
    def test_print_info(self):
        with self.assertLogs(self.log_name) as cm:
            print_info('This is an info message')
            print_info('This is a bold info message', bold=True)
        self.assertEqual(cm.output,
                         [ f'INFO:{ self.log_name }:This is an info message{ tc.ENDC }',
                           f'INFO:{ self.log_name }:{ tc.BOLD }This is a bold info message{ tc.ENDC }'])
            
    def test_print_warning(self):
        with self.assertLogs(self.log_name) as cm:
            print_warning('This is a warning message')
        self.assertEqual(cm.output,
                         [ f'WARNING:{ self.log_name }:{ tc.BOLD }{ tc.YELLOW }This is a warning message{ tc.ENDC }'])

    def test_print_error(self):
        with self.assertLogs(self.log_name) as cm:
            print_error('This is an error message')
        self.assertEqual(cm.output,
                         [ f'ERROR:{ self.log_name }:{ tc.BOLD }{ tc.RED }This is an error message{ tc.ENDC }'])
        
    def test_print_color(self):
        with patch('sys.stdout', new = StringIO()) as mystdout:
            print_color(tc.GREEN, "This is a green message")
            self.assertEqual(mystdout.getvalue(), f"{ tc.GREEN }This is a green message{ tc.ENDC }\n")
            
        with patch('sys.stdout', new = StringIO()) as mystdout:
            print_color(tc.CYAN, "This is a cyan message")
            self.assertEqual(mystdout.getvalue(), f"{ tc.CYAN }This is a cyan message{ tc.ENDC }\n")
            
        with patch('sys.stdout', new = StringIO()) as mystdout:
            print_color(tc.MAGENTA, "This is a magenta message")
            self.assertEqual(mystdout.getvalue(), f"{ tc.MAGENTA }This is a magenta message{ tc.ENDC }\n")

        with patch('sys.stdout', new = StringIO()) as mystdout:
            print_color(tc.RED, "This is a bold red message", bold=True)
            self.assertEqual(mystdout.getvalue(), f"{ tc.BOLD }{ tc.RED }This is a bold red message{ tc.ENDC }\n")
            
    def test_which(self):
        path_to_sh = which('true')
        self.assertEqual(path_to_sh, '/usr/bin/true')
    
    def test_run_command(self):
        rc, out, err = run_command("echo hello world", print_output=False)
        self.assertEqual(rc, 0)
        self.assertEqual(out, 'hello world\n')

if __name__ == "__main__":
    unittest.main()
