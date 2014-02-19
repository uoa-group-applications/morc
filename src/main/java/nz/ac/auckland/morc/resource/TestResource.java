package nz.ac.auckland.morc.resource;

/**
 * An interface for providing test resources for expectations
 * or requests/inputs
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public interface TestResource<T> {
    /**
     * @return The test resource in the appropriate format
     * @throws java.lang.Exception if there is a problem obtaining the value
     */
    public T getValue() throws Exception;

}
