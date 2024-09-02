import os
import logging


logger = logging.getLogger()


def make_log_file():
    base_dir = os.getcwd()
    log_dir = os.path.join(base_dir, 'log')
    if not os.path.exists(log_dir):
        os.makedirs(log_dir)