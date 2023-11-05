import jakarta.servlet.ServletOutputStream;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;


public class SSLService {

    public static Certificate[] getCertificate(String host) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        TrustManager trMng = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

        };
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new TrustManager[]{trMng}, null);
        SSLSocketFactory factory = sslContext.getSocketFactory();
        try (SSLSocket sslSocket = (SSLSocket) factory.createSocket()) {
            sslSocket.connect(new InetSocketAddress(host, 443), 300);
            sslSocket.setSoTimeout(2000);
            sslSocket.startHandshake();
            return sslSocket.getSession().getPeerCertificates();
        } catch (SocketTimeoutException e) {
            //System.out.println("Connection Timeout. SSL connection is not available");
            return null;
        } catch (SSLHandshakeException e) {
            System.out.println("Handshake Exception. SSL connection is not available");
            return null;
        }
    }

    public static List<String> getDNSList(Certificate certificate) throws CertificateException {
        var resultList = new ArrayList<String>();
        if (certificate == null) {
            return null;
        }
        ByteArrayInputStream is = new ByteArrayInputStream(certificate.getEncoded());
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate x509 = (X509Certificate) certificateFactory.generateCertificate(is);
        Collection<List<?>> subjectAlternativeNames = x509.getSubjectAlternativeNames();
        if (subjectAlternativeNames != null) {
            for (List<?> list : subjectAlternativeNames) {
                if (list.get(0).equals(2)) {
                    resultList.add(list.get(1).toString());
                }
            }
            return resultList;
        }
        return null;
    }

    public static List<String> getAllDomainsInRange(String range, int threadsNumber, int maxWaitTimeInSec) throws UnknownHostException, ExecutionException {
        List<String> resultList = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(threadsNumber);
        List<InetAddress> inetAddresses = IPUtil.parseIPv4ByRange(range);
        List<Future<List<String>>> futures = new ArrayList<>();
        for (int i = 0; i < inetAddresses.size(); i++) {
            Callable<List<String>> task = new FindDomainsCallable(inetAddresses.get(i).getHostAddress());
            futures.add(executorService.submit(task));

        }
        try {
            executorService.shutdown();
            boolean isAllTasksSucceed = executorService.awaitTermination(maxWaitTimeInSec, TimeUnit.SECONDS);
            System.out.println(isAllTasksSucceed);
            System.out.println("All futures: " + futures.size());
            if (isAllTasksSucceed) {
                for (Future<List<String>> domain : futures) {
                    if (domain.get() != null) {
                        resultList.add(domain.get().toString());
                    }
                }
                System.out.println("Completed futures: " + futures.size());
            } else {
                executorService.shutdownNow();
                System.out.println("ExecutorService has been shutdown");
                int k = 0;
                for (Future<List<String>> domain : futures) {
                    if (domain.isDone()) {
                        try {
                            if ((domain.get() != null)) {
                                resultList.add(domain.get().toString());
                                System.out.println(domain.get());
                            }
                        } catch (ExecutionException executionException) {
                            System.out.println("exception was thrown in the future obj");
                        }
                        k++;
                    }
                }
                System.out.println("Completed futures: " + k);
            }
        } catch (InterruptedException e) {

        }

        System.out.println("domains lines = " + resultList.size());
        writeDomainsToFile("src/main/resources/public/outResult.txt", resultList);

        return resultList;
    }

    public static void writeDomainsToFile(String fileName, List<String> domains) {
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(Path.of(fileName))) {
            for (String domain : domains) {
                bufferedWriter.write(domain);
                bufferedWriter.newLine();
            }
        } catch (IOException e) {
            System.out.println("IO Exception");
        }
    }

    public static void downloadResult(ServletOutputStream outputStream) throws MalformedURLException {

        try (BufferedReader bufferedReader = Files.newBufferedReader(Path.of
                ("src/main/resources/public/outResult.txt"))) {
            Thread.sleep(10);
            while(bufferedReader.ready()) {
                outputStream.write(bufferedReader.readLine().getBytes());
                outputStream.write(13);
                outputStream.write(10);
            }
        }
        catch(IOException | InterruptedException ex){
            throw new RuntimeException(ex);
        }
    }

}

