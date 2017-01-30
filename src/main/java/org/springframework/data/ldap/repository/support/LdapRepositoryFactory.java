/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.ldap.repository.support;

import static org.springframework.data.querydsl.QuerydslUtils.*;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Optional;

import org.springframework.data.ldap.repository.query.AnnotatedLdapRepositoryQuery;
import org.springframework.data.ldap.repository.query.LdapQueryMethod;
import org.springframework.data.ldap.repository.query.PartTreeLdapRepositoryQuery;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.util.Assert;

/**
 * Factory to create {@link org.springframework.data.ldap.repository.LdapRepository} instances.
 *
 * @author Mattias Hellborg Arthursson
 * @author Eddu Melendez
 * @author Mark Paluch
 */
public class LdapRepositoryFactory extends RepositoryFactorySupport {

	private final LdapQueryLookupStrategy queryLookupStrategy;
	private final LdapOperations ldapOperations;

	/**
	 * Creates a new {@link LdapRepositoryFactory}.
	 *
	 * @param ldapOperations must not be {@literal null}.
	 */
	public LdapRepositoryFactory(LdapOperations ldapOperations) {

		Assert.notNull(ldapOperations, "LdapOperations must not be null!");

		this.queryLookupStrategy = new LdapQueryLookupStrategy(ldapOperations);
		this.ldapOperations = ldapOperations;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getEntityInformation(java.lang.Class)
	 */
	@Override
	public <T, ID extends Serializable> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getRepositoryBaseClass(org.springframework.data.repository.core.RepositoryMetadata)
	 */
	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {

		boolean isQueryDslRepository = QUERY_DSL_PRESENT
				&& QuerydslPredicateExecutor.class.isAssignableFrom(metadata.getRepositoryInterface());

		return isQueryDslRepository ? QueryDslLdapRepository.class : SimpleLdapRepository.class;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getTargetRepository(org.springframework.data.repository.core.RepositoryInformation)
	 */
	@Override
	protected Object getTargetRepository(RepositoryInformation information) {
		return getTargetRepositoryViaReflection(information, ldapOperations, ldapOperations.getObjectDirectoryMapper(),
				information.getDomainType());
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getQueryLookupStrategy(org.springframework.data.repository.query.QueryLookupStrategy.Key, org.springframework.data.repository.query.EvaluationContextProvider)
	 */
	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(Key key,
			EvaluationContextProvider evaluationContextProvider) {
		return Optional.of(queryLookupStrategy);
	}

	private static final class LdapQueryLookupStrategy implements QueryLookupStrategy {

		private LdapOperations ldapOperations;

		/**
		 * @param ldapOperations
		 */
		public LdapQueryLookupStrategy(LdapOperations ldapOperations) {
			this.ldapOperations = ldapOperations;
		}

		@Override
		public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
				NamedQueries namedQueries) {

			LdapQueryMethod queryMethod = new LdapQueryMethod(method, metadata, factory);
			Class<?> domainType = metadata.getDomainType();

			if (queryMethod.hasQueryAnnotation()) {
				return new AnnotatedLdapRepositoryQuery(queryMethod, domainType, ldapOperations);
			} else {
				return new PartTreeLdapRepositoryQuery(queryMethod, domainType, ldapOperations);
			}
		}
	}
}
