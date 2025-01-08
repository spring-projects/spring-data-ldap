/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.ldap.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Annotation which indicates that a method parameter should be encoded using a specific {@link LdapEncoder} for a
 * repository query method invocation.
 * <p>
 * If no {@link LdapEncoder} is configured, bound method parameters are encoded using
 * {@link org.springframework.ldap.support.LdapEncoder#filterEncode(String)}. The default encoder considers chars such
 * as {@code *} (asterisk) to be encoded which might interfere with the intent of running a Like query. Since Spring
 * Data LDAP doesn't parse queries it is up to you to decide which encoder to use.
 * <p>
 * {@link LdapEncoder} implementations must declare a no-args constructor so they can be instantiated during repository
 * initialization.
 * <p>
 * Note that parameter encoding applies only to parameters that are directly bound to a query. Parameters used in Value
 * Expressions (SpEL, Configuration Properties) are not considered for encoding and must be encoded properly by using
 * SpEL Method invocations or a SpEL Extension.
 *
 * @author Marcin Grzejszczak
 * @author Mark Paluch
 * @since 3.5
 * @see LdapEncoder.LikeEncoder
 * @see LdapEncoder.NameEncoder
 */
@Target({ ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LdapEncode {

	/**
	 * {@link LdapEncoder} to encode query parameters.
	 *
	 * @return {@link LdapEncoder} class
	 */
	@AliasFor("encoder")
	Class<? extends LdapEncoder> value();

	/**
	 * {@link LdapEncoder} to encode query parameters.
	 *
	 * @return {@link LdapEncoder} class
	 */
	@AliasFor("value")
	Class<? extends LdapEncoder> encoder() default LdapEncoder.class;

}
