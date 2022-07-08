package co.arago.hiro.client.mock;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit5 extension for the {@link MockGraphitServer}.
 */
public class MockGraphitServerExtension implements BeforeAllCallback, AfterAllCallback {

    private MockGraphitServer mockingServer;

    /**
     * @param extensionContext the current extension context; never {@code null}
     * @throws Exception When the {@link MockGraphitServer} cannot be created.
     */
    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        mockingServer = new MockGraphitServer();
    }

    /**
     * @param extensionContext the current extension context; never {@code null}
     */
    @Override
    public void afterAll(ExtensionContext extensionContext) {
        if (mockingServer != null) {
            mockingServer.close();
        }
    }

}
