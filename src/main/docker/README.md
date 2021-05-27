# Docker images

## Image preparation

* [Oracle Database 11g Release 2 (11.2.0.2) Express Edition (XE)](https://github.com/oracle/docker-images/tree/main/OracleDatabase/SingleInstance/dockerfiles)
  .
  _Prerequisite: `docker` and Oracle binaries._

  You will have to provide the installation binaries of Oracle Database. The needed file is
  named `oracle-xe-11.2.0-1.0.x86_64.rpm.zip` and should be put in the folder `11.2.0.2`. Then run:
  ```
  $ ./buildContainerImage.sh -v 11.2.0.2 -x
  ```
  The script builds the image `oracle/database:11.2.0.2-xe`.

* **Backend core**.
  _Prerequisite: `Java 11`._

  You must run:
  ```
  $ ./gradlew build -x test
  ```
  Then copy the JAR file at `build/libs` to `src/main/docker/backend/sitmun-backend-core.jar`.

* **Administration application**
  _Prerequisite: `npm`._

  Download the **Sitmun Admin App** source code and then update `src/environment/environment.testdeployment.ts` to:
  ```
  export const environment = {
    production: false,
    apiBaseURL: '/sitmun',
  };
  ```
  Then edit `.npmrc` and comment the line
  ```
  #registry=https://npm.pkg.github.com/sitmun
  ```
  Then run:
  ```
  $ npm ci
  $ npm run build -- --configuration=testdeployment
  ```
  Next, copy the contents of `dist/admin-app` to Backend Core `src/main/docker/admin/build`

## Oracle-backed run

Oracle image has a slow start, so the run must be done in three stages. In a terminal open at `src/main/docker`:

```
$ docker-compose -f docker-compose-dev-oracle.yml up -d oracle
```

Use a console provided by a docker tool to monitor this container. When the log console outputs:

```
#########################
DATABASE IS READY TO USE!
#########################
```

we can proceed with the next step. Run:

```
$ docker-compose -f docker-compose-dev-oracle.yml up -d backend
```

Monitor this container and wait until a message similar to:

```
Started Application in nnn seconds (JVM running for mmm)
```

Finally, we can run the front:

```
$ docker-compose -f docker-compose-dev-oracle.yml up -d admin
```

Admin is fast, so you can open your browser and navigate to localhost:8000 to the **Sitmun Admin App**.

## Postgres-backed run

In a terminal open at `src/main/docker`:

```
$ docker-compose -f docker-compose-dev-postgres.yml up -d
```

The bootstrap is quick. Next, you can open your browser and navigate to localhost:8000 to the **Sitmun Admin App**.
