package firenze.cdi

import kotlin.test.Test
import kotlin.test.assertSame

class ModuleTest {

    @Test
    fun should_bind_instance_to_interface(){
        val module = Module()
        val component = object: Component {}

        module.bind(Component::class.java, component)

        assertSame(component, module.get(Component::class.java))
    }

    interface Component
}