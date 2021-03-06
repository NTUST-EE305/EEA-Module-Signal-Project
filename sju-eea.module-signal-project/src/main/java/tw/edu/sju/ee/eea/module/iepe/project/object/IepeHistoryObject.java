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
package tw.edu.sju.ee.eea.module.iepe.project.object;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.core.api.multiview.MultiViews;
import org.openide.filesystems.FileObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.TopComponent;
import tw.edu.sju.ee.eea.module.iepe.project.IepeProject;
import tw.edu.sju.ee.eea.utils.io.tools.EEAInput;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import tw.edu.sju.ee.eea.module.iepe.channel.SourceChannel;
import tw.edu.sju.ee.eea.module.iepe.channel.ChannelList;
import tw.edu.sju.ee.eea.module.iepe.file.IepeFile;
import tw.edu.sju.ee.eea.module.iepe.project.IepeProjectProperties;
import tw.edu.sju.ee.eea.module.iepe.project.data.Pattern;
import tw.edu.sju.ee.eea.module.iepe.project.data.Warning;
import tw.edu.sju.ee.eea.utils.io.ValueInputStream;
import tw.edu.sju.ee.eea.utils.io.ValueOutput;

/**
 *
 * @author Leo
 */
public class IepeHistoryObject implements IepeProject.Child, Runnable, Serializable, Lookup.Provider {

    private Lookup lkp;
    private long interval;
    private IepeProjectProperties properties;

    public IepeHistoryObject(IepeProject project) {
        this.lkp = new ProxyLookup(new Lookup[]{Lookups.singleton(project), Lookups.singleton(this)});
        properties = project.getProperties();
        interval = 60000;
        try {
            initRecord();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public Lookup getLookup() {
        return lkp;
    }

    public String getDisplayName() {
        return "History";
    }

    private String pattern(int channel) {
        return new SimpleDateFormat(properties.history().getPattern()).format(Calendar.getInstance().getTime()).concat(".iepe")
                .replaceAll("channel", String.valueOf(channel));
    }

    private class FileChannel extends DataOutputStream implements ValueOutput {

        private FileObject folder;
        private int channel;
        private long length;
        private long index;

        public FileChannel(FileObject folder, int channel, long length) throws IOException {
            super(new IepeFile.Output(folder.createAndOpen(pattern(channel)),
                    new IepeFile(Calendar.getInstance().getTimeInMillis(), properties.device().getSampleRate(), channel))
                    .getOutputStream());
            this.folder = folder;
            this.channel = channel;
            this.length = length;
        }

        @Override
        public void writeValue(double value) throws IOException {
            if (!(index++ < length)) {
                super.flush();
                super.close();
                out = new IepeFile.Output(folder.createAndOpen(pattern(channel)),
                        new IepeFile(Calendar.getInstance().getTimeInMillis(), properties.device().getSampleRate(), channel))
                        .getOutputStream();
                index = 0;
            }
                writeDouble(value);
        }
    }

    public void initRecord() throws IOException {
        IepeProject context = lkp.lookup(IepeProject.class);
        FileObject projectDirectory = context.getProjectDirectory();
        try {
            projectDirectory.createFolder("Record");
        } catch (IOException ex) {
            Logger.getLogger(IepeHistoryObject.class.getName()).log(Level.INFO, null, ex);
        }
        folder = projectDirectory.getFileObject("Record");
//        list = lkp.lookup(IepeProject.class).getList();
        input = lkp.lookup(IepeProject.class).getInput();
        fileChannels = new FileChannel[8];
    }

    private EEAInput[] input;
//    private ChannelList list;
    private FileObject folder;
    private FileChannel[] fileChannels;

    public void recording(boolean run) throws IOException {
        if (run) {
            long length = this.interval / 1000 * properties.device().getSampleRate();
            for (int i = 0; i < fileChannels.length; i++) {
//                Channel channel = list.get(i);
                fileChannels[i] = new FileChannel(folder, i, length);
                input[0].getIOChannel(i).addStream(fileChannels[i]);
            }
        } else {
            for (int i = 0; i < fileChannels.length; i++) {
//                Channel channel = list.get(i);
                input[0].getIOChannel(i).removeStream(fileChannels[i]);
                fileChannels[i].close();
            }
        }
    }

    @Override
    public void run() {
        IepeProject project = lkp.lookup(IepeProject.class);
        InputOutput io = IOProvider.getDefault().getIO(this.getDisplayName(), false);

        java.util.Map<String, FileObject> files = new TreeMap<String, FileObject>();
        FileObject[] children = folder.getChildren();
        for (FileObject fileObject : children) {
            files.put(fileObject.getName(), fileObject);
        }

        int index = 0;
        ValueInputStream[] stream = new ValueInputStream[4];
        long time = 0;
        SimpleDateFormat dateFormat = new SimpleDateFormat(properties.history().getPattern());
        try {
            for (Map.Entry<String, FileObject> entry : files.entrySet()) {
                IepeFile.Input input = new IepeFile.Input(entry.getValue().getInputStream());
                stream[index++] = new ValueInputStream(input.getInputStream());
                if (index < 4) {
                    continue;
                } else {
                    index = 0;
                    time = input.getHeader().getTime();
                    try {
                        do {
                            try {
                                double[][] channels = new double[stream.length][1024];
                                for (int i = 0; i < channels[0].length; i++) {
                                    for (int j = 0; j < channels.length; j++) {
                                        channels[j][i] = stream[j].readValue();
                                    }
                                }
                                time += 1000.0 * 1024 / properties.device().getSampleRate();
                                Pattern pattern = new Pattern(new Date(time), input.getHeader().getSamplerate(), 1024, channels);
                                List<Warning> list = pattern.rules(properties.rules());
                                for (Warning warning : list) {
                                    warning.print(io);
                                }
                            } catch (IOException ex) {
                            }
                        } while (stream[0].available() > 1024);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    TopComponent tc;

    public org.openide.nodes.Node createNodeDelegate() {
        return new AbstractNode(new ChannelChildren(lkp.lookup(IepeProject.class).getProjectDirectory().getFileObject("Record")), lkp) {

            @Override
            public Action getPreferredAction() {
                return new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (tc == null) {
                            tc = MultiViews.createMultiView("application/iepe-history", IepeHistoryObject.this);
                        }
                        tc.open();
                        tc.requestActive();
                    }
                };
            }

            @Override
            public Image getIcon(int type) {
                return ImageUtilities.loadImage("tw/edu/sju/ee/eea/module/iepe/project/iepe_project.png");
            }

            @Override
            public Image getOpenedIcon(int type) {
                return getIcon(type);
            }

            @Override
            public String getDisplayName() {
                return "History";
            }

        };
    }

    public class ChannelChildren extends Children.Keys<FileObject> {

        private FileObject folder;

        public ChannelChildren(FileObject folder) {
            this.folder = folder;
        }

        @Override
        protected Node[] createNodes(FileObject key) {
            try {
                return new Node[]{DataObject.find(key).getNodeDelegate()};
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
            return null;
        }

        @Override
        protected void addNotify() {
            super.addNotify();
            java.util.Map<String, FileObject> list = new TreeMap<String, FileObject>();
            FileObject[] children = folder.getChildren();
            for (FileObject fileObject : children) {
                list.put(fileObject.getName(), fileObject);
            }
            setKeys(list.values());
        }
    }
}
