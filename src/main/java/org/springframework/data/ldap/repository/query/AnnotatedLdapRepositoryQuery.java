/*
 * Copyright 2016-present the original author or authors.
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

import static org.springframework.ldap.query.LdapQueryBuilder.*;

import org.springframework.data.domain.Limit;
import org.springframework.data.expression.ValueEvaluationContext;
import org.springframework.data.expression.ValueEvaluationContextProvider;
import org.springframework.data.ldap.repository.Query;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.util.Assert;

/**
 * Handles queries for repository methods annotated with {@link org.springframework.data.ldap.repository.Query}.
 *
 * @author Mattias Hellborg Arthursson
 * @author Mark Paluch
 * @author Marcin Grzejszczak
 */
public class AnnotatedLdapRepositoryQuery extends AbstractLdapRepositoryQuery {

	private final Query queryAnnotation;
	private final StringBasedQuery query;
	private final StringBasedQuery base;
	private final ValueEvaluationContextProvider valueContextProvider;

	/**
	 * Construct a new instance.
	 *
	 * @param queryMethod the QueryMethod.
	 * @param entityType the managed class.
	 * @param ldapOperations the LdapOperations instance to use.
	 * @param mappingContext must not be {@literal null}.
	 * @param instantiators must not be {@literal null}.
	 * @deprecated use the constructor with {@link ValueExpressionDelegate}
	 */
	@Deprecated(since = "3.4")
	public AnnotatedLdapRepositoryQuery(LdapQueryMethod queryMethod, Class<?> entityType, LdapOperations ldapOperations,
			MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> mappingContext,
			EntityInstantiators instantiators) {

		this(queryMethod, entityType, ldapOperations, mappingContext, instantiators, ValueExpressionDelegate.create());
	}

	/**
	 * Construct a new instance.
	 *
	 * @param queryMethod the QueryMethod.
	 * @param entityType the managed class.
	 * @param ldapOperations the LdapOperations instance to use.
	 * @param mappingContext must not be {@literal null}.
	 * @param instantiators must not be {@literal null}.
	 * @param valueExpressionDelegate must not be {@literal null}
	 * @since 3.5
	 */
	public AnnotatedLdapRepositoryQuery(LdapQueryMethod queryMethod, Class<?> entityType, LdapOperations ldapOperations,
			MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> mappingContext,
			EntityInstantiators instantiators, ValueExpressionDelegate valueExpressionDelegate) {

		super(queryMethod, entityType, ldapOperations, mappingContext, instantiators);

		Assert.notNull(queryMethod.getQueryAnnotation(), "Annotation must be present");
		Assert.hasLength(queryMethod.getQueryAnnotation().value(), "Query filter must be specified");

		this.queryAnnotation = queryMethod.getRequiredQueryAnnotation();
		this.query = new StringBasedQuery(queryAnnotation.value(), queryMethod.getParameters(), valueExpressionDelegate);
		this.base = new StringBasedQuery(queryAnnotation.base(), queryMethod.getParameters(), valueExpressionDelegate);
		this.valueContextProvider = valueExpressionDelegate.createValueContextProvider(getQueryMethod().getParameters());
	}

	@Override
	protected LdapQuery createQuery(LdapParameterAccessor parameters) {

		String query = bind(parameters, valueContextProvider, this.query);
		String base = bind(parameters, valueContextProvider, this.base);

		int countLimit = queryAnnotation.countLimit();

		if (getQueryMethod().getParameters().hasLimitParameter()) {
			Limit limit = parameters.getLimit();
			countLimit = limit.isLimited() ? limit.max() : 0;
		}

		return query().base(base) //
				.searchScope(queryAnnotation.searchScope()) //
				.countLimit(countLimit) //
				.timeLimit(queryAnnotation.timeLimit()) //
				.filter(query, parameters.getBindableParameterValues());
	}

	private String bind(LdapParameterAccessor parameters, ValueEvaluationContextProvider valueContextProvider, StringBasedQuery query) {

		ValueEvaluationContext evaluationContext = valueContextProvider
				.getEvaluationContext(parameters.getValues(), query.getExpressionDependencies());

		return query.bindQuery(parameters,
				expression -> expression.evaluate(evaluationContext));
	}

}
