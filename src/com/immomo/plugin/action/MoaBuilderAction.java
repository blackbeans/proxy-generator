package com.immomo.plugin.action;

import com.immomo.plugin.dialog.MoaBuilderForm;
import com.immomo.plugin.dialog.MoaBuilderArgs;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.*;
import com.intellij.ui.awt.RelativePoint;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.Filter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import java.io.*;
import java.util.*;

/**
 * moa工程生成器
 * Created by blackbeans onFormOk 12/3/14.
 */
public class MoaBuilderAction extends AnAction {

    private Project project;
    private StatusBar statusBar;

    @Override
    public void actionPerformed(final AnActionEvent e) {

        project = e.getProject();
        statusBar = WindowManager.getInstance().getStatusBar(DataKeys.PROJECT.getData(e.getDataContext()));

        //获取trunk的文件目录
        VirtualFile currentDir = DataKeys.VIRTUAL_FILE.getData(e.getDataContext());

        if (!"trunk".equalsIgnoreCase(currentDir.getName())) {
            currentDir = finldVirtualFile(e.getProject().getBaseDir(), "trunk");
        }

        final VirtualFile rootDir = currentDir;

        if (null == rootDir) {
            this.alert("请选择trunk目录或者根目录！");
            return ;
        }



        //选择当服务的根目录
        //显示当前的填写的dialog
        MoaBuilderForm dialog = new MoaBuilderForm(new MoaBuilderForm.IBuilderFormListener() {
            @Override
            public void onOk(MoaBuilderArgs args) {
                onFormOk(rootDir, args);
            }
        });

        //默认的服务名称
        dialog.getJt_serviceUri().setText("/service/" + project.getName());
        dialog.getJt_deployPath().setText("/home/deploy/moaservice/" + project.getName());
        dialog.pack();
        dialog.setSize(600, 320);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);


    }


    private void onFormOk(final VirtualFile rootDir, final MoaBuilderArgs args) {


        //处理文件名称
        final String serviceName = args.getServiceUri().substring(args.getServiceUri().lastIndexOf("/") + 1);
        final String interfaceName = args.getInterfaceName();

        //拼接服务包名称
        String[] packages = serviceName.split("\\-");
        String servicePackage = basePackage;
        for (int i = 0; i < packages.length; i++) {
            servicePackage += packages[i];
            if (i < packages.length - 1) {
                servicePackage += ".";
            }
        }

        final String sPackage = servicePackage;
        args.setInterfaceUri(sPackage + "." + interfaceName);
        ApplicationManager.getApplication().runWriteAction(new Computable<VirtualFile>() {
            @Override
            public VirtualFile compute() {
                //修改moa_tcp_client
                modifyMoaTcpClient(rootDir, serviceName, interfaceName, sPackage);

                //修改接口名称
                createInterface(rootDir, sPackage, interfaceName);

                //重命名文件目录 module名称
                renameDir(args, rootDir, serviceName);
                return null;
            }
        });

        //修改impl中的文件
        ApplicationManager.getApplication().runWriteAction(new Computable<VirtualFile>() {
            @Override
            public VirtualFile compute() {
                final VirtualFile serviceXml = MoaBuilderAction.this.finldVirtualFile(rootDir, "service.xml");
                //修改Moa配置文件
                modifyMoaXml(serviceXml, args);
                final VirtualFile log4m = MoaBuilderAction.this.finldVirtualFile(rootDir, "log4m.properties");
                //修改log4m
                modifyLog4m(log4m, args);
                final VirtualFile supervisor = MoaBuilderAction.this.finldVirtualFile(rootDir, "service.supervisor");
                modifySupervisor(supervisor, args);
                return null;
            }
        });

    }

    private void modifySupervisor(final VirtualFile supervisor, final MoaBuilderArgs args) {
        InputStreamReader isr = null;
        BufferedReader br = null;
        DataOutputStream dos = null;
        try {
            isr = new InputStreamReader(supervisor.getInputStream());
            br = new BufferedReader(isr);
            String line = null;
            StringBuilder sb = new StringBuilder();
            while (StringUtils.isNotBlank(line = br.readLine())) {
                sb.append(line.replaceAll("\\{0\\}", args.getServiceName())
                        .replaceAll("\\{1\\}", args.getDeployPath())
                        .replaceAll("\\{2\\}", args.getLogPath()));
                sb.append("\n");
            }
            //存储进去
            dos = new DataOutputStream(supervisor.getOutputStream(supervisor));
            dos.writeUTF(sb.toString());
            //修改名称
            supervisor.rename(supervisor, args.getServiceName() + ".supervisor");
        } catch (Exception e) {
            this.alert("处理service.supervisor失败！" + e);
        } finally {
            if (null != br) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != dos) {
                try {
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void modifyLog4m(final VirtualFile log4m, final MoaBuilderArgs args) {
        Properties prop = new Properties();
        InputStream is = null;
        OutputStream os = null;

        try {
            is = log4m.getInputStream();
            prop.load(is);
            for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                if ("log4m.alarm.appname".equalsIgnoreCase(String.valueOf(entry.getKey()))) {
                    entry.setValue(args.getServiceName());
                } else if ("log4m.root.path".equalsIgnoreCase(String.valueOf(entry.getKey()))) {
                    entry.setValue(args.getLogPath());
                }
            }
            //存储进去
            os = log4m.getOutputStream(log4m);
            prop.store(os, "momo-tools自动生成");
            //修改名称
            log4m.rename(log4m, "log4m-" + args.getServiceName() + ".properties");
        } catch (Exception e) {
            this.alert("处理log4m.properties失败！" + e);
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != os) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    private void modifyMoaXml(final VirtualFile serviceXml, final MoaBuilderArgs args) {

        Document doc = this.readXml(serviceXml);
        Element root = doc.getRootElement();
        //替换props
        List<Element> props = this.searchElement(root, "prop");
        for (Element e : props) {
            String key = e.getAttributeValue("key");
            String value = null;
            if ("moaLogPath".equalsIgnoreCase(key)) {
                value = args.getLogPath();
            } else if ("momo.log.name".equalsIgnoreCase(key)) {
                value = "./log4m-" + args.getServiceName() + ".properties";
            } else if ("moaPort".equalsIgnoreCase(key)) {
                value = "" + args.getServicePort();
            } else if ("momo.alarm.appname".equalsIgnoreCase(key)) {
                value = args.getServiceName();
            }
            if (StringUtils.isNotBlank(value)) {
                e.setText(value);
            }
        }


        //替换MOAProvider

//        <property name="serviceUri" value="{0}"/>
//        <property name = "interface" value = "{2}" / >
        List<Element> property = this.searchElement(root, "property");
        for (Element e : property) {
            String name = e.getAttributeValue("name");
            if ("serviceUri".equalsIgnoreCase(name)) {
                e.getAttribute("value").setValue(args.getServiceUri());
            } else if ("interface".equalsIgnoreCase(name)) {
                e.getAttribute("value").setValue(args.getInterfaceUri());
            }
        }


        //重新命名xml
        try {
            this.writeXml(serviceXml, doc);
            serviceXml.rename(serviceXml, args.getServiceName() + ".xml");
        } catch (IOException e) {
            this.alert("写入 service.xml 失败！" + e);
        }

    }


    private void createInterface(final VirtualFile rootDir,
                                 final String servicePackageName,
                                 final String interfaceName) {

        VirtualFile api = this.finldVirtualFile(rootDir.findChild("service-api"), "main");
        String[] packages = (String[]) ArrayUtils.add(servicePackageName.split("\\."), 0, "java");
        //创建目录
        VirtualFile pre = api;
        for (String p : packages) {
            try {
                if (null == pre.findChild(p) ||
                        pre.findChild(p).isDirectory()) {
                    pre = pre.createChildDirectory(pre, p);
                }
            } catch (IOException e) {
                //出错的话取消创建
                e.printStackTrace();
                return;
            }
        }

        final PsiDirectory preDir = PsiManager.getInstance(MoaBuilderAction.this.project).findDirectory(pre);
        final PsiJavaFile psiJavaFile = (PsiJavaFile) ApplicationManager.getApplication().runWriteAction(new Computable<PsiFile>() {
            @Override
            public PsiFile compute() {
                return preDir.createFile(interfaceName + ".java");
            }
        });

        final PsiElementFactory factory = JavaPsiFacade.getElementFactory(this.project);
        final PsiClass interfacePsi = factory.createInterface(interfaceName);
        new WriteCommandAction.Simple(this.project, psiJavaFile) {
            @Override
            protected void run() throws Throwable {
                psiJavaFile.setPackageName(servicePackageName);
            }
        }.execute();

        new WriteCommandAction.Simple(this.project, psiJavaFile) {
            @Override
            protected void run() throws Throwable {
                psiJavaFile.add(interfacePsi);
            }
        }.execute();


    }

    private final String basePackage = "com.immomo.moaservice.api.";
    private final String moaClientProp = "#客户端需要调用时候填写的serviceuri和接口名称\n" +
            "runMode=online\n" +
            "/service/{0}.interface={1}.{2}\n" +
            "/service/{0}.protocol=tcp";

    private void modifyMoaTcpClient(final VirtualFile rootDir,
                                    final String serviceName,
                                    final String interfaceName,
                                    final String servicePackage) {

        //修改api的moa_tcp_client
        VirtualFile moaClient = this.finldVirtualFile(rootDir.findChild("service-api"), "moa_tcp_client.properties");
        OutputStream os = null;
        DataOutputStream dos = null;
        try {
            os = moaClient.getOutputStream(moaClient);
            dos = new DataOutputStream(os);
            String text = moaClientProp.
                    replaceAll("\\{0\\}", serviceName).
                    replaceAll("\\{1\\}", servicePackage).
                    replaceAll("\\{2\\}", interfaceName);

            dos.writeUTF(text);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != os) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private VirtualFile renameDir(MoaBuilderArgs args, final VirtualFile rootDir, final String serviceName) {

        VirtualFile apiDir = rootDir.findChild("service-api");
        VirtualFile implDir = rootDir.findChild("service-impl");

        modifyModule(args, this.finldVirtualFile(apiDir, "pom.xml"), serviceName + "-api");
        modifyModule(args, this.finldVirtualFile(implDir, "pom.xml"), serviceName + "-impl");
        modifyModule(args, this.finldVirtualFile(rootDir, "pom.xml"), serviceName + "-api", serviceName + "-impl");

        //修改目录名称
        try {
            apiDir.rename(apiDir, serviceName + "-api");
            implDir.rename(implDir, serviceName + "-impl");
        } catch (IOException e1) {
            this.alert("修改工程目录失败:" + e1);
        }
        return rootDir;
    }


    private void modifyModule(MoaBuilderArgs args, final VirtualFile pomfile, String... names) {

        Document doc = this.readXml(pomfile);
        Element root = doc.getRootElement();
        //属于根的pom
        if (names.length == 2) {
            //修改module
            List<Element> modules = searchElement(root, "module");
            for (int i = 0; i < names.length; i++) {
                modules.get(i).setText(names[i]);
            }

            //svn修改
            List<Element> svnTags = searchElement(root, "tagBase");
            List<Element> connection = searchElement(root, "connection");
            List<Element> developerConnection = searchElement(root, "developerConnection");

            svnTags.addAll(connection);
            svnTags.addAll(developerConnection);

            //scm设置
            for (Element e : svnTags) {
                // connection
                //如果release插件则修改svn
                String svn = e.getText().replaceAll("\\{svn\\}", args.getSvn());
                e.setText(svn);
            }

        } else {
            List<Element> artifactId = this.searchElement(root, "artifactId");
            artifactId.get(0).setText(names[0]);
        }

        this.writeXml(pomfile, doc);

    }

    private Document readXml(VirtualFile xmlfile) {
        InputStream is = null;
        SAXBuilder saxBuilder = new SAXBuilder(false);
        Document doc = null;

        try {
            is = xmlfile.getInputStream();
            doc = saxBuilder.build(is);
            return doc;
        } catch (Exception e) {
            this.alert("读取" + xmlfile + "失败！" + e);
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    private void writeXml(VirtualFile xmlfile, Document doc) {
        OutputStream os = null;
        try {
            os = xmlfile.getOutputStream(xmlfile);
            XMLOutputter out = new XMLOutputter();
            out.output(doc, os);
        } catch (IOException e) {
            this.alert(e);
        } finally {
            if (null != os) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static List<Element> searchElement(Element root, String nodeName) {
        List<Element> result = new ArrayList<Element>();
        Queue<List<Element>> queue = new LinkedList<List<Element>>();

        List<Element> list = root.getContent(new Filter() {
            @Override
            public boolean matches(Object o) {
                return o instanceof Element;
            }
        });
        while (null != list && list.size() > 0) {
            for (Element child : list) {
                if (nodeName.equalsIgnoreCase(child.getName())) {
                    result.add(child);
                }
                list = child.getContent(new Filter() {
                    @Override
                    public boolean matches(Object o) {
                        return o instanceof Element;
                    }
                });
                if (list.size() > 0) {
                    queue.offer(list);
                }
            }
            list = queue.poll();
        }
        return result;
    }

    private void alert(Object text) {
        JBPopupFactory.getInstance().
                createHtmlTextBalloonBuilder("" + text, MessageType.WARNING, null).setFadeoutTime(60 * 1000)
                .createBalloon()
                .show(RelativePoint.getCenterOf(this.statusBar.getComponent()), Balloon.Position.above);
    }

    private VirtualFile finldVirtualFile(VirtualFile rootDir, String targetName) {
        Queue<VirtualFile[]> queue = new LinkedList<VirtualFile[]>();
        VirtualFile[] children = rootDir.getChildren();
        while (!ArrayUtils.isEmpty(children)) {

            for (VirtualFile child : children) {
                if (targetName.equalsIgnoreCase(child.getName())) {
                    return child;
                }
                if (!ArrayUtils.isEmpty(child.getChildren())) {
                    queue.offer(child.getChildren());
                }
            }
            children = queue.poll();
        }

        return null;
    }

    public static void main(String[] args) {
        String[] arr = new String[]{"1", "2"};
        arr = (String[]) ArrayUtils.add(arr, 0, "0");
        System.out.println(arr);
    }

}
