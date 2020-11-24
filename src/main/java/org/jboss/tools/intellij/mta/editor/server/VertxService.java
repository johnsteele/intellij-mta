package org.jboss.tools.intellij.mta.editor.server;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.PathUtil;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import org.bouncycastle.math.ec.ScaleYNegateXPointMap;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class VertxService {

    private Vertx vertx;
    private EventBus eventBus;
    private HttpServer server;
    private Router router;

    public VertxService() {
        this.init();
    }

    private void init() {
        System.setProperty("vertx.disableFileCPResolving", "true");
//        System.setProperty("$APP_HOME", "./webroot");
        this.vertx = Vertx.vertx();
        this.eventBus = vertx.eventBus();
        this.router = Router.router(this.vertx);
        this.startServer();
    }

    private InputStream getFileFromResourceAsStream(String fileName) {

        // The class loader that loaded the class
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        // the stream holding the file content
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }

    }

    private List<File> getAllFilesFromResource(String folder)
            throws URISyntaxException, IOException {

        ClassLoader classLoader = getClass().getClassLoader();

        URL resource = classLoader.getResource(folder);

        // dun walk the root path, we will walk all the classes
        List<File> collect = Files.walk(Paths.get(resource.toURI()))
                .filter(Files::isRegularFile)
                .map(x -> x.toFile())
                .collect(Collectors.toList());

        return collect;
    }

    private void startServer() {
        SockJSBridgeOptions opts = new SockJSBridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddressRegex("to.server.*"))
                .addOutboundPermitted(new PermittedOptions().setAddressRegex("to.client.*"));
        Router ebHandler = SockJSHandler.create(this.vertx).bridge(opts);
//        this.router.mountSubRouter("/bus/*", ebHandler);
        this.router.mountSubRouter("/bus/", ebHandler);

        IdeaPluginDescriptor descriptor = PluginManager.getPlugin(PluginId.getId("org.jboss.tools.intellij.mta"));
        File webroot = new File(descriptor.getPath(), "lib/webroot");
        System.out.println("Files: " + Arrays.toString(webroot.list()));

        String root = webroot.getAbsolutePath();

        this.router.route("/static/*").handler(StaticHandler.create().setAllowRootFileSystemAccess(true).setWebRoot(root));
        router.route("/").handler(ctx -> {
            System.out.println("Got an HTTP request to /");
            ctx.response().sendFile("webroot/index.html").end();
        });
        this.server = this.vertx.createHttpServer().requestHandler(this.router).listen(8077);
    }

    public Router getRouter() {
        return this.router;
    }

    public Vertx getVertx() {
        return this.vertx;
    }
}