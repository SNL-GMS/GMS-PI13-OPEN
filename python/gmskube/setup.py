from setuptools import setup, find_packages

'''
To rebuild, run python3 setup.py
'''

VERSION = '0.1.0'

setup(
    name='gmskube',
    version=VERSION,
    description='A command line application to manage gms instances on Kubernetes',
    packages=find_packages(),
    scripts=['bin/gmskube.py'],
    python_requires='>=3'
    # install_requires=['docker==4.0.2',
    #                   'pyyaml==5.1.1']
)
