package com.linsage;


import com.google.common.collect.Sets;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationEnumFieldValue;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;


public class GenerateUrlAction extends AnAction {

    private static NotificationGroup notificationGroup = new NotificationGroup("Java2Json" +
            ".NotificationGroup", NotificationDisplayType.BALLOON, true);


    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = (Editor) e.getDataContext().getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = (PsiFile) e.getDataContext().getData(CommonDataKeys.PSI_FILE);
        Project project = editor.getProject();
        int offset = editor.getCaretModel().getOffset();
        PsiMethod psimethod = PsiTreeUtil.findElementOfClassAtOffset(psiFile, offset,
                PsiMethod.class, false);
        PsiParameterList parameterList = psimethod.getParameterList();
        //
        PsiAnnotation[] annotations = psimethod.getAnnotations();
        String requestMapping = "org.springframework.web.bind.annotation.RequestMapping";
        String getMapping = "org.springframework.web.bind.annotation.GetMapping";
        String path = "";
        String method = "GET";
        for (PsiAnnotation annotation : annotations) {
            if (Objects.equals(annotation.getQualifiedName(), requestMapping)) {
                // check 是不是get方法
                JvmAnnotationAttribute methodAttribute = annotation.findAttribute("method");
                if (methodAttribute != null) {
                    JvmAnnotationAttributeValue attributeValue =
                            methodAttribute.getAttributeValue();
                    if (attributeValue instanceof JvmAnnotationEnumFieldValue) {
                        Object constantValue =
                                ((JvmAnnotationEnumFieldValue) attributeValue).getFieldName();
                        if (Objects.equals(methodAttribute, method)) {
                            //这是GET 方法
                            // 可以直接return
                            break;
                        }
                    }
                }

                // 获取到value的值
                JvmAnnotationAttribute jvmAnnotationAttribute = annotation.findAttribute("value");
                if (jvmAnnotationAttribute != null) {
                    JvmAnnotationAttributeValue attributeValue =
                            jvmAnnotationAttribute.getAttributeValue();
                    if (attributeValue instanceof JvmAnnotationConstantValue) {
                        Object constantValue =
                                ((JvmAnnotationConstantValue) attributeValue).getConstantValue();
                        path += constantValue;
                    }
                }

            } else if (Objects.equals(annotation.getQualifiedName(), getMapping)) {
                //
                // 获取到value的值
                JvmAnnotationAttribute jvmAnnotationAttribute = annotation.findAttribute("value");
                if (jvmAnnotationAttribute != null) {
                    JvmAnnotationAttributeValue attributeValue =
                            jvmAnnotationAttribute.getAttributeValue();
                    if (attributeValue instanceof JvmAnnotationConstantValue) {
                        Object constantValue =
                                ((JvmAnnotationConstantValue) attributeValue).getConstantValue();
                        path += constantValue;
                    }
                }
            }
        }
        // hard code spring的注解
        PsiParameter[] parameters = parameterList.getParameters();
        String param = "?";
        for (int i = 0; i < parameters.length; i++) {
            // int main(String[] args)
            // argType String[]
            // parameter.getName; args
            PsiParameter parameter = parameters[i];
            // 这里是获取类型的文本
            PsiType argType = parameter.getType();

            if (argType instanceof PsiPrimitiveType) {
                param += parameter.getName() + "=" + PsiTypesUtil.getDefaultValue(argType);
            } else {
                param += parameter.getName() + "=" + Java2JsonAction.normalTypes.getOrDefault(argType.getPresentableText()
                        , "null");
            }
            // 需要获取对应参数的注解
        }

        try {
            StringSelection selection = new StringSelection(path + param);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
            String message = "generate " + psimethod.getName() + " to url success, copied to " +
                    "clipboard.";
            Notification success = notificationGroup.createNotification(message,
                    NotificationType.INFORMATION);
            Notifications.Bus.notify(success, project);
        } catch (Exception ex) {
            Notification error = notificationGroup.createNotification("generate to url failed.",
                    NotificationType.ERROR);
            Notifications.Bus.notify(error, project);
        }
    }
}
