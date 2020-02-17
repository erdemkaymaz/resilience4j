/*
 * Copyright 2020 Robert Winkler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.utils;

import io.github.resilience4j.core.lang.Nullable;
import io.vavr.CheckedFunction0;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class ProceedingJoinPointWrapper {

    private final ProceedingJoinPoint proceedingJoinPoint;
    private final Method method;
    private final String declaringMethodName;
    private final Class<?> targetClass;
    private final Class<?> returnType;
    private CheckedFunction0<Object> proceedFunction;

    public ProceedingJoinPointWrapper(
        ProceedingJoinPoint proceedingJoinPoint) {
        this.proceedingJoinPoint = proceedingJoinPoint;
        Method method = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();
        this.targetClass = AopUtils.getTargetClass(proceedingJoinPoint.getThis());
        this.method = ClassUtils.getMostSpecificMethod(method, targetClass);
        this.declaringMethodName =  targetClass.getName() + "#" + method.getName();
        this.returnType = method.getReturnType();
        this.proceedFunction = proceedingJoinPoint::proceed;
    }

    public ProceedingJoinPointWrapper decorate(
        Function<CheckedFunction0<Object>, CheckedFunction0<Object>> decorator) {
        proceedFunction = decorator.apply(proceedFunction);
        return this;
    }

    public Object proceed() throws Throwable{
        return proceedFunction.apply();
    }

    public ProceedingJoinPoint getProceedingJoinPoint() {
        return proceedingJoinPoint;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public <A extends Annotation> Set<A> findAllRepeatableAnnotations(Class<A> annotationClass) {
        Set<A> repeatableAnnotations = findRepeatableClassAnnotations(annotationClass);
        Set<A> methodAnnotations = findRepeatableMethodAnnotations(annotationClass);
        repeatableAnnotations.addAll(methodAnnotations);
        return repeatableAnnotations;
    }


    public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationClass) {
        A annotation = findMethodAnnotation(annotationClass);
        if(annotation == null){
            annotation = findClassAnnotation(annotationClass);
        }
        return Optional.ofNullable(annotation);
    }

    private <A extends Annotation> Set<A> findRepeatableMethodAnnotations(Class<A> annotationClass) {
        return new HashSet<>(AnnotationUtils.getRepeatableAnnotations(this.method, annotationClass));
    }

    private <A extends Annotation> Set<A> findRepeatableClassAnnotations(Class<A> annotationClass) {
        return new HashSet<>(AnnotationUtils.getRepeatableAnnotations(this.targetClass, annotationClass));
    }

    @Nullable
    private <A extends Annotation> A findMethodAnnotation(Class<A> annotationClass) {
        return AnnotationUtils.findAnnotation(this.method, annotationClass);
    }

    @Nullable
    private <A extends Annotation> A findClassAnnotation(Class<A> annotationClass) {
        return AnnotationUtils.findAnnotation(this.targetClass, annotationClass);
    }

    public String getDeclaringMethodName() {
        return declaringMethodName;
    }

}