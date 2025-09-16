package com.batch.android.messaging.model

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.batch.android.messaging.model.cep.CEPMessage
import com.batch.android.messaging.model.cep.CloseOptions
import com.batch.android.messaging.model.cep.InAppComponent
import com.batch.android.messaging.model.cep.InAppProperty
import com.batch.android.messaging.model.cep.RootContainer
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CEPMessageTest {

    @Test
    fun testIsFullscreen() {
        assertTrue(
            buildMock(InAppProperty.Format.FULLSCREEN, InAppProperty.VerticalAlignment.CENTER)
                .isFullscreen()
        )
        assertFalse(
            buildMock(InAppProperty.Format.MODAL, InAppProperty.VerticalAlignment.CENTER)
                .isFullscreen()
        )
    }

    @Test
    fun testIsWebview() {
        assertTrue(
            buildMock(InAppProperty.Format.WEBVIEW, InAppProperty.VerticalAlignment.CENTER)
                .isWebView()
        )
        assertFalse(
            buildMock(InAppProperty.Format.MODAL, InAppProperty.VerticalAlignment.CENTER)
                .isWebView()
        )
    }

    @Test
    fun testIsModal() {
        assertTrue(
            buildMock(InAppProperty.Format.MODAL, InAppProperty.VerticalAlignment.CENTER).isModal()
        )
        assertTrue(
            buildMock(InAppProperty.Format.MODAL, InAppProperty.VerticalAlignment.TOP).isModal()
        )
        assertTrue(
            buildMock(InAppProperty.Format.MODAL, InAppProperty.VerticalAlignment.BOTTOM).isModal()
        )
        assertFalse(
            buildMock(InAppProperty.Format.FULLSCREEN, InAppProperty.VerticalAlignment.CENTER)
                .isModal()
        )
    }

    @Test
    fun testIsCenterModal() {
        assertTrue(
            buildMock(InAppProperty.Format.MODAL, InAppProperty.VerticalAlignment.CENTER)
                .isCenterModal()
        )
        assertFalse(
            buildMock(InAppProperty.Format.MODAL, InAppProperty.VerticalAlignment.TOP)
                .isCenterModal()
        )
        assertFalse(
            buildMock(InAppProperty.Format.MODAL, InAppProperty.VerticalAlignment.BOTTOM)
                .isCenterModal()
        )
        assertFalse(
            buildMock(InAppProperty.Format.FULLSCREEN, InAppProperty.VerticalAlignment.CENTER)
                .isCenterModal()
        )
    }

    @Test
    fun testIsBanner() {
        assertFalse(
            buildMock(InAppProperty.Format.MODAL, InAppProperty.VerticalAlignment.CENTER).isBanner()
        )
        assertTrue(
            buildMock(InAppProperty.Format.MODAL, InAppProperty.VerticalAlignment.TOP).isBanner()
        )
        assertTrue(
            buildMock(InAppProperty.Format.MODAL, InAppProperty.VerticalAlignment.BOTTOM).isBanner()
        )
        assertFalse(
            buildMock(InAppProperty.Format.FULLSCREEN, InAppProperty.VerticalAlignment.CENTER)
                .isBanner()
        )
    }

    @Test
    fun testIsTopBanner() {
        assertFalse(
            buildMock(InAppProperty.Format.MODAL, InAppProperty.VerticalAlignment.CENTER)
                .isTopBanner()
        )
        assertFalse(
            buildMock(InAppProperty.Format.MODAL, InAppProperty.VerticalAlignment.BOTTOM)
                .isTopBanner()
        )
        assertFalse(
            buildMock(InAppProperty.Format.FULLSCREEN, InAppProperty.VerticalAlignment.CENTER)
                .isTopBanner()
        )
        assertTrue(
            buildMock(InAppProperty.Format.MODAL, InAppProperty.VerticalAlignment.TOP).isTopBanner()
        )
    }

    @Test
    fun testIsAttachedTopBanner() {
        assertFalse(
            buildMock(
                    InAppProperty.Format.MODAL,
                    InAppProperty.VerticalAlignment.TOP,
                    listOf(),
                    InAppProperty.Margin(4, 0),
                )
                .isAttachedTopBanner()
        )
        assertFalse(
            buildMock(
                    InAppProperty.Format.MODAL,
                    InAppProperty.VerticalAlignment.TOP,
                    listOf(),
                    InAppProperty.Margin(0, 4),
                )
                .isAttachedTopBanner()
        )
        assertTrue(
            buildMock(
                    InAppProperty.Format.MODAL,
                    InAppProperty.VerticalAlignment.TOP,
                    listOf(),
                    InAppProperty.Margin(0, 0),
                )
                .isAttachedTopBanner()
        )
    }

    @Test
    fun testIsAttachedBottomBanner() {
        assertFalse(
            buildMock(
                    InAppProperty.Format.MODAL,
                    InAppProperty.VerticalAlignment.BOTTOM,
                    listOf(),
                    InAppProperty.Margin(4, 0),
                )
                .isAttachedBottomBanner()
        )
        assertFalse(
            buildMock(
                    InAppProperty.Format.MODAL,
                    InAppProperty.VerticalAlignment.BOTTOM,
                    listOf(),
                    InAppProperty.Margin(0, 4),
                )
                .isAttachedBottomBanner()
        )
        assertTrue(
            buildMock(
                    InAppProperty.Format.MODAL,
                    InAppProperty.VerticalAlignment.BOTTOM,
                    listOf(),
                    InAppProperty.Margin(0, 0),
                )
                .isAttachedBottomBanner()
        )
    }

    @Test
    fun testShouldFitsSystemWindows() {
        assertTrue(
            buildMock(
                    InAppProperty.Format.MODAL,
                    InAppProperty.VerticalAlignment.BOTTOM,
                    listOf(),
                    InAppProperty.Margin(4, 0),
                )
                .shouldFitsSystemWindows()
        )
        assertTrue(
            buildMock(
                    InAppProperty.Format.MODAL,
                    InAppProperty.VerticalAlignment.BOTTOM,
                    listOf(),
                    InAppProperty.Margin(0, 4),
                )
                .shouldFitsSystemWindows()
        )
        assertFalse(
            buildMock(
                    InAppProperty.Format.MODAL,
                    InAppProperty.VerticalAlignment.BOTTOM,
                    listOf(),
                    InAppProperty.Margin(0, 0),
                )
                .shouldFitsSystemWindows()
        )
        assertTrue(
            buildMock(
                    InAppProperty.Format.MODAL,
                    InAppProperty.VerticalAlignment.TOP,
                    listOf(),
                    InAppProperty.Margin(4, 0),
                )
                .shouldFitsSystemWindows()
        )
        assertTrue(
            buildMock(
                    InAppProperty.Format.MODAL,
                    InAppProperty.VerticalAlignment.TOP,
                    listOf(),
                    InAppProperty.Margin(0, 4),
                )
                .shouldFitsSystemWindows()
        )
        assertFalse(
            buildMock(
                    InAppProperty.Format.MODAL,
                    InAppProperty.VerticalAlignment.TOP,
                    listOf(),
                    InAppProperty.Margin(0, 0),
                )
                .shouldFitsSystemWindows()
        )
        assertFalse(
            buildMock(InAppProperty.Format.FULLSCREEN, InAppProperty.VerticalAlignment.TOP)
                .shouldFitsSystemWindows()
        )
    }

    @Test
    fun testIsImageFormat() {
        val messageImageFormat =
            buildMock(
                InAppProperty.Format.MODAL,
                InAppProperty.VerticalAlignment.CENTER,
                listOf(
                    InAppComponent.Image(
                        "IMG_1",
                        InAppProperty.Size("300px"),
                        InAppProperty.Margin(8, 8, 8, 8),
                        InAppComponent.Image.AspectRatio.FIT,
                        InAppProperty.CornerRadius(8, 8, 8, 8),
                    )
                ),
            )
        assertTrue(messageImageFormat.isImageFormat())

        val messageNonImageFormat =
            buildMock(
                InAppProperty.Format.MODAL,
                InAppProperty.VerticalAlignment.CENTER,
                listOf(
                    InAppComponent.Image(
                        "IMG_1",
                        InAppProperty.Size("300px"),
                        InAppProperty.Margin(8, 8, 8, 8),
                        InAppComponent.Image.AspectRatio.FIT,
                        InAppProperty.CornerRadius(8, 8, 8, 8),
                    ),
                    InAppComponent.Image(
                        "IMG_2",
                        InAppProperty.Size("300px"),
                        InAppProperty.Margin(8, 8, 8, 8),
                        InAppComponent.Image.AspectRatio.FIT,
                        InAppProperty.CornerRadius(8, 8, 8, 8),
                    ),
                ),
            )
        assertFalse(messageNonImageFormat.isImageFormat())
    }

    @Test
    fun testGetImagesComponents() {
        val message =
            buildMock(
                InAppProperty.Format.MODAL,
                InAppProperty.VerticalAlignment.CENTER,
                componentListMock,
            )
        assertTrue(message.getImagesComponents().size == 2)
        assertTrue(message.getImageComponentById("IMG_1") != null)
        assertTrue(message.getImageComponentById("IMG_2") != null)
    }

    @Test
    fun testGetImageComponentById() {
        val message =
            buildMock(
                InAppProperty.Format.MODAL,
                InAppProperty.VerticalAlignment.CENTER,
                componentListMock,
            )
        assertTrue(message.getImageComponentById("IMG_1") != null)
        assertTrue(message.getImageComponentById("IMG_2") != null)
        assertTrue(message.getImageComponentById("TXT_1") == null)
    }

    private fun buildMock(
        format: InAppProperty.Format,
        position: InAppProperty.VerticalAlignment,
        components: List<InAppComponent> = listOf(),
        rootContainerMargin: InAppProperty.Margin = InAppProperty.Margin(0, 8),
    ): CEPMessage {
        return CEPMessage(
            format,
            RootContainer(
                components,
                InAppProperty.ThemeColors("#FFFFFF00", "#00000000"),
                rootContainerMargin,
                InAppProperty.CornerRadius(10, 15, 20, 25),
                InAppProperty.Border(1, InAppProperty.ThemeColors("#FFFFFF00", "#00000000")),
            ),
            position,
            CloseOptions(
                CloseOptions.Auto(5, InAppProperty.ThemeColors("#00000000", "#FFFFFF00")),
                CloseOptions.Button(
                    InAppProperty.ThemeColors("#FFFFFF00", "#00000000"),
                    InAppProperty.ThemeColors("#00000000", "#FFFFFF00"),
                ),
            ),
            mapOf(),
            mapOf(),
            mapOf(),
        )
    }

    private val componentListMock: List<InAppComponent> =
        listOf(
            InAppComponent.Image(
                "IMG_1",
                InAppProperty.Size("300px"),
                InAppProperty.Margin(8, 8, 8, 8),
                InAppComponent.Image.AspectRatio.FIT,
                InAppProperty.CornerRadius(8, 8, 8, 8),
            ),
            InAppComponent.Text(
                "TXT_1",
                InAppProperty.ThemeColors("#CCFF22", "#330000"),
                InAppProperty.Margin(4, 4, 4, 4),
                InAppProperty.HorizontalAlignment.CENTER,
                setOf(InAppProperty.FontDecoration.BOLD, InAppProperty.FontDecoration.ITALIC),
                12,
                0,
            ),
            InAppComponent.Text(
                "TXT_2",
                InAppProperty.ThemeColors("#CCFF22", "#330000"),
                InAppProperty.Margin(4, 4, 4, 4),
                InAppProperty.HorizontalAlignment.CENTER,
                setOf(InAppProperty.FontDecoration.ITALIC),
                14,
                0,
            ),
            InAppComponent.Columns(
                floatArrayOf(0.4F, 0.2F, 0.4F),
                8,
                InAppProperty.Margin(4),
                InAppProperty.VerticalAlignment.CENTER,
                listOf(
                    InAppComponent.Button(
                        "BTN_1",
                        InAppProperty.ThemeColors("#FFFFFFFF", "#00000000"),
                        InAppProperty.ThemeColors("#CCFF22", "#330000"),
                        InAppProperty.Margin(4),
                        InAppProperty.Margin(4),
                        InAppProperty.Size("80%"),
                        InAppProperty.HorizontalAlignment.CENTER,
                        InAppProperty.CornerRadius(4),
                        null,
                        InAppProperty.HorizontalAlignment.CENTER,
                        setOf(
                            InAppProperty.FontDecoration.ITALIC,
                            InAppProperty.FontDecoration.BOLD,
                        ),
                        11,
                        1,
                    ),
                    InAppComponent.Image(
                        "IMG_2",
                        InAppProperty.Size("300px"),
                        InAppProperty.Margin(8, 8, 8, 8),
                        InAppComponent.Image.AspectRatio.FIT,
                        InAppProperty.CornerRadius(8, 8, 8, 8),
                    ),
                    InAppComponent.Button(
                        "BTN_2",
                        InAppProperty.ThemeColors("#FFFFFFFF", "#00000000"),
                        InAppProperty.ThemeColors("#CCFF22", "#330000"),
                        InAppProperty.Margin(4),
                        InAppProperty.Margin(4),
                        InAppProperty.Size("80%"),
                        InAppProperty.HorizontalAlignment.CENTER,
                        InAppProperty.CornerRadius(4),
                        null,
                        InAppProperty.HorizontalAlignment.CENTER,
                        setOf(InAppProperty.FontDecoration.BOLD),
                        11,
                        1,
                    ),
                ),
            ),
        )
}
