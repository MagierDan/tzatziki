package tzatziki.analysis.tag;

import org.apache.commons.io.IOUtils;
import tzatziki.util.PropertiesLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Properties;

/**
 * @author <a href="http://twitter.com/aloyer">@aloyer</a>
 */
public class TagDictionaryLoader {
    public TagDictionary fromUTF8PropertiesResource(String resourcePath) throws IOException {
        URL resource = PropertiesLoader.class.getResource(resourcePath);
        if (resource == null)
            throw new IllegalArgumentException("Resource not found '" + resourcePath + "'");

        InputStream stream = resource.openStream();
        try {
            Properties properties = new PropertiesLoader().loadFromUTF8Stream(stream);

            return new TagDictionary().declareTags(properties);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF8 not supported...", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }
}
