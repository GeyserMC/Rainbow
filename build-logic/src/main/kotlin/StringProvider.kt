import org.gradle.api.provider.Provider

class StringProvider<T : Any>(val provider: Provider<T>) {

    override fun toString(): String {
        return provider.get().toString()
    }
}