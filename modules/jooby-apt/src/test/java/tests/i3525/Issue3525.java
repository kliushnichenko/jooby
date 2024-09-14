/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3525;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3525 {

  @Test
  public void shouldNotRunOnInfiniteLoop() throws Exception {
    new ProcessorRunner(new C3525())
        .withSourceCode(
            source -> {
              assertTrue(source.contains("\"Parameter.description\", \"paramA\","));
              assertTrue(source.contains("\"Parameter.description\", \"paramB\","));
            });
  }
}
