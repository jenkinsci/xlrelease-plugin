/**
 * Copyright (c) 2014, XebiaLabs B.V., All rights reserved.
 * <p/>
 * <p/>
 * The XL Release plugin for Jenkins is licensed under the terms of the GPLv2
 * <http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>, like most XebiaLabs Libraries.
 * There are special exceptions to the terms and conditions of the GPLv2 as it is applied to
 * this software, see the FLOSS License Exception
 * <https://github.com/jenkinsci/xlrelease-plugin/blob/master/LICENSE>.
 * <p/>
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; version 2
 * of the License.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth
 * Floor, Boston, MA 02110-1301  USA
 */

package com.xebialabs.xlrelease.ci.util;


import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class Folder {

    private String id;
    private String title;

    private List<Folder> folderList;
    private List<Release> templates;

    public Folder() {
    }

    public Folder(final String id, final String title) {
        this.id = id;
        this.title = title;
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

    public void setFolderList(List<Folder> folderList) {
        this.folderList = folderList;
    }

    public void setTemplates(List<Release> templates) {
        this.templates = templates;
    }

    public List<Folder> getFolderList() {
        return folderList;
    }

    public List<Release> getTemplates() {
        return templates;
    }

    public List<Release> getAllTemplates() {
        List<Release> templates = new ArrayList<Release>();

        for (Release release : this.templates) {
            Release rel = new Release();
            rel.setId(release.getId());
            rel.setTitle(title + "/" + release.getTitle());
            templates.add(rel);
        }
        for (Folder folder : folderList) {
            for (Release release : folder.getAllTemplates()) {
                release.setTitle(title + "/" + release.getTitle());
                templates.add(release);
            }
        }
        return templates;
    }

    @Override
    public String toString() {
        return "Folder{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Folder that = (Folder) o;

        if (title != null ? !title.equals(that.title) : that.title != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return title != null ? title.hashCode() : 0;
    }
}
