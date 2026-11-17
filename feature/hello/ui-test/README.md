# feature:hello:ui-test

Holds the page object for the hello UI: the one place that knows how to find and drive its name
field and its greet control.

It sits in a main source set because Gradle cannot share test code across projects.
