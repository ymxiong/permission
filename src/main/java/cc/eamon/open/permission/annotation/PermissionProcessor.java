package cc.eamon.open.permission.annotation;

import cc.eamon.open.permission.LimitInit;
import com.squareup.javapoet.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.reflect.Method;
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
            if (roundEnv.getElementsAnnotatedWith(Permission.class).size() > 0) {
                String packageNameRoot = "cc.eamon.open.permission";
                String packageNameMvc = "cc.eamon.open.permission.mvc";
                String classNameChecker = "DefaultChecker";


                ClassName checker = ClassName.get("cc.eamon.open.permission.mvc", "PermissionChecker");
                ClassName httpServletRequest = ClassName.get("javax.servlet.http", "HttpServletRequest");
                ClassName httpServletResponse = ClassName.get("javax.servlet.http", "HttpServletResponse");

                // 新建操作检查
                Map<String, String> maps = LimitInit.getLimitMap();

                //新建Limit类
                TypeSpec.Builder typeSpecLimit = TypeSpec.interfaceBuilder("Limit").addModifiers(Modifier.PUBLIC);


                for (Map.Entry<String, String> mapEntry: maps.entrySet()){
                    typeSpecLimit.addField(
                            FieldSpec
                                    .builder(String.class, mapEntry.getKey())
                                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                    .initializer("$S", mapEntry.getValue()).build()
                    );
                }

                JavaFile.builder(packageNameRoot, typeSpecLimit.build()).build().writeTo(filer);


                //新建Checker类
                TypeSpec.Builder typeSpecMvc = TypeSpec.classBuilder(classNameChecker)
                        .addSuperinterface(checker)
                        .addModifiers(Modifier.PUBLIC)
                        .addModifiers(Modifier.ABSTRACT);

                //添加handleException函数至DefaultChecker
                MethodSpec.Builder handleExceptionMethod = MethodSpec.methodBuilder("handleException")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(httpServletResponse, "response")
                        .addParameter(Exception.class, "ex")
                        .addAnnotation(Override.class)
                        .returns(Object.class);
                handleExceptionMethod.addStatement("return null");

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

                //添加beforeCheckMethod至DefaultChecker
                MethodSpec.Builder beforeCheckMethod = MethodSpec.methodBuilder("preCheck")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(httpServletRequest, "request")
                        .addParameter(httpServletResponse, "response")
                        .addParameter(Object[].class, "args")
                        .addException(Exception.class)
                        .varargs()
                        .returns(TypeName.BOOLEAN);
                beforeCheckMethod.addStatement("return true");

                //添加afterCheckMethod至DefaultChecker
                MethodSpec.Builder afterCheckMethod = MethodSpec.methodBuilder("postCheck")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(httpServletRequest, "request")
                        .addParameter(httpServletResponse, "response")
                        .addParameter(Object[].class, "args")
                        .addException(Exception.class)
                        .varargs()
                        .returns(TypeName.BOOLEAN);
                afterCheckMethod.addStatement("return true");

                //添加checkClassMethod至DefaultChecker
                MethodSpec.Builder checkClassMethod = MethodSpec.methodBuilder("checkClass")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(httpServletRequest, "request")
                        .addParameter(httpServletResponse, "response")
                        .addParameter(Class.class, "clazz")
                        .addParameter(String.class, "value")
                        .addParameter(Object[].class, "args")
                        .addException(Exception.class)
                        .varargs()
                        .returns(TypeName.BOOLEAN);
                checkClassMethod.addStatement("return true");

                //添加checkMethodMethod至DefaultChecker
                MethodSpec.Builder checkInterfaceMethod = MethodSpec.methodBuilder("checkInterface")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(httpServletRequest, "request")
                        .addParameter(httpServletResponse, "response")
                        .addParameter(Method.class, "method")
                        .addParameter(String.class, "value")
                        .addParameter(Object[].class, "args")
                        .addException(Exception.class)
                        .varargs()
                        .returns(TypeName.BOOLEAN);
                checkInterfaceMethod.addStatement("return true");

                //添加checkMethod至DefaultChecker
                MethodSpec.Builder check = MethodSpec.methodBuilder("check")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(httpServletRequest, "request")
                        .addParameter(httpServletResponse, "response")
                        .addParameter(Object[].class, "args")
                        .addAnnotation(Override.class)
                        .addException(Exception.class)
                        .varargs()
                        .returns(TypeName.BOOLEAN);

                check.beginControlFlow("if (!preCheck(request, response, args))");
                check.addStatement("return false");
                check.endControlFlow();

                check.beginControlFlow("if (!checkClass(request, response, ($T) args[0], ($T)args[1], ($T)args[2]))", Class.class, String.class, Object[].class);
                check.addStatement("return false");
                check.endControlFlow();

                check.beginControlFlow("if (!checkInterface(request, response, ($T) args[3], ($T)args[4], ($T)args[5]))", Method.class, String.class, Object[].class);
                check.addStatement("return false");
                check.endControlFlow();

                check.beginControlFlow("if (!postCheck(request, response, args))");
                check.addStatement("return false");
                check.endControlFlow();

                check.addStatement("return true");


                typeSpecMvc.addMethod(preHandle.build());
                typeSpecMvc.addMethod(postHandle.build());
                typeSpecMvc.addMethod(afterCompletion.build());
                typeSpecMvc.addMethod(handleExceptionMethod.build());
                typeSpecMvc.addMethod(check.build());
                typeSpecMvc.addMethod(beforeCheckMethod.build());
                typeSpecMvc.addMethod(afterCheckMethod.build());
                typeSpecMvc.addMethod(checkClassMethod.build());
                typeSpecMvc.addMethod(checkInterfaceMethod.build());


                JavaFile.builder(packageNameMvc, typeSpecMvc.build()).build().writeTo(filer);


            }


        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return true;
    }


}
