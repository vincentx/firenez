package firenze.cdi

class Module {
    private val components = HashMap<Class<*>, Any>()

    fun <T> bind(type: Class<T>, instance: T) {
        components[type] = instance!!
    }

    fun <T> get(type: Class<T>): T = components[type] as T
}