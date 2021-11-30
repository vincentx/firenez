package firenze.cdi

import javax.inject.Provider

class Module {
    private val components = HashMap<Class<*>, Provider<*>>()

    fun <T> bind(type: Class<T>, instance: T) {
        components[type] = Provider { instance }
    }

    fun <T> bind(type: Class<T>, implementationClass: Class<out T>) {
        components[type] = Provider {
            val defaultConstructor = implementationClass.getConstructor()
            defaultConstructor.newInstance()
        }
    }

    fun <T> get(type: Class<T>): T = components[type]!!.get() as T
}