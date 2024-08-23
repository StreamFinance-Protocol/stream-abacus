package exchange.stream.abacus.tests.mock

import exchange.stream.abacus.protocols.V3PrivateSignerProtocol

class V3MockSigner : V3PrivateSignerProtocol {
    override fun sign(text: String, secret: String): String {
        return "Dummy"
    }
}
