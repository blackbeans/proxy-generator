package com.immomo.plugin.action;

import com.immomo.plugin.builder.PHPCodeBuilder;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.ui.awt.RelativePoint;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * User: Xmx
 * Date: 13-11-22
 * Time: 下午4:37
 */
public class PhpCodeGenAction extends AnAction {

    private final PHPCodeBuilder phpCodeBuilder = new PHPCodeBuilder();

    @Override
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


        /**
         * 判断当前是否为接口文件
         */
        final String proxyClassName = "moa_" + ((PsiClass) interfaceClass).getName().toLowerCase();

        final PsiFile proxyFile = ApplicationManager.getApplication().runWriteAction(new Computable<PsiFile>() {
            @Override
            public PsiFile compute() {
                String fileName = proxyClassName.concat(".php");
                if (null != directory.findFile(fileName)) {
                    directory.findFile(fileName).delete();
                }
                return directory.createFile(fileName);
            }
        });

        final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(e.getProject());

        final Document doc = documentManager.getDocument(proxyFile);


        final StringBuilder sb = new StringBuilder("<?php\n ");
        if (null != ((PsiClass) interfaceClass).getDocComment()) {
            StringReader reader = new StringReader(((PsiClass) interfaceClass).getDocComment().getText());
            BufferedReader br = new BufferedReader(reader);
            String line = null;
            try {
                while (StringUtils.isNotBlank(line = br.readLine())) {
                    sb.append("\t").append(line).append("\n");
                }
            } catch (IOException e1) {
                //INGORE
                e1.printStackTrace();
            } finally {
                if (null != br) {
                    try {
                        br.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        sb.append("\tinterface ");
        sb.append(((PsiClass) interfaceClass).getName()).append("{\n");

        final PsiClass phpClass = factory.createInterface(((PsiClass) interfaceClass).getName());


        final PsiElement[] interfaceChild = interfacePsiFile.getChildren();

        //php的类名
        for (final PsiElement element : interfaceChild) {
            if (element instanceof PsiDocComment) {
                sb.append(element.getText());

            } else if (element instanceof PsiClass) {

                PsiClass demoClass = ((PsiClass) element);


                /**
                 * 获取java中的所有方法
                 */
                for (PsiMethod method : demoClass.getMethods()) {


                    String methodName = method.getName();

                    PsiParameterList parameterList = method.getParameterList();

                    String methodCode = this.phpCodeBuilder.buildPhpMethod(method.getDocComment(), methodName, parameterList);
                    sb.append(methodCode);

                }

            }
        }

        sb.append("}\n?>");


        new WriteCommandAction.Simple(e.getProject(), proxyFile) {
            @Override
            protected void run() throws Throwable {
                doc.setText(sb.toString());
                documentManager.commitDocument(doc);
            }
        }.execute();

    }
}
