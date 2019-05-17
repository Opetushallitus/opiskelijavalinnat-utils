package fi.vm.sade.suomifi.valtuudet;

import java.util.List;

public class OrganisationDto {

    /**
     * Business id.
     */
    public String identifier;

    /**
     * Organisation name.
     */
    public String name;

    /**
     * Roles person has to organisation.
     */
    public List<String> roles;

}
