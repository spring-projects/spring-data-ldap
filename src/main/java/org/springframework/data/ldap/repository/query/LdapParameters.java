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
package org.springframework.data.ldap.repository.query;

import java.lang.reflect.Method;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.data.geo.Distance;
import org.springframework.data.ldap.repository.LdapEncode;
import org.springframework.data.ldap.repository.LdapEncoder;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersSource;
import org.springframework.data.util.TypeInformation;

/**
 * Custom extension of {@link Parameters} discovering additional
 *
 * @author Marcin Grzejszczak
 * @since 3.5
 */
public class LdapParameters extends Parameters<LdapParameters, LdapParameters.LdapParameter> {

	private final TypeInformation<?> domainType;

	/**
	 * Creates a new {@link LdapParameters} instance from the given {@link Method} and {@link LdapQueryMethod}.
	 *
	 * @param parametersSource must not be {@literal null}.
	 */
	public LdapParameters(ParametersSource parametersSource) {

		super(parametersSource, methodParameter -> new LdapParameter(methodParameter,
				parametersSource.getDomainTypeInformation()));

		this.domainType = parametersSource.getDomainTypeInformation();
	}

	private LdapParameters(List<LdapParameter> parameters, TypeInformation<?> domainType) {

		super(parameters);
		this.domainType = domainType;
	}

	@Override
	protected LdapParameters createFrom(List<LdapParameter> parameters) {
		return new LdapParameters(parameters, this.domainType);
	}


	/**
	 * Custom {@link Parameter} implementation adding parameters of type {@link Distance} to the special ones.
	 *
	 * @author Marcin Grzejszczak
	 */
	protected static class LdapParameter extends Parameter {

		private final @Nullable LdapEncoder ldapEncoder;
		private final MethodParameter parameter;

		/**
		 * Creates a new {@link LdapParameter}.
		 *
		 * @param parameter must not be {@literal null}.
		 * @param domainType must not be {@literal null}.
		 */
		LdapParameter(MethodParameter parameter, TypeInformation<?> domainType) {

			super(parameter, domainType);
			this.parameter = parameter;

			LdapEncode encode = parameter.getParameterAnnotation(LdapEncode.class);

			if (encode != null) {
				this.ldapEncoder = BeanUtils.instantiateClass(encode.value());
			} else {
				this.ldapEncoder = null;
			}
		}

		public boolean hasLdapEncoder() {
			return ldapEncoder != null;
		}

		public LdapEncoder getLdapEncoder() {

			if (ldapEncoder == null) {
				throw new IllegalStateException("No LdapEncoder found for parameter " + parameter);
			}
			return ldapEncoder;
		}
	}

}
