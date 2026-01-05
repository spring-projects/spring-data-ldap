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
package org.springframework.data.ldap.repository.query;

import static org.springframework.data.ldap.repository.query.StringBasedQuery.BindingContext.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.expression.ValueExpression;
import org.springframework.data.expression.ValueExpressionParser;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.lang.Nullable;
import org.springframework.ldap.support.LdapEncoder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * String-based Query abstracting a query with parameter bindings.
 *
 * @author Marcin Grzejszczak
 * @author Mark Paluch
 * @since 3.5
 */
class StringBasedQuery {

	private final String query;
	private final LdapParameters parameters;
	private final List<ParameterBinding> queryParameterBindings = new ArrayList<>();
	private final ExpressionDependencies expressionDependencies;

	/**
	 * Create a new {@link StringBasedQuery} given {@code query}, {@link Parameters} and {@link ValueExpressionDelegate}.
	 *
	 * @param query must not be empty.
	 * @param parameters must not be {@literal null}.
	 * @param expressionParser must not be {@literal null}.
	 */
	public StringBasedQuery(String query, LdapParameters parameters, ValueExpressionDelegate expressionParser) {

		this.query = ParameterBindingParser.parseAndCollectParameterBindingsFromQueryIntoBindings(query,
				this.queryParameterBindings, expressionParser);
		this.parameters = parameters;
		this.expressionDependencies = createExpressionDependencies();
	}

	private ExpressionDependencies createExpressionDependencies() {

		if (queryParameterBindings.isEmpty()) {
			return ExpressionDependencies.none();
		}

		List<ExpressionDependencies> dependencies = new ArrayList<>();

		for (ParameterBinding binding : queryParameterBindings) {
			if (binding.isExpression()) {
				dependencies.add(binding.getRequiredExpression().getExpressionDependencies());
			}
		}

		return ExpressionDependencies.merged(dependencies);
	}

	/**
	 * Obtain {@link ExpressionDependencies} from the parsed query.
	 *
	 * @return the {@link ExpressionDependencies} from the parsed query.
	 */
	public ExpressionDependencies getExpressionDependencies() {
		return expressionDependencies;
	}

	/**
	 * Bind the query to actual parameters using {@link LdapParameterAccessor},
	 *
	 * @param parameterAccessor must not be {@literal null}.
	 * @param evaluator must not be {@literal null}.
	 * @return the bound String query containing formatted parameters.
	 */
	public String bindQuery(LdapParameterAccessor parameterAccessor, Function<ValueExpression, Object> evaluator) {

		Assert.notNull(parameterAccessor, "LdapParameterAccessor must not be null");
		Assert.notNull(evaluator, "ExpressionEvaluator must not be null");

		BindingContext bindingContext = new BindingContext(this.parameters, parameterAccessor, this.queryParameterBindings,
				evaluator);

		List<Object> arguments = bindingContext.getBindingValues();

		return ParameterBinder.bind(this.query, arguments);
	}

	/**
	 * A parser that extracts the parameter bindings from a given query string.
	 *
	 * @author Mark Paluch
	 */
	static class ParameterBindingParser {

		private static final char CURRLY_BRACE_OPEN = '{';
		private static final char CURRLY_BRACE_CLOSE = '}';

		private static final Pattern INDEX_PARAMETER_BINDING_PATTERN = Pattern.compile("\\?(\\d+)");
		private static final Pattern NAMED_PARAMETER_BINDING_PATTERN = Pattern.compile("\\:(\\w+)");
		private static final Pattern INDEX_BASED_EXPRESSION_PATTERN = Pattern.compile("\\?\\#\\{");
		private static final Pattern NAME_BASED_EXPRESSION_PATTERN = Pattern.compile("\\:\\#\\{");
		private static final Pattern INDEX_BASED_PROPERTY_PLACEHOLDER_PATTERN = Pattern.compile("\\?\\$\\{");
		private static final Pattern NAME_BASED_PROPERTY_PLACEHOLDER_PATTERN = Pattern.compile("\\:\\$\\{");

		private static final Set<Pattern> VALUE_EXPRESSION_PATTERNS = Set.of(INDEX_BASED_EXPRESSION_PATTERN,
				NAME_BASED_EXPRESSION_PATTERN, INDEX_BASED_PROPERTY_PLACEHOLDER_PATTERN,
				NAME_BASED_PROPERTY_PLACEHOLDER_PATTERN);

		private static final String ARGUMENT_PLACEHOLDER = "?_param_?";

