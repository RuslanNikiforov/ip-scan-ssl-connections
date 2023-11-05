import io.javalin.Javalin;

import io.javalin.community.ssl.SSLPlugin;
import io.javalin.http.staticfiles.Location;
import io.javalin.plugin.bundled.CorsPluginConfig;
import io.javalin.rendering.template.JavalinThymeleaf;
import jakarta.servlet.ServletOutputStream;

import java.util.Objects;


public class IPScanApp {

    public static void main(String[] args) throws InterruptedException {

        SSLPlugin ssl = new SSLPlugin(sslConfig -> {
            sslConfig.keystoreFromPath("koda.p12", "qwerty");
            sslConfig.securePort = ServerConfig.SECURE_PORT;
            sslConfig.insecurePort = ServerConfig.INSECURE_PORT;
            sslConfig.sniHostCheck = false;
        });
        var app = Javalin.create(javalinConfig -> {
            javalinConfig.staticFiles.add(staticFileConfig -> {
                staticFileConfig.directory = "/public";
                staticFileConfig.location = Location.CLASSPATH;
                staticFileConfig.hostedPath = "/static";
            });
            javalinConfig.plugins.enableCors(corsContainer -> corsContainer.add(CorsPluginConfig::anyHost));
            JavalinThymeleaf.init(ServerConfig.getTemplateEngine());
            javalinConfig.plugins.register(ssl);
        }).start(ServerConfig.SECURE_PORT);
        app.events(eventListener -> eventListener.serverStopped(() -> System.exit(0)));
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
        app.get("/ip-scan", ctx -> {
                    ctx.render("index.html");
                })
                /*.get("/getCert/{host}", context -> {
                    Certificate[] certificates = SSLService.getCertificate(context.pathParam("host"));
                    assert certificates != null;
                    context.result(Objects.requireNonNull(SSLService.getDNSList(certificates[0])).toString());


                })
                .get("/getIpRange/<subnet>", ctx -> {
                    ctx.result(IPUtil.getIPv4Range(ctx.pathParam("subnet")));
                })
                .get("/getByteMask/{mask}", ctx -> {
                    ctx.result(Arrays.toString(IPUtil.getSubnetMask(Integer.parseInt(ctx.pathParam("mask")))));
                })
                .get("/getAllIPByRange/{range}", ctx -> {
                    ctx.result(IPUtil.parseIPv4ByRange(ctx.pathParam("range")).toString());
                })*/
                .get("/getAllDNSByIPRange", ctx -> {
                    int threadsNumber = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("threadsNumber")));
                    String range = IPUtil.getIPv4Range(Objects.requireNonNull(ctx.queryParam("ip-range")));
                    int searchTime = 10;
                    if (ctx.queryParam("searchTime") != null && !ctx.queryParam("searchTime").equals("")) {
                        searchTime = Integer.parseInt(ctx.queryParam("searchTime"));
                    }
                    SSLService.getAllDomainsInRange(range, threadsNumber, searchTime);
                    ctx.res().setContentType("application/octet-stream");
                    ctx.res().setHeader("Content-Disposition", "attachment; filename=result" + ".txt");
                    ServletOutputStream servletOutputStream = ctx.res().getOutputStream();
                    SSLService.downloadResult(servletOutputStream);
                });
            Thread.sleep(150000);
            app.stop();
    }
}

