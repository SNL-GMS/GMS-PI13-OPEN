from setuptools import setup, find_packages

'''
To rebuild, run python3 setup.py
'''

VERSION = '0.1.0'

setup(
    name='gms-artifactory',
    version=VERSION,
    description='A command line application for managing artifactory for GMS',
    packages=find_packages(),
    scripts=['bin/gms-artifactory'],
    python_requires='>=3',
    install_requires=['requests==2.22.0',
                      'pyyaml==5.1.1']
)
