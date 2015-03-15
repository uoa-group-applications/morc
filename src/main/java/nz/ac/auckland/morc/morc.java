package nz.ac.auckland.morc;

/**
 * A convenience class that helps to quickly instantiate test cases using a scripting tool like Groovy.
 * I realize this breaks Java naming convention, but it fits the name used elsewhere better.
 *
 * @author David MacDonald - d.macdonald@auckland.ac.nz
 */
public class morc {

    /**
     * A method that will run the builder as a suite of JUnit tests
     *
     * @param builder A set of test specifications that need to be built
     * @return The number of failed tests
     */
    public static int run(MorcTestBuilder builder) {
        return builder.run();
    }
}
