package com.xebialabs.xlrelease.ci.util;

import java.util.List;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateReleaseView {

    private List<TemplateVariable> variables;
    private String templateId;
    private String title;

    public CreateReleaseView() {
    }

    public CreateReleaseView(final String templateId, final String title, final List<TemplateVariable> variables) {
        this.variables = variables;
        this.templateId = templateId;
        this.title = title;
    }

    public List<TemplateVariable> getVariables() {
        return variables;
    }

    public void setVariables(final List<TemplateVariable> variables) {
        this.variables = variables;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(final String templateId) {
        this.templateId = templateId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }
}
