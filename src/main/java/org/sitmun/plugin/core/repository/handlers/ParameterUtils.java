package org.sitmun.plugin.core.repository.handlers;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.sitmun.plugin.core.domain.TaskParameter;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParameterUtils {

  private static final Configuration conf = Configuration.builder()
    .mappingProvider(new JacksonMappingProvider())
    .jsonProvider(new JacksonJsonProvider())
    .build()
    .addOptions(Option.ALWAYS_RETURN_LIST);

  static public <T> List<TaskParameter> collect(Map<String, Object> properties, String s, Class<T> type, Function<T, TaskParameter> builder) {

    return JsonPath.using(conf).parse(properties)
      .read(s, new TokenRef<>(type))
      .stream()
      .map(builder)
      .collect(Collectors.toList());
  }

  static public <T> List<TaskParameter> flatCollect(Map<String, Object> properties, String s, Class<T> type, Function<T, Stream<TaskParameter>> builder) {

    return JsonPath.using(conf).parse(properties)
      .read(s, new TokenRef<>(type))
      .stream()
      .flatMap(builder).collect(Collectors.toList());
  }

  static public <T> T obtain(Map<String, Object> properties, String s, Class<T> clazz) {
    Configuration conf = Configuration.defaultConfiguration()
      .mappingProvider(new JacksonMappingProvider());
    return JsonPath.using(conf).parse(properties)
      .read(s, clazz);
  }

  @SuppressWarnings("UnstableApiUsage")
  static class TokenRef<T> extends TypeRef<List<T>> {
    private final TypeToken<List<T>> token;

    public TokenRef(Class<T> type) {
      super();
      this.token = new TypeToken<List<T>>() {
      }.where(new TypeParameter<T>() {
      }, type);
    }

    @Override
    public Type getType() {
      return this.token.getType();
    }
  }

}
