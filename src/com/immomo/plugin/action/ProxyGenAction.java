package com.immomo.plugin.action;

import com.immomo.plugin.builder.ProxyLogBuilder;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.impl.light.LightPackageReference;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * Created with IntelliJ IDEA.
 * User: Xmx
 * Date: 13-11-20
 * Time: 上午10:48
 */
public class ProxyGenAction extends AnAction {

    private final ProxyLogBuilder logBuilder = new ProxyLogBuilder();

    public void actionPerformed(AnActionEvent e) {

        VirtualFile chooseFile = e.getProject().getBaseDir();
        System.out.println(chooseFile.getPath());


        VirtualFile file = e.getData(DataKeys.VIRTUAL_FILE);
        System.out.println(file.getName());


        VirtualFile virtualFile = e.getProject().getBaseDir();

        System.out.println(e.getData(DataKeys.PROJECT_FILE_DIRECTORY));


        System.out.println(virtualFile.getName());


        final PsiJavaFile interfacePsiFile = (PsiJavaFile) DataKeys.PSI_FILE.getData(e.getDataContext());

        final PsiDirectory directory = interfacePsiFile.getParent();


        final PsiElementFactory factory = JavaPsiFacade.getElementFactory(e.getProject());
        final PsiElement interfaceClass = DataKeys.PSI_ELEMENT.getData(e.getDataContext());
        final String proxyClassName = ((PsiClass) interfaceClass).getName().concat("Proxy");
        final PsiJavaFile proxyFile = (PsiJavaFile) ApplicationManager.getApplication().runWriteAction(new Computable<PsiFile>() {
            @Override
            public PsiFile compute() {
                PsiDirectory implDir = directory.findSubdirectory("impl");
                String fileName = proxyClassName.concat(".java");
                if (null != implDir) {

                    if (null != implDir.findFile(fileName)) {
                        implDir.findFile(fileName).delete();
                        System.out.println("删除旧的proxy文件");
                    }
                } else {
                    implDir = directory.createSubdirectory("impl");
                }
                return implDir.createFile(fileName);
            }
        });


        final PsiElement[] interfaceChild = interfacePsiFile.getChildren();

        final PsiClass proxyClass = factory.createClass(proxyClassName);

        for (final PsiElement element : interfaceChild) {
            System.out.println("inteface------------" + element);
            if (element instanceof PsiPackageStatement ||
                    element instanceof PsiImportList) {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        if (element instanceof PsiImportList) {
                            proxyFile.add(element);
                            System.out.println("添加interface的类导入");
                        } else {
                            String packageStr = ((PsiPackageStatement) element).getPackageName().concat(".impl");
                            proxyFile.setPackageName(packageStr);
                            System.out.println("package 写入:" + packageStr);
                        }

                    }
                });

            } else if (element instanceof PsiClass) {
                /**
                 *开始解析类
                 */

                /**
                 * 1.生成proxy类名并创建这个类
                 */
                final PsiClass clazz = ((PsiClass) element);

                /**
                 * 2.引入import和Package
                 */


                /**
                 * 3.修改继承接口
                 */
                boolean isSkip = true;
                PsiElement[] proxyChild = proxyClass.getChildren();
                for (final PsiElement child : proxyChild) {

                    System.out.println("proxy------------" + child);
                    /**
                     * 如果是关键词即class那么就添加abstract
                     */
                    if (child instanceof PsiModifierList) {
                        child.add(factory.createKeyword(PsiKeyword.ABSTRACT));
                    } else if (child instanceof PsiReferenceList) {

                        if (isSkip) {
                            isSkip = false;
                            continue;
                        }

                        /**
                         * 如果是类名了这个时候需要添加实现接口
                         */
                        final PsiKeyword implKey = factory.createKeyword(PsiKeyword.IMPLEMENTS);
                        child.add(implKey);
                        child.add(factory.createClassReferenceElement(clazz));
                        /**
                         * 实现的接口
                         */
                    }
                }

                /**
                 * 创建个构造器
                 */
                proxyClass.add(factory.createConstructor());

                PsiField logField = factory.createFieldFromText("private static final org.apache.log4j.Logger LOG =" +
                        " org.apache.log4j.Logger.getLogger(" + proxyClassName + ".class);", proxyClass);
                proxyClass.add(logField);
                /**
                 * 对方方法进行proxy包装
                 */

                PsiMethod[] methods = clazz.getMethods();
                for (PsiMethod method : methods) {

                    /**
                     * 生成我们需要的代码
                     */
                    PsiElement impMethod = method.copy();
                    String proxyCode = logBuilder.genProxyCodeBlock(method);
                    System.out.println("代码：" + proxyCode);
                    PsiCodeBlock codeBlock = factory.createCodeBlockFromText(proxyCode, impMethod);
                    impMethod.add(codeBlock);

                    //将该方法加入
                    proxyClass.add(impMethod);

                }
            }

        }

        final PsiClass clazz = proxyClass;
        new WriteCommandAction.Simple(e.getProject(), proxyFile) {
            @Override
            public void run() throws Throwable {
                proxyFile.add(clazz);
            }
        }.execute();

    }
}
