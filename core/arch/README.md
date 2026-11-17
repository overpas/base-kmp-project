# core:arch

Holds the primitives every feature builds its unidirectional data flow on: the intent and action
handler types, the executor that dispatches an event to the handler registered for it, and the
scope that moves a state change onto the main thread as MviKotlin requires. It also holds the two
pieces of Decompose glue a component needs — an instance-scoped coroutine scope, and the sharing
policy a component's state flow uses — and it provides the named dispatchers through the
graph.

It knows nothing about any particular feature, and nothing about a platform: anything that names a
screen, a store's state, or a target belongs in the feature or in a platform module, not here.
