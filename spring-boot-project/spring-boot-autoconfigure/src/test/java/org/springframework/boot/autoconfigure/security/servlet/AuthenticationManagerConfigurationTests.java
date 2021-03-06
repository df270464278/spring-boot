/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.servlet;

import org.junit.Test;

import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AuthenticationManagerConfiguration}.
 *
 * @author Madhura Bhave
 */
public class AuthenticationManagerConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	public void userDetailsServiceWhenPasswordEncoderAbsentAndDefaultPassword() {
		this.contextRunner.withUserConfiguration(TestSecurityConfiguration.class,
				AuthenticationManagerConfiguration.class).run(((context) -> {
					InMemoryUserDetailsManager userDetailsService = context
							.getBean(InMemoryUserDetailsManager.class);
					String password = userDetailsService.loadUserByUsername("user")
							.getPassword();
					assertThat(password).startsWith("{noop}");
				}));
	}

	@Test
	public void userDetailsServiceWhenPasswordEncoderAbsentAndRawPassword() {
		testPasswordEncoding(TestSecurityConfiguration.class, "secret", "{noop}secret");
	}

	@Test
	public void userDetailsServiceWhenPasswordEncoderAbsentAndEncodedPassword() {
		String password = "{bcrypt}$2a$10$sCBi9fy9814vUPf2ZRbtp.fR5/VgRk2iBFZ.ypu5IyZ28bZgxrVDa";
		testPasswordEncoding(TestSecurityConfiguration.class, password, password);
	}

	@Test
	public void userDetailsServiceWhenPasswordEncoderBeanPresent() {
		testPasswordEncoding(TestConfigWithPasswordEncoder.class, "secret", "secret");
	}

	@Test
	public void userDetailsServiceWhenClientRegistrationRepositoryBeanPresent() {
		this.contextRunner
				.withUserConfiguration(TestConfigWithClientRegistrationRepository.class,
						AuthenticationManagerConfiguration.class)
				.run(((context) -> assertThat(context)
						.doesNotHaveBean(InMemoryUserDetailsManager.class)));
	}

	private void testPasswordEncoding(Class<?> configClass, String providedPassword,
			String expectedPassword) {
		this.contextRunner
				.withUserConfiguration(configClass,
						AuthenticationManagerConfiguration.class)
				.withPropertyValues("spring.security.user.password=" + providedPassword)
				.run(((context) -> {
					InMemoryUserDetailsManager userDetailsService = context
							.getBean(InMemoryUserDetailsManager.class);
					String password = userDetailsService.loadUserByUsername("user")
							.getPassword();
					assertThat(password).isEqualTo(expectedPassword);
				}));
	}

	@Configuration
	@EnableWebSecurity
	@EnableConfigurationProperties(SecurityProperties.class)
	protected static class TestSecurityConfiguration {

	}

	@Configuration
	@Import(TestSecurityConfiguration.class)
	protected static class TestConfigWithPasswordEncoder {

		@Bean
		public PasswordEncoder passwordEncoder() {
			return mock(PasswordEncoder.class);
		}

	}

	@Configuration
	@Import(TestSecurityConfiguration.class)
	protected static class TestConfigWithClientRegistrationRepository {

		@Bean
		public ClientRegistrationRepository clientRegistrationRepository() {
			return mock(ClientRegistrationRepository.class);
		}

	}

}
