/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extras.jsf.myfaces.injection;

import jakarta.faces.context.ExternalContext;
import org.apache.myfaces.spi.AnnotationProvider;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Map;
import java.util.Set;

/**
 * {@link }AnnotationProvider} implementation which provides the Jakarta Faces annotations which we parsed from a
 * Jandex index.
 *
 * @author Stan Silvert
 */
public class MyFacesAnnotationProvider extends AnnotationProvider {

    @Override
    public Map<Class<? extends Annotation>, Set<Class<?>>> getAnnotatedClasses(ExternalContext externalCtx) {
        return AnnotationMap.get(externalCtx);
    }

    @Override
    public Set<URL> getBaseUrls(ExternalContext externalContext) throws IOException {
        return null;
    }

}
