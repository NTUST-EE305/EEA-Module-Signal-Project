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
package tw.edu.sju.ee.eea.module.signal;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.text.DefaultEditorKit;
import org.netbeans.spi.navigator.NavigatorPanel;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import tw.edu.sju.ee.eea.module.signal.io.ChannelChildFactory;
import tw.edu.sju.ee.eea.module.signal.io.ChannelList;

/**
 * Top component which displays something.
 */
@NavigatorPanel.Registration(displayName = "ssss", mimeType = "application/oscillogram")
@Messages({
    "CTL_NavigatorAction=Navigator",
    "CTL_NavigatorTopComponent=Navigator Window",
    "HINT_NavigatorTopComponent=This is a Navigator window"
})
public final class SignalNavigatorPanel extends JPanel implements NavigatorPanel, ExplorerManager.Provider, LookupListener {

    private Lookup.Result<ChannelList> result = null;

    private ExplorerManager manager;
    private BeanTreeView listView;
    private Lookup lookup;

    public SignalNavigatorPanel() {

        setLayout(new BorderLayout());
        manager = new ExplorerManager();
        
        ActionMap map = getActionMap();
        map.put(DefaultEditorKit.copyAction, ExplorerUtils.actionCopy(manager));
        map.put(DefaultEditorKit.cutAction, ExplorerUtils.actionCut(manager));
        map.put(DefaultEditorKit.pasteAction, ExplorerUtils.actionPaste(manager));
        map.put("delete", ExplorerUtils.actionDelete(manager, true));

        lookup = ExplorerUtils.createLookup(manager, map);
        listView = new BeanTreeView();
        add(listView);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    public String getDisplayName() {
        return "List view panel";
    }

    public String getDisplayHint() {
        return "List view based navigator panel";
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public void panelActivated(Lookup lkp) {
        Logger.getLogger(SignalNavigatorPanel.class.getName()).log(Level.INFO, "pa");
        this.result = Utilities.actionsGlobalContext().lookupResult(ChannelList.class);
        this.result.addLookupListener(this);
        ExplorerUtils.activateActions(manager, true);
        resultChanged(new LookupEvent(result));
    }

    @Override
    public void panelDeactivated() {
        Logger.getLogger(SignalNavigatorPanel.class.getName()).log(Level.INFO, "pd");
        this.result.removeLookupListener(this);
        this.result = null;
        ExplorerUtils.activateActions(manager, false);
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return manager;
    }

    @Override
    public void resultChanged(LookupEvent le) {
        Logger.getLogger(SignalNavigatorPanel.class.getName()).log(Level.INFO, manager.getRootContext().getDisplayName());
        Collection<? extends ChannelList> allInstances = this.result.allInstances();
        if (!allInstances.isEmpty()) {
            ChannelList list = allInstances.iterator().next();
            AbstractNode rn = new AbstractNode(Children.create(new ChannelChildFactory(list.getChannels()), false));
            rn.setName(list.getName());
            rn.setIconBaseWithExtension("tw/edu/sju/ee/eea/module/iepe/project/iepe_project.png");
            manager.setRootContext(rn);
            return ;
        }
//        manager.setRootContext(Node.EMPTY);
    }
    
    

}
