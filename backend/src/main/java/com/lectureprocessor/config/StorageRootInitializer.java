package com.lectureprocessor.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Ensures the storage root exists before the SQLite datasource (which lives
 * under that root) runs the schema script. Runs as an early
 * {@link BeanFactoryPostProcessor}, before any datasource bean is created.
 */
@Component
public class StorageRootInitializer implements BeanFactoryPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(StorageRootInitializer.class);

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Environment environment = beanFactory.getBean(Environment.class);
        String root = environment.getProperty("app.storage.root", "./data");
        Path path = Path.of(root).toAbsolutePath().normalize();
        try {
            Files.createDirectories(path);
            log.info("Storage root ready: {}", path);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось создать каталог хранения: " + path, e);
        }
    }
}
