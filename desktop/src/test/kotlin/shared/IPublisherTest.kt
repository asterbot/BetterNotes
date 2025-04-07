import shared.IPublisher
import shared.ISubscriber
import kotlin.test.*


class IPublisherTest() {
    lateinit var publisher: IPublisher
    lateinit var subscriber: MockSubscriber
    lateinit var subscriber2: MockSubscriber

    class MockIPublisher : IPublisher() {
    }
    class MockSubscriber : ISubscriber {
        var updated = false
        override fun update() {
            updated = true
        }
    }

    @BeforeTest
    fun setup() {
        publisher = MockIPublisher()
        subscriber = MockSubscriber()
        subscriber2 = MockSubscriber()
    }

    @Test
    fun addSubscriber() {
        val oldCount = publisher.subscribers.size
        publisher.subscribe(subscriber)
        assertEquals(oldCount + 1, publisher.subscribers.size)
    }

    @Test
    fun notifySubscribers() {
        publisher.subscribe(subscriber)
        publisher.subscribe(subscriber2)
        publisher.notifySubscribers()
        assertTrue(subscriber.updated)
        assertTrue(subscriber2.updated)
    }

    @Test
    fun removeSubscriber() {
        publisher.subscribe(subscriber)
        val oldCount = publisher.subscribers.size
        publisher.unsubscribe(subscriber)
        assertEquals(oldCount - 1, publisher.subscribers.size)
    }

    @AfterTest
    fun teardown() {
        publisher.subscribers.clear()
    }
}