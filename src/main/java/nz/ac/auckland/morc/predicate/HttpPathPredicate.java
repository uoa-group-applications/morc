package nz.ac.auckland.morc.predicate;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A resource for returning non-200 HTTP codes back to the client, or testing that such an response was received
 *
 * @author David MacDonald - d.macdonald@auckland.ac.nz
 */
public class HttpPathPredicate implements Predicate {

    private static final Logger logger = LoggerFactory.getLogger(HttpPathPredicate.class);
    private String path;

    public HttpPathPredicate(String path) {
        this.path = path;
    }

    @Override
    public boolean matches(Exchange exchange) {
        if (exchange == null) return false;

        if (exchange.getIn().getHeader(Exchange.HTTP_PATH) == null) {
            logger.warn("The HTTP path does not exist");
            return false;
        }

        String receivedHttpPath = exchange.getIn().getHeader(Exchange.HTTP_PATH,String.class);
        if (!receivedHttpPath.equals(path)) {
            logger.warn("HTTP Path is not expected, received: {}, expected: {}", receivedHttpPath,
                    this.path);
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "HttpPathPredicate: Path:" + path;
    }
}
