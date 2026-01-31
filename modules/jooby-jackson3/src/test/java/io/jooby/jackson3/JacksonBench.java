/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jackson3;

import io.jooby.output.BufferedOutput;
import io.jooby.output.OutputFactory;
import io.jooby.output.OutputOptions;
import org.openjdk.jmh.annotations.*;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.util.JsonRecyclerPools;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Fork(5)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class JacksonBench {
  private ObjectMapper mapper;
  private Map<String, Object> message;

  private OutputFactory factory;
  private ThreadLocal<BufferedOutput> cache = ThreadLocal.withInitial(() -> factory.allocate(1024));

  @Setup
  public void setup() {
    message = Map.of("id", 98, "value", "Hello World");
//    mapper = new ObjectMapper();
    JsonFactory jsonFactory = JsonFactory.builder()
        .recyclerPool(JsonRecyclerPools.threadLocalPool())
        .build();
    mapper = JsonMapper.builder(jsonFactory)
        .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .build();
    factory = OutputFactory.create(OutputOptions.small());
  }

  @Benchmark
  public void bytes() {
    mapper.writeValueAsBytes(message);
  }

  @Benchmark
  public void wrapBytes() {
    factory.wrap(mapper.writeValueAsBytes(message));
  }

  @Benchmark
  public void output() {
    var buffer = cache.get().clear();
    mapper.writeValue(buffer.asOutputStream(), message);
  }
}
