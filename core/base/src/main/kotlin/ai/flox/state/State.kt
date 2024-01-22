package ai.flox.state

/**
 * Marker Interface for State objects understood by @see [ai.flox.arch.Store],
 * to be extended later if more functionality needs to be enforced in State objects
 */
interface State

/**
 * Interface for Action objects understood by any @see [ai.flox.arch.Store],
 * All event types supported by the @see [ai.flox.arch.Store] should be
 * added here as base functionality to be extended by individual features
 */
interface Action {
    sealed interface UI : Action {
        val componentIdentifier: ComponentIdentifier

        interface RenderEvent : UI
        interface ClickedEvent : UI
        interface DragEvent : UI
        interface LongPressEvent : UI
        interface NavigateEvent : UI
    }

    sealed interface Data<T> : Action {

        val resource: Resource<T>

        interface CreateData<T> : Data<T>
        interface LoadData<T> : Data<T>
        interface UpdateData<T> : Data<T>
        interface DeleteData<T> : Data<T>
    }
}

sealed interface Resource<T> {
    /**
     * The data operation succeeded.
     */
    data class Success<T>(val data: List<T>?) : Resource<T>

    /**
     * The data operation failed.
     */
    data class Failure<T>(val error: Exception, val data : List<T>? = null) : Resource<T>
}

/**
 * Each Component rendered to be identified by the below type understood
 * by @see [Action.UI]
 */
typealias ComponentIdentifier = String

/**
 * State identifier in the larger map of feature states
 */
typealias StateKey = String
