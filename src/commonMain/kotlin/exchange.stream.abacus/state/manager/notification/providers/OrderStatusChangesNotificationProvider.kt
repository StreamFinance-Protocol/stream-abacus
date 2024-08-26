package exchange.stream.abacus.state.manager.notification.providers

import exchange.stream.abacus.output.Notification
import exchange.stream.abacus.output.NotificationPriority
import exchange.stream.abacus.output.NotificationType
import exchange.stream.abacus.output.account.SubaccountOrder
import exchange.stream.abacus.output.input.OrderStatus
import exchange.stream.abacus.output.input.OrderType
import exchange.stream.abacus.protocols.ParserProtocol
import exchange.stream.abacus.state.manager.notification.NotificationsProviderProtocol
import exchange.stream.abacus.state.model.TradingStateMachine
import exchange.stream.abacus.utils.IMap
import exchange.stream.abacus.utils.JsonEncoder
import exchange.stream.abacus.utils.Numeric
import exchange.stream.abacus.utils.UIImplementations
import exchange.stream.abacus.utils.iMapOf
import exchange.stream.abacus.utils.typedSafeSet
import kollections.toIMap

class OrderStatusChangesNotificationProvider(
    private val stateMachine: TradingStateMachine,
    private val uiImplementations: UIImplementations,
    private val parser: ParserProtocol,
    private val jsonEncoder: JsonEncoder,
) : NotificationsProviderProtocol {
    override fun buildNotifications(
        subaccountNumber: Int
    ): IMap<String, Notification> {
        /*
      We have to go through fills instead of orders, because
      1. Order doesn't have an updatedAt timestamp
      2. Order doesn't have an average filled price
         */
        val notifications = exchange.stream.abacus.utils.mutableMapOf<String, Notification>()
        val subaccount = stateMachine.state?.subaccount(subaccountNumber) ?: return kollections.iMapOf()
        subaccount.orders

        val subaccountOrders = subaccount.orders

        if (subaccountOrders != null) {
            for (order in subaccountOrders) {
                val orderStatusNotification = createOrderStatusNotification(stateMachine, order)
                if (orderStatusNotification != null) {
                    val orderId = order.id
                    val notificationId = "order_status:$orderId"
                    notifications.typedSafeSet(notificationId, orderStatusNotification)
                }
            }
        }
        return notifications
    }

    private fun createOrderStatusNotification(
        stateMachine: TradingStateMachine,
        order: SubaccountOrder,
    ): Notification? {
        var timestamp: Double? = null
        val statusNotificationStringKey = when (order.status) {
            OrderStatus.Open -> {
                when (order.type) {
                    OrderType.StopLimit, OrderType.StopMarket, OrderType.TakeProfitLimit, OrderType.TakeProfitMarket -> {
                        timestamp = order.updatedAtMilliseconds
                        if (timestamp != null && order.totalFilled == Numeric.double.ZERO) {
                            "NOTIFICATIONS.ORDER_TRIGGERED"
                        } else {
                            null
                        }
                    }

                    OrderType.Limit, OrderType.Market -> {
                        /*
                        Short term orders should get filled/partially filled immediately, so we don't need to handle OPENED notification
                        And it doesn't have a timestamp
                         */
                        timestamp = order.createdAtMilliseconds
                        if (timestamp != null && order.totalFilled == Numeric.double.ZERO) {
                            "NOTIFICATIONS.ORDER_OPENED"
                        } else {
                            null
                        }
                    }

                    else -> null
                }
            }

            OrderStatus.Canceled -> {
                if ((order.totalFilled ?: Numeric.double.ZERO) > Numeric.double.ZERO) {
                    "NOTIFICATIONS.ORDER_CANCEL_WITH_PARTIAL_FILL"
                } else {
                    "NOTIFICATIONS.ORDER_CANCEL"
                }
            }

            else -> null
        }
        return if (statusNotificationStringKey != null && timestamp != null) {
            val marketId = order.marketId
            val asset = stateMachine.state?.assetOfMarket(marketId) ?: return null
            val marketImageUrl = asset.resources?.imageUrl
            val side = order.side.rawValue
            val sideText = uiImplementations.localizer?.localize("APP.GENERAL.$side")
            val amountText = parser.asString(order.size)
            val totalFilled = parser.asString(order.totalFilled)
            val orderType = order.type.rawValue
            val orderTypeText = text(orderType)
            val params = (
                iMapOf(
                    "MARKET" to marketId,
                    "SIDE" to sideText,
                    "AMOUNT" to amountText,
                    "TOTAL_FILLED" to totalFilled,
                    "ORDER_TYPE" to orderType,
                    "ORDER_TYPE_TEXT" to orderTypeText,
                    "ORDER_STATUS" to order.status.rawValue,
                ).filterValues { it != null } as Map<String, String>
                ).toIMap()
            val paramsAsJson = jsonEncoder.encode(params)

            val title =
                uiImplementations.localizer?.localize("$statusNotificationStringKey.TITLE")
                    ?: return null
            val text =
                uiImplementations.localizer?.localize(
                    "$statusNotificationStringKey.BODY",
                    paramsAsJson,
                )

            val orderId = order.id
            val notificationId = "orderstatus:$orderId"
            return Notification(
                notificationId,
                NotificationType.INFO,
                NotificationPriority.NORMAL,
                marketImageUrl,
                title,
                text,
                "/orders/$orderId",
                paramsAsJson,
                timestamp,
            )
        } else {
            null
        }
    }

    private fun text(orderType: String): String? {
        return uiImplementations.localizer?.localize("APP.ENUMS.ORDER_TYPE.$orderType")
            ?: return null
    }
}
