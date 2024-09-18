/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3530;

import jakarta.validation.constraints.NotEmpty;

public class Bean3530 {
  @NotEmpty private String name;

  public @NotEmpty String getName() {
    return name;
  }

  public void setName(@NotEmpty String name) {
    this.name = name;
  }
}