package pl.panszelescik.proxy_protocol_support.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import pl.panszelescik.proxy_protocol_support.shared.config.Config;
import pl.panszelescik.proxy_protocol_support.shared.config.Configuration;
import pl.panszelescik.proxy_protocol_support.shared.ProxyProtocolSupport;

import java.io.IOException;

/**
 * Forge's main file
 *
 * @author PanSzelescik
 */
@Mod(ProxyProtocolSupport.MODID)
public class ProxyProtocolInitializer {

    public ProxyProtocolInitializer() {
        ProxyProtocolSupport.infoLogger.accept("[代理协议] 代理协议支持模组正在初始化...");
        
        DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> {
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 运行在专用服务器上，正在加载配置...");
            try {
                java.io.File configDir = FMLPaths.CONFIGDIR.get().toFile();
                java.io.File configFile = new java.io.File(configDir, "proxy_protocol_support.json");
                
                Config config = Configuration.loadConfig(configDir);
                ProxyProtocolSupport.initialize(config);
                ProxyProtocolSupport.infoLogger.accept("[代理协议] 代理协议支持模组初始化成功！");
            } catch (IOException e) {
                ProxyProtocolSupport.errorLogger.accept("[代理协议] 加载配置文件时发生错误：");
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }
}
