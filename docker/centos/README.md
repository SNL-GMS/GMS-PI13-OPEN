# Centos 7 image

This is a docker container with CentOS7 and interception and ldap certs 
preinstalled.

To build and push run the [build.sh](./build.sh) and [push.sh](./push.sh)
scripts respectively.

#Subdirectories:
src/         Directory containing files used for the docker build
   
builder/     Builder image containing everything needed for gms builds
java/        Runtime java image
python/      Runtime python image
typescript/  Runtime typescript image

