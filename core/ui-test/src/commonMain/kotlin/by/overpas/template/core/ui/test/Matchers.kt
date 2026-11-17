package by.overpas.template.core.ui.test

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher

fun textIsNotEmpty(): SemanticsMatcher =
    SemanticsMatcher("Text is not empty") { node ->
        node.config.getOrNull(SemanticsProperties.Text)?.isEmpty() == false
    }
