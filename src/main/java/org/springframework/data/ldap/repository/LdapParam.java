/*
 * Copyright 2016-2024 the original author or authors.
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
import org.springframework.data.repository.query.Param;

/**
 * A {@link Param} alias that allows passing of custom {@link LdapEncoder}.
 *
 * @author Marcin Grzejszczak
 * @since 3.5.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LdapParam {

	@AliasFor(annotation = Param.class)
	String value();

	/**
	 * {@link LdapEncoder} to instantiate to encode query parameters.
	 *
	 * @return {@link LdapEncoder} class
	 */
	Class<? extends LdapEncoder> encoder() default LdapEncoder.DefaultLdapEncoder.class;
}
