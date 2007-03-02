//
// $Id$

package com.threerings.msoy.swiftly.server.storage;

import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord;
import com.threerings.msoy.swiftly.server.persist.SwiftlySVNStorageRecord;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.semanticdesktop.aperture.mime.identifier.MimeTypeIdentifier;
import org.semanticdesktop.aperture.mime.identifier.magic.MagicMimeTypeIdentifier;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;

import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;

import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;

import org.tmatesoft.svn.core.wc.ISVNRepositoryPool;
import org.tmatesoft.svn.core.wc.DefaultSVNRepositoryPool;

/**
 * Handles the subversion-based project repository.
 */
public class ProjectSVNStorage
    implements ProjectStorage
{

    /** Standard SVN Protocol String. */
    public static final String PROTOCOL_SVN = "svn";

    /** SVN over SSH Protocol String. */
    public static final String PROTOCOL_SSH = "svn+ssh";
    
    /** File-local SVN Protocol String. */
    public static final String PROTOCOL_FILE = "file";

    /** SVN over HTTPS WebDAV Protocol String. */
    public static final String PROTOCOL_HTTPS = "https";

    /*
     * Static initialization of the SVNKit library
     */
    static {
        // Initialize SVNKit repository protocol factories

        // http:// and https://
        DAVRepositoryFactory.setup();

        // svn:// and svn+...://
        SVNRepositoryFactoryImpl.setup();

        // file:///
        FSRepositoryFactory.setup();
    }

    /**
     * Initialize project storage for a given project record. Any .svn directories in the given
     * template directory will be silently ignored.
     * TODO: Destroy the repository directory on failure.
     */
    public static ProjectSVNStorage initializeStorage (SwiftlyProjectRecord projectRecord,
        SwiftlySVNStorageRecord storageRecord, File templateDir)
        throws ProjectStorageException
    {
        ProjectSVNStorage storage;
        SVNRepository svnRepo;
        ISVNEditor editor;
        SVNCommitInfo commitInfo;
        long latestRevision;

        // If this is a local repository, we'll attempt to create it now
        if (storageRecord.protocol.equals(PROTOCOL_FILE)) {
            File repoDir = new File(storageRecord.baseDir, Integer.toString(projectRecord.projectId));
            try {
                SVNRepositoryFactory.createLocalRepository(repoDir, true, false);                
            } catch (SVNException e) {
                throw new ProjectStorageException.InternalError("Failure creating local project " +
                    "repository: " + e, e);                
            }
        }

        // Connect to the repository
        storage = new ProjectSVNStorage(projectRecord, storageRecord);

        // If the revision is not 0, this is not a new project. Exit immediately.
        try {
            svnRepo = storage.getSVNRepository();
            latestRevision = svnRepo.getLatestRevision();
            if (latestRevision != 0) {
                throw new ProjectStorageException.ConsistencyError("Invalid request to initialize a " +
                    "previously initialized storage repository");
            }
        } catch (SVNException e) {
            throw new ProjectStorageException.InternalError("Failure retrieving project " +
                "revision: " + e, e);
        }


        // New project, set up our commit editor
        try {
            editor = svnRepo.getCommitEditor("Swiftly Project Initialization", null);
        } catch (SVNException e) {
            throw new ProjectStorageException.InternalError("Failure initializing commit editor: "
                + e, e);
        }

        // 
        try {
            // Open the repository root
            editor.openRoot(-1);           

            // Add the template directory to the commit
            svnAddDirectory(editor, "", templateDir);

            // Close the repository root
            editor.closeDir();

            // Commit the whole lot
            commitInfo = editor.closeEdit();
    
        } catch (SVNException e) {
            try {
                // We have to abort the open edit. It can also raise an SVNException!
                editor.abortEdit();                
            } catch (SVNException eabort) {
                throw new ProjectStorageException.InternalError("Failure aborting subversion commit: "
                    + eabort, eabort);                
            }
            
            // Report failure
            throw new ProjectStorageException.InternalError("Failure committing project template: "
                + e, e);

        } catch (FileNotFoundException fnfe) {
            // Either someone handed us a bad template directory, or someone removed things from it
            // while we were running
            throw new ProjectStorageException.ConsistencyError("Could not load template: "
                + fnfe, fnfe);
        } catch (IOException ioe) {
            throw new ProjectStorageException.InternalError("Could not load template, failure reading input file:" +
                ioe, ioe);
        }

        // Validate the commit
        if (commitInfo == null) {
            throw new ProjectStorageException.InternalError("Subversion commit failed, null commit info returned");            
        }

        if (commitInfo.getNewRevision() != latestRevision + 1) {
            throw new ProjectStorageException.InternalError("Subversion commit failed: " + commitInfo.getErrorMessage());
        }

        // Everything worked, return the now-initialized storage instance
        return storage;
    }


    /** 
     * Construct a new storage instance for the given project record.
     */
    public ProjectSVNStorage (SwiftlyProjectRecord projectRecord, SwiftlySVNStorageRecord storageRecord)
        throws ProjectStorageException
    {
        _projectRecord = projectRecord;
        _storageRecord = storageRecord;

        // Initialize subversion magic
        try {
            _svnURL = getSVNURL();
            // TODO -- Remote authentication manager
            _svnPool = new DefaultSVNRepositoryPool(null, null, true, DefaultSVNRepositoryPool.INSTANCE_POOL);
        } catch (SVNException svne) {
            throw new ProjectStorageException.InternalError("Could not parse subversion URL: " + svne, svne);
        } catch (URISyntaxException urie) {
            throw new ProjectStorageException.InternalError(
                "Invalid URL provided by SwiftlySVNStorageRecord: " + urie, urie);            
        }
    }


    // from interface ProjectStorage
    // Recurse over the entirity of the subversion repository, building a list
    // of project path elements.
    // TODO: Extend the Swiftly client to support readdir()-style functionality instead of this sillyness.
    public List<PathElement> getProjectTree ()
        throws ProjectStorageException
    {
        SVNRepository svnRepo;

        // Stat the project root.
        try {
            svnRepo = getSVNRepository();
            SVNNodeKind nodeKind = svnRepo.checkPath("", -1);

            // Are we starting at a directory?
            if (nodeKind != SVNNodeKind.DIR) {
                throw new ProjectStorageException.InternalError("The subversion root path is not a directory");
            }
        } catch (SVNException svne) {
            throw new ProjectStorageException.InternalError("Unable to stat() the project root path: " + svne, svne);
        }

        try {
            long latestRevision = svnRepo.getLatestRevision();
            PathElement root = PathElement.createRoot(_projectRecord.projectName);
            List<PathElement> elements = recurseTree(svnRepo, root, null, latestRevision);
            
            // Root element must be added by the caller
            elements.add(root);
            return elements;
        } catch (SVNException svne) {
            throw new ProjectStorageException.InternalError("A subversion failure occured while" +
                " recursing over the directory tree: " + svne, svne);
        }

    }

    // from interface ProjectStorage
    public SwiftlyDocument getDocument (String path)
        throws ProjectStorageException
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    /**
     * Given a URI and a project Id, return the project's subversion URL.
     * This is composed of the base server URL + the project ID.
     */
    protected SVNURL getSVNURL ()
        throws SVNException, URISyntaxException
    {
        URI baseURI = _storageRecord.toURI();            
        SVNURL url = SVNURL.parseURIDecoded(baseURI.toString());
        return url.appendPath(Integer.toString(_projectRecord.projectId), false);
    }


    /** 
     * Recursively retrieves the entire subversion directory structure.
     */
    private List<PathElement> recurseTree (SVNRepository svnRepo, PathElement parent,
        List<PathElement> result, long revision)
        throws SVNException, ProjectStorageException
    {
        if (result == null) {
            result = new ArrayList<PathElement>();
        }

        @SuppressWarnings("unchecked")
        Collection<SVNDirEntry> entries = (Collection<SVNDirEntry>) svnRepo.getDir(parent.getAbsolutePath(), revision, null, (Collection)null);
        for (SVNDirEntry entry : entries) {
            PathElement node;
            SVNNodeKind kind;
            Map<String,String> properties;
            String mimeType;
            
            kind = entry.getKind();
            if (kind == SVNNodeKind.DIR) {
                node = PathElement.createDirectory(entry.getName(), parent);
                // Recurse
                recurseTree(svnRepo, node, result, revision);
            } else if (kind == SVNNodeKind.FILE) {
                // Fetch the file properties
                properties = new HashMap<String,String>();
                svnRepo.getFile(parent.getAbsolutePath() + "/" + entry.getName(), revision,
                    properties, null);
                
                // Pull out the mime type
                mimeType = properties.get(SVNProperty.MIME_TYPE);
                
                // Initialize a new PathElement node
                node = PathElement.createFile(entry.getName(), parent, mimeType);
            } else {
                throw new ProjectStorageException.InternalError("Received an unhandled subversion node type: " + kind);
            }

            result.add(node);
        }

        return result;
    }


    /**
     * Recursively add a given local directory to a repository edit instance.
     */
    private static void svnAddDirectory (ISVNEditor editor, String parentPath, File sourceDir)
        throws SVNException, FileNotFoundException, IOException
    {
        MimeTypeIdentifier identifier = new MagicMimeTypeIdentifier();

        for (File file : sourceDir.listFiles()) {
            String fileName = file.getName();
            String subPath = parentPath + "/" + fileName;
            File targetFile = new File(sourceDir, fileName);

            if (file.isDirectory()) {

                // Skip .svn directories.
                if (fileName.equals(".svn")) {
                    continue;
                }

                // Add the directory
                editor.addDir(subPath, null, -1);

                // Recurse
                svnAddDirectory(editor, subPath, targetFile);
                editor.closeDir();
            } else if (file.isFile()) {
                SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
                String checksum;
                String mimeType = null;

                /* Identify the mime type */
                FileInputStream stream = new FileInputStream(targetFile);
                byte[] firstBytes = new byte[identifier.getMinArrayLength()];

                // Read identifying bytes from the to-be-added file
                stream.read(firstBytes, 0, firstBytes.length);

                // Attempt magic identification
                mimeType = identifier.identify(firstBytes, targetFile.getName(), null);
                
                // If that failed, try our internal path-based type detection
                if (mimeType == null) {
                    // Get the miserly byte mime-type
                    byte miserMimeType = MediaDesc.suffixToMimeType(targetFile.getName());
                    
                    // If a valid type was returned, convert to a string
                    // Otherwise, don't set a mime type
                    // TODO: binary file detection
                    if (miserMimeType != -1) {
                        mimeType = MediaDesc.mimeTypeToString(miserMimeType);
                    }
                }

                // Add the file, generating the delta and checksum
                editor.addFile(subPath, null, -1);
                editor.applyTextDelta(subPath, null);
                checksum = deltaGenerator.sendDelta(subPath, new FileInputStream(targetFile), editor, true);
                
                // Set the mimetype, if any
                if (mimeType != null) {
                    editor.changeFileProperty(subPath, SVNProperty.MIME_TYPE, mimeType);
                }

                // Ship it
                editor.closeFile(subPath, checksum);
            }
        }
    }

    /**
     * Return a re-usable svn repository instance from the pool.
     */
    private SVNRepository getSVNRepository()
        throws SVNException
    {
        return _svnPool.createRepository(_svnURL, true);
    }

    /** Reference to the project record. */
    protected SwiftlyProjectRecord _projectRecord;

    /** Reference to the project storage record. */
    protected SwiftlySVNStorageRecord _storageRecord;

    /** Reference to the project storage subversion URL. */
    private SVNURL _svnURL;

    /** 
      * Reference to the project storage's subversion repository pool.
      * Repositories are not re-entrant or thread-safe -- transactional
      * operations (such as an edit), lock all access to the repository instance. The pool
      * provides one repository instance per thread, and as such, one should be careful to
      * open and close all transactions within the same method call. */
    private ISVNRepositoryPool _svnPool;

}
