package cc.eamon.open.permission.annotation;

import cc.eamon.open.permission.PermissionInit;
import com.squareup.javapoet.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.element.Element;
import javax.lang.model.util.Types;
import java.util.*;

/**
 * Created by Eamon on 2018/9/30.
 */
@SupportedAnnotationTypes(
        {
                "cc.eamon.open.permission.annotation.Permission"
        }
)
public class PermissionProcessor extends AbstractProcessor {

    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;

    public PermissionProcessor() {
    }


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Permission.class.getCanonicalName());
        return annotations;
    }


    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            HashMap<String, FieldSpec> fieldHashMap = new HashMap<>();
            HashMap<String, String> methodNameHashMap = new HashMap<>();
            ClassName string = ClassName.get("java.lang", "String");

            for (Element elem : roundEnv.getElementsAnnotatedWith(Permission.class)) {
                if (elem.getKind() != ElementKind.CLASS) {
                    return true;
                }

                Permission type = elem.getAnnotation(Permission.class);
                String typeName = type.value();

                for (Element elemMethod : elem.getEnclosedElements()) {
                    if (elemMethod.getKind() == ElementKind.METHOD) {
                        PermissionLimit limit = elemMethod.getAnnotation(PermissionLimit.class);
                        String methodName;
                        String methodDetail;

                        if ((limit != null) && !limit.name().equals("")) {
                            methodName = typeName.toUpperCase() + "_" + limit.name().toUpperCase();
                            methodDetail = typeName.toLowerCase() + "_" + limit.name();
                        } else if ((limit != null) && limit.name().equals("")) {
                            methodName = typeName.toUpperCase() + "_" + elemMethod.getSimpleName().toString().toUpperCase();
                            methodDetail = typeName.toLowerCase() + "_" + elemMethod.getSimpleName().toString();
                        } else {
                            continue;
                        }

                        FieldSpec.Builder fieldBuilder = FieldSpec.builder(
                                string,
                                methodName,
                                Modifier.PUBLIC,
                                Modifier.STATIC,
                                Modifier.FINAL)
                                .initializer("$S", methodDetail);

                        if (fieldHashMap.get(methodName) != null) {
                            throw new ProcessingException(elem, "PermissionInit identifier can not be same @%s", Permission.class.getSimpleName());
                        }
                        methodNameHashMap.put(methodName, methodDetail);
                        fieldHashMap.put(methodName, fieldBuilder.build());
                    }
                }
            }

            if (fieldHashMap.size() > 0) {
                String packageNameRoot = "cc.eamon.open.permission";
                String packageNameMvc = "cc.eamon.open.permission.mvc";

                String classNameValue = "PermissionValue";
                String classNameRole = "PermissionRole";
                String classNameChecker = "DefaultChecker";

                // 新建角色常量类
                TypeSpec.Builder typeSpecRole = TypeSpec.classBuilder(classNameRole)
                        .addModifiers(Modifier.PUBLIC);

                for (String key : PermissionInit.getNameToPermissionMap().keySet()) {
                    FieldSpec.Builder fieldBuilder = FieldSpec.builder(
                            string,
                            key,
                            Modifier.PUBLIC,
                            Modifier.STATIC,
                            Modifier.FINAL)
                            .initializer("$S", key.toLowerCase());

                    typeSpecRole.addField(fieldBuilder.build());
                }
                //写入角色常量类
                JavaFile.builder(packageNameRoot, typeSpecRole.build()).build().writeTo(filer);


                ClassName checker = ClassName.get("cc.eamon.open.permission.mvc", "PermissionChecker");
                ClassName httpServletRequest = ClassName.get("javax.servlet.http", "HttpServletRequest");
                ClassName httpServletResponse = ClassName.get("javax.servlet.http", "HttpServletResponse");
                ClassName permissionValue = ClassName.get("cc.eamon.open.permission", "PermissionValue");

                // 新建Value类
                TypeSpec.Builder typeSpecValue = TypeSpec.classBuilder(classNameValue)
                        .addModifiers(Modifier.PUBLIC);

                HashMap<String, String> methodNames = new HashMap<>();

                for (String key : fieldHashMap.keySet()) {
                    System.out.println("Key: " + key);
                    //添加Value类的域
                    typeSpecValue.addField(fieldHashMap.get(key));

                    //添加DefaultChecker的方法
                    String mNameSplits[] = methodNameHashMap.get(key).split("_");
                    StringBuilder methodNameBuilder = new StringBuilder("check");
                    //生成方法名
                    for (String mNameSplit : mNameSplits) {
                        methodNameBuilder.append(mNameSplit.substring(0, 1).toUpperCase()).append(mNameSplit.substring(1));
                    }
                    String methodName = methodNameBuilder.toString();
                    methodNames.put(key, methodName);
                }
                //写Value类
                JavaFile.builder(packageNameRoot, typeSpecValue.build()).build().writeTo(filer);

                //新建Checker类
                TypeSpec.Builder typeSpecMvc = TypeSpec.classBuilder(classNameChecker)
                        .addSuperinterface(checker)
                        .addModifiers(Modifier.PUBLIC)
                        .addModifiers(Modifier.ABSTRACT);

                //添加handleMethod函数至DefaultChecker
                MethodSpec.Builder handleException = MethodSpec.methodBuilder("handleException")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(httpServletResponse, "response")
                        .addParameter(Exception.class, "ex")
                        .returns(Object.class);
                handleException.addStatement("return null");


                //添加checkRole函数至DefaultChecker
                MethodSpec.Builder checkRole = MethodSpec.methodBuilder("checkRole")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(httpServletRequest, "request")
                        .addParameter(httpServletResponse, "response")
                        .addParameter(string, "roleLimit")
                        .addException(Exception.class)
                        .returns(TypeName.BOOLEAN);

                checkRole.addStatement("if(roleLimit.equals(\"\")) return true");
                checkRole.addStatement("String[] roles = checkRequestRoles(request, response)");
                checkRole.addStatement("for(String role:roles) if ($T.checkRolePermission(roleLimit, role)) return true", PermissionInit.class);
                checkRole.addStatement("return false");

                //添加requestRole函数至DefaultChecker
                MethodSpec.Builder checkRequestRole = MethodSpec.methodBuilder("checkRequestRoles")
                        .addModifiers(Modifier.PUBLIC)
//                        .addModifiers(Modifier.ABSTRACT)
                        .addParameter(httpServletRequest, "request")
                        .addParameter(httpServletResponse, "response")
                        .addException(Exception.class)
                        .returns(String[].class);
                checkRequestRole.addStatement("return new String[0]");


                //添加checkMethod函数至DefaultChecker
                MethodSpec.Builder checkMethod = MethodSpec.methodBuilder("checkMethod")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(httpServletRequest, "request")
                        .addParameter(httpServletResponse, "response")
                        .addParameter(string, "methodName")
                        .addException(Exception.class)
                        .returns(TypeName.BOOLEAN);


                //添加beforeCheckMethod至DefaultChecker
                MethodSpec.Builder beforeCheckMethod = MethodSpec.methodBuilder("preCheck")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(httpServletRequest, "request")
                        .addParameter(httpServletResponse, "response")
                        .addParameter(string, "methodName")
                        .addParameter(string, "roleLimit")
                        .addException(Exception.class)
                        .returns(TypeName.BOOLEAN);
                beforeCheckMethod.addStatement("return true");

                //添加afterCheckMethod至DefaultChecker
                MethodSpec.Builder afterCheckMethod = MethodSpec.methodBuilder("postCheck")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(httpServletRequest, "request")
                        .addParameter(httpServletResponse, "response")
                        .addParameter(string, "methodName")
                        .addParameter(string, "roleLimit")
                        .addException(Exception.class)
                        .returns(TypeName.BOOLEAN);
                afterCheckMethod.addStatement("return true");

                //添加preHandle至DefaultChecker
                MethodSpec.Builder preHandle = MethodSpec.methodBuilder("preHandle")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(httpServletRequest, "request")
                        .addParameter(httpServletResponse, "response")
                        .addParameter(Object.class, "handler")
                        .addAnnotation(Override.class)
                        .addException(Exception.class)
                        .returns(TypeName.BOOLEAN);
                preHandle.addStatement("return true");

                //添加postHandle至DefaultChecker
                MethodSpec.Builder postHandle = MethodSpec.methodBuilder("postHandle")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(httpServletRequest, "request")
                        .addParameter(httpServletResponse, "response")
                        .addParameter(Object.class, "handler")
                        .addParameter(ModelAndView.class, "modelAndView")
                        .addAnnotation(Override.class)
                        .addException(Exception.class)
                        .returns(TypeName.VOID);

                //添加afterCompletion至DefaultChecker
                MethodSpec.Builder afterCompletion = MethodSpec.methodBuilder("afterCompletion")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(httpServletRequest, "request")
                        .addParameter(httpServletResponse, "response")
                        .addParameter(Object.class, "handler")
                        .addParameter(Exception.class, "ex")
                        .addAnnotation(Override.class)
                        .addException(Exception.class)
                        .returns(TypeName.VOID);

                //添加checkMethod至DefaultChecker
                MethodSpec.Builder check = MethodSpec.methodBuilder("check")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(httpServletRequest, "request")
                        .addParameter(httpServletResponse, "response")
                        .addParameter(string, "methodName")
                        .addParameter(string, "roleLimit")
                        .addAnnotation(Override.class)
                        .addException(Exception.class)
                        .returns(TypeName.BOOLEAN);

                check.beginControlFlow("if (!preCheck(request, response, methodName, roleLimit))");
                check.addStatement("return false");
                check.endControlFlow();

                check.beginControlFlow("if (!checkRole(request, response, roleLimit))");
                check.addStatement("return false");
                check.endControlFlow();

                check.beginControlFlow("if (!checkMethod(request, response, methodName))");
                check.addStatement("return false");
                check.endControlFlow();

                checkMethod.beginControlFlow("switch (methodName)");
                //添加具体方法
                for (String key : methodNames.keySet()) {
                    //添加方法
                    MethodSpec.Builder checkMethodDetail = MethodSpec.methodBuilder(methodNames.get(key))
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(httpServletRequest, "request")
                            .addParameter(httpServletResponse, "response")
                            .addParameter(string, "method")
                            .addException(Exception.class)
                            .returns(TypeName.BOOLEAN);
                    checkMethodDetail.addStatement("return true");
                    typeSpecMvc.addMethod(checkMethodDetail.build());

                    checkMethod.addStatement("case $T." + key + ": return " + methodNames.get(key) + "(request, response, $T."+ key +")", permissionValue, permissionValue);
                }
                checkMethod.addStatement("default: break");
                checkMethod.endControlFlow();
                checkMethod.addStatement("return false");

                check.beginControlFlow("if (!postCheck(request, response, methodName, roleLimit))");
                check.addStatement("return false");
                check.endControlFlow();
                check.addStatement("return true");

                typeSpecMvc.addMethod(handleException.build());
                typeSpecMvc.addMethod(checkRequestRole.build());
                typeSpecMvc.addMethod(checkRole.build());
                typeSpecMvc.addMethod(checkMethod.build());
                typeSpecMvc.addMethod(preHandle.build());
                typeSpecMvc.addMethod(postHandle.build());
                typeSpecMvc.addMethod(afterCompletion.build());
                typeSpecMvc.addMethod(beforeCheckMethod.build());
                typeSpecMvc.addMethod(afterCheckMethod.build());
                typeSpecMvc.addMethod(check.build());
                JavaFile.builder(packageNameMvc, typeSpecMvc.build()).build().writeTo(filer);
            }

        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return true;
    }


}
