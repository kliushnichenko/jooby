/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.verifytype;

import io.jooby.annotation.FlashParam;
import io.jooby.annotation.POST;

import java.net.URI;
import java.net.URL;
import java.time.Period;
import java.util.Locale;
import java.util.Optional;

class Controller {

  @POST("/flash")
  public Object validateFlashBean(@FlashParam Optional<String> bean) {
    return bean;
  }

}
