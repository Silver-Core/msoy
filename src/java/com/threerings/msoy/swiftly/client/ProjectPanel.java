//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.PathElementTreeNode;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;
import com.threerings.msoy.swiftly.data.ProjectTreeModel;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.util.SwiftlyContext;

import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.presents.client.InvocationService.InvocationListener;

import com.samskivert.swing.util.TaskAdapter;
import com.samskivert.swing.util.TaskMaster;
import com.samskivert.swing.util.TaskObserver;

public class ProjectPanel extends JPanel
    implements TreeSelectionListener, TreeModelListener
{
    public ProjectPanel (SwiftlyContext ctx, SwiftlyEditor editor)
    {
        super(new BorderLayout());
        _ctx = ctx;
        _editor = editor;
        add(_scrollPane, BorderLayout.CENTER);
        setupToolbar();
        setupPopup();
        add(_toolbar, BorderLayout.PAGE_END);
    }

    /**
     * Initializes and adds the {@link ProjectTreeModel} to the panel.
     * @param roomObj the {@link ProjectRoomObject} used as the root node.
     */
    public void setProject (ProjectRoomObject roomObj)
    {
        _roomObj = roomObj;
        _treeModel = new ProjectTreeModel(roomObj);
        _treeModel.addTreeModelListener(this);

        _tree = new JTree(_treeModel);
        _tree.setDragEnabled(true);
        _tree.setEditable(true);
        _tree.setShowsRootHandles(true);
        _tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        _tree.addTreeSelectionListener(this);

        _scrollPane.getViewport().setView(_tree);
        disableToolbar();
    }

    // from interface TreeSelectionListener
    public void valueChanged (TreeSelectionEvent e)
    {
        PathElementTreeNode node = (PathElementTreeNode) _tree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }

        setSelectedNode(node);

        PathElement element = getSelectedPathElement();
        if (element.getType() == PathElement.Type.FILE) {
            _editor.openPathElement(element);
        }
    }

    // from interface TreeModelListener
    public void treeNodesChanged (TreeModelEvent e)
    {
        // TODO we are in single selection mode so only one node will ever change [right?]
        PathElementTreeNode node = (PathElementTreeNode)e.getChildren()[0];
        if (node.getElement().getType() == PathElement.Type.FILE) {
            _editor.updateTabTitleAt(node.getElement());
        }
    }

    // from interface TreeModelListener
    public void treeNodesInserted (TreeModelEvent e)
    {
        // TODO is it clear this is what we want to happen?
        PathElementTreeNode node = (PathElementTreeNode)e.getChildren()[0];
        _tree.scrollPathToVisible(new TreePath(node.getPath()));
    }

    // from interface TreeModelListener
    public void treeNodesRemoved (TreeModelEvent e)
    {
        // TODO iterate over every child and close any open tabs.
    }

    // from interface TreeModelListener
    public void treeStructureChanged (TreeModelEvent e)
    {
        // nada
    }

    @Override // from JComponent
    public Dimension getPreferredSize ()
    {
        Dimension d = super.getPreferredSize();
        d.width = Math.min(250, d.width);
        return d;
    }

    protected Action createPlusButtonAction ()
    {
        // TODO need icon
        return new AbstractAction("+") {
            // from AbstractAction
            public void actionPerformed (ActionEvent e) {
                _popup.show(_plusButton, _plusButton.getX(),
                    _plusButton.getY() - _popup.getHeight());
            }
        };
    }

    protected Action createMinusButtonAction ()
    {
        // TODO need icon
        return new AbstractAction("-") {
            // from AbstractAction
            public void actionPerformed (ActionEvent e) {
                deletePathElement();
            }
        };
    }

    protected Action createAddFileAction ()
    {
        return new AbstractAction(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.action.add_file")) {
            // from AbstractAction
            public void actionPerformed (ActionEvent e) {
                addPathElement(PathElement.Type.FILE);
            }
        };
    }

    protected Action createUploadFileAction ()
    {
        return new AbstractAction(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.action.upload_file")) {
            // from AbstractAction
            public void actionPerformed (ActionEvent e) {
                // TODO: implement filters based on supported MediaDesc mime types
                // FileNameExtensionFilter filter =
                // new FileNameExtensionFilter("JPG & GIF Images", "jpg", "gif");
                // chooser.setFileFilter(filter);
                JFileChooser fc = new JFileChooser();
                fc.setApproveButtonText(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.action.upload"));
                int returnVal = fc.showOpenDialog(_editor);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    final File file = fc.getSelectedFile();
                    // mime type will be determined on the server after the upload
                    PathElement uploadedFile = PathElement.createFile(
                        file.getName(), getCurrentParent(), null);
                    _roomObj.service.startFileUpload(_ctx.getClient(), uploadedFile,
                        new ConfirmListener () {
                        // from interface ConfirmListener
                        public void requestProcessed ()
                        {
                            UploadTask task = new UploadTask(file);
                            // TODO: constant
                            TaskMaster.invokeTask("upload file", task, new UploadTaskObserver());
                        }
                        // from interface ConfirmListener
                        public void requestFailed (String reason)
                        {
                            _editor.showErrorDialog(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, reason));
                        }
                    });
                }
            }
        };
    }

    protected Action createAddDirectoryAction ()
    {
        return new AbstractAction(
            _ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.action.add_directory")) {
            // from AbstractAction
            public void actionPerformed (ActionEvent e) {
                addPathElement(PathElement.Type.DIRECTORY);
            }
        };
    }

    /**
     * Adds a {@link PathElement} to the tree and broadcasts that fact to the server.
     */
    protected void addPathElement (PathElement.Type type)
    {
        PathElement parentElement = getCurrentParent();

        PathElement element = null;
        if (type == PathElement.Type.DIRECTORY) {
            // prompt the user for the name of the path element
            String name = _editor.showSelectPathElementNameDialog(type);
            if (name == null) {
                return; // if the user hit cancel do no more
            }
            element = PathElement.createDirectory(name, parentElement);
            _roomObj.service.addPathElement(_ctx.getClient(), element);
        } else if (type == PathElement.Type.FILE) {
            element = _editor.showCreateFileDialog(parentElement);
            if (element == null) {
                return; // if the user hit cancel do no more
            }
            _roomObj.service.addDocument(_ctx.getClient(), element, new InvocationListener () {
                public void requestFailed (String reason)
                {
                    _editor.showErrorDialog(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, reason));
                }
            });
        }
    }

    /**
     * Removes a {@link PathElement} and broadcasts that fact to the server.
     */
    protected void deletePathElement ()
    {
        // TODO throw up a Are you sure yes/no dialog
        PathElement element = getSelectedPathElement();

        if (_roomObj.project.getTemplateSourceName().equals(element.getName())) {
            _editor.showErrorDialog(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS,
                "e.cannot_delete_template"));
            return;
        }

        // XXX we know the tab was selected in order for delete to work. This might be dangerous.
        // we also know the tab was open.. hmmm
        if (element.getType() == PathElement.Type.FILE) {
            _editor.closeCurrentTab();
        } else if (element.getType() == PathElement.Type.DIRECTORY) {
            // TODO oh god we have to remove all the tabs associated with this directory
            // soo.. every tab that has a common parent id() ?
        } else if (element.getType() == PathElement.Type.ROOT){
            // TODO you're trying to remove the project itself? Does Homey play that?
            return;
        }
        _roomObj.service.deletePathElement(_ctx.getClient(), element.elementId);
    }

    protected PathElement getCurrentParent ()
    {
        // the parent element is the directory or project the selected element is in, or if
        // a project or directory is selected, that is the parent element
        PathElement parentElement = getSelectedPathElement();

        if (parentElement.getType() == PathElement.Type.FILE) {
            parentElement = parentElement.getParent();
        }
        return parentElement;
    }

    protected void setupToolbar ()
    {
        _plusButton = new JButton(createPlusButtonAction());
        _toolbar.add(_plusButton);

        _minusButton = new JButton(createMinusButtonAction());
        _toolbar.add(_minusButton);

        _toolbar.setFloatable(false);
        disableToolbar();
    }

    protected void setupPopup ()
    {
        _popup = new JPopupMenu();
        _popup.add(createAddDirectoryAction());
        _popup.add(createUploadFileAction());
        _popup.add(createAddFileAction());
        // in order for getHeight() to give a reasonable value the first time, we need to show
        // the popup at least once. pack() doesn't seem to do this.
        _popup.setVisible(true);
        _popup.setVisible(false);
    }

    protected void enableToolbar ()
    {
        setToolbarEnabled(true);
    }

    protected void disableToolbar ()
    {
        setToolbarEnabled(false);
    }

    protected void setToolbarEnabled (boolean value)
    {
        _plusButton.setEnabled(value);
        _minusButton.setEnabled(value);
    }

    protected PathElementTreeNode getSelectedNode ()
    {
        return _selectedNode;
    }

    protected PathElement getSelectedPathElement ()
    {
        return _selectedNode == null ? null : (PathElement)_selectedNode.getUserObject();
    }

    protected void setSelectedNode (PathElementTreeNode node)
    {
        // if this is the first selection enable the buttons
        if (_selectedNode == null) {
            enableToolbar();
        }
        _selectedNode = node;
    }

    protected class UploadTask extends TaskAdapter
    {
        public UploadTask (File file)
        {
            super();
            _file = file;
        }

        @Override
        public Object invoke()
            throws Exception
        {
            // TODO: update a modal progress bar
            FileInputStream input = new FileInputStream(_file);
            int len;
            byte[] buf = new byte[UPLOAD_BLOCK_SIZE];
            while ((len = input.read(buf)) > 0) {
                if (len < UPLOAD_BLOCK_SIZE) {
                    byte[] nbuf = new byte[len];
                    System.arraycopy(buf, 0, nbuf, 0, len);
                    _roomObj.service.uploadFile(_ctx.getClient(), nbuf);
                } else {
                    _roomObj.service.uploadFile(_ctx.getClient(), buf);
                }
                // wait a little to avoid sending too many messages to presents
                Thread.sleep(200);
            }
            input.close();
            return null; // TODO: meh
        }

        @Override
        public boolean abort()
        {
            // TODO: support clicking cancel?
            return false; // TODO: meh
        }

        protected File _file;
    }

    protected class UploadTaskObserver
        implements TaskObserver, ConfirmListener
    {
        // from interface TaskObserver
        public void taskCompleted(String name, Object result)
        {
            _roomObj.service.finishFileUpload(_ctx.getClient(), this);
        }

        // from interface TaskObserver
        public void taskFailed(String name, Throwable exception)
        {
            _editor.showErrorDialog(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "e.upload_failed"));
        }

        // from interface ConfirmListener
        public void requestProcessed ()
        {
            // nada
        }

        // from interface ConfirmListener
        public void requestFailed (String reason)
        {
            _editor.showErrorDialog(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, reason));
        }
    }

    /** Upload block size is 256K to avoid Presents freakouts. */
    protected static final int UPLOAD_BLOCK_SIZE = 262144;

    protected SwiftlyContext _ctx;
    protected SwiftlyEditor _editor;
    protected ProjectRoomObject _roomObj;
    protected ProjectTreeModel _treeModel;
    protected PathElementTreeNode _selectedNode;

    protected JTree _tree;
    protected JToolBar _toolbar = new JToolBar();
    protected JButton _plusButton;
    protected JButton _minusButton;
    protected JScrollPane _scrollPane = new JScrollPane();
    protected JPopupMenu _popup;
}
