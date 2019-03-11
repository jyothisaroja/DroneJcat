package com.jcat.cloud.fw.components.system.cee.target;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.jcat.cloud.fw.common.exceptions.EcsFileException;

/**
 *
 * This class parses the content of "config.yaml" and extracts the following parameters:
 *
 * - idam user name tagged as "system admin"
 * - ...
 *
 * <b>Copyright:</b> Copyright (c) 2016
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ezhgyin - 2016-03-08 - initial version
 */

public class BaseYamlParser {

    private final File mYamlFile;
    private Map<String, Object> mYamlConfigMap;

    /**
     * Constructor
     *
     * @param yamlFile
     */
    public BaseYamlParser(File yamlFile) {
        mYamlFile = yamlFile;
    }

    /**
     * local help method to read yaml config
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getYamlConfig() {
        if (null == mYamlConfigMap) {
            String fileContent = null;
            try {
                fileContent = new String(Files.readAllBytes(mYamlFile.toPath()));
            } catch (IOException e) {
                throw new EcsFileException("Failed to read content of " + mYamlFile);
            }
            Yaml configRes = new Yaml();
            mYamlConfigMap = (Map<String, Object>) configRes.load(fileContent);
        }
        return mYamlConfigMap;
    }
}
