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
package tw.edu.sju.ee.eea.module.iepe.project.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;
import org.openide.util.TaskListener;
import tw.edu.sju.ee.eea.module.iepe.project.IepeProject;
import tw.edu.sju.ee.eea.module.iepe.project.object.IepeHistoryObject;

@ActionID(
        category = "IEPE",
        id = "tw.edu.sju.ee.eea.module.iepe.project.action.RecordAction"
)
@ActionRegistration(
        displayName = "#CTL_AnalysisHistoryAction"
)
@ActionReference(path = "Menu/Analyzers", position = 600)
@Messages("CTL_AnalysisHistoryAction=Analysis History")
public final class AnalysisHistoryAction implements ActionListener {

    private final IepeProject context;
    private IepeHistoryObject history;
    private final static RequestProcessor RP = new RequestProcessor("interruptible tasks", 1, true);
    private ProgressHandle progr;

    public AnalysisHistoryAction(IepeProject context) {
        this.context = context;
        history = context.getLookup().lookup(IepeProject.IepeProjectLogicalView.class).getLookup().lookup(IepeHistoryObject.class);
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        // TODO use context
        RequestProcessor.Task task = RP.create(this.history);
        progr = ProgressHandleFactory.createHandle("Record task", task);
        task.addTaskListener(new TaskListener() {
            public void taskFinished(org.openide.util.Task task) {
                System.out.println("record task finished");
                progr.finish();
            }
        });
        progr.start();
        task.schedule(0);
    }
}
