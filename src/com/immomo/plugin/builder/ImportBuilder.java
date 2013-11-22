package com.immomo.plugin.builder;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatement;

/**
 * User: Xmx
 * Date: 13-11-22
 * Time: 下午4:11
 */
public class ImportBuilder {

    /**
     * @param factory
     * @param importList
     */
    public void fillExtraImport(PsiElementFactory factory, PsiImportList importList) {

        importList.add(this.umarshal(factory, "org.apache.log4j.Logger"));
        importList.add(this.umarshal(factory, "com.immomo.mcf.util.LogUtils"));
        importList.add(this.umarshal(factory, "com.immomo.mcf.util.LogWrapper"));
        importList.add(this.umarshal(factory, "java.util.Random"));
    }


    private PsiImportStatement umarshal(PsiElementFactory factory, String extraImport) {
        PsiImportStatement importExtra = factory.createImportStatementOnDemand(extraImport);
        PsiElement[] children = importExtra.getChildren();
        importExtra.deleteChildRange(children[children.length - 3], children[children.length - 2]);
        return importExtra;
    }
}
