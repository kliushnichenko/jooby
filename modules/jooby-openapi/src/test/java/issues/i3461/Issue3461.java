/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3461;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class Issue3461 {

  @OpenAPITest(value = App3461.class)
  public void shouldParseAvajeBeanScopeControllers(OpenAPIResult result) {
    assertEquals(
        "openapi: 3.0.1\n"
            + "info:\n"
            + "  title: 3461 API\n"
            + "  description: 3461 API description\n"
            + "  version: \"1.0\"\n"
            + "paths:\n"
            + "  /3461:\n"
            + "    get:\n"
            + "      operationId: getBlah\n"
            + "      parameters:\n"
            + "      - name: orgId\n"
            + "        in: query\n"
            + "        description: Param annotation\n"
            + "        required: true\n"
            + "        schema:\n"
            + "          type: string\n"
            + "          format: uuid\n"
            + "      responses:\n"
            + "        \"200\":\n"
            + "          description: Success\n"
            + "          content:\n"
            + "            application/json:\n"
            + "              schema:\n"
            + "                type: string\n",
        result.toYaml());
  }
}