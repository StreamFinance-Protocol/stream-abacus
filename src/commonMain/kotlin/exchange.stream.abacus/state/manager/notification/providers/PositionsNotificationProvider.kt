package exchange.stream.abacus.state.manager.notification

import exchange.stream.abacus.output.Notification
import exchange.stream.abacus.output.NotificationPriority
import exchange.stream.abacus.output.NotificationType
import exchange.stream.abacus.protocols.ParserProtocol
import exchange.stream.abacus.state.model.TradingStateMachine
import exchange.stream.abacus.utils.IMap
import exchange.stream.abacus.utils.JsonEncoder
import exchange.stream.abacus.utils.UIImplementations
import exchange.stream.abacus.utils.iMapOf
import kollections.toIMap

class PositionsNotificationProvider(
    private val stateMachine: TradingStateMachine,
    private val uiImplementations: UIImplementations,
    private val parser: ParserProtocol,
    private val jsonEncoder: JsonEncoder,
    private val useParentSubaccount: Boolean = false,
) : NotificationsProviderProtocol {
    override fun buildNotifications(
        subaccountNumber: Int
    ): IMap<String, Notification> {
        /*
        We have to go to the dynamic data to find closed positions
        Struct contains open positions only
         */
        val notifications = exchange.stream.abacus.utils.mutableMapOf<String, Notification>()
        val positions = parser.asMap(
            parser.value(
                stateMachine.data,
                if (useParentSubaccount) {
                    "wallet.account.groupedSubaccounts.$subaccountNumber.positions"
                } else {
                    "wallet.account.subaccounts.$subaccountNumber.positions"
                },
            ),
        )

        if (positions != null) {
            for ((marketId, data) in positions) {
                val position = parser.asMap(data) ?: continue
                val positionStatus = parser.asString(position["status"])
                if (positionStatus == "CLOSED") {
                    val closedAt = parser.asDatetime(position["closedAt"]) ?: continue
                    val asset = stateMachine.state?.assetOfMarket(marketId) ?: continue
                    val assetText = asset.name
                    val marketImageUrl = asset.resources?.imageUrl
                    val params = (
                        iMapOf(
                            "MARKET" to marketId,
                            "ASSET" to assetText,
                        ).filterValues { it != null } as Map<String, String>
                        ).toIMap()
                    val paramsAsJson = jsonEncoder.encode(params)

                    val title =
                        uiImplementations.localizer?.localize("NOTIFICATIONS.POSITION_CLOSED.TITLE")
                            ?: continue
                    val text = uiImplementations.localizer?.localize(
                        "NOTIFICATIONS.POSITION_CLOSED.BODY",
                        paramsAsJson,
                    )

                    val notificationId = "position:$marketId"
                    notifications[notificationId] = Notification(
                        notificationId,
                        NotificationType.INFO,
                        NotificationPriority.NORMAL,
                        marketImageUrl,
                        title,
                        text,
                        null,
                        paramsAsJson,
                        closedAt.toEpochMilliseconds().toDouble(),
                    )
                }
            }
        }
        return notifications
    }
}
