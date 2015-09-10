package fr.leansys.business;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Properties;

/**
 * Factory for IOC (Guice version)
 *
 * @author jacky.renno@leansys.fr
 */
public class FactoryModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger(FactoryModule.class);

    private static final String PROPERTIES_XML = "properties.xml";

    private static Properties loadProperties() throws Exception {
        Properties properties = new Properties();
        ClassLoader loader = FactoryModule.class.getClassLoader();
        URL url = loader.getResource(PROPERTIES_XML);
        assert url != null;
        properties.loadFromXML(url.openStream());
        return properties;
    }

    @Override
    protected void configure() {
        logger.trace("Configuring IOC");
        try {
            Properties props = loadProperties();
            Names.bindProperties(binder(), props);
        } catch (Exception e) {
            logger.error("Error while reading config file", e);
        }
    }
}
