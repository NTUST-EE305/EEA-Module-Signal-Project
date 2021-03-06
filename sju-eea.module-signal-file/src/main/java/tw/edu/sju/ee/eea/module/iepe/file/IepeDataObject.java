/*
 * Copyright (C) 2014 Leo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package tw.edu.sju.ee.eea.module.iepe.file;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import tw.edu.sju.ee.eea.module.iepe.cookie.PlayStreamCookie;

@Messages({
    "LBL_Iepe_LOADER=Files of Iepe"
})
@MIMEResolver.ExtensionRegistration(
        displayName = "#LBL_Iepe_LOADER",
        mimeType = "application/iepe",
        extension = {"iepe"}
)
@DataObject.Registration(
        mimeType = "application/iepe",
        iconBase = "tw/edu/sju/ee/eea/module/iepe/file/iepe.png",
        displayName = "#LBL_Iepe_LOADER",
        position = 300
)
@ActionReferences({
    @ActionReference(
            path = "Loaders/application/iepe/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
            position = 100,
            separatorAfter = 200
    ),
    @ActionReference(
            path = "Loaders/application/iepe/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.CutAction"),
            position = 300
    ),
    @ActionReference(
            path = "Loaders/application/iepe/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.CopyAction"),
            position = 400,
            separatorAfter = 500
    ),
    @ActionReference(
            path = "Loaders/application/iepe/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"),
            position = 600
    ),
    @ActionReference(
            path = "Loaders/application/iepe/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.RenameAction"),
            position = 700,
            separatorAfter = 800
    ),
    @ActionReference(
            path = "Loaders/application/iepe/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.SaveAsTemplateAction"),
            position = 900,
            separatorAfter = 1000
    ),
    @ActionReference(
            path = "Loaders/application/iepe/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"),
            position = 1100,
            separatorAfter = 1200
    ),
    @ActionReference(
            path = "Loaders/application/iepe/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.ToolsAction"),
            position = 1300
    ),
    @ActionReference(
            path = "Loaders/application/iepe/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"),
            position = 1400
    )
})
public class IepeDataObject extends MultiDataObject implements IepeDataInfo, PlayStreamCookie {

    private IepeCursor cursor;
    private InputStream stream;

    public IepeDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        registerEditor("application/iepe", true);
        stream = getInputStream();
        cursor = new IepeCursor(getSamplerate());
        cursor.addIepeCursorListener(new IepeCursorListener() {

            @Override
            public void cursorMoved(IepeCursorEvent e) {
                if (e.getType() == IepeCursorEvent.SET) {
                    try {
                        stream = getInputStream();
                        stream.skip(e.getIndex());
                    } catch (FileNotFoundException ex) {
                        Exceptions.printStackTrace(ex);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
        });
    }

    @Override
    protected int associateLookup() {
        return 1;
    }

    @Override
    public IepeCursor getCursor() {
        return cursor;
    }

    @Override
    public int readStream(byte[] b) throws IOException {
        cursor.increase(b.length);
        return stream.read(b);
    }

    @Override
    public String getDisplayName() {
        return getPrimaryFile().getNameExt();
    }

    private IepeFile fileHeader;

    @Override
    public InputStream getInputStream() {
        try {
            IepeFile.Input input = new IepeFile.Input(getPrimaryFile().getInputStream());
            this.fileHeader = input.getHeader();
            return input.getInputStream();
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    @Override
    public int getSamplerate() {
        return fileHeader.getSamplerate();
    }

}
