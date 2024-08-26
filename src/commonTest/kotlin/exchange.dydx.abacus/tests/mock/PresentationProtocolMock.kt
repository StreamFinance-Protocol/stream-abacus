package exchange.stream.abacus.tests.mock

import exchange.stream.abacus.protocols.PresentationProtocol
import exchange.stream.abacus.protocols.Toast

class PresentationProtocolMock : PresentationProtocol {
    var showToastCallCount = 0
    var toasts: MutableList<Toast> = mutableListOf()

    override fun showToast(toast: Toast) {
        showToastCallCount++
        toasts.add(toast)
    }
}
