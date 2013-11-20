package com.immomo.plugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;

/**
 * Created with IntelliJ IDEA.
 * User: Xmx
 * Date: 13-11-20
 * Time: 上午10:48
 */
public class ProxyGenAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {

        VirtualFile chooseFile = e.getProject().getBaseDir();
        System.out.println(chooseFile.getPath());


        VirtualFile file = e.getData(DataKeys.VIRTUAL_FILE);
        System.out.println(file.getName());


        VirtualFile virtualFile = e.getProject().getBaseDir();

        System.out.println(e.getData(DataKeys.PROJECT_FILE_DIRECTORY));


        System.out.println(virtualFile.getName());


        final PsiFile interfacePsiFile = DataKeys.PSI_FILE.getData(e.getDataContext());

        final PsiDirectory directory = interfacePsiFile.getParent();


        final PsiElementFactory factory = JavaPsiFacade.getElementFactory(e.getProject());
        final PsiElement interfaceClass = DataKeys.PSI_ELEMENT.getData(e.getDataContext());
        final String interfaceName = ((PsiClass) interfaceClass).getName().concat("Proxy");
        final PsiFile proxyFile = ApplicationManager.getApplication().runWriteAction(new Computable<PsiFile>() {
            @Override
            public PsiFile compute() {
                PsiDirectory implDir = directory.findSubdirectory("impl");
                String fileName = interfaceName.concat(".java");
                if (null != implDir) {

                    if (null != implDir.findFile(fileName)) {
                        implDir.delete();
                        System.out.println("删除旧的proxy文件");
                    }
                } else {
                    implDir = directory.createSubdirectory("impl");
                }
                return implDir.createFile(fileName);
            }
        });


        final PsiElement[] interfaceChild = interfacePsiFile.getChildren();

        final PsiClass proxyClass = factory.createClass(interfaceName);

        for (final PsiElement element : interfaceChild) {
            System.out.println("inteface------------" + element);
            if (element instanceof PsiPackageStatement ||
                    element instanceof PsiImportList) {

                if (element instanceof PsiImportList) {
                    proxyClass.add(element);
                    System.out.println("添加interface的类导入");
                    continue;
                } else {
                    proxyClass.add(factory.createPackageStatement(((PsiPackageStatement) element).getPackageName().concat(".impl")));
                }



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


                PsiMethod[] methods = clazz.getMethods();
                for (PsiMethod method : methods) {
                    PsiParameterList list = method.getParameterList();
                    for (PsiParameter parameter : list.getParameters()) {
                        StringBuilder sb = new StringBuilder(method.getName());
                        sb.append("params:").append(parameter.getType()).append(":").append(parameter.getName());
                        System.out.println(sb.toString());
                    }
                }
            }

        }

        final PsiClass clazz = proxyClass;
        new WriteCommandAction.Simple(e.getProject(), proxyFile)

        {
            @Override
            public void run() throws Throwable {
                proxyFile.add(clazz);
            }
        }.execute();

    }
}
