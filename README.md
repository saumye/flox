# flox
Flux Architecture on top of Kotlin Flows

[![CI](https://github.com/pointfreeco/swift-composable-architecture/workflows/CI/badge.svg)](https://github.com/pointfreeco/swift-composable-architecture/actions?query=workflow%3ACI)
![Static Badge](https://img.shields.io/badge/Kotlin_Compatibility-1.9.0-green)

Flux on Flow (Flox, for short) is a library for building Android applications via using the unidirectional flow of the Flux pattern. Redux the most popular state management system in the web applications world is built on this architecture used by >50% of web applications. In times of declarative UI becoming popular (via Jetpack Compose) in the Android ecosystem, this ease of state management system leads to faster development, less boiler plate, easy debuggability and readability of large scale Android apps.


## What is the Flox and why should I use it?

This library provides a few core tools that can be used to build applications of varying purpose and 
complexity. It provides compelling stories that you can follow to solve many problems you encounter 
day-to-day when building applications, such as:

* **State management**
  <br> How to manage the state of your application using simple value types, and share state across 
  many screens so that mutations in one screen can be immediately observed in another screen.

* **Composition**
  <br> How to break down large features into smaller components that can be extracted to their own, 
  isolated modules and be easily glued back together to form the feature.

* **Side effects**
  <br> How to let certain parts of the application talk to the outside world in the most testable 
  and understandable way possible.

* **Testing**
  <br> How to not only test a feature built in the architecture, but also write integration tests 
  for features that have been composed of many parts, and write end-to-end tests to understand how 
  side effects influence your application. This allows you to make strong guarantees that your 
  business logic is running in the way you expect.

* **Ergonomics**
  <br> How to accomplish all of the above in a simple API with as few concepts and moving parts as 
  possible.



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

As a basic example, consider a UI that shows a number along with "+" and "−" buttons that increment 
and decrement the number. To make things interesting, suppose there is also a button that when 
tapped makes an API request to fetch a random fact about that number and then displays the fact in 
an alert.

### Store & State

The `Store` exposes a flow which emits the whole state of the app every time there's a change and a method to send actions that will modify that state.  The `State` is just a data class that contains ALL the state of the application. It also includes the local state of all the specific modules that need local state. More on this later.

The store interface looks like this:

```kotlin
interface Store<State, Action : Any> {
    val state: Flow<State>
    fun send(actions: List<Action>)
    // more code
}
```

And you can create a new store using:

```kotlin
createStore(
    initialState = AppState(),
    reducer = reducer,
    subscription = subscription,
    dispatcherProvider = dispatcherProvider,
    storeScopeProvider = application as StoreScopeProvider
)
```

actions are sent like this:

```kotlin
store.send(AppAction.BackPressed)
```

and views can subscribe like this:

```kotlin
store.state
    .onEach { Log.d(tag, "The whole state: \($0)") }
    .launchIn(scope)

// or

store.state
    .map { it.email }
    .onEach { emailTextField.text = it }
    .launchIn(scope)
```

The store can be "viewed into", which means that we'll treat a generic store as if it was a more specific one which deals with only part of the app state and a subset of the actions. More on the Store Views section.

### Actions

Actions are sealed classes, which makes it easier to discover which actions are available and also add the certainty that we are handling all of them in reducers.

```kotlin
sealed class EditAction {
    data class TitleChanged(val title: String) : EditAction()
    data class DescriptionChanged(val description: String) : EditAction()
    object CloseTapped : EditAction()
    object SaveTapped : EditAction()
    object Saved : EditAction()
}
```

These sealed actions are embedded into each other starting with the "root" `AppAction`

```kotlin
sealed class AppAction {
    class List(override val action: ListAction) : AppAction(), ActionWrapper<ListAction>
    class Edit(override val action: EditAction) : AppAction(), ActionWrapper<EditAction>
    object BackPressed : AppAction()
}
```

So to send an `EditAction` to a store that takes `AppActions` we would do

```kotlin
store.send(AppAction.Edit(EditAction.TitleChanged("new title")))
```

But if the store is a view that takes `EditAction`s we'd do it like this:

```kotlin
store.send(EditAction.TitleChanged("new title"))
```

### Reducers & Effects

Reducers are classes that implement the following interface:

```kotlin
fun interface Reducer<State, Action> {
    fun reduce(state: State, action: Action): ReduceResult<State, Action>
}
```

The idea is they take the state and an action and modify the state depending on the action and its payload.

In order to send actions asynchronously we use `Effect`s. Reducers return an array of `Effect`s. The store waits for those effects and sends whatever action they emit, if any.

An effect interface is also straightforward:

```kotlin
interface Effect<out Action> {
    suspend fun execute(): Action?
}
```


## Installation

You can add ComposableArchitecture to an Xcode project by adding it as a package dependency.


## Contribute

If you'd like to contribute a library, please [open a
PR](https://github.com/saumye/flox/edit/main/README.md) with a link
to it!



## FAQ

* How does the Flox compare to Elm, Redux, and others?
  <details>
    <summary>Expand to see answer</summary>
    Flox is built on a foundation of ideas popularized by the Elm 
    Architecture (TEA) and Redux, but customised for Kotlin apps on top of Kotlin Flows in times of declarative UI such as 
    Jetpack Compose on Google's platform.

    In some ways TCA is a little more opinionated than the other libraries. For example, Redux is 
    not prescriptive with how one executes side effects, but TCA requires all side effects to be 
    modeled in the `Effect` type and returned from the reducer.

    In other ways TCA is a little more lax than the other libraries. For example, Elm controls what 
    kinds of effects can be created via the `Cmd` type, but TCA allows an escape hatch to any kind 
    of effect since `Effect` wraps around an async operation.

    And then there are certain things that TCA prioritizes highly that are not points of focus for 
    Redux, Elm, or most other libraries. For example, composition is very important aspect of TCA, 
    which is the process of breaking down large features into smaller units that can be glued 
    together. This is accomplished with reducer builders and operators like `Scope`, and it aids in 
    handling complex features as well as modularization for a better-isolated code base and improved 
    compile times.
  </details>


## Other libraries

The Composable Architecture was built on a foundation of ideas started by other libraries, in 
particular [Elm](https://elm-lang.org) and [Redux](https://redux.js.org/).

There are also many architecture libraries in the Android and Kotlin community. Each one of these has 
their own set of priorities and trade-offs that differ from Flox.

* [RIBs](https://github.com/uber/RIBs)
* [Komposable](https://github.com/ReactiveCocoa/Loop)](https://github.com/toggl/komposable-architecture)
* [Redux-Kotlin](https://github.com/reduxkotlin/redux-kotlin)
* [Android-RDX](https://github.com/flipkart-incubator/android-RDX)
* [RxFlux](https://github.com/JohnnyShieh/RxFlux)
* [Mobius](https://github.com/spotify/mobius)
