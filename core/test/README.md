# core:test

Holds the helpers that unit tests in every module share: an assertion that runs a block on a value,
and the collection of a flow on a background coroutine of the running test scope, so a test reads
the latest value without blocking.

It holds no Compose UI test support; that belongs in `core:ui-test`.
