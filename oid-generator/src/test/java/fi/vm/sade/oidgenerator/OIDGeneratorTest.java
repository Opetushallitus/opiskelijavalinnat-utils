package fi.vm.sade.oidgenerator;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OIDGeneratorTest {
    @Test
    public void henkilöOidUsesIBMChecksum() {
        String oid = OIDGenerator.generateOID(24);
        String lastPart = oid.substring(oid.lastIndexOf('.') + 1);
        long number = Long.parseLong(lastPart.substring(0, lastPart.length() - 1));
        int checksum = Integer.parseInt(lastPart.substring(lastPart.length() - 1));
        assertEquals(OIDGenerator.ibmChecksum(number), checksum);
    }

    @Test
    public void henkilöOidHasConsistentLength() {
        for (int i = 0; i < 1000000; i++) {
            String oid = OIDGenerator.generateOID(24);
            assertEquals("try " + i, 26, oid.length());
        }
    }

    @Test
    public void organisaatioOidUsesLuhnChecksum() {
        String oid = OIDGenerator.generateOID(10);
        String lastPart = oid.substring(oid.lastIndexOf('.') + 1);
        long number = Long.parseLong(lastPart.substring(0, lastPart.length() - 1));
        int checksum = Integer.parseInt(lastPart.substring(lastPart.length() - 1));
        assertEquals(OIDGenerator.luhnChecksum(number), checksum);
    }

    @Test
    public void organisaatioOidHasConsistentLength() {
        for (int i = 0; i < 1000000; i++) {
            String oid = OIDGenerator.generateOID(10);
            assertEquals("try " + i, 26, oid.length());
        }
    }
}
