package org.jboss.arquillian.extension.rest.client;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.glassfish.jersey.filter.LoggingFilter;
import org.jboss.arquillian.extension.rest.client.annotation.ArquillianJerseyConfiguration;
import org.jboss.arquillian.test.spi.TestEnricher;

public class RestEnricher extends BaseRestEnricher implements TestEnricher {

    @Override
    protected Object enrichByType(Class<?> clazz, Method method, ArquillianResteasyResource annotation, Consumes consumes, Produces produces)
    {
        Object value;
        ArquillianJerseyConfiguration jerseyConfig = method.getDeclaringClass().getAnnotation(ArquillianJerseyConfiguration.class);
        Client client;
        if (jerseyConfig != null) {
            client = JerseyClientBuilder.newClient(new ClientConfig(jerseyConfig.providers()));
            if (jerseyConfig.log()) {
                client.register(new LoggingFilter(Logger.getLogger("os"), 10000));
            }
        } else {
            client = JerseyClientBuilder.newClient();
        }
        WebTarget webTarget = client.target(getBaseURL(annotation.context()) + annotation.value());
        final Map<String, String> headers = getHeaders(clazz, method);
        if (!headers.isEmpty()) {
            webTarget.register(new HeaderFilter(headers));
        }
        JerseyWebTarget jerseyWebTarget = (JerseyWebTarget) webTarget;
        if (WebTarget.class.isAssignableFrom(clazz)) {
            value = jerseyWebTarget;
        } else {
            final Class<?> parameterType;
            try {
                final Annotation[] methodDeclaredAnnotations = method.getDeclaredAnnotations();
//                                This is test method so if it only contains @Test annotation then we don't need to hassel with substitutions
                parameterType = methodDeclaredAnnotations.length <= 1 ? clazz : ClassModifier.getModifiedClass(clazz, methodDeclaredAnnotations);
            } catch (Exception e) {
                throw new RuntimeException("Cannot substitute annotations for method " + method.getName(), e);
            }
            value = WebResourceFactory.newResource(parameterType, jerseyWebTarget);
        }
        return value;
    }

    @Override
    protected boolean isSupportedParameter(Class<?> clazz)
    {
        return true; // it's proxy based, exception will be thrown when proxying.
    }
}