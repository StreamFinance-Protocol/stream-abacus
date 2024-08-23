package exchange.stream.abacus.utils

import exchange.stream.abacus.protocols.DataNotificationProtocol
import exchange.stream.abacus.protocols.FileSystemProtocol
import exchange.stream.abacus.protocols.FormatterProtocol
import exchange.stream.abacus.protocols.LocalizerProtocol
import exchange.stream.abacus.protocols.LoggingProtocol
import exchange.stream.abacus.protocols.PresentationProtocol
import exchange.stream.abacus.protocols.RestProtocol
import exchange.stream.abacus.protocols.StateNotificationProtocol
import exchange.stream.abacus.protocols.StreamChainTransactionsProtocol
import exchange.stream.abacus.protocols.ThreadingProtocol
import exchange.stream.abacus.protocols.TimerProtocol
import exchange.stream.abacus.protocols.TrackingProtocol
import exchange.stream.abacus.protocols.V3PrivateSignerProtocol
import exchange.stream.abacus.protocols.WebSocketProtocol
import kollections.JsExport

@JsExport
open class ProtocolNativeImpFactory(
    var rest: RestProtocol? = null,
    var webSocket: WebSocketProtocol? = null,
    var chain: StreamChainTransactionsProtocol? = null,
    var localizer: LocalizerProtocol? = null,
    var formatter: FormatterProtocol? = null,
    var tracking: TrackingProtocol? = null,
    var threading: ThreadingProtocol? = null,
    var timer: TimerProtocol? = null,
    var stateNotification: StateNotificationProtocol? = null,
    var dataNotification: DataNotificationProtocol? = null,
    var fileSystem: FileSystemProtocol? = null,
    var v3Signer: V3PrivateSignerProtocol? = null,
    var presentation: PresentationProtocol? = null,
    var logging: LoggingProtocol? = null,
)

@JsExport
class IOImplementations(
    var rest: RestProtocol?,
    var webSocket: WebSocketProtocol?,
    var chain: StreamChainTransactionsProtocol?,
    var tracking: TrackingProtocol?,
    var threading: ThreadingProtocol?,
    var timer: TimerProtocol?,
    var fileSystem: FileSystemProtocol?,
    var logging: LoggingProtocol?,
)

@JsExport
class UIImplementations(
    var localizer: LocalizerProtocol?,
    var formatter: FormatterProtocol?,
)
