package au.gov.api.repository

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.http.HttpMethod


@Configuration
@Profile("prod")
open class SecurityConfig : WebSecurityConfigurerAdapter() {

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.requiresChannel().anyRequest().requiresSecure()
        http.csrf().disable()
        http.authorizeRequests().antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
    }
}
