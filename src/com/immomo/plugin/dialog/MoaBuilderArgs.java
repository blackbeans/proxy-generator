package com.immomo.plugin.dialog;

/**
 * Created by blackbeans on 12/3/14.
 */
public class MoaBuilderArgs {
    private String serviceUri;

    private String logPath;
    private String interfaceName;
    private String interfaceUri;
    private int servicePort ;
    private String svn ;
    private String deployPath;


    public String getInterfaceUri() {
        return interfaceUri;
    }

    public void setInterfaceUri(String interfaceUri) {
        this.interfaceUri = interfaceUri;
    }

    public String getServiceName() {
        return serviceUri.substring(serviceUri.lastIndexOf("/") + 1);
    }

    public String getLogPath() {
        return "/home/logs/moa-service/"+this.getServiceName();
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getServiceUri() {
        return serviceUri;
    }

    public void setServiceUri(String serviceUri) {
        this.serviceUri = serviceUri;
    }

    public int getServicePort() {
        return servicePort;
    }

    public void setServicePort(int servicePort) {
        this.servicePort = servicePort;
    }

    public String getSvn() {
        return svn;
    }

    public void setSvn(String svn) {
        this.svn = svn;
    }

    public String getDeployPath() {
        return deployPath;
    }

    public void setDeployPath(String deployPath) {
        this.deployPath = deployPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MoaBuilderArgs that = (MoaBuilderArgs) o;

        if (servicePort != that.servicePort) return false;
        if (deployPath != null ? !deployPath.equals(that.deployPath) : that.deployPath != null) return false;
        if (interfaceName != null ? !interfaceName.equals(that.interfaceName) : that.interfaceName != null)
            return false;
        if (interfaceUri != null ? !interfaceUri.equals(that.interfaceUri) : that.interfaceUri != null) return false;
        if (serviceUri != null ? !serviceUri.equals(that.serviceUri) : that.serviceUri != null) return false;
        if (svn != null ? !svn.equals(that.svn) : that.svn != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = serviceUri != null ? serviceUri.hashCode() : 0;
        result = 31 * result + (interfaceName != null ? interfaceName.hashCode() : 0);
        result = 31 * result + (interfaceUri != null ? interfaceUri.hashCode() : 0);
        result = 31 * result + servicePort;
        result = 31 * result + (svn != null ? svn.hashCode() : 0);
        result = 31 * result + (deployPath != null ? deployPath.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MoaBuilderArgs{" +
                "serviceUri='" + serviceUri + '\'' +
                ", interfaceName='" + interfaceName + '\'' +
                ", interfaceUri='" + interfaceUri + '\'' +
                ", servicePort=" + servicePort +
                ", svn='" + svn + '\'' +
                ", deployPath='" + deployPath + '\'' +
                '}';
    }
}
