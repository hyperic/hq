#!/usr/bin/python

import logging
import os,sys

# Add console logging handler to the root logger
def add_console_logging_handler():
    hdlr = logging.StreamHandler()
    formatter = logging.Formatter('%(levelname)-8s %(message)s')
    hdlr.setFormatter(formatter)
    hdlr.setLevel(logging.DEBUG)
    logging.getLogger('').addHandler(hdlr)
    return hdlr

# add a file logging handler to the root logger
def add_logging_handler(logging_folder,log_filename):
    hdlr = logging.FileHandler(os.path.join(logging_folder,log_filename))
    formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
    hdlr.setFormatter(formatter)
    hdlr.setLevel(logging.DEBUG)
    logging.getLogger('').addHandler(hdlr) 
    return hdlr
    
