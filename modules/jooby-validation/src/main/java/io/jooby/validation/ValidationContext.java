/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.validation;

import static io.jooby.validation.BeanValidator.apply;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.*;

/**
 * Wrap a context and run {@link BeanValidator#validate(Context, Object)} over HTTP request objects.
 *
 * @author edgar
 * @since 3.1.1
 */
public class ValidationContext extends ForwardingContext {
  private record ValidationInvocationHandler(Context ctx, Object target)
      implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
      var method = overrideMethod(target, m);
      return switch (method.getName()) {
        case "to", "toNullable", "toList", "toSet", "toMap", "toMultimap" ->
            apply(ctx, method.invoke(target, args));
        default -> method.invoke(target, args);
      };
    }

    private Method overrideMethod(Object target, Method method) throws NoSuchMethodException {
      // Call nullable version to let bean validator to run
      return "to".equals(method.getName())
          ? target.getClass().getMethod("toNullable", Class.class)
          : method;
    }
  }

  /**
   * Creates a new forwarding context.
   *
   * @param context Source context.
   */
  public ValidationContext(@NonNull Context context) {
    super(context);
  }

  @NonNull @Override
  public <T> T body(@NonNull Type type) {
    return body().to(type);
  }

  @NonNull @Override
  public <T> T body(@NonNull Class<T> type) {
    // Call nullable version to let bean validator to run
    return body().toNullable(type);
  }

  @NonNull @Override
  public Body body() {
    return proxy(Body.class, super.body());
  }

  @NonNull @Override
  public <T> T query(@NonNull Class<T> type) {
    // Call nullable version to let bean validator to run
    return query().toNullable(type);
  }

  @NonNull @Override
  public QueryString query() {
    return proxy(QueryString.class, super.query());
  }

  @NonNull @Override
  public Map<String, String> queryMap() {
    return apply(ctx, super.queryMap());
  }

  @NonNull @Override
  public <T> T form(@NonNull Class<T> type) {
    // Call nullable version to let bean validator to run
    return form().toNullable(type);
  }

  @NonNull @Override
  public Formdata form() {
    return proxy(Formdata.class, super.form());
  }

  @NonNull @Override
  public Map<String, String> formMap() {
    return apply(ctx, super.formMap());
  }

  @NonNull @Override
  public ValueNode header() {
    return proxy(ValueNode.class, super.header());
  }

  @NonNull @Override
  public Map<String, String> headerMap() {
    return apply(ctx, super.headerMap());
  }

  private <T> T proxy(Class<T> type, Object target) {
    return type.cast(
        Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[] {type},
            new ValidationInvocationHandler(ctx, target)));
  }
}
