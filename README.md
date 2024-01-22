# flox
Flux Architecture on top of Kotlin Flows

[![CI](https://github.com/pointfreeco/swift-composable-architecture/workflows/CI/badge.svg)](https://github.com/pointfreeco/swift-composable-architecture/actions?query=workflow%3ACI)
![Static Badge](https://img.shields.io/badge/Kotlin_Compatibility-1.9.0-green)

Flux on Flow (Flox, for short) is a library for building Android applications via using the unidirectional flow of the Flux pattern. Redux the most popular state management system in the web applications world is built on this architecture used by >50% of web applications. In times of declarative UI becoming popular (via Jetpack Compose) in the Android ecosystem, this ease of state management system leads to faster development, less boiler plate, easy debuggability and readability of large scale Android apps.


## Why

This library provides a few core tools that can be used to build applications of varying purpose and
complexity. As declarative UI picks up in the Android ecosystem with popularity of Redux in the web world, Flox comes in with a state management pattern to ensure state of large scale apps is maintainable and deterministic.

* **State management**
  <br> How to manage the state of your application using simple value types, and share state across
  many screens so that mutations in one screen can be immediately observed in another screen.

* **Composition**
  <br> How to break down large features into smaller components that can be extracted to their own,
  isolated modules and be easily glued back together to form the feature via Dagger multibinding.

* **Side effects**
  <br> How to let certain parts of the application talk to the outside world in the most testable
  and understandable way possible.


## Overview

> **Note**
> For a step-by-step interactive tutorial, be sure to check out [Meet the Composable
> Architecture][meet-tca].

To build a feature using the Composable Architecture you define some types and values that model
your domain:

* **State**: A type that describes the data your feature needs to perform its logic and render its
  UI.
* **Action**: A type that represents all of the actions that can happen in your feature, such as
  user actions, notifications, event sources and more.
* **Reducer**: A function that describes how to evolve the current state of the app to the next
  state given an action. The reducer is also responsible for returning any effects that should be
  run, such as API requests, which can be done by returning an `Effect` value.
* **Store**: The runtime that actually drives your feature. You send all user actions to the store
  so that the store can run the reducer and effects, and you can observe state changes in the store
  so that you can update UI.

The benefits of doing this are that you will instantly unlock testability of your feature, and you
will be able to break large, complex features into smaller domains that can be glued together.

As a basic example, consider a UI that is an architecture of a AI assistant application with chats, conversations inspired from ChatGPT. 
Each screen has it's viewmodel broken down into feature modules that expose Reducer<State, Action> and Jetpack Compose Screens that map into global state and action with a single Application State maintained inside a Store.

### Store & State

The `Store` exposes a flow which emits the whole state of the app every time there's a change and a method to send actions that will modify that state.  The `State` is just a data class that contains ALL the state of the application. It also includes the local state of all the specific modules that need local state. More on this later.

The store interface looks like this:

```kotlin
interface Store<State, Action : Any> {
  val state: StateFlow<State>
  fun dispatch(vararg actions: A)
}
```

You can create your own AppState and child features states such as below. Any data class can be in AppState but as a modularisation example in this app, we take featureStates via Dagger Multibinding.

```kotlin
data class AppState(
    val featureStates: Map<String, State>,
    val applifecycle: Lifecycle.State = Lifecycle.State.INITIALIZED,
    val userState: UserState = UserState.NoUser,
    val networkState: NetworkState = NetworkState.Offline
) : State {
    companion object {
        const val stateKey = "appState"
    }
}
```

And you can create a new store using:

```kotlin
createStore(
  initialState = AppState(),
  reducer = PullbackReducer(
    innerReducer = reducers[AppState.stateKey] as Reducer<AppState, Action>,
    mapToChildAction = { action -> action },
    mapToChildState = { state -> state },
    mapToParentAction = { action -> action },
    mapToParentState = { state, _ -> state },
  )
)
```

actions are sent like this:

```kotlin
store.dispatch(AppAction.BackPressed)
// or
store.dispatch(AppAction.BottomBarClicked(BottomTab.entries[index], navController))
// or
store.dispatch(AppAction.LoadConversations(
  Resource.Success(
    conversationDAO.getAll().map { it.toDomain() })
))
```

and views can subscribe like this:

```kotlin
    val state: AppState by store.state.collectAsStateWithLifecycle()
    // Propagate state down to child UI components
```

### Actions

Actions are sealed classes extending ai.flox.state.Action which have multiple types an Action can be. The Action can be of Action.UI with componentIdentifiers or Action.Data with the attached Resource on which the action is called on.
Any reducer can change the state off the app, based on any type of actions.

```kotlin
sealed interface AppAction : Action {
  data class Navigate(val navController: NavController, val route: String) : Action.UI.NavigateEvent, AppAction {
    override val componentIdentifier = AppIds.BottomBarIcon
  }

  data class BottomBarClicked(val id: BottomTab, val navController: NavController) : Action.UI.RenderEvent, AppAction {
    override val componentIdentifier = AppIds.BottomBarIcon
  }

  data class LoadConversations(override val resource: Resource<Conversation>) : Action.Data.LoadData<Conversation>,
    AppAction
}
```


### Reducers & Effects

Reducers are classes that implement the following interface:

```kotlin
fun interface Reducer<State, Action> {
    fun reduce(state: State, action: Action): ReduceResult<State, Action>
}
```

The idea is they take the state and an action and modify the state depending on the action and its payload.

In order to send actions asynchronously back to the store we use `Effect`s. Reducers return an array of `Effect`s.

We have 2 types of `Effect` builders, `withFlowEffect()` or `noEffect()`. If a reducer wants to dispatch a flow of `Action`s it can return a `Flow<Action>` as `ReduceResult` for async Action dispatch

else they can return `noEffect` which returns an empty flow.



## Contribute

If you'd like to contribute a library, please [open a
PR](https://github.com/saumye/flox/edit/main/README.md) with a link
to it!


## Other libraries

Flox was built on a foundation of ideas started by other libraries, in 
particular [Elm](https://elm-lang.org) and [Redux](https://redux.js.org/).

There are also many architecture libraries in the Android and Kotlin community. Each one of these has 
their own set of priorities and trade-offs that differ from Flox.

* [RIBs](https://github.com/uber/RIBs)
* [Komposable](https://github.com/toggl/komposable-architecture)
* [Redux-Kotlin](https://github.com/reduxkotlin/redux-kotlin)
* [Android-RDX](https://github.com/flipkart-incubator/android-RDX)
* [RxFlux](https://github.com/JohnnyShieh/RxFlux)
* [Mobius](https://github.com/spotify/mobius)
