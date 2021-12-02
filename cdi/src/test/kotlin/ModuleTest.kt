package firenze.cdi

import org.junit.jupiter.api.assertThrows
import javax.inject.Inject
import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.test.*

@Suppress("UNUSED_PARAMETER")
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

        assertSame(component, module.get(ComponentConsumer::class.java)?.component())
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

    @Test
    fun `cyclic dependencies between constructor injections not allowed`() {
        class ConstructorInjectionA @Inject constructor(b: Component)
        class ConstructorInjectionB @Inject constructor(a: ConstructorInjectionA) : Component

        module.bind(ConstructorInjectionA::class.java, ConstructorInjectionA::class.java)
        module.bind(Component::class.java, ConstructorInjectionB::class.java)

        val (components) = assertThrows<CyclicConstructorInjectionDependenciesFoundException> { module.get(ConstructorInjectionA::class.java) }
        assertEquals(2, components.size)
        assertContains(components, ConstructorInjectionA::class.java)
        assertContains(components, ConstructorInjectionB::class.java)
    }

    @Test
    fun `transitive cyclic constructor injections not allowed`() {
        class ConstructorInjectionA @Inject constructor(b: Component)
        class ConstructorInjectionB @Inject constructor(c: AnotherComponent) : Component
        class ConstructorInjectionC @Inject constructor(a: ConstructorInjectionA) : AnotherComponent

        module.bind(ConstructorInjectionA::class.java, ConstructorInjectionA::class.java)
        module.bind(Component::class.java, ConstructorInjectionB::class.java)
        module.bind(AnotherComponent::class.java, ConstructorInjectionC::class.java)

        val (components) = assertThrows<CyclicConstructorInjectionDependenciesFoundException> { module.get(ConstructorInjectionA::class.java) }
        assertEquals(3, components.size)
        assertContains(components, ConstructorInjectionA::class.java)
        assertContains(components, ConstructorInjectionB::class.java)
        assertContains(components, ConstructorInjectionC::class.java)
    }

    @Test
    fun `can retrieve component by qualifiers`() {
        @Marker
        class SpecificComponent : Component

        module.bind(Component::class.java, object : Component {})
        module.bind(Component::class.java, SpecificComponent::class.java, SpecificComponent::class.java.annotations[0])

        assertFalse(module.get(Component::class.java) is SpecificComponent)
        assertTrue(module.get(Component::class.java, SpecificComponent::class.java.annotations[0]) is SpecificComponent)
    }

    @Test
    fun `returns null if no qualified component provided`() {
        @Marker
        class SpecificComponent : Component

        module.bind(Component::class.java, object : Component {})
        assertNull(module.get(Component::class.java, SpecificComponent::class.java.annotations[0]))
    }

    @Test
    fun `should not allow bind different component with same qualifiers`() {
        @Marker
        class SpecificComponent : Component

        module.bind(Component::class.java, SpecificComponent::class.java, SpecificComponent::class.java.annotations[0])

        val (type, qualifiers) = assertThrows<AmbiguousBindingException> { module.bind(Component::class.java, object : Component {}, SpecificComponent::class.java.annotations[0]) }
        assertEquals(Component::class.java, type)
        assertEquals(listOf(SpecificComponent::class.java.annotations[0]), qualifiers)
    }

    @Test
    fun `dependencies of constructor injection can be annotated with qualifier`() {
        @Marker
        class SpecificComponent : Component

        class SpecificConsumer @Inject constructor(@Marker val injected: Component) : ComponentConsumer {
            override fun component(): Component = injected
        }

        module.bind(Component::class.java, object : Component {})
        module.bind(Component::class.java, SpecificComponent::class.java, SpecificComponent::class.java.annotations[0])
        module.bind(ComponentConsumer::class.java, SpecificConsumer::class.java)

        assertTrue(module.get(ComponentConsumer::class.java)?.component() is SpecificComponent)
    }

    @Test
    fun `should ignore non qualifier annotation for specific class`() {
        @Marker
        class SpecificComponent : Component

        @Marker @NotQualifier
        class AnotherSpecificComponent : Component

        module.bind(Component::class.java, SpecificComponent::class.java, *SpecificComponent::class.java.annotations)

        val (component, annotations) = assertThrows<AmbiguousBindingException> { module.bind(Component::class.java, AnotherSpecificComponent::class.java, *AnotherSpecificComponent::class.java.annotations) }

        assertSame(Component::class.java, component)
        assertEquals(1, annotations.size)
        assertContains(annotations, SpecificComponent::class.java.annotations[0])
    }

    @Test
    fun `should ignore non qualifier annotation for instance`() {
        @Marker
        class SpecificComponent : Component

        @Marker @NotQualifier
        class AnotherSpecificComponent : Component

        module.bind(Component::class.java, object : Component {}, *SpecificComponent::class.java.annotations)

        val (component, annotations) = assertThrows<AmbiguousBindingException> { module.bind(Component::class.java, object : Component {}, *AnotherSpecificComponent::class.java.annotations) }

        assertSame(Component::class.java, component)
        assertEquals(1, annotations.size)
        assertContains(annotations, SpecificComponent::class.java.annotations[0])
    }

    interface Component

    interface AnotherComponent

    interface ComponentConsumer {
        fun component(): Component
    }

    @Qualifier
    @MustBeDocumented
    @Retention(RUNTIME)
    annotation class Marker

    @MustBeDocumented
    @Retention(RUNTIME)
    annotation class NotQualifier
}