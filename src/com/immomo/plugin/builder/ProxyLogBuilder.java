package com.immomo.plugin.builder;

import com.intellij.psi.*;

/**
 * Created with IntelliJ IDEA.
 * User: Xmx
 * Date: 13-11-21
 * Time: 上午12:52
 * To change this template use File | Settings | File Templates.
 */
public class ProxyLogBuilder {

    public  String genProxyCodeBlock(PsiMethod method) {
        StringBuilder proxyInvoke = new StringBuilder("{").append(method.getReturnType().getCanonicalText())
                .append("\tresponse = null;\n try{\nresponse=this.").append(method.getName()).append("(");

        StringBuilder paramsSb = new StringBuilder();
        /**
         * 遍历一下这个方法的参数
         */
        PsiParameterList list = method.getParameterList();
        for (PsiParameter parameter : list.getParameters()) {
            paramsSb.append(parameter.getName()).append(",");
        }

        if (list.getParameters().length > 0) {
            paramsSb.deleteCharAt(paramsSb.lastIndexOf(","));
            //调用的语句
            proxyInvoke.append(paramsSb).append(");\n");

            //将返回值也加入
            paramsSb.append("response");
        }

        proxyInvoke.append("}catch(Exception e){\n").append("e.printStackTrace();").append("}");
        proxyInvoke.append("finally{\n  LOG.info(\"YYYY|XXXX|{0}|{2}|{3}|{4}").append(paramsSb).append("\");}\nreturn response;}");

        return proxyInvoke.toString();
    }
}
