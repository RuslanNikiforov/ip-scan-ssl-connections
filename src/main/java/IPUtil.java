import org.apache.hc.core5.net.InetAddressUtils;


import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class IPUtil {

    public static String getIPv4Range(String subnet) {
        String[] ipAndMask = subnet.split("/");
        int[] subnetMask = getSubnetMask(Integer.parseInt(ipAndMask[1]));
        String ipString = ipAndMask[0];
        int[] startRange = new int[4];
        int[] endRange = new int[4];
        StringBuilder resultString = new StringBuilder();
        List<Integer> list = Arrays.stream(ipString.split("\\.")).map(Integer::parseInt).
                collect(Collectors.toList());
        for (int i = 0; i < list.size(); i++) {
            startRange[i] = (list.get(i) & subnetMask[i]);
            resultString.append(startRange[i]);
            if(i != 3) {
                resultString.append(".");
            }
        }
        resultString.append("-");
        for (int i = 0; i < endRange.length; i++) {
            endRange[i] = (startRange[i] | (~subnetMask[i]));
            if(endRange[i] < 0) {
                endRange[i] += 256;
            }
            resultString.append(endRange[i]);
            if(i != 3) {
                resultString.append(".");
            }
        }
        return resultString.toString();


    }

    public static int[] getSubnetMask(int number) {
        int[] bytes = new int[4];
        if (number >= 0 && number <= 32) {
            for (int i = 0; i < bytes.length; i++) {
                if(number < 0) {
                    bytes[i] = 0;
                }
                else if (number - 8 < 0) {
                    bytes[i] = (((1 << number) - 1) << (8 - number));
                } else {
                    bytes[i] = 255;
                }
                number -= 8;
            }
        }
        return bytes;
    }

    public static List<InetAddress> parseIPv4ByRange(String range) throws UnknownHostException {
        String[] addresses = range.split("-");
        int firstIP = convertIPToInt(addresses[0]);
        int lastIP = convertIPToInt(addresses[1]);
        ArrayList<InetAddress> resultRange = new ArrayList<>();
        for (int i = firstIP; i <= lastIP; i++) {
            InetAddress inetAddress = convertIntToIP(i);
            resultRange.add(inetAddress);
        }
        return resultRange;
    }

    public static int convertIPToInt(String host) throws UnknownHostException {
        return ByteBuffer.allocate(4).put(Inet4Address.getByName(host).getAddress()).getInt(0);
    }

    public static InetAddress convertIntToIP(int address) throws UnknownHostException {
        return Inet4Address.getByAddress(ByteBuffer.allocate(4).putInt(address).array());
    }
}
