package exchange.stream.abacus.state.manager.notification

import exchange.stream.abacus.output.Notification
import exchange.stream.abacus.protocols.ParserProtocol
import exchange.stream.abacus.state.manager.notification.providers.OrderStatusChangesNotificationProvider
import exchange.stream.abacus.state.model.TradingStateMachine
import exchange.stream.abacus.utils.IMap
import exchange.stream.abacus.utils.JsonEncoder
import exchange.stream.abacus.utils.ParsingHelper
import exchange.stream.abacus.utils.UIImplementations
import kollections.toIMap

interface NotificationsProviderProtocol {
    fun buildNotifications(
        subaccountNumber: Int
    ): IMap<String, Notification>
}

class NotificationsProvider(
    private val stateMachine: TradingStateMachine,
    private val uiImplementations: UIImplementations,
    private val parser: ParserProtocol,
    private val jsonEncoder: JsonEncoder,
    private val useParentSubaccount: Boolean = false,
    private val providers: List<NotificationsProviderProtocol> = listOf(
        FillsNotificationProvider(
            stateMachine,
            uiImplementations,
            parser,
            jsonEncoder,
        ),
        PositionsNotificationProvider(
            stateMachine,
            uiImplementations,
            parser,
            jsonEncoder,
            useParentSubaccount,
        ),
        OrderStatusChangesNotificationProvider(
            stateMachine,
            uiImplementations,
            parser,
            jsonEncoder,
        ),
    ),
) : NotificationsProviderProtocol {

    override fun buildNotifications(
        subaccountNumber: Int
    ): IMap<String, Notification> {
        var merged: Map<String, Notification>? = null

        providers.forEach { provider ->
            val notifications = provider.buildNotifications(subaccountNumber)
            merged = ParsingHelper.merge(merged, notifications) as? Map<String, Notification>
        }

        return merged?.toIMap() ?: kollections.iMapOf()
    }
}
