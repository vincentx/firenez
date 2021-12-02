package firenze.cdi

import java.lang.reflect.Constructor
import java.lang.reflect.Parameter
import javax.inject.*

@Suppress("UNCHECKED_CAST")
class Module {
    private val bindings = HashMap<Binding<*>, Provider<*>>()

    fun <T> bind(type: Class<T>, instance: T, vararg qualifiers: Annotation) =
        bind(type, qualifiers) { instance }

    fun <T> bind(type: Class<T>, specific: Class<out T>, vararg qualifiers: Annotation) =
        bind(type, qualifiers, instantiationProvider(specific))

    fun <T> get(type: Class<T>, vararg qualifiers: Annotation): T? =
        bindings[Binding(type, listOf(*qualifiers))]?.get() as T

    private fun <T> bind(type: Class<T>, qualifiers: Array<out Annotation>, provider: Provider<T>) =
        noDuplicate(Binding(type, listOf(*qualifiers).filter(::isQualifier))) { bindings[it] = provider }

    private fun <T> instantiationProvider(specific: Class<out T>): Provider<T> {
        val provider = ConstructorInjection(specific)
        return if (specific.isAnnotationPresent(Singleton::class.java)) SingletonProvider(provider) else provider
    }

    private fun noDuplicate(binding: Binding<*>, next: (Binding<*>) -> Unit) =
        if (bindings.containsKey(binding)) throw AmbiguousBindingException(binding.type, binding.qualifiers)
        else next(binding)

    private fun isQualifier(annotation: Annotation) =
        annotation.annotationClass.annotations.any { it.annotationClass.java == Qualifier::class.java }

    private class Binding<T>(val type: Class<T>, val qualifiers: List<Annotation>) {
        override fun equals(other: Any?) =
            (other is Binding<*>) && (other.type == type) && (other.qualifiers.size == qualifiers.size)
                    && (other.qualifiers.containsAll(qualifiers))

        override fun hashCode() = 31 * type.hashCode() + qualifiers.hashCode()
    }

    private inner class ConstructorInjection<T>(componentClass: Class<out T>) :
        CyclicDependencyNotAllowed<T>(componentClass) {
        private val constructor = injectionConstructor()

        override fun create(): T = constructor.newInstance(*constructor.parameters.map(::toDependency).toTypedArray())

        private fun injectionConstructor(): Constructor<out T> {
            val constructors = componentClass.constructors.filter { it.isAnnotationPresent(Inject::class.java) }
            return when {
                constructors.size == 1 -> constructors[0] as Constructor<T>
                constructors.size > 1 -> throw AmbiguousConstructorInjectionException(componentClass)
                else -> defaultConstructor()
            }
        }

        private fun defaultConstructor(): Constructor<out T> = try {
            componentClass.getConstructor()
        } catch (e: NoSuchMethodException) {
            throw ConstructorInjectionNotFoundException(componentClass)
        }

        private fun toDependency(parameter: Parameter) =
            get(parameter.type, *parameter.annotations.filter(::isQualifier).toTypedArray())
    }

    private abstract class CyclicDependencyNotAllowed<T>(protected val componentClass: Class<out T>) : Provider<T> {
        private var constructing = false

        override fun get(): T {
            if (constructing) throw CyclicConstructorInjectionDependenciesFoundException()
            try {
                constructing = true
                return create()
            } catch (e: CyclicConstructorInjectionDependenciesFoundException) {
                throw CyclicConstructorInjectionDependenciesFoundException(e.components + listOf(componentClass))
            } finally {
                constructing = false
            }
        }

        protected abstract fun create(): T
    }

    private class SingletonProvider<T>(private val provider: Provider<T>) : Provider<T> {
        private var singleton: T? = null

        override fun get(): T {
            if (singleton == null)
                singleton = provider.get()
            return singleton!!
        }
    }
}

data class AmbiguousConstructorInjectionException(val componentClass: Class<*>) : RuntimeException()
data class ConstructorInjectionNotFoundException(val componentClass: Class<*>) : RuntimeException()
data class CyclicConstructorInjectionDependenciesFoundException(val components: List<Class<*>> = listOf()) :
    RuntimeException()

data class AmbiguousBindingException(val type: Class<*>, val qualifiers: List<Annotation>) : RuntimeException()
