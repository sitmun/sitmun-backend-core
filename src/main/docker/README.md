# Docker images

## Requirements

* [Oracle Database 11g Release 2 (11.2.0.2) Express Edition (XE)](https://github.com/oracle/docker-images/tree/main/OracleDatabase/SingleInstance/dockerfiles). 
  You will have to provide the installation binaries of Oracle Database. The needed file is named `oracle-xe-11.2.0-1.0.x86_64.rpm.zip` and should be put in the folder `11.2.0.2`. Then run:
  ```
  $ ./buildContainerImage.sh -v 11.2.0.2 -x
  ```
  The script builds the image `oracle/database:11.2.0.2-xe`. 
  The container can be started with:
  ```
  $ docker-compose -f oracle.yml up -d oracle
  ```
  

