# Pre production

Add the [Liquibase Gradle Plugin](https://github.com/liquibase/liquibase-gradle-plugin).
```groovy
buildscript {
  dependencies {
    classpath 'com.oracle.database.jdbc:ojdbc10:19.22.0.0'
  }
}

plugins {
  id 'org.liquibase.gradle' version '2.2.2'
}
```

Set up dependencies.
```groovy
dependencies {
  implementation 'org.postgresql:postgresql'
  implementation 'com.oracle.database.jdbc:ojdbc8'

  liquibaseRuntime 'org.liquibase:liquibase-core:4.16.1'
  liquibaseRuntime 'org.liquibase:liquibase-groovy-dsl:3.0.2'
  liquibaseRuntime 'info.picocli:picocli:4.6.1'
  liquibaseRuntime 'org.postgresql:postgresql'
  liquibaseRuntime 'com.oracle.database.jdbc:ojdbc8'
}
```

Liquibase configuration.

```groovy
liquibase {
  activities {
    main {
      changelogFile 'data/main.xml'
      url project.ext.database_url
      username project.ext.database_sitmun_user
      password project.ext.database_sitmun_password
      logLevel 'info'
      changeLogParameters([
        index_tablespace: project.ext.database_sitmun_index_tablespace
      ])
    }
  }
}
```

Management tasks.

```groovy
tasks.register('dropSitmunUser') {
  dependsOn 'prepareSql'
  group = 'sitmun'
  description = 'Drop the SITMUN User'
  doLast {
    def user = project.ext.database_admin_user
    def password = project.ext.database_admin_password
    def type = project.ext.database_type
    def script = layout.buildDirectory.file("data/templates/drop-user-${type}.sql").get()
    sql(script, user, password)
  }
}

tasks.register('createSitmunUser') {
  dependsOn 'prepareSql'
  group = 'sitmun'
  description = 'Create the SITMUN User'
  doLast {
    def user = project.ext.database_admin_user
    def password = project.ext.database_admin_password
    def type = project.ext.database_type
    def script = layout.buildDirectory.file("data/templates/create-user-${type}.sql").get()
    sql(script, user, password)
  }
}

tasks.register('prepareSql', Copy) {
  group = 'sitmun'
  description = 'Prepare the sql files'
  from layout.projectDirectory.dir('data/templates')
  expand(
    user: project.ext.database_sitmun_user,
    password: project.ext.database_sitmun_password,
    tablespace: project.ext.database_sitmun_tablespace
  )
  into layout.buildDirectory.dir('data')
}

def sql(scriptName, user, password) {
  ant.sql(
          classpath:  buildscript.configurations.classpath.asPath,
          driver: project.ext.database_driver,
          url: project.ext.database_url,
          userid: user,
          password: password,
          src: scriptName
  )
}
```

`gradle.properties`

```properties
database_type=oracle
database_url=jdbc:oracle:thin:@localhost:1521/FREE
database_driver=oracle.jdbc.driver.OracleDriver
database_sitmun_user=stm3
database_sitmun_password=stm3
database_sitmun_data_tablespace=users
database_sitmun_index_tablespace=users
database_admin_user=sys as sysdba
database_admin_password=st
```