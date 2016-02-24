package fi.vm.sade.oidgenerator;

import java.util.Random;

/**
 *  OIDGenerator for generating OIDs for different node under the 1.2.246.562 root node.
 *
 *  Uses two "ibm" checksum algorithm for person oids (node 24) and "luhn" for other oids.
 */
public class OIDGenerator {
    private static final String root = "1.2.246.562";

    private static final int HENKILÖ_OID_NODE = 24;
    static long min = 1000000000L;
    static long max = 10000000000L;
    private static Random r = new Random();

    public static String generateOID(int node) {
        long number = min + ((long) (r.nextDouble() * (max - min)));
        return makeOID(node, number);
    }

    static String makeOID(final int node, final long number) {
        final int checkDigit = checksum(number, node);
        return root + "." + node + "." + number + checkDigit;
    }

    private static int checksum(final long number, final int node) {
        if (HENKILÖ_OID_NODE == node) {
            return ibmChecksum(number);
        } else {
            return luhnChecksum(number);
        }
    }

    static int ibmChecksum(long oid) {
        String oidStr = String.valueOf(oid);

        int sum = 0;
        int[] alternate = {7, 3 , 1};

        for (int i = oidStr.length() - 1, j = 0; i >= 0; i--, j++) {
            int n = Integer.parseInt(oidStr.substring(i, i + 1));

            sum += n * alternate[j % 3];
        }

        int checksum =  10 - sum % 10;
        if(checksum == 10) {
            return 0;
        }  else {
            return checksum;
        }
    }

    static int luhnChecksum(long oid) {
        String oidStr = String.valueOf(oid);

        int sum = 0;
        boolean alternate = true;

        for (int i = oidStr.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(oidStr.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }

        int checksum = 10 - sum % 10;
        if(checksum == 10) {
            return 0;
        }  else {
            return checksum;
        }
    }
}