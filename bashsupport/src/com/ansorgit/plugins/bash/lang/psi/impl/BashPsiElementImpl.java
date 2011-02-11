/*
 * Copyright 2010 Joachim Ansorg, mail@ansorg-it.com
 * File: BashPsiElementImpl.java, Class: BashPsiElementImpl
 * Last modified: 2010-07-08
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ansorgit.plugins.bash.lang.psi.impl;

import com.ansorgit.plugins.bash.file.BashFileType;
import com.ansorgit.plugins.bash.lang.psi.FileInclusionManager;
import com.ansorgit.plugins.bash.lang.psi.api.BashFile;
import com.ansorgit.plugins.bash.lang.psi.api.BashPsiElement;
import com.ansorgit.plugins.bash.lang.psi.util.BashPsiUtils;
import com.ansorgit.plugins.bash.util.BashFunctions;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

/**
 * Date: 11.04.2009
 * Time: 23:29:38
 *
 * @author Joachim Ansorg
 */
public abstract class BashPsiElementImpl extends ASTWrapperPsiElement implements BashPsiElement {
    private final String name;

    public BashPsiElementImpl(final ASTNode astNode) {
        this(astNode, null);
    }

    public BashPsiElementImpl(final ASTNode astNode, final String name) {
        super(astNode);
        this.name = name;
    }

    @NotNull
    @Override
    public Language getLanguage() {
        return BashFileType.BASH_LANGUAGE;
    }

    @Override
    public String toString() {
        return "[PSI] " + (name == null ? super.toString() : name);
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        //all files which include this element's file belong to the requested scope
        //fixme quite slow, fix with reverse index included file->including file

        Set<PsiFile> includingFiles = FileInclusionManager.findIncludingFiles(getProject(), getContainingFile());
        if (includingFiles.isEmpty()) {
            //we should return a local search scope if we only have local references
            //not return a local scope then inline renaming is not possible
            return new LocalSearchScope(getContainingFile());
        }

        Collection<VirtualFile> virtualFiles = Collections2.transform(includingFiles, BashFunctions.psiToVirtualFile());
        return GlobalSearchScope.fileScope(getContainingFile()).union(GlobalSearchScope.filesScope(getProject(), virtualFiles));
    }

    @NotNull
    @Override
    public GlobalSearchScope getResolveScope() {
        BashFile psiFile = (BashFile) getContainingFile();

        GlobalSearchScope currentFileScope = GlobalSearchScope.fileScope(getContainingFile());

        Set<BashFile> includedFiles = psiFile.findIncludedFiles(true);
        Collection<VirtualFile> files = Collections2.transform(includedFiles, new Function<PsiFile, VirtualFile>() {
            public VirtualFile apply(PsiFile psiFile) {
                return psiFile.getVirtualFile();
            }
        });

        return currentFileScope.uniteWith(GlobalSearchScope.filesScope(getProject(), files));
    }

    @Override
    public boolean processDeclarations(@NotNull PsiScopeProcessor processor, @NotNull ResolveState state, PsiElement lastParent, @NotNull PsiElement place) {
        return BashPsiUtils.processChildDeclarations(this, processor, state, lastParent, place);
    }
}
