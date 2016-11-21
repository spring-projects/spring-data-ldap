/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.ldap.repository.query;

import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.odm.core.ObjectDirectoryMapper;
import org.springframework.ldap.query.LdapQuery;

/**
 * @author Mattias Hellborg Arthursson
 */
public class PartTreeLdapRepositoryQuery extends AbstractLdapRepositoryQuery {

	private final PartTree partTree;
	private final Parameters<?, ?> parameters;
	private final ObjectDirectoryMapper objectDirectoryMapper;

	public PartTreeLdapRepositoryQuery(LdapQueryMethod queryMethod, Class<?> clazz, LdapOperations ldapOperations) {

		super(queryMethod, clazz, ldapOperations);

		partTree = new PartTree(queryMethod.getName(), clazz);
		parameters = queryMethod.getParameters();
		objectDirectoryMapper = ldapOperations.getObjectDirectoryMapper();
	}

	@Override
	protected LdapQuery createQuery(Object[] actualParameters) {

		org.springframework.data.ldap.repository.query.LdapQueryCreator queryCreator = new LdapQueryCreator(partTree,
				this.parameters, getClazz(), objectDirectoryMapper, actualParameters);
		return queryCreator.createQuery();
	}
}
