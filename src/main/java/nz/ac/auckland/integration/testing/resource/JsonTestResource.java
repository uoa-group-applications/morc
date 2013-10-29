package nz.ac.auckland.integration.testing.resource;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URL;

/**
 * Provides a mechanism for retrieving JSON values from a file/URL/String and also
 * validating the response from a target service.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class JsonTestResource extends StaticTestResource<String> {

    public JsonTestResource(String value) {
        super(value);
    }

    public JsonTestResource(File file) {
        super(file);
    }

    public JsonTestResource(URL url) {
        super(url);
    }

    /**
     * @param file a reference to the JSON test resource
     * @return A JSON string
     * @throws Exception
     */
    protected String getResource(File file) throws Exception {
        return FileUtils.readFileToString(file);
    }
}
