# core:platform

Holds what the app must ask the running target about: which platform it is on, and the per-target
services that only that target can supply — the Android context, the desktop system properties, and
the desktop file-backed saver that keeps root navigation state across a restart. Each target
contributes its own graph so a caller reaches these through injection rather than through a target
check.

Behaviour that is the same on every target does not belong here — it belongs in whichever module
owns the concept.
