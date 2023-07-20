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

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.apache.myfaces.webapp.StartupServletContextListener;

import java.util.Set;

/**
 * @author Dmitrii Tikhomirov <chani@me.com>
 * Created by treblereel on 8/13/18.
 */
public class MyFacesContainerInitializer extends org.apache.myfaces.webapp.MyFacesContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        super.onStartup(c, ctx);
        ctx.addListener(new StartupServletContextListener());
    }
}
