# UserDetailsService

Spring Securityn UserDetailsService-rajapinnan toteutus, joka hakee käyttäjän tiedot käyttöoikeuspalvelusta.

## Konfigurointi

XML

    <beans:bean id="userDetailsService" class="fi.vm.sade.javautils.kayttooikeusclient.OphUserDetailsServiceImpl">
        <beans:constructor-arg index="0" value="${host.alb}" />
        <beans:constructor-arg index="1" value="kutsuvan_palvelun_tunniste_esim_oppijanumerorekisteri" />
    </beans:bean>

    <beans:bean id="casAuthenticationProvider" class="org.springframework.security.cas.authentication.CasAuthenticationProvider">
        <beans:property name="userDetailsService" ref="userDetailsService"/>
        ...
    </beans:bean>

Java

    @Bean
    public UserDetailsService userDetailsService(Environment environment) {
        return new OphUserDetailsServiceImpl(environment.getRequiredProperty("host.alb"), "kutsuvan_palvelun_tunniste_esim_oppijanumerorekisteri");
    }

    @Bean
    public CasAuthenticationProvider casAuthenticationProvider(UserDetailsService userDetailsService) {
        CasAuthenticationProvider casAuthenticationProvider = new CasAuthenticationProvider();
        casAuthenticationProvider.setUserDetailsService(userDetailsService);
        ...
        return casAuthenticationProvider;
    }
