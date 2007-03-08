//
// $Id$

package com.threerings.msoy.swiftly.server.build;

import com.threerings.msoy.swiftly.server.storage.ProjectStorage;
import com.threerings.msoy.swiftly.server.storage.ProjectStorageException;

import com.threerings.msoy.swiftly.data.BuildResult;
import com.threerings.msoy.swiftly.data.CompilerOutput;
import com.threerings.msoy.swiftly.data.FlexCompilerOutput;

import com.threerings.msoy.web.data.SwiftlyProject;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.Map;

import static com.threerings.msoy.Log.log;

/**
 * Server-local project builder.
 * TODO: Cache and update checkouts.
 */
public class LocalProjectBuilder
    implements ProjectBuilder
{    
    /**
     * Create a new local project builder.
     * @param flexSDK: Local path to the Flex SDK.
     */
    public LocalProjectBuilder (SwiftlyProject project, ProjectStorage storage,
        File flexSDK, File whirledSDK)
    {
        _project = project;
        _storage = storage;
        _flexSDK = flexSDK;
        _whirledSDK = whirledSDK;
    }

    public BuildResult build (File buildRoot)
        throws ProjectBuilderException
    {
        // Export the project data
        try {
            _storage.export(buildRoot);            
        } catch (ProjectStorageException pse) {
            throw new ProjectBuilderException("Exporting project data from storage failed: " + pse,
                pse);
        }

        // Build the project
        // TODO: The mxmlc process and #include any file on the system.
        // We need to start it under a permissions-limited JVM.
        try {
            Process proc;
            ProcessBuilder procBuilder;
            InputStream stdout;
            BufferedReader bufferedOutput;
            BuildResult result = new BuildResult();
            String line;
            
            // Refer the to "Using the Flex Compilers" documentation
            // http://livedocs.adobe.com/flex/2/docs/00001477.html
            procBuilder = new ProcessBuilder(
                _flexSDK.getAbsolutePath() + "/bin/mxmlc",
                "-load-config",
                _whirledSDK.getAbsolutePath() + "/whirled-config.xml",
                "-compiler.source-path=.",
                "+flex_sdk=" + _flexSDK.getAbsolutePath(),
                "+whirled_sdk=" + _whirledSDK.getAbsolutePath(),
                "-file-specs",
                _project.getTemplateSourceName()
            );

            // Direct stderr to stdout.
            Map<String, String>env = procBuilder.environment();
            env.clear();

            // Set the working directory to the build root.
            procBuilder.directory(buildRoot);

            // Sanitize the environment.
            procBuilder.redirectErrorStream(true);

            // Run the process and gather output
            proc = procBuilder.start();

            stdout = proc.getInputStream();
            bufferedOutput = new BufferedReader(new InputStreamReader(stdout));

            while ((line = bufferedOutput.readLine()) != null) {
                CompilerOutput output = new FlexCompilerOutput(line);

                // Don't provide the user with unparsable output.
                if (output.getLevel() == CompilerOutput.Level.UNKNOWN) {
                    // TODO: Remove this logging once we know what output parsing we're missing.
                    // landonf, Mar 7th, 2007
                    log.warning("Unparsable swiftly flex compiler output. [line=" + line + "]");
                    continue;
                } else {
                    result.appendOutput(output);
                }
            }

            return result;
        } catch (IOException ioe) {
            throw new ProjectBuilderException.InternalError("Failed to execute build process: " + ioe, ioe);
        }
    }

    /** Reference to our project. */
    protected SwiftlyProject _project;

    /** Reference to our backing project storage. */
    protected ProjectStorage _storage;
    
    /** Path to the Flex SDK. */
    protected File _flexSDK;

    /** Path to the Whirled SDK. */
    protected File _whirledSDK;
}
