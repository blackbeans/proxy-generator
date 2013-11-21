package com.immomo.plugin.action;

import com.immomo.plugin.builder.ProxyLogBuilder;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.ui.awt.RelativePoint;
import org.apache.commons.lang.StringUtils;

/**
 * Created with IntelliJ IDEA.
 * User: Xmx
 * Date: 13-11-20
 * Time: 上午10:48
 */
public class ProxyGenAction extends AnAction {

    private final ProxyLogBuilder logBuilder = new ProxyLogBuilder();

    public void actionPerformed(AnActionEvent e) {



        /**
         * 获取当前选择的文件
         */
        final PsiJavaFile interfacePsiFile = (PsiJavaFile) DataKeys.PSI_FILE.getData(e.getDataContext());

        if (null == interfacePsiFile) {
            StatusBar statusBar = WindowManager.getInstance().getStatusBar(DataKeys.PROJECT.getData(e.getDataContext()));
            JBPopupFactory.getInstance().
                    createHtmlTextBalloonBuilder("请选择对应的.Java文件", MessageType.WARNING, null).setFadeoutTime(10 * 1000)
                    .createBalloon()
                    .show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.above);
            return;
        }


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
            if (element instanceof PsiPackageStatement ||
                    element instanceof PsiImportList) {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        if (element instanceof PsiImportList) {
                            PsiElement importStatement = element.copy();
//                            importStatement.add(factory.createImportStatement(proxyClass));
                            proxyFile.add(importStatement);
                        } else {
                            String packageStr = ((PsiPackageStatement) element).getPackageName().concat(".impl");
                            proxyFile.setPackageName(packageStr);
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
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        PsiDocComment classDesc = (PsiDocComment) clazz.getDocComment().copy();
                        proxyFile.add(classDesc);
                    }
                });

                /**
                 * 2.引入import和Package
                 */


                /**
                 * 3.修改继承接口
                 */
                boolean isSkip = true;
                PsiElement[] proxyChild = proxyClass.getChildren();
                for (final PsiElement child : proxyChild) {

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

                PsiField logField = factory.createFieldFromText(
                        logBuilder.genLogField(StringUtils.uncapitalize(proxyClassName)), proxyClass);
                proxyClass.add(logField);
                /**
                 * 对方方法进行proxy包装
                 */

                PsiMethod[] methods = clazz.getMethods();
                for (PsiMethod method : methods) {

                    /**
                     * 生成我们需要的代码
                     */
                    PsiMethod impMethod = (PsiMethod) method.copy();
                    String proxyCode = logBuilder.genProxyCodeBlock(proxyClassName,method);
                    PsiCodeBlock codeBlock = factory.createCodeBlockFromText(proxyCode, impMethod);
                    impMethod.add(codeBlock);

                    //将该方法加入
                    proxyClass.add(impMethod);

                    PsiMethod proxyMethod = (PsiMethod) method.copy();
                    //生成代理方法 抽象的方法
                    proxyMethod.setName(proxyMethod.getName().concat("Proxy"));
                    proxyMethod.getModifierList().add(factory.createKeyword(PsiKeyword.ABSTRACT));
                    proxyClass.add(proxyMethod);

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
