package exchange.stream.abacus.processor.wallet.account

import exchange.stream.abacus.output.account.SubaccountOrder
import exchange.stream.abacus.tests.mock.LocalizerProtocolMock
import exchange.stream.abacus.tests.mock.processor.wallet.account.OrderProcessorMock
import exchange.stream.abacus.utils.Parser
import indexer.models.IndexerCompositeOrderObject
import kotlin.test.Test
import kotlin.test.assertEquals

class OrdersProcessorTests {
    private val orderProcessor = OrderProcessorMock()
    private val ordersProcessor = OrdersProcessor(
        parser = Parser(),
        localizer = LocalizerProtocolMock(),
        orderProcessor = orderProcessor,
    )

    @Test
    fun testProcess_emptyPayload() {
        val output = ordersProcessor.process(
            existing = null,
            payload = emptyList(),
            subaccountNumber = 0,
            height = null,
        )
        assertEquals(0, output.size)
    }

    @Test
    fun testProcess_nonEmptyPayload() {
        orderProcessor.processAction = { _, _, _, _ ->
            createSubaccountOrder("1")
        }

        val output = ordersProcessor.process(
            existing = null,
            payload = listOf(
                IndexerCompositeOrderObject(id = "1"),
            ),
            subaccountNumber = 0,
            height = null,
        )
        assertEquals(1, output.size)
        assertEquals(output[0].id, "1")
    }

    @Test
    fun testProcess_withMerge() {
        orderProcessor.processAction = { _, input, _, _ ->
            createSubaccountOrder(input.id!!)
        }

        val output = ordersProcessor.process(
            existing = listOf(
                createSubaccountOrder("1"),
            ),
            payload = listOf(
                IndexerCompositeOrderObject(
                    id = "1",
                ),
                IndexerCompositeOrderObject(
                    id = "2",
                ),
            ),
            subaccountNumber = 0,
            height = null,
        )
        assertEquals(2, output.size)
        assertEquals(output[0].id, "1")
        assertEquals(output[1].id, "2")
    }

    private fun createSubaccountOrder(id: String): SubaccountOrder {
        return OrderProcessorTests.orderMock.copy(id = id)
    }
}
