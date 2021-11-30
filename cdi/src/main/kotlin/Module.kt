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
        override fun get(): T {
            val constructor = componentClass.constructors.filter { constructor ->
                constructor.isAnnotationPresent(Inject::class.java) ||
                        constructor.parameterCount == 0
            }.first() as Constructor<T>
            val dependencies = constructor.parameters.map { parameter -> get(parameter.type) }
            return constructor.newInstance(*dependencies.toTypedArray())
        }
    }
}