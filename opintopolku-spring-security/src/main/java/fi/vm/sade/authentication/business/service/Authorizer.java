package fi.vm.sade.authentication.business.service;

import fi.vm.sade.authorization.NotAuthorizedException;

public interface Authorizer {
    void checkUserIsNotSame(String userOid) throws NotAuthorizedException;

    void checkOrganisationAccess(String targetOrganisationOid, String... roles) throws NotAuthorizedException;
}
