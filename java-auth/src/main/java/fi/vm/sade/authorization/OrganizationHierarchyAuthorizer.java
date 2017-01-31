package fi.vm.sade.authorization;

import java.util.*;

public class OrganizationHierarchyAuthorizer {
    public static final String ANY_ROLE = "*";

    private OrganizationOidProvider oidProvider;


    public OrganizationHierarchyAuthorizer(OrganizationOidProvider oidProvider) {
        this.oidProvider = oidProvider;
    }

    public void checkAccessToTargetOrParentOrganization(List<String> userRoles, String targetOrganisationOid, String[] requiredRoles) throws NotAuthorizedException {
        if (requiredRoles == null || requiredRoles.length == 0) {
            throw new NotAuthorizedException("No required roles.");
        }

        List<String> targetOrganisationAndParentsOids = oidProvider.getSelfAndParentOidsCached(targetOrganisationOid);
        if (targetOrganisationAndParentsOids == null || targetOrganisationAndParentsOids.size() == 0) {
            throw new NotAuthorizedException("Target organization and parents oids cannot be found.");
        }

        for (String role : requiredRoles) {
            for (String oid : targetOrganisationAndParentsOids) {
                for (String userRole : userRoles) {
                    if (roleMatchesToAuthority(role, userRole) && authorityIsTargetedToOrganisation(userRole, oid)) {
                        return;
                    }
                }
            }
        }
        final String msg = "Not authorized! targetOrganisationAndParentsOids: " + targetOrganisationAndParentsOids + ", requiredRoles: " + Arrays.asList(requiredRoles) + ", userRoles: " + userRoles;
        throw new NotAuthorizedException(msg);
    }

    public void checkAccessToGivenRoles(List<String> userRoles, String[] requiredRoles) throws NotAuthorizedException {
        if (requiredRoles == null || requiredRoles.length == 0) {
            throw new NotAuthorizedException("No required roles.");
        }

        for(String role: requiredRoles) {
            for(String authority : userRoles) {
                if(roleMatchesToAuthority(role, authority)) {
                    return;
                }
            }
        }

        final String msg = "Not authorized! requiredRoles: " + Arrays.asList(requiredRoles) + ", userRoles: " + userRoles;
        throw new NotAuthorizedException(msg);
    }

    public static String getOrganizationTheUserHasPermissionTo(List<String> userRoles, String... permissionCandidates) {
        List<String> whatRoles = Arrays.asList(permissionCandidates);
        Set<String> orgs = new HashSet<String>();
        for (String userRole : userRoles) {
            if (!userRole.endsWith("READ") && !userRole.endsWith("READ_UPDATE") && !userRole.endsWith("CRUD")) {
                int x = userRole.lastIndexOf("_");
                if (x != -1) {
                    String rolePart = userRole.substring(0, x);
                    if (whatRoles.contains(rolePart)) {
                        String orgPart = userRole.substring(x + 1);
                        orgs.add(orgPart);
                    }
                }
            }
        }
        if (orgs.isEmpty()) {
            return null;
        }
        if (orgs.size() > 1) {
            throw new RuntimeException("Not supported: user has role " + whatRoles + " to more than 1 organisaatios: " + orgs);
        }
        return orgs.iterator().next();
    }

    private static boolean roleMatchesToAuthority(String role, String authority) {
        if (ANY_ROLE.equals(role)) {
            return true;
        }
        role = stripRolePrefix(role);
        return authority.contains(role);
    }

    private static String stripRolePrefix(String role) {
        return role.replace("APP_", "").replace("ROLE_", "");
    }

    private static boolean authorityIsTargetedToOrganisation(String authority, String oid) {
        return authority.endsWith(oid);
    }

    public static OrganizationHierarchyAuthorizer createMockAuthorizer(final String parentOrg, final String[] childOrgs) {
        return new OrganizationHierarchyAuthorizer(new OrganizationOidProvider(){
            @Override
            public List<String> getSelfAndParentOids(String organisaatioOid) {
                if (parentOrg.equals(organisaatioOid)) {
                    return Arrays.asList(organisaatioOid);
                }
                if (Arrays.asList(childOrgs).contains(organisaatioOid)) {
                    return Arrays.asList(organisaatioOid, parentOrg);
                }
                return new ArrayList<String>();
            }
        });
    }
}
