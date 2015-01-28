package ir;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.logging.log4j.util.Strings;

/**
 * @author Victor Guo Collection of application properties
 */
public class Configuration {

    private static final String CONFIG_FILE            = "app.properties";
    private static final String LOGGER_CONFIG          = "logger.config";
    private static final String LOGGER_SYSTEM_PROPERTY = "log4j.configurationFile";
    private static final String STOP_WORDS             = "stopwords.path";
    private static final String STOP_WORDS_SIZE        = "stopwords.size";
    private static final String LUCENE_INDEX_PATH      = "lucene.index.path";
    private static final String QUERIES                = "queries.path";
    private static final String TWITTERDATA            = "twitter.data.path";
    private static final String RESULT                 = "result.path";

    private Properties          properties;

    private Configuration() {
        loadProperties();
        String loggerConfig = getLoggerConfig();
        if (Strings.isNotEmpty(loggerConfig)) {
            System.setProperty(LOGGER_SYSTEM_PROPERTY, loggerConfig);
        }
    }

    private void loadProperties() {
        properties = new Properties();
        try {
            properties.load(new FileInputStream(CONFIG_FILE));
        } catch (Exception e) {
            throw new RuntimeException("Unable to find  property file", e);
        }
    }

    public static Configuration getInstance() {
        return Singleton.INSTANCE;
    }

    public String getStopWordsFilePath() {
        return properties.getProperty(STOP_WORDS);
    }

    public int getStopWordsSize() {
        return Integer.parseInt(properties.getProperty(STOP_WORDS_SIZE));
    }

    public String getQueriesFilePath() {
        return properties.getProperty(QUERIES);
    }

    public String getTwitterDataFilePath() {
        return properties.getProperty(TWITTERDATA);
    }

    public String getResultPath() {
        return properties.getProperty(RESULT);
    }

    public String getLuenceIndexPath() {
        return properties.getProperty(LUCENE_INDEX_PATH);
    }

    private String getLoggerConfig() {
        return properties.getProperty(LOGGER_CONFIG);
    }

    /**
     * Singleton pattern recommended in Effective Java by Joshua Bloch
     */
    private static class Singleton {
        private static final Configuration INSTANCE = new Configuration();
    }

}