		/**
		 * Returns a list of {@link ParameterBinding}s found in the given {@code input}.
		 *
		 * @param input can be {@literal null} or empty.
		 * @param bindings must not be {@literal null}.
		 * @param expressionParser must not be {@literal null}.
		 * @return a list of {@link ParameterBinding}s found in the given {@code input}.
		 */
		public static String parseAndCollectParameterBindingsFromQueryIntoBindings(String input,
				List<ParameterBinding> bindings, ValueExpressionParser expressionParser) {

			if (!StringUtils.hasText(input)) {
				return input;
			}

			Assert.notNull(bindings, "Parameter bindings must not be null");

			return transformQueryAndCollectExpressionParametersIntoBindings(input, bindings, expressionParser);
		}

		private static String transformQueryAndCollectExpressionParametersIntoBindings(String input,
				List<ParameterBinding> bindings, ValueExpressionParser expressionParser) {

			StringBuilder result = new StringBuilder();

			int startIndex = 0;
			int currentPosition = 0;

			while (currentPosition < input.length()) {

				Matcher matcher = findNextBindingOrExpression(input, currentPosition);

				// no expression parameter found
				if (matcher == null) {
					break;
				}

				int exprStart = matcher.start();
				currentPosition = exprStart;

				if (isValueExpression(matcher)) {
					// eat parameter expression
					int curlyBraceOpenCount = 1;
					currentPosition += 3;

					while (curlyBraceOpenCount > 0 && currentPosition < input.length()) {
						switch (input.charAt(currentPosition++)) {
							case CURRLY_BRACE_OPEN:
								curlyBraceOpenCount++;
								break;
							case CURRLY_BRACE_CLOSE:
								curlyBraceOpenCount--;
								break;
							default:
						}
					}
					result.append(input.subSequence(startIndex, exprStart));
				} else {
					result.append(input.subSequence(startIndex, exprStart));
				}

				result.append(ARGUMENT_PLACEHOLDER);

				if (isValueExpression(matcher)) {
					bindings.add(ParameterBinding
							.expression(expressionParser.parse(input.substring(exprStart + 1, currentPosition)), true));
				} else {
					if (matcher.pattern() == INDEX_PARAMETER_BINDING_PATTERN) {
						bindings.add(ParameterBinding.indexed(Integer.parseInt(matcher.group(1))));
					} else {
						bindings.add(ParameterBinding.named(matcher.group(1)));
					}

					currentPosition = matcher.end();
				}

				startIndex = currentPosition;
			}

			return result.append(input.subSequence(currentPosition, input.length())).toString();
		}

		private static boolean isValueExpression(Matcher matcher) {
			return VALUE_EXPRESSION_PATTERNS.contains(matcher.pattern());
		}

		@Nullable
		private static Matcher findNextBindingOrExpression(String input, int startPosition) {

			List<Matcher> matchers = new ArrayList<>(6);

			matchers.add(INDEX_PARAMETER_BINDING_PATTERN.matcher(input));
			matchers.add(NAMED_PARAMETER_BINDING_PATTERN.matcher(input));
			matchers.add(INDEX_BASED_EXPRESSION_PATTERN.matcher(input));
			matchers.add(NAME_BASED_EXPRESSION_PATTERN.matcher(input));
			matchers.add(INDEX_BASED_PROPERTY_PLACEHOLDER_PATTERN.matcher(input));
			matchers.add(NAME_BASED_PROPERTY_PLACEHOLDER_PATTERN.matcher(input));

			Map<Integer, Matcher> matcherMap = new TreeMap<>();

			for (Matcher matcher : matchers) {
				if (matcher.find(startPosition)) {
					matcherMap.put(matcher.start(), matcher);
				}
			}

			return (matcherMap.isEmpty() ? null : matcherMap.values().iterator().next());
		}
	}

	/**
	 * A parser that extracts the parameter bindings from a given query string.
	 *
	 * @author Mark Paluch
	 */
	static class ParameterBinder {

		private static final String ARGUMENT_PLACEHOLDER = "?_param_?";
		private static final Pattern ARGUMENT_PLACEHOLDER_PATTERN = Pattern.compile(Pattern.quote(ARGUMENT_PLACEHOLDER));

