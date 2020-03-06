package com.drcuiyutao.idea.plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;

import static com.drcuiyutao.idea.plugin.util.ClassNameUtil.CLASS_NAME_INSERT;

public class PsiUtil {

    private static final String TAG = "PsiUtil";

    public static PsiClass getPsiClass(PsiType psiType) {
        if (psiType instanceof PsiClassType) {
            return ((PsiClassType) psiType).resolve();
        }
        return null;
    }

    public static PsiClass getPsiClass(PsiElement element, String className) {
        Project project = element.getProject();
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        return javaPsiFacade.findClass(className, GlobalSearchScope.allScope(project));
    }

    public static PsiMethod getPsiMethod(PsiElement element, String className, String method) {
        Project project = element.getProject();
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        PsiClass psiClass = javaPsiFacade.findClass(className, GlobalSearchScope.allScope(project));
        if (null != psiClass) {
            PsiMethod[] methods = psiClass.findMethodsByName(method, true);
            if ((null != methods) && (methods.length > 0)) {
                return methods[0];
            }
        }
        return null;
    }

    public static boolean isAnnotatedByInset(PsiElement psiElement) {
        return isAnnotated(psiElement, CLASS_NAME_INSERT);
    }

    public static boolean isAnnotated(PsiElement psiElement, String annotation) {
//        LogUtil.i(TAG, "isAnnotated psiElement[" + psiElement + "] + annotation[" + annotation + "]");
        if (psiElement instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) psiElement;
            PsiModifierList modifierList = method.getModifierList();
            for (PsiAnnotation psiAnnotation : modifierList.getAnnotations()) {
//                LogUtil.i(TAG, "isAnnotated psiAnnotation[" + psiAnnotation + "]");
                if (psiAnnotation.getQualifiedName().equals(annotation)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isCallMethod(PsiElement psiElement, String cls, String methodName) {
        if (psiElement instanceof PsiCallExpression) {
            PsiCallExpression callExpression = (PsiCallExpression) psiElement;
            PsiMethod method = callExpression.resolveMethod();
            if (method != null) {
                String name = method.getName();
                PsiElement parent = method.getParent();
                if (name != null && name.equals(methodName) && parent instanceof PsiClass) {
                    PsiClass implClass = (PsiClass) parent;
                    if (isClass(implClass, cls) || isSuperClass(implClass, cls)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean compareMethodParams(PsiMethod psiMethod1, PsiMethod psiMethod2) {
//        LogUtil.i(TAG, "compareMethodParams psiMethod1[" + psiMethod1 + "] psiMethod2[" + psiMethod2 + "]");
        if ((null == psiMethod1) || (null == psiMethod2)) {
//            LogUtil.i(TAG, "compareMethodParams null");
            return false;
        }
        PsiParameterList parameterList1 = psiMethod1.getParameterList();
        PsiParameterList parameterList2 = psiMethod2.getParameterList();

        if (parameterList1.getParametersCount() != parameterList2.getParametersCount()) {
//            LogUtil.i(TAG, "compareMethodParams length");
            return false;
        }
        for (int i = 0; i < parameterList1.getParametersCount(); i++) {
//            LogUtil.i(TAG, "compareMethodParams type 1[" + parameterList1.getParameters()[i].getName() + "] 2[" + parameterList2.getParameters()[i].getName() + "]");
            if (!parameterList1.getParameters()[i].getName().equals(parameterList2.getParameters()[i].getName())) {
                return false;
            }
        }
//        LogUtil.i(TAG, "compareMethodParams true");
        return true;
    }

    private static boolean isClass(PsiClass psiClass, String cls) {
        try {
            return psiClass.getName().equals(cls);
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean isSuperClass(PsiClass psiClass, String cls) {
        PsiClass[] supers = psiClass.getSupers();
        if (supers.length == 0) {
            return false;
        }
        for (PsiClass superClass : supers) {
            try {
                if (superClass.getName().equals(cls)) {
                    return true;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
