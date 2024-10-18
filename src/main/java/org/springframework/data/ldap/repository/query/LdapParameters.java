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
package org.springframework.data.ldap.repository.query;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersSource;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.data.repository.util.ReactiveWrappers;
import org.springframework.data.util.TypeInformation;

/**
 * Custom extension of {@link Parameters} discovering additional properties of query method parameters.
 *
 * @author Marcin Grzejszczak
 */
public class LdapParameters extends Parameters<LdapParameters, LdapParameters.LdapParameter> {

	/**
	 * Create a new {@link LdapParameters} instance from the given {@link Method}.
	 *
	 * @param parametersSource must not be {@literal null}.
	 */
	public LdapParameters(ParametersSource parametersSource) {
		super(parametersSource,
				methodParameter -> new LdapParameter(methodParameter, parametersSource.getDomainTypeInformation()));
	}

	private LdapParameters(List<LdapParameter> originals) {

		super(originals);
	}

	@Override
	protected LdapParameters createFrom(List<LdapParameter> parameters) {
		return new LdapParameters(parameters);
	}

	/**
	 * Custom {@link Parameter}.
	 *
	 * @author Marcin Grzejszczak
	 */
	static class LdapParameter extends Parameter {

		private final Class<?> parameterType;

		LdapParameter(MethodParameter parameter, TypeInformation<?> domainType) {

			super(parameter, domainType);

			parameterType = potentiallyUnwrapParameterType(parameter);
		}

		@Override
		public Class<?> getType() {
			return this.parameterType;
		}

		/**
		 * Returns the component type if the given {@link MethodParameter} is a wrapper type and the wrapper should be
		 * unwrapped.
		 *
		 * @param parameter must not be {@literal null}.
		 */
		private static Class<?> potentiallyUnwrapParameterType(MethodParameter parameter) {

			Class<?> originalType = parameter.getParameterType();

			if (isWrapped(parameter) && shouldUnwrap(parameter)) {
				return ResolvableType.forMethodParameter(parameter).getGeneric(0).getRawClass();
			}

			return originalType;
		}

		/**
		 * Returns whether the {@link MethodParameter} is wrapped in a wrapper type.
		 *
		 * @param parameter must not be {@literal null}.
		 * @see QueryExecutionConverters
		 */
		private static boolean isWrapped(MethodParameter parameter) {
			return QueryExecutionConverters.supports(parameter.getParameterType())
					|| ReactiveWrapperConverters.supports(parameter.getParameterType());
		}

		/**
		 * Returns whether the {@link MethodParameter} should be unwrapped.
		 *
		 * @param parameter must not be {@literal null}.
		 * @see QueryExecutionConverters
		 */
		private static boolean shouldUnwrap(MethodParameter parameter) {
			return QueryExecutionConverters.supportsUnwrapping(parameter.getParameterType())
					|| ReactiveWrappers.supports(parameter.getParameterType());
		}
	}

}
