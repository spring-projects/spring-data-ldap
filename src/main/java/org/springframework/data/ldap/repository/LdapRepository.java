/*
 * Copyright 2016-2025 the original author or authors.
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

import java.util.List;
import java.util.Optional;

import javax.naming.Name;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.ldap.query.LdapQuery;

/**
 * Ldap specific extensions to {@link CrudRepository}.
 *
 * @author Mattias Hellborg Arthursson
 * @author Mark Paluch
 */
public interface LdapRepository<T> extends ListCrudRepository<T, Name> {

	/**
	 * Find one entry matching the specified query.
	 *
	 * @param ldapQuery the query specification.
	 * @return the found entry or <code>null</code> if no matching entry was found.
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if more than one entry matches the query.
	 */
	Optional<T> findOne(LdapQuery ldapQuery);

	/**
	 * Find all entries matching the specified query.
	 *
	 * @param ldapQuery the query specification.
	 * @return the entries matching the query.
	 */
	List<T> findAll(LdapQuery ldapQuery);

}
