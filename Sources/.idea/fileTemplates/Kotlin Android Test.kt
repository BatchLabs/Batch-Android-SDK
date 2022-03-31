#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME}

#end
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith

#parse("File Header.java")
@RunWith(AndroidJUnit4::class)
@SmallTest
class ${NAME} {

    @Test
    fun testFeature() {
    }
}