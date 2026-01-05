/*
 * Copyright 2024-present the original author or authors.
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

import org.springframework.util.ObjectUtils;

/**
 * Strategy interface to escape values for use in LDAP filters.
 * <p>
 * Accepts an LDAP filter value to be encoded (escaped) for String-based LDAP query usage as LDAP queries do not feature
 * an out-of-band parameter binding mechanism.
 * <p>
 * Make sure that your implementation escapes special characters in the value adequately to prevent injection attacks.
 *
 * @author Marcin Grzejszczak
 * @author Mark Paluch
 * @since 3.5
 */
public interface LdapEncoder {

	/**
	 * Encode a value for use in a filter.
	 *
	 * @param value the value to encode.
	 * @return a properly encoded representation of the supplied value.
	 */
	String encode(String value);

	/**
	 * {@link LdapEncoder} using {@link org.springframework.ldap.support.LdapEncoder#nameEncode(String)}. Encodes a value
	 * for use with a DN. Escapes for LDAP, not JNDI!
	 */
	class NameEncoder implements LdapEncoder {

		@Override
		public String encode(String value) {
			return org.springframework.ldap.support.LdapEncoder.nameEncode(value);
		}
	}

	/**
	 * Escape a value for use in a filter retaining asterisks ({@code *}) for like/contains searches.
	 */
	class LikeEncoder implements LdapEncoder {

		@Override
		public String encode(String value) {

			if (ObjectUtils.isEmpty(value)) {
				return value;
			}

			String[] substrings = value.split("\\*", -2);

			if (substrings.length == 1) {
				return org.springframework.ldap.support.LdapEncoder.filterEncode(substrings[0]);
			}

			StringBuilder buff = new StringBuilder();
			for (int i = 0; i < substrings.length; i++) {
				buff.append(org.springframework.ldap.support.LdapEncoder.filterEncode(substrings[i]));
				if (i < substrings.length - 1) {
					buff.append("*");
				}
			}

			return buff.toString();
		}
	}

}
