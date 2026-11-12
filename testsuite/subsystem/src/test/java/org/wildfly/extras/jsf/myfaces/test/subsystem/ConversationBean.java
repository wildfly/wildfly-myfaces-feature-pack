/*
 * Copyright 2026 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extras.jsf.myfaces.test.subsystem;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Conversation;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.faces.application.Application;
import jakarta.faces.application.ApplicationWrapper;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.event.ToggleEvent;

/**
 * Holds data loaded only on the initial request so conversation loss is visible on postback.
 */
@Named
@ConversationScoped
public class ConversationBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Inject
    private Conversation conversation;

    private final String token = UUID.randomUUID().toString();
    private List<Integer> rows = List.of();
    private int submissions;
    private int toggles;

    /**
     * Starts a conversation before the initial form is rendered, matching the reported failure.
     */
    @PostConstruct
    public void beginConversation() {
        conversation.begin();
    }

    /**
     * Loads rows through a view action that deliberately does not run on postbacks.
     */
    public void load() {
        rows = List.of(0, 1);
    }

    /**
     * Exposes an instance-specific token so HTTP tests can detect replacement backing beans.
     */
    public String getToken() {
        return token;
    }

    /**
     * Supplies the initial request's data for PrimeFaces row decoding on postback.
     */
    public List<Integer> getRows() {
        return rows;
    }

    /**
     * Supplies a non-List Iterable so UIRepeat must resolve its ResultSet type check.
     */
    public Iterable<Integer> getRepeatRows() {
        return rows::iterator;
    }

    /**
     * Records a full form submission so the test verifies action execution as well as rendering.
     */
    public void submit() {
        submissions++;
    }

    /**
     * Exposes the number of full postbacks handled by this conversation's bean.
     */
    public int getSubmissions() {
        return submissions;
    }

    /**
     * Records the decoded row-toggle event so an expanded response must reach the listener.
     */
    public void onRowToggle(ToggleEvent event) {
        toggles++;
    }

    /**
     * Exposes row-toggle listener execution in the partial response.
     */
    public int getToggles() {
        return toggles;
    }

    /**
     * Identifies the deployed Faces implementation after unwrapping integration decorators.
     */
    public String getImplementation() {
        Application application = FacesContext.getCurrentInstance().getApplication();
        while (application instanceof ApplicationWrapper wrapper) {
            application = wrapper.getWrapped();
        }
        return application.getClass().getName();
    }

    /**
     * Requires Faces annotation scanning without a CDI bean-defining annotation.
     */
    @FacesConverter("annotationConverter")
    public static class AnnotationConverter implements Converter<String> {

        /**
         * Preserves submitted text because the fixture only needs to observe rendering conversion.
         */
        @Override
        public String getAsObject(FacesContext context, UIComponent component, String value) {
            return value;
        }

        /**
         * Emits a distinct marker proving that the annotated converter was discovered and invoked.
         */
        @Override
        public String getAsString(FacesContext context, UIComponent component, String value) {
            return "ANNOTATED-" + value;
        }
    }
}
