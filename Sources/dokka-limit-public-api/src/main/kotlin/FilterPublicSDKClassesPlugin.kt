package com.batch.dokka.plugin

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement

class FilterPublicSDKClassesPlugin : DokkaPlugin() {

    val filterExtension by extending {
        plugin<DokkaBase>().preMergeDocumentableTransformer providing ::FilterPublicSDKClassesTransformer
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement() = PluginApiPreviewAcknowledgement
}
