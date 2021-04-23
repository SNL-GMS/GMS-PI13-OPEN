from setuptools import setup, find_packages

'''
To rebuild, run python3 setup.py
'''

VERSION = '0.1.0'

setup(
  name='gms-data-loader',
  version=VERSION,
  description='A command line application for loading gms data',
  packages=find_packages(),
  include_package_data=True,
  scripts=['bin/gms-data-loader.py'],
  python_requires='>=3.7',
  install_requires=['pyyaml==5.3',
                    'requests==2.22.0']
)
