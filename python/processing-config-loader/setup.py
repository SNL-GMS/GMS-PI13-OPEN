from setuptools import setup, find_packages

'''
To rebuild, run python3 setup.py
'''

VERSION = '0.1.0'

setup(
    name='processing-config-loader',
    version=VERSION,
    description='A command line application for loading processing config',
    packages=find_packages(),
    scripts=['./processing-config-loader.py'],
    python_requires='>=3',
    install_requires=['pyyaml==5.1.1',
                      'requests==2.22.0']
)
