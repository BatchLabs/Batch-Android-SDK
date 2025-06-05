package com.batch.dokka.plugin

import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext

class FilterPublicSDKClassesTransformer(
    context: DokkaContext,
) : SuppressedByConditionDocumentableFilterTransformer(context) {

    override fun shouldBeSuppressed(d: Documentable): Boolean {

        // Non class types are public, as otherwise dokka will skip complete packages
        // Enums, fields, etc are also public as all that matters is the Class' PublicSDK
        // annotation.
        val isPublic = when (d) {
            is DClasslike -> d.isBatchPublicSDK()
            else -> true
        }

        if (!isPublic) {
            context.logger.warn("Suppressing non-PublicSDK documentable '${d.name}'")
        }

        return !isPublic
    }
}

fun DClasslike.isBatchPublicSDK(): Boolean {
    val annotations: List<Annotations.Annotation> =
        (this as? WithExtraProperties<*>)
            ?.extra
            ?.allOfType<Annotations>()
            ?.flatMap { a -> a.directAnnotations.flatMap { it.value } }
            ?: emptyList()

    return annotations.any { it.isBatchPublicSDK() }
}

fun Annotations.Annotation.isBatchPublicSDK(): Boolean =
    dri.packageName == "com.batch.android.annotation" && dri.classNames == "PublicSDK"
