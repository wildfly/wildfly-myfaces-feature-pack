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

import org.apache.myfaces.spi.InjectionProvider;
import org.apache.myfaces.spi.InjectionProviderException;
import org.jboss.as.web.common.StartupContext;
import org.jboss.as.web.common.WebInjectionContainer;

/**
 * {@link InjectionProvider} implementation which provides MyFaces 4.x support
 *
 * @author Dmitrii Tikhomirov dtikhomi@redhat.com
 */
public class MyFacesInjectionProvider extends InjectionProvider implements AutoCloseable {

    private WebInjectionContainer injectionContainer;

    public MyFacesInjectionProvider() {
        this.injectionContainer = StartupContext.getInjectionContainer();
    }

    @Override
    public Object inject(Object instance) throws InjectionProviderException {
        return null;
    }

    @Override
    public void postConstruct(Object instance, Object creationMetaData) throws InjectionProviderException {
        if (injectionContainer != null) {
            try {
                injectionContainer.newInstance(instance);
            } catch (Exception e) {
                throw new InjectionProviderException(e);
            }
        }
    }

    @Override
    public void preDestroy(Object instance, Object creationMetaData) throws InjectionProviderException {
        if (injectionContainer != null) {
            injectionContainer.destroyInstance(instance);
        }
    }

    @Override
    public boolean isAvailable() {
        return this.injectionContainer != null;
    }

    @Override
    public void close() {
        StartupContext.removeInjectionContainer();
        this.injectionContainer = null;
    }

}
