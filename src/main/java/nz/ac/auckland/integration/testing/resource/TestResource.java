package nz.ac.auckland.integration.testing.resource;

public interface TestResource {
    /**
     * @return The test resource in the appropriate format
     * @throws java.lang.Exception if there is a problem obtaining the value
     */
    public Object getValue() throws Exception;
}
