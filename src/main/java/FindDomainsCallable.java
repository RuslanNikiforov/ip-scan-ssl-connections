import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.Callable;

public class FindDomainsCallable implements Callable<List<String>> {

    private final String ip;

    public FindDomainsCallable(String ip) {
        this.ip = ip;
    }

    @Override
    public List<String> call() {
        Certificate[] certificate;
        try {
            certificate = SSLService.getCertificate(ip);
        } catch (Exception e) {

            return null;
        }
        if (certificate != null) {
            try {
                return SSLService.getDNSList(certificate[0]);
            } catch (CertificateException e) {

            }
        }
        return null;
    }
}
