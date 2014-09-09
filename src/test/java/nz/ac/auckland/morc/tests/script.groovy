import nz.ac.auckland.morc.MorcTestBuilder
import nz.ac.auckland.morc.morc

morc.run(new MorcTestBuilder() {
    public void configure() {
        syncTest("Simple WS PING test", "http://localhost:8090/services/pingService")
                .requestBody(text("ping"))
                .expectedResponseBody(text("pong"))
    }
})