package com.drcuiyutao.idea.plugin.insert;

import com.drcuiyutao.idea.plugin.util.LogUtil;
import com.drcuiyutao.idea.plugin.util.PsiUtil;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import com.drcuiyutao.idea.plugin.util.IconUtil;
import com.drcuiyutao.idea.plugin.util.Util;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.drcuiyutao.idea.plugin.util.ClassNameUtil.CLASS_NAME_INSERT;

class InsertNavTable {
    private static final String TAG = "InsertNavTable";
    private static CopyOnWriteArrayList<InsertNavInfo> navInfos = new CopyOnWriteArrayList<>();
    private static PsiClass insertClass = null;

    static void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo> result) {
        updateByScan(element);
        if ((element instanceof PsiMethod) || (element instanceof PsiField)) {
            Icon icon = IconUtil.InsertIcon;
            ArrayList<PsiNameIdentifierOwner> targets = new ArrayList<>();
            for (InsertNavInfo navInfo : navInfos) {
                if ((null != navInfo.element) && (navInfo.element.isEquivalentTo(element))) {
                    if (null == navInfo.target) {
                        icon = AllIcons.General.Error;
                        targets.add(navInfo.element);
                    } else {
                        targets.add(navInfo.target);
                    }
                } else if ((null != navInfo.target) && (navInfo.target.isEquivalentTo(element))) {
                    targets.add(navInfo.element);
                }
            }

            if (targets.size() > 0) {
                LogUtil.i(TAG, "collectNavigationMarkers element[" + element + "] targets[" + targets + "]");
                try {
                    result.add(NavigationGutterIconBuilder.create(icon)
                            .setPopupTitle("Insert")
                            .setTargets(targets)
                            .setTooltipText("Navigate to Insert or invocation")
                            .createLineMarkerInfo(element));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
//            printNavTable();
        }
    }

    private static PsiClass getInsertClass(PsiElement element) {
        if (null == insertClass) {
            insertClass = PsiUtil.getPsiClass(element, CLASS_NAME_INSERT);
        }
        return insertClass;
    }

    private static void updateByScan(PsiElement element) {
//        LogUtil.i(TAG, "updateByScan element[" + element + "]");
        try {
            if (element instanceof PsiFile) {
                PsiClass targetClass = getInsertClass(element);
//            LogUtil.i(TAG, "updateByScan PsiFile targetClass[" + targetClass + "]");
                if (null != targetClass) {
                    navInfos.clear();
                    AnnotatedElementsSearch.searchElements(targetClass, ProjectScope.getAllScope(element.getProject()), PsiMethod.class, PsiField.class).forEach(
                            (Processor<PsiNameIdentifierOwner>) psiNameIdentifierOwner -> {
                                addOrUpdateElement(psiNameIdentifierOwner);
                                return true;
                            }
                    );
                }
            } else if (element instanceof PsiAnnotation) {
                PsiAnnotation psiAnnotation = (PsiAnnotation) element;
                if (Objects.equals(psiAnnotation.getQualifiedName(), CLASS_NAME_INSERT)) {
//                LogUtil.i(TAG, "updateByScan PsiAnnotation getParent[" + psiAnnotation.getParent() + "] grand[" + psiAnnotation.getParent().getParent() + "]");
                    addOrUpdateElement(PsiTreeUtil.getParentOfType(psiAnnotation, PsiMethod.class, PsiField.class));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void addOrUpdateElement(PsiNameIdentifierOwner psiNameIdentifierOwner) {
        boolean finded = false;
        for (InsertNavInfo info : navInfos) {
            if ((null != info) && (null != info.element) && info.element.isEquivalentTo(psiNameIdentifierOwner)) {
                info.target = getTarget(psiNameIdentifierOwner);
                finded = true;
                break;
            }
        }
        if (!finded) {
            InsertNavInfo info = new InsertNavInfo();
            info.element = psiNameIdentifierOwner;
            info.target = getTarget(psiNameIdentifierOwner);
//            LogUtil.i(TAG, "addOrUpdateElement psiNameIdentifierOwner[" + psiNameIdentifierOwner + "] info[" + info + "]");
            navInfos.add(info);
        }
    }

    private static PsiNameIdentifierOwner getTarget(PsiNameIdentifierOwner psiElement) {
        String annotationClass = null;
        String annotationName = null;
        if (psiElement instanceof PsiModifierListOwner) {
            PsiAnnotation psiAnnotation = ((PsiModifierListOwner) psiElement).getAnnotation(CLASS_NAME_INSERT);
//                LogUtil.i(TAG, "addOrUpdateElement getQualifiedName[" + psiAnnotation.getQualifiedName() + "]");
            if (null != psiAnnotation) {
                PsiAnnotationMemberValue targetAttribute = psiAnnotation.findAttributeValue("target");
                if (targetAttribute instanceof PsiClassObjectAccessExpression) {
                    PsiClassObjectAccessExpression classObjectAccessExpression = (PsiClassObjectAccessExpression) targetAttribute;
                    annotationClass = classObjectAccessExpression.getOperand().getType().getCanonicalText();
                }
                PsiAnnotationMemberValue nameAttribute = psiAnnotation.findAttributeValue("name");
                if ((nameAttribute instanceof PsiLiteralExpression)
                        && (null != ((PsiLiteralExpression) nameAttribute).getValue())
                        && (!Util.stringIsEmpty(((PsiLiteralExpression) nameAttribute).getValue().toString()))) {
                    annotationName = ((PsiLiteralExpression) nameAttribute).getValue().toString();
                }
//                    LogUtil.i(TAG, "addOrUpdateElement targetAttribute[" + targetAttribute + "] nameAttribute[" + nameAttribute + "]");
            }
        }
//        LogUtil.i(TAG, "addOrUpdateElement annotationClass[" + annotationClass + "] annotationName[" + annotationName + "]");
        if (!Util.stringIsEmpty(annotationClass)) {
            if (Util.stringIsEmpty(annotationName)) {
                annotationName = psiElement.getName();
            }
            Project project = psiElement.getProject();
            JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
            PsiClass targetClass = javaPsiFacade.findClass(annotationClass, GlobalSearchScope.allScope(project));
            if (psiElement instanceof PsiMethod) {
                PsiMethod method = (PsiMethod) psiElement;
                PsiMethod targetMethod = null;
                if (targetClass != null) {
                    if (targetClass.isInterface()) {
                        targetMethod = method;
                    } else {
                        PsiMethod[] methodList = targetClass.findMethodsByName(annotationName, false);
//                        LogUtil.i(TAG, "addOrUpdateElement methodList[" + methodList + "]");
                        for (PsiMethod psiMethod : methodList) {
                            if (PsiUtil.compareMethodParams(psiMethod, method)) {
                                targetMethod = psiMethod;
                                break;
                            }
                        }
                    }
                }
//                LogUtil.i(TAG, "addOrUpdateElement targetClass[" + targetClass + "] targetMethod[" + targetMethod + "]");
                return targetMethod;
            } else if (psiElement instanceof PsiField) {
                PsiField targetField = null;
                if (targetClass != null) {
                    targetField = targetClass.findFieldByName(annotationName, false);
                }
//                LogUtil.i(TAG, "addOrUpdateElement targetClass[" + targetClass + "] targetField[" + targetField + "]");
                return targetField;
            }
        }
        return null;
    }

    private static void printNavTable() {
        for (InsertNavInfo navInfo : navInfos) {
            LogUtil.i(TAG, "printNavTable element[" + navInfo.element + "]");
        }
    }

    static class InsertNavInfo {
        PsiNameIdentifierOwner element;
        PsiNameIdentifierOwner target;

        @Override
        public String toString() {
            return "[\n     InsertNavInfo" +
                    "\n         element[" + element + "]" +
                    "\n         target[" + target + "]]";
        }
    }
}
