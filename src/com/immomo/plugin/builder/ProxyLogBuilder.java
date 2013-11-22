package com.immomo.plugin.builder;

import com.intellij.psi.*;
import org.apache.commons.lang.ArrayUtils;

/**
 * Created with IntelliJ IDEA.
 * User: Xmx
 * Date: 13-11-21
 * Time: 上午12:52
 * To change this template use File | Settings | File Templates.
 */
public class ProxyLogBuilder {

    public String genProxyCodeBlock(String clazzName, PsiMethod method) {

        StringBuilder proxyInvoke = new StringBuilder("{");

        /**
         * 无返回值
         */
        if (method.getReturnType().isAssignableFrom(PsiType.VOID)) {
            proxyInvoke
                    .append("\tThrowable t = null;\n try{\nthis.").append(method.getName()).append("Proxy(");

        } else if (method.getReturnType() instanceof PsiPrimitiveType) {
            proxyInvoke.append(method.getReturnType().getCanonicalText())
                    .append("\tresponse = ");
            if (method.getReturnType() == PsiType.BOOLEAN) {
                proxyInvoke.append("\tfalse;");
            } else {
                /**
                 * 数字
                 */
                proxyInvoke.append("0;");
            }
            proxyInvoke.append("Throwable t = null;try{\nresponse=this.").append(method.getName()).append("Proxy(");
        } else {
            proxyInvoke.append(method.getReturnType().getCanonicalText())
                    .append("\tresponse = null;Throwable t = null;\n try{\nresponse=this.").append(method.getName()).append("Proxy(");
        }

        StringBuilder paramsSb = new StringBuilder();

        StringBuilder paramsLogSB = new StringBuilder();

        /**
         * 遍历一下这个方法的参数
         */
        PsiParameterList list = method.getParameterList();
        int paramIdx = 0;
        for (PsiParameter parameter : list.getParameters()) {
            paramsSb.append(parameter.getName()).append(",");
            paramsLogSB.append(parameter.getName()).append(":{").append(paramIdx).append("}|");
            paramIdx++;
        }

        if (list.getParameters().length > 0) {
            paramsSb.deleteCharAt(paramsSb.lastIndexOf(","));
            //调用的语句
            proxyInvoke.append(paramsSb).append(");\n");

            /**
             * 有返回值
             */
            if (!method.getReturnType().isAssignableFrom(PsiType.VOID)) {
                //将返回值也加入
                paramsSb.append(",response");
                paramsLogSB.append("{").append(paramIdx++).append("}");
            }
            paramsSb.insert(0,",");
        } else {
            proxyInvoke.append(");\n");
            //没有参数有返回值
            if (!method.getReturnType().isAssignableFrom(PsiType.VOID)) {
                paramsLogSB.append("{").append(paramIdx++).append("}");
                //将返回值也加入
                paramsSb.append("response");
                paramsSb.insert(0,",");
            } else {
                 //参数为空，返回也为空
            }
        }



        /**
         * catch
         */

        proxyInvoke.append("}catch(Exception e){\n")
                .append("t=e;\n");


        proxyInvoke.append("LogUtils.error(LOG,e,\"").append(clazzName).append("|").append(method.getName()).append("|")
                .append(paramsLogSB.toString()).append("\"").append(paramsSb).append(");");

        if (!ArrayUtils.isEmpty(method.getThrowsList().getChildren())) {
            proxyInvoke.append("throw e;\n");
        }
        /**
         * finally
         */
        proxyInvoke.append("}finally{ if(null ==t || RANDOM.nextInt(1000) == 0 ){ ")
                .append("LogUtils.info(LOG,\"").append(clazzName).append("|").append(method.getName()).append("|")
                .append(paramsLogSB.toString()).append("\"").append(paramsSb).append(");}}\n");

        if (!method.getReturnType().isAssignableFrom(PsiType.VOID)) {
            /**
             * return
             */
            proxyInvoke.append("return response;");
        }

        proxyInvoke.append("}");
        return proxyInvoke.toString();
    }


    public String genLogField(String logName) {
        return "private static final Logger LOG =" +
                "LogWrapper.getLogger(\"" + logName + "\");";
    }


}
