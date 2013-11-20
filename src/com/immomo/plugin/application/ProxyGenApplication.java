package com.immomo.plugin.application;

import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 * User: Xmx
 * Date: 13-11-20
 * Time: 上午10:46
 * To change this template use File | Settings | File Templates.
 */
public class ProxyGenApplication implements ApplicationComponent {
    public ProxyGenApplication() {
    }

    public void initComponent() {
        // TODO: insert component initialization logic here
    }

    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    @NotNull
    public String getComponentName() {
        return "ProxyGenApplication";
    }
}
