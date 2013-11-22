package com.immomo.plugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.popup.JBPopupFactory;

/**
 * User: Xmx
 * Date: 13-11-22
 * Time: 下午4:37
 */
public class PhpCodeGenAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {

        JBPopupFactory.getInstance().createMessage("正在努力开发中....").showCenteredInCurrentWindow(e.getProject());

    }
}
