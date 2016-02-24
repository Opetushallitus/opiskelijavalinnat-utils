package fi.vm.sade.oidgenerator;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OIDGeneratorTest {
    @Test
    public void henkilöOidUsesIBMChecksum() {
        String oid = OIDGenerator.generateOID(24);
        assertEquals(26, oid.length());
        String lastPart = oid.substring(oid.lastIndexOf('.') + 1);
        long number = Long.parseLong(lastPart.substring(0, lastPart.length() - 1));
        int checksum = Integer.parseInt(lastPart.substring(lastPart.length() - 1));
        assertEquals(OIDGenerator.ibmChecksum(number), checksum);
    }

    @Test
    public void organisaatioOidUsesIBMChecksum() {
        String oid = OIDGenerator.generateOID(10);
        assertEquals(26, oid.length());
        String lastPart = oid.substring(oid.lastIndexOf('.') + 1);
        long number = Long.parseLong(lastPart.substring(0, lastPart.length() - 1));
        int checksum = Integer.parseInt(lastPart.substring(lastPart.length() - 1));
        assertEquals(OIDGenerator.luhnChecksum(number), checksum);
    }
}
