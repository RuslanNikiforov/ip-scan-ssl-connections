import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class ServerConfig {

    public static final int SECURE_PORT = 443;

    public static final int INSECURE_PORT = 80;

    public static TemplateEngine getTemplateEngine() {
        TemplateEngine templateEngine = new TemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/public/templates/");
        templateResolver.setCharacterEncoding("UTF-8");
        templateEngine.addTemplateResolver(templateResolver);
        return templateEngine;
    }
}
