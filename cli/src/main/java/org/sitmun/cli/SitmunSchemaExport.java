package org.sitmun.cli;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.reflections.Reflections;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import java.io.File;
import java.util.EnumSet;
import java.util.concurrent.Callable;

@Command(name = "schema")
public class SitmunSchemaExport implements Callable<Void> {

  private static final String[] ENTITY_PACKAGES = {"org.sitmun.domain"};
  @Option(names = {"-d", "--dialect"}, description = "Hibernate Dialect", required = true)
  private Class<Dialect> dialect;
  @Option(names = {"-f", "--file"}, description = "Schema file", required = true)
  private File target;

  public static void main(String[] args) {
    CommandLine cmd = new CommandLine(new SitmunSchemaExport());
    cmd.execute(args);
  }

  @Override
  public Void call() {
    ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
      .applySetting(AvailableSettings.DIALECT, dialect)
      .build();

    MetadataSources source = mapAnnotatedClasses(serviceRegistry);

    MetadataImplementor metadata = (MetadataImplementor) source.buildMetadata();

    SchemaExport schemaExport = new SchemaExport();
    schemaExport.setOutputFile(target.getAbsolutePath());
    schemaExport.setDelimiter(";");
    schemaExport.setFormat(true);
    schemaExport.create(EnumSet.of(TargetType.SCRIPT), metadata);
    ((StandardServiceRegistryImpl) serviceRegistry).destroy();
    return null;
  }

  private static MetadataSources mapAnnotatedClasses(ServiceRegistry serviceRegistry) {
    MetadataSources sources = new MetadataSources(serviceRegistry);

    final Reflections reflections = new Reflections((Object[]) ENTITY_PACKAGES);
    for (final Class<?> mappedSuperClass : reflections
      .getTypesAnnotatedWith(MappedSuperclass.class)) {
      sources.addAnnotatedClass(mappedSuperClass);
      System.out.println("Mapped = " + mappedSuperClass.getName());
    }
    for (final Class<?> entityClasses : reflections.getTypesAnnotatedWith(Entity.class)) {
      sources.addAnnotatedClass(entityClasses);
      System.out.println("Mapped = " + entityClasses.getName());
    }
    return sources;
  }

}
