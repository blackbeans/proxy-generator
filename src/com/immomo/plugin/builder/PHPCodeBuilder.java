package com.immomo.plugin.builder;

import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.javadoc.PsiDocComment;
import org.apache.commons.lang.StringUtils;

/**
 * Created with IntelliJ IDEA.
 * User: Xmx
 * Date: 13-11-25
 * Time: 下午4:57
 * To change this template use File | Settings | File Templates.
 */
public class PHPCodeBuilder {


    /**
     * 创建PHP方法
     *
     * @param methodName
     * @param parameterList
     * @return
     */
    public String buildPhpMethod(PsiDocComment doc, String methodName, PsiParameterList parameterList) {


        StringBuilder sb = new StringBuilder();
        sb.append("\n\t");
        if (null != doc && StringUtils.isNotBlank(doc.getText())) {
            sb.append(doc.getText());
        }
        sb.append("\n");
        sb.append("\tfunction ").append(methodName).append("(");


        for (PsiParameter param : parameterList.getParameters()) {
            sb.append("$").append(param.getName()).append(",");
        }

        if (parameterList.getParameters().length > 0) {
            sb.deleteCharAt(sb.lastIndexOf(","));
        }
        sb.append(");\n");

        return sb.toString();

    }
}
