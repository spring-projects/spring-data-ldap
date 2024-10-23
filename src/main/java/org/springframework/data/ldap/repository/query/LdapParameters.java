/*
 * Copyright 2024 the original author or authors.
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
import org.springframework.data.geo.Distance;
import org.springframework.data.ldap.repository.LdapEncoder;
import org.springframework.data.ldap.repository.LdapParam;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersSource;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

/**
 * Custom extension of {@link Parameters} discovering additional
 *
 * @author Marcin Grzejszczak
 * @since 3.5.0
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

	@Nullable
	LdapEncoder encoderForParameterWithName(String parameterName) {
		for (LdapParameter parameter : this) {
			if (parameterName.equals(parameter.getName().orElse(null))) {
				LdapParam ldapParam = parameter.parameter.getParameterAnnotation(LdapParam.class);
				if (ldapParam == null) {
					return null;
				}
				Class<? extends LdapEncoder> encoder = ldapParam.encoder();
				try {
					return encoder.getDeclaredConstructor().newInstance();
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}
		}
		return null;
	}

	/**
	 * Custom {@link Parameter} implementation adding parameters of type {@link Distance} to the special ones.
	 *
	 * @author Marcin Grzejszczak
	 */
	protected static class LdapParameter extends Parameter {

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
		}
	}

}
