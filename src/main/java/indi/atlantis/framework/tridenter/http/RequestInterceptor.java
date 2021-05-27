package indi.atlantis.framework.tridenter.http;

import org.springframework.lang.Nullable;

/**
 * 
 * RequestInterceptor
 * 
 * @author Fred Feng
 *
 * @since 1.0
 */
public interface RequestInterceptor {

	default boolean beforeSubmit(String provider, Request request) {
		return true;
	}

	default void afterSubmit(String provider, Request request, @Nullable Object responseEntity, @Nullable Throwable reason) {
	}

	default boolean matches(String provider, Request request) {
		return true;
	}

}
