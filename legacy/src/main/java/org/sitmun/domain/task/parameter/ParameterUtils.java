package org.sitmun.domain.task.parameter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public final class ParameterUtils {

  private ParameterUtils() {}

  private static final Configuration conf = Configuration.builder()
    .mappingProvider(new JacksonMappingProvider())
    .jsonProvider(new JacksonJsonProvider())
    .build()
    .addOptions(Option.SUPPRESS_EXCEPTIONS)
    .addOptions(Option.ALWAYS_RETURN_LIST);

  public static <T> List<TaskParameter> collect(Map<String, Object> properties, String s, Class<T> type, Function<T, TaskParameter> builder) {

    return JsonPath.using(conf).parse(properties)
      .read(s, new TokenRef<>(type))
      .stream()
      .map(builder)
      .collect(Collectors.toList());
  }

  public static <T> List<TaskParameter> flatCollect(Map<String, Object> properties, String jsonPath, Class<T> type, Function<T, Stream<TaskParameter>> builder) {
    return JsonPath.using(conf).parse(properties)
      .read(jsonPath, new TokenRef<>(type))
      .stream()
      .flatMap(builder).collect(Collectors.toList());
  }

  public static <T> T obtain(Map<String, Object> properties, String s, Class<T> clazz) {
    Configuration conf = Configuration.defaultConfiguration()
      .mappingProvider(new JacksonMappingProvider(new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)));
    return JsonPath.using(conf).parse(properties)
      .read(s, clazz);
  }

  @SuppressWarnings("UnstableApiUsage")
  static class TokenRef<T> extends TypeRef<List<T>> {
    private final TypeToken<List<T>> token;

    TokenRef(Class<T> type) {
      super();
      token = new TypeToken<List<T>>() {
      }.where(new TypeParameter<>() {
      }, type);
    }

    @Override
    public Type getType() {
      return token.getType();
    }
  }

}
