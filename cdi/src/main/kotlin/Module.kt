package firenze.cdi

import java.lang.reflect.Constructor
import javax.inject.Inject
import javax.inject.Provider

@Suppress("UNCHECKED_CAST")
class Module {
    private val components = HashMap<Class<*>, Provider<*>>()

    fun <T> bind(type: Class<T>, instance: T) {
        components[type] = Provider { instance }
    }

    fun <T> bind(type: Class<T>, implementationClass: Class<out T>) {
        components[type] = ComponentProvider(implementationClass)
    }

    fun <T> get(type: Class<T>): T = components[type]!!.get() as T

    private inner class ComponentProvider<T>(private val componentClass: Class<T>) : Provider<T> {
        private val constructor = injectionConstructor()

        private fun injectionConstructor(): Constructor<T> {
            val constructors = componentClass.constructors.filter { it.isAnnotationPresent(Inject::class.java) }
            return when {
                constructors.size == 1 -> constructors.first()!! as Constructor<T>
                constructors.size > 1 -> throw AmbiguousConstructorInjectionException(componentClass)
                else -> defaultConstructor()
            }
        }

        private fun defaultConstructor(): Constructor<T> = try {
            componentClass.getConstructor()
        } catch (e: NoSuchMethodException) {
            throw ConstructorInjectionNotFoundException(componentClass)
        }

        override fun get(): T =
            constructor.newInstance(*constructor.parameters.map { parameter -> get(parameter.type) }
                .toTypedArray())
    }
}

data class AmbiguousConstructorInjectionException(val componentClass: Class<*>) : RuntimeException()
data class ConstructorInjectionNotFoundException(val componentClass: Class<*>) : RuntimeException()