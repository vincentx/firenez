package firenze.cdi

import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ModuleTest {

    @Test
    fun should_bind_instance_to_type(){
        val module = Module()
        val component = object: Component {}

        module.bind(Component::class.java, component)

        assertSame(component, module.get(Component::class.java))
    }

    @Test
    fun should_bind_implement_class_to_type() {
        val module = Module()

        module.bind(Component::class.java, Implementation::class.java)

        assertTrue(module.get(Component::class.java) is Implementation)
    }

    interface Component

    class Implementation : Component
}