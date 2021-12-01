package firenze.cdi

import java.lang.reflect.Constructor
import javax.inject.Inject
import javax.inject.Provider

@Suppress("UNCHECKED_CAST")
class Module {
    private val bindings = HashMap<Class<*>, List<Binding<*>>>()

    fun <T> bind(type: Class<T>, instance: T) = bind(type, Binding({ instance }, listOf()))

    fun <T> bind(type: Class<T>, specific: Class<out T>, vararg qualifiers: Annotation) =
        bind(type, Binding(CyclicDependencyNotAllowed(ConstructorInjection(specific), specific), listOf(*qualifiers)))

    fun <T> get(type: Class<T>, vararg qualifiers: Annotation): T =
        bindings[type]!!.first { it.isQualified(listOf(*qualifiers)) }.provider.get() as T

    private fun <T> bind(type: Class<T>, binding: Binding<T>) {
        bindings[type] = (bindings[type] ?: listOf()) + listOf(binding)
    }

    private class Binding<T>(val provider: Provider<T>, val qualifiers: List<Annotation>) {
        fun isQualified(required: List<Annotation>) =
            qualifiers.size == required.size && qualifiers.containsAll(required)
    }

    private inner class ConstructorInjection<T>(private val componentClass: Class<out T>) : Provider<T> {
        private val constructor = injectionConstructor()

        private fun injectionConstructor(): Constructor<out T> {
            val constructors = componentClass.constructors.filter { it.isAnnotationPresent(Inject::class.java) }
            return when {
                constructors.size == 1 -> constructors.first()!! as Constructor<T>
                constructors.size > 1 -> throw AmbiguousConstructorInjectionException(componentClass)
                else -> defaultConstructor()
            }
        }

        private fun defaultConstructor(): Constructor<out T> = try {
            componentClass.getConstructor()
        } catch (e: NoSuchMethodException) {
            throw ConstructorInjectionNotFoundException(componentClass)
        }

        override fun get(): T = constructor.newInstance(*constructor.parameters.map { parameter -> get(parameter.type) }
            .toTypedArray())
    }

    private class CyclicDependencyNotAllowed<T>(
        private val provider: Provider<T>,
        private val componentClass: Class<out T>
    ) : Provider<T> {
        private var constructing = false

        override fun get(): T {
            if (constructing) throw CyclicConstructorInjectionDependenciesFound()
            try {
                constructing = true
                return provider.get()
            } catch (e: CyclicConstructorInjectionDependenciesFound) {
                throw CyclicConstructorInjectionDependenciesFound(e.components + listOf(componentClass))
            } finally {
                constructing = false
            }
        }
    }
}

data class AmbiguousConstructorInjectionException(val componentClass: Class<*>) : RuntimeException()
data class ConstructorInjectionNotFoundException(val componentClass: Class<*>) : RuntimeException()
data class CyclicConstructorInjectionDependenciesFound(val components: List<Class<*>> = listOf()) : RuntimeException()
