package exchange.stream.abacus.tests.mock

import exchange.stream.abacus.protocols.ThreadingProtocol
import exchange.stream.abacus.protocols.ThreadingType

class ThreadingProtocolMock : ThreadingProtocol {
    var asyncCallCount = 0

    override fun async(type: ThreadingType, block: () -> Unit) {
        asyncCallCount++
        block()
    }
}
