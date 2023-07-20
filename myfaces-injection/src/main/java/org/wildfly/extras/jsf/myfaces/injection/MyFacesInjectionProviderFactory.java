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
import org.apache.myfaces.spi.InjectionProvider;
import org.apache.myfaces.spi.InjectionProviderFactory;

/**
 * {@link InjectionProviderFactory} implementation which provides MyFaces 4.x support
 * @author Dmitrii Tikhomirov dtikhomi@redhat.com
 */
public class MyFacesInjectionProviderFactory extends InjectionProviderFactory {

    private InjectionProvider injectionProvider;

    public MyFacesInjectionProviderFactory(){
        injectionProvider = new MyFacesInjectionProvider();
    }

    @Override
    public InjectionProvider getInjectionProvider(ExternalContext externalContext) {
        return injectionProvider;
    }

    @Override
    public void release() {
        MyFacesInjectionProvider toClose = (MyFacesInjectionProvider) injectionProvider;
        injectionProvider = null;
        toClose.close();
    }

}
