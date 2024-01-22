package ai.flox

//import androidx.lifecycle.ViewModel
//
//abstract class BaseViewModel<E: BaseEvent<E>, S: BaseState<S>> : ViewModel() {
//
//    abstract val viewState: S
//    abstract val eventClass: Class<E>
//
//    abstract fun handleEvent(event: E, currentState: S) : S
//}
//
//
//abstract class BaseState<T> {
//    private val lock: Any = Any()
//    fun update(new: BaseState<T>) {
//        // state. Atomic, immutable TODO
//        return synchronized(lock) {
//            update(new)
//        }
//    }
//
//    protected abstract fun update(new: T): T
//}
//
//interface BaseEvent<T> {
//    val metaData: Map<String, String> = mapOf()
//}
