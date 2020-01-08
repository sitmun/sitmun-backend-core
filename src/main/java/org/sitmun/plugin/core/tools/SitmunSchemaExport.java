package org.sitmun.plugin.core.tools;

import java.io.File;
import java.util.concurrent.Callable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.reflections.Reflections;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "schema")
public class SitmunSchemaExport implements Callable<Void> {

  private final String[] ENTITY_PACKAGES = {"org.sitmun.plugin.core.domain"};
  @Option(names = {"-d", "--dialect"}, description = "Hibernate Dialect", required = true)
  private Class<Dialect> dialect;
  @Option(names = {"-f", "--file"}, description = "Schema file", required = true)
  private File target;

  public static void main(String[] args) {
    CommandLine.call(new SitmunSchemaExport(), args);
  }

  @Override
  public Void call() {
    ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                                          .applySetting(AvailableSettings.DIALECT, dialect)
                                          .build();

    MetadataSources source = mapAnnotatedClasses(serviceRegistry);

    MetadataImplementor metadata = (MetadataImplementor) source.buildMetadata();

    SchemaExport schemaExport = new SchemaExport(metadata);
    schemaExport.setOutputFile(target.getAbsolutePath());
    schemaExport.setDelimiter(";");
    schemaExport.setFormat(true);
    schemaExport.create(true, false);
    ((StandardServiceRegistryImpl) serviceRegistry).destroy();
    return null;
  }

  private MetadataSources mapAnnotatedClasses(ServiceRegistry serviceRegistry) {
    MetadataSources sources = new MetadataSources(serviceRegistry);

    final Reflections reflections = new Reflections(ENTITY_PACKAGES);
    for (final Class<?> mappedSuperClass : reflections.getTypesAnnotatedWith(MappedSuperclass.class)) {
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
