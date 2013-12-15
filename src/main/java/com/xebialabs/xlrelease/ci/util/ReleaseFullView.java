package com.xebialabs.xlrelease.ci.util;


import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReleaseFullView {
    private String id;
    private String title;
    private List<TemplateVariable> variables;

    public ReleaseFullView() {
    }

    public ReleaseFullView(final String id, final String title, final List<TemplateVariable> variables) {
        this.id = id;
        this.title = title;
        this.variables = variables;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public List<TemplateVariable> getVariables() {
        return variables;
    }

    public void setVariables(final List<TemplateVariable> variables) {
        this.variables = variables;
    }

    @Override
    public String toString() {
        return "ReleaseFullView{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ReleaseFullView that = (ReleaseFullView) o;

        if (title != null ? !title.equals(that.title) : that.title != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return title != null ? title.hashCode() : 0;
    }
}
