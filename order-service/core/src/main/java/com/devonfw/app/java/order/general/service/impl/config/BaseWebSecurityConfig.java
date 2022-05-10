package com.devonfw.app.java.order.general.service.impl.config;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.sql.DataSource;

import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.devonfw.module.security.common.api.config.WebSecurityConfigurer;
import com.devonfw.module.security.common.impl.rest.AuthenticationSuccessHandlerSendingOkHttpStatusCode;
import com.devonfw.module.security.common.impl.rest.JsonUsernamePasswordAuthenticationFilter;
import com.devonfw.module.security.common.impl.rest.LogoutSuccessHandlerReturningOkHttpStatusCode;

/**
 * This type serves as a base class for extensions of the {@code WebSecurityConfigurerAdapter} and provides a default
 * configuration. <br/>
 * Security configuration is based on {@link WebSecurityConfigurerAdapter}. This configuration is by purpose designed
 * most simple for two channels of authentication: simple login form and rest-url.
 */
public abstract class BaseWebSecurityConfig extends WebSecurityConfigurerAdapter {

  @Inject
  private UserDetailsService userDetailsService;

  @Inject
  private PasswordEncoder passwordEncoder;

  @Inject
  private WebSecurityConfigurer webSecurityConfigurer;

  @Inject
  private DataSource dataSource;

  /**
   * Configure spring security to enable a simple webform-login + a simple rest login.
   */
  @Override
  public void configure(HttpSecurity http) throws Exception {

    String[] unsecuredResources = new String[] { "/login", "/security/**", "/services/rest/login",
    "/services/rest/logout" };

    http = http.headers().frameOptions().sameOrigin().and();
    http = http.httpBasic().and();

    // disable CSRF protection by default, use csrf starter to override.
    http = http.csrf().disable();
    // load starters as pluggins.
    http = this.webSecurityConfigurer.configure(http);

    http
        //
        .userDetailsService(this.userDetailsService)
        // define all urls that are not to be secured
        .authorizeRequests().antMatchers(unsecuredResources).permitAll().anyRequest().authenticated().and()
        // configure parameters for simple form login (and logout)
        .formLogin().successHandler(new SimpleUrlAuthenticationSuccessHandler()).defaultSuccessUrl("/")
        .failureUrl("/login.html?error").loginProcessingUrl("/j_spring_security_login").usernameParameter("username")
        .passwordParameter("password").and()
        // logout via POST is possible
        .logout().logoutSuccessUrl("/login.html").and()
        // register login and logout filter that handles rest logins
        .addFilterAfter(getSimpleRestAuthenticationFilter(), BasicAuthenticationFilter.class)
        .addFilterAfter(getSimpleRestLogoutFilter(), LogoutFilter.class);
  }

  /**
   * Create a simple filter that allows logout on a REST Url /services/rest/logout and returns a simple HTTP status 200
   * ok.
   *
   * @return the filter.
   */
  protected Filter getSimpleRestLogoutFilter() {

    LogoutFilter logoutFilter = new LogoutFilter(new LogoutSuccessHandlerReturningOkHttpStatusCode(),
        new SecurityContextLogoutHandler());

    // configure logout for rest logouts
    logoutFilter.setLogoutRequestMatcher(new AntPathRequestMatcher("/services/rest/logout"));

    return logoutFilter;
  }

  /**
   * Create a simple authentication filter for REST logins that reads user-credentials from a json-parameter and returns
   * status 200 instead of redirect after login.
   *
   * @return the {@link JsonUsernamePasswordAuthenticationFilter}.
   * @throws Exception if something goes wrong.
   */
  protected JsonUsernamePasswordAuthenticationFilter getSimpleRestAuthenticationFilter() throws Exception {

    JsonUsernamePasswordAuthenticationFilter jsonFilter = new JsonUsernamePasswordAuthenticationFilter(
        new AntPathRequestMatcher("/services/rest/login"));
    jsonFilter.setPasswordParameter("j_password");
    jsonFilter.setUsernameParameter("j_username");
    jsonFilter.setAuthenticationManager(authenticationManager());
    // set failurehandler that uses no redirect in case of login failure; just HTTP-status: 401
    jsonFilter.setAuthenticationManager(authenticationManagerBean());
    jsonFilter.setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler());
    // set successhandler that uses no redirect in case of login success; just HTTP-status: 200
    jsonFilter.setAuthenticationSuccessHandler(new AuthenticationSuccessHandlerSendingOkHttpStatusCode());
    return jsonFilter;
  }

  @SuppressWarnings("javadoc")
  @Inject
  public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {

    // base exercise
    auth.inMemoryAuthentication()
      .withUser("admin").password(this.passwordEncoder.encode("admin")).authorities("Admin").and()
      .withUser("waiter").password(this.passwordEncoder.encode("waiter")).authorities("Waiter").and()
      .withUser("cook").password(this.passwordEncoder.encode("cook")).authorities("Cook").and()
      .withUser("barkeeper").password(this.passwordEncoder.encode("barkeeper")).authorities("Barkeeper").and()
      .withUser("chief").password(this.passwordEncoder.encode("chief")).authorities("Chief");

    // additional
//    auth.jdbcAuthentication().dataSource(dataSource)
//      .usersByUsernameQuery("select username, password, enabled from users where username=?")
//      .authoritiesByUsernameQuery("select username, authority from authorities where username=?")
//      .passwordEncoder(passwordEncoder);
  }

}
