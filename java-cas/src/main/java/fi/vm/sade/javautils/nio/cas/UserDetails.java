package fi.vm.sade.javautils.nio.cas;

import java.util.Objects;
import java.util.Set;

public class UserDetails {
    private final String user;
    private final String henkiloOid;
    private final String kayttajaTyyppi;
    private final String idpEntityId;
    private final Set<String> roles;

    public UserDetails(String user, String henkiloOid, String kayttajaTyyppi, String idpEntityId, Set<String> roles) {
        this.user = user;
        this.henkiloOid = henkiloOid;
        this.kayttajaTyyppi = kayttajaTyyppi;
        this.idpEntityId = idpEntityId;
        this.roles = roles;
    }

    /** Never null. */
    public String getUser() {
        return user;
    }

    /** Never null. */
    public String getHenkiloOid() {
        return henkiloOid;
    }

    /** Can be null. */
    public String getKayttajaTyyppi() {
        return kayttajaTyyppi;
    }

    /** Can be null. */
    public String getIdpEntityId() {
        return idpEntityId;
    }

    /** Never null, but can be empty. */
    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public String toString() {
        return "UserDetails{" +
                "henkiloOid='" + henkiloOid + '\'' +
                ", user='" + user + '\'' +
                ", kayttajaTyyppi='" + kayttajaTyyppi + '\'' +
                ", idpEntityId='" + idpEntityId + '\'' +
                ", roles=" + roles +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserDetails that = (UserDetails) o;
        return Objects.equals(user, that.user) && Objects.equals(henkiloOid, that.henkiloOid) && Objects.equals(kayttajaTyyppi, that.kayttajaTyyppi) && Objects.equals(idpEntityId, that.idpEntityId) && Objects.equals(roles, that.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, henkiloOid, kayttajaTyyppi, idpEntityId, roles);
    }
}
