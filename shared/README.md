# shared

The application shell. It holds the composable root, the Decompose root component and its navigation
stack, and the dependency graph that aggregates every module's contribution. It is also where each feature's navigator interface is implemented, so no feature ever names another.

A rule belonging to one feature does not belong here.
