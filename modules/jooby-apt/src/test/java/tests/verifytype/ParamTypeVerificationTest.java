/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.verifytype;

import io.jooby.apt.ProcessorRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParamTypeVerificationTest {

  @Test
  public void flashParam_illegalType_shouldThrowException() throws Exception {
    new ProcessorRunner(new Controller())
        .withSourceCode(
            false,
            source -> {
              // todo: verify ex thrown
            });
  }
}
