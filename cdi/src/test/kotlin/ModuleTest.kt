package firenze.cdi

import org.junit.jupiter.api.assertThrows
import javax.inject.Inject
import kotlin.test.*

class ModuleTest {
    lateinit var module: Module

    @BeforeTest
    fun before() {
        module = Module()
    }

    @Test
    fun `bind a type to a specific instance`() {
        val component = object : Component {}

        module.bind(Component::class.java, component)

        assertSame(component, module.get(Component::class.java))
    }

    @Test
    fun `bind a type to a specific implementation`() {
        class Implementation : Component

        module.bind(Component::class.java, Implementation::class.java)

        assertTrue(module.get(Component::class.java) is Implementation)
    }

    @Test
    fun `inject dependency via constructor when retrieving type from implementation`() {
        val component = object : Component {}

        class ConstructorInjectedComponentConsumer @Inject constructor(private val injected: Component) :
            ComponentConsumer {
            override fun component(): Component = injected
        }

        module.bind(Component::class.java, component)
        module.bind(ComponentConsumer::class.java, ConstructorInjectedComponentConsumer::class.java)

        assertSame(component, module.get(ComponentConsumer::class.java).component())
    }

    @Test
    fun `multiple constructors annotated with inject consider ambiguous`() {
        class TwoInjectedConstructors {
            @Inject
            constructor(component: Component) {
            }

            @Inject
            constructor(component: Component, consumer: ComponentConsumer) {
            }
        }

        val (componentClass) = assertThrows<AmbiguousConstructorInjectionException> {
            module.bind(
                TwoInjectedConstructors::class.java,
                TwoInjectedConstructors::class.java
            )
        }

        assertEquals(TwoInjectedConstructors::class.java, componentClass)
    }

    @Test
    fun `no constructor annotated with inject nor default constructor considers constructor injection not found`() {
        class NoInjectedNorDefaultConstructors {
            constructor(component: Component) {
            }

            constructor(component: Component, consumer: ComponentConsumer) {
            }
        }

        val (componentClass) = assertThrows<ConstructorInjectionNotFoundException> {
            module.bind(
                NoInjectedNorDefaultConstructors::class.java,
                NoInjectedNorDefaultConstructors::class.java
            )
        }

        assertEquals(NoInjectedNorDefaultConstructors::class.java, componentClass)
    }

    interface Component


    interface ComponentConsumer {
        fun component(): Component
    }
}