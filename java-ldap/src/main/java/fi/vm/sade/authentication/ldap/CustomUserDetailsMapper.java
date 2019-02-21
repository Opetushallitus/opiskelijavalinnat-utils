package fi.vm.sade.authentication.ldap;
/*
 *
 * Copyright (c) 2012 The Finnish Board of Education - Opetushallitus
 *
 * This program is free software:  Licensed under the EUPL, Version 1.1 or - as
 * soon as they will be approved by the European Commission - subsequent versions
 * of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at: http://www.osor.eu/eupl/
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 */

import fi.vm.sade.generic.common.auth.SadeUserDetailsWrapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import java.util.Collection;

/**
 * extends spring LdapUserDetailsMapper to set user's oid into username instead of mail - not necessarily needed, might be able to configure same thing
 */
public class CustomUserDetailsMapper extends LdapUserDetailsMapper {
    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> authorities) {
        String oid = ctx.getStringAttribute("employeeNumber");
        if (oid == null) {
            oid = ctx.getStringAttribute("uid");
        }
        String lang = ctx.getStringAttribute("preferredLanguage");

        UserDetails userDetails = super.mapUserFromContext(ctx, oid, authorities);

        return new SadeUserDetailsWrapper(userDetails,lang);
    }
}
