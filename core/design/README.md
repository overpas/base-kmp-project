# core:design

Holds the design system: the theme, the spacing and size values, the icons, and the components
that more than one feature needs. A component takes its text as parameters and its test tag
through its modifier, so the feature that places it owns the tag and the words.

The template ships only a theme over Material 3 and two spacing values. Put the project's own
design system here: feature modules may not import low-level Compose primitives such as `dp`,
shapes, colors or fonts, so every visual decision comes from this module.

A control only one feature will ever show belongs in that feature.
