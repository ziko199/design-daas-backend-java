package de.frauas.design.backend.auth.exception;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation used to scope {@link SessionExceptionHandler} to the session controller
 * without the exception package depending directly on the controller class.
 *
 * <p>Placed on the controller class(es) whose {@link SessionException}s should be handled by
 * {@link SessionExceptionHandler}.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SessionEndpoint {}
