package funnymap.utils

import net.minecraftforge.fml.common.FMLModContainer
import net.minecraftforge.fml.common.ILanguageAdapter
import net.minecraftforge.fml.common.ModContainer
import net.minecraftforge.fml.relauncher.Side
import java.lang.reflect.Field
import java.lang.reflect.Method

class KotlinAdapter : ILanguageAdapter {
    override fun supportsStatics(): Boolean = false
    override fun getNewInstance(
        container: FMLModContainer?, objectClass: Class<*>, classLoader: ClassLoader?, factoryMarkedAnnotation: Method?
    ): Any = objectClass.kotlin.objectInstance ?: objectClass.getDeclaredConstructor().newInstance()

    override fun setProxy(target: Field, proxyTarget: Class<*>, proxy: Any?) {
        target.set(proxyTarget.kotlin.objectInstance, proxy)
    }

    override fun setInternalProxies(mod: ModContainer?, side: Side?, loader: ClassLoader?) {}
}
