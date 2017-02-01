package com.xebialabs.xlrelease.ci.server;

import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.xebialabs.xlrelease.ci.Messages;
import com.xebialabs.xlrelease.ci.util.Folder;
import com.xebialabs.xlrelease.ci.util.Release;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;


public class XLReleaseConnectorPost6Impl extends XLReleaseConnectorImpl {

    public static final String SLASH_CHARACTER = "/";
    public static final String SLASH_MARKER = "::SLASH::";
    public static final String SLASH_ESCAPE_SEQ = "\\\\/";

    public XLReleaseConnectorPost6Impl(String serverUrl, String proxyUrl, String username, String password) {
        super(serverUrl, proxyUrl, username, password);
    }

    @Override
    public List<Release> searchTemplates(String filter) {
        filter = markSlashEscapeSeq(filter);
        String folderId = getFolderId(filter);
        List<Release> templates = getTemplates(folderId);
        List<Folder> folders = getFolders(folderId);

        CollectionUtils.filter(folders, getFilterPredicate(getSearchString(filter)));
        CollectionUtils.filter(templates, getFilterPredicate(getSearchString(filter)));

        String folderPath = getFolderPath(filter);

        List<Release> releases = new ArrayList<Release>();
        for (Release template : templates) {
            template.setTitle(folderPath + escapeSlashSeq(template.getTitle()));

            template.setStatus("template");
            releases.add(template);
            logger.info(template.toString());
        }

        for (Folder folder : folders) {
            Release release = new Release();
            release.setId(folder.getId());
            release.setTitle(folderPath + escapeSlashSeq(folder.getTitle()));
            releases.add(release);
            release.setStatus("folder");
        }

        return releases;
    }

    @Override
    protected String getTemplateInternalId(String queryString) {
        final String templateTitle = unEscapeSlashSeq(queryString);
        String folderId = getFolderId(templateTitle);
        List<Release> templates = getTemplates(folderId);

        CollectionUtils.filter(templates, new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                return ((Release) o).getTitle().equals(templateTitle.substring(templateTitle.lastIndexOf('/') + 1));
            }
        });
        if (templates.size() > 0) {
            return templates.get(0).getInternalId();
        }
        throw new IllegalArgumentException(Messages.XLReleaseNotifier_templateNotFound(queryString));
    }

    @Override
    public List<Release> getAllTemplates() {
        List<Release> templates = new ArrayList<Release>();
        templates.addAll(getTemplates(getFolderId("")));
        List<Folder> folders = getFolders(getFolderId(""));
        for (Folder folder : folders) {
            fillFolders(folder);
            templates.addAll(folder.getAllTemplates());
        }
        return templates;
    }

    private void fillFolders(Folder folder) {
        folder.setFolderList(getFolders(folder.getId()));
        folder.setTemplates(getTemplates(folder.getId()));
        if (folder.getFolderList() != null) {
            for (Folder folder1 : folder.getFolderList()) {
                fillFolders(folder1);
            }
        }
    }

    private String getFolderId(String queryString) {
        String folderId = "Applications";
        if (queryString.contains(SLASH_CHARACTER)) {
            String folderPath = queryString.substring(0, queryString.lastIndexOf(SLASH_CHARACTER));
            Folder folder = getFolderByPath(folderPath);
            folderId = folder.getId();
        }
        return folderId;
    }

    private Folder getFolderByPath(String path) {
        WebResource service = buildWebResource();
        return service
                .path("api/v1/folders/find")
                .queryParam("byPath", path)
                .accept(MediaType.APPLICATION_JSON)
                .get(Folder.class);
    }

    private List<Release> getTemplates(String folderId) {
        WebResource service = buildWebResource();
        GenericType<List<Release>> genericType = new GenericType<List<Release>>() {
        };
        return service
                .path("api/v1/folders")
                .path(folderId)
                .path("templates")
                .accept(MediaType.APPLICATION_JSON)
                .get(genericType);
    }

    private List<Folder> getFolders(String folderId) {
        WebResource service = buildWebResource();
        GenericType<List<Folder>> genericType = new GenericType<List<Folder>>() {
        };
        return service
                .path("api/v1/folders")
                .path(folderId)
                .path("list")
                .accept(MediaType.APPLICATION_JSON)
                .get(genericType);
    }

    private Predicate getFilterPredicate(final String searchString) {
        return new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                if (object instanceof Release) {
                    return ((Release) (object)).getTitle().toLowerCase().startsWith(searchString.toLowerCase());
                } else if (object instanceof Folder) {
                    return ((Folder) (object)).getTitle().toLowerCase().startsWith(searchString.toLowerCase());
                }
                return false;
            }
        };
    }

    private String getSearchString(String queryString) {
        String searchString = "";
        if (queryString.charAt(queryString.length() - 1) != '/')
            searchString = queryString.split(SLASH_CHARACTER)[queryString.split(SLASH_CHARACTER).length - 1];
        return unEscapeSlashSeq(searchString);
    }

    private String escapeSlashSeq(String string) {
        return string.replaceAll(SLASH_CHARACTER, SLASH_ESCAPE_SEQ);
    }

    private String markSlashEscapeSeq(String string) {
        return string.replaceAll(SLASH_ESCAPE_SEQ, SLASH_MARKER);
    }

    private String unEscapeSlashSeq(String string) {
        return string.replaceAll(SLASH_MARKER, SLASH_CHARACTER);
    }

    private String getFolderPath(String queryString) {
        String folderPath = "";
        if (queryString.split(SLASH_CHARACTER).length > 1) {
            folderPath = queryString.substring(0, queryString.lastIndexOf(SLASH_CHARACTER)) + SLASH_CHARACTER;
        }
        if (queryString.charAt(queryString.length() - 1) == '/')
            folderPath = queryString;

        return folderPath;
    }


}