		public static String bind(String input, List<Object> parameters) {

			if (parameters.isEmpty()) {
				return input;
			}

			StringBuilder result = new StringBuilder();

			int startIndex = 0;
			int currentPosition = 0;
			int parameterIndex = 0;

			Matcher matcher = ARGUMENT_PLACEHOLDER_PATTERN.matcher(input);

			while (currentPosition < input.length()) {

				if (!matcher.find()) {
					break;
				}

				int exprStart = matcher.start();

				result.append(input.subSequence(startIndex, exprStart)).append(parameters.get(parameterIndex));

				parameterIndex++;
				currentPosition = matcher.end();
				startIndex = currentPosition;
			}

			return result.append(input.subSequence(currentPosition, input.length())).toString();
		}
	}

	/**
	 * Value object capturing the binding context to provide {@link #getBindingValues() binding values} for queries.
	 *
	 * @author Mark Paluch
	 */
	static class BindingContext {

		private final LdapParameters parameters;
		private final ParameterAccessor parameterAccessor;
		private final List<ParameterBinding> bindings;
		private final Function<ValueExpression, Object> evaluator;

		/**
		 * Create new {@link BindingContext}.
		 */
		BindingContext(LdapParameters parameters, ParameterAccessor parameterAccessor, List<ParameterBinding> bindings,
				Function<ValueExpression, Object> evaluator) {

			this.parameters = parameters;
			this.parameterAccessor = parameterAccessor;
			this.bindings = bindings;
			this.evaluator = evaluator;
		}

		/**
		 * @return {@literal true} when list of bindings is not empty.
		 */
		private boolean hasBindings() {
			return !bindings.isEmpty();
		}

		/**
		 * Bind values provided by {@link LdapParameterAccessor} to placeholders in {@link BindingContext} while considering
		 * potential conversions and parameter types.
		 *
		 * @return {@literal null} if given {@code raw} value is empty.
		 */
		public List<Object> getBindingValues() {

			if (!hasBindings()) {
				return Collections.emptyList();
			}

			List<Object> parameters = new ArrayList<>(bindings.size());

			for (ParameterBinding binding : bindings) {
				Object parameterValueForBinding = getParameterValueForBinding(binding);
				parameters.add(parameterValueForBinding);
			}

			return parameters;
		}

		/**
		 * Return the value to be used for the given {@link ParameterBinding}.
		 *
		 * @param binding must not be {@literal null}.
		 * @return the value used for the given {@link ParameterBinding}.
		 */
		@Nullable
		private Object getParameterValueForBinding(ParameterBinding binding) {

			if (binding.isExpression()) {
				return evaluator.apply(binding.getRequiredExpression());
			}

			int index = binding.isNamed() ? getParameterIndex(parameters, binding.getRequiredParameterName())
					: binding.getParameterIndex();
			Object value = parameterAccessor.getBindableValue(index);

			if (value == null) {
				return null;
			}

			String toString = value.toString();
			LdapParameters.LdapParameter parameter = parameters.getBindableParameter(index);

			return parameter.hasLdapEncoder() ? parameter.getLdapEncoder().encode(toString)
					: LdapEncoder.filterEncode(toString);
		}

		private int getParameterIndex(Parameters<?, ?> parameters, String parameterName) {

			for (Parameter parameter : parameters) {

				if (parameter.getName().filter(it -> it.equals(parameterName)).isPresent()) {
					return parameter.getIndex();
				}
			}

			throw new IllegalArgumentException(
					String.format("Invalid parameter name; Cannot resolve parameter [%s]", parameterName));
		}

		/**
		 * A generic parameter binding with name or position information.
		 *
		 * @author Mark Paluch
		 */
		static class ParameterBinding {

			private final int parameterIndex;
			private final @Nullable ValueExpression expression;
			private final @Nullable String parameterName;

			private ParameterBinding(int parameterIndex, @Nullable ValueExpression expression,
					@Nullable String parameterName) {

				this.parameterIndex = parameterIndex;
				this.expression = expression;
				this.parameterName = parameterName;
			}

			static ParameterBinding expression(ValueExpression expression, boolean quoted) {
				return new ParameterBinding(-1, expression, null);
			}

			static ParameterBinding indexed(int parameterIndex) {
				return new ParameterBinding(parameterIndex, null, null);
			}

			static ParameterBinding named(String name) {
				return new ParameterBinding(-1, null, name);
			}

			boolean isNamed() {
				return (parameterName != null);
			}

			int getParameterIndex() {
				return parameterIndex;
			}

			ValueExpression getRequiredExpression() {

				Assert.state(expression != null, "ParameterBinding is not an expression");
				return expression;
			}

			boolean isExpression() {
				return (this.expression != null);
			}

			String getRequiredParameterName() {

				Assert.state(parameterName != null, "ParameterBinding is not named");

				return parameterName;
			}
		}
	}
}
