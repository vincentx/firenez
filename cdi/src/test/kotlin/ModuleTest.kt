package firenze.cdi

import javax.inject.Inject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ModuleTest {
    lateinit var module: Module

    @BeforeTest
    fun before() {
        module = Module()
    }

    @Test
    fun should_bind_instance_to_type(){
        val component = object: Component {}

        module.bind(Component::class.java, component)

        assertSame(component, module.get(Component::class.java))
    }

    @Test
    fun should_bind_implementation_class_to_type() {
        module.bind(Component::class.java, Implementation::class.java)

        assertTrue(module.get(Component::class.java) is Implementation)
    }

    @Test
    fun should_inject_dependency_via_constructor() {
        val component = object: Component {}

        module.bind(Component::class.java, component)
        module.bind(ComponentConsumer::class.java, ConstructorInjectedComponentConsumer::class.java)

        assertSame(component, module.get(ComponentConsumer::class.java).component())
    }

    interface Component

    class Implementation : Component

    interface ComponentConsumer {
        fun component() : Component
    }

    class ConstructorInjectedComponentConsumer @Inject constructor(private val component: Component): ComponentConsumer {
        override fun component(): Component = component
    }
}