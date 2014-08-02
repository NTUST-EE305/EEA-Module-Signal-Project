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
package tw.edu.sju.ee.eea.module.iepe.project.window;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Iterator;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.netbeans.core.spi.multiview.CloseOperationState;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.MultiViewElementCallback;
import org.openide.awt.UndoRedo;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import tw.edu.sju.ee.eea.module.iepe.project.IepeProject;
import tw.edu.sju.ee.eea.module.iepe.project.object.IepeRealtimeObject;
import tw.edu.sju.ee.eea.module.iepe.project.ui.SampledManager;
import tw.edu.sju.ee.eea.module.iepe.project.ui.SampledSeries;
import tw.edu.sju.ee.eea.ui.chart.SampledChart;
import tw.edu.sju.ee.eea.util.iepe.IEPEInput;
import tw.edu.sju.ee.eea.util.iepe.io.VoltageOutputStream;
import tw.edu.sju.ee.eea.util.iepe.io.SampledOutputStream;
import tw.edu.sju.ee.eea.util.iepe.io.SampledInputStream;
import tw.edu.sju.ee.eea.util.iepe.io.VoltageOutput;

@MultiViewElement.Registration(
        displayName = "#LBL_Iepe_VISUAL",
        iconBase = "tw/edu/sju/ee/eea/module/iepe/file/iepe.png",
        mimeType = "application/iepe-realtime",
        persistenceType = TopComponent.PERSISTENCE_NEVER,
        preferredID = "IepeVisual",
        position = 2000
)
@Messages("LBL_Iepe_VISUAL=Voltage Oscillogram")
public final class IepeRealtimeVisualElement extends JPanel implements MultiViewElement, Runnable {

    private class IepeVisualToolBar extends JToolBar {

        private JButton head;
        private JButton tail;
        private JButton zoomIn;
        private JButton zoomOut;

        public IepeVisualToolBar() {
            this.setFloatable(false);
            this.addSeparator();
            head = new JButton(new javax.swing.ImageIcon(getClass().getResource("/tw/edu/sju/ee/eea/module/iepe/file/iepe_visual_cursor_head.png")));
            head.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
//                    info.getCursor().setTime(0);
                }
            });
            this.add(head);
            tail = new JButton(new javax.swing.ImageIcon(getClass().getResource("/tw/edu/sju/ee/eea/module/iepe/file/iepe_visual_cursor_tail.png")));
            tail.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
//                    info.getCursor().setTime(total);
                }
            });
            this.add(tail);
            this.addSeparator();
            zoomIn = new JButton(new javax.swing.ImageIcon(getClass().getResource("/tw/edu/sju/ee/eea/module/iepe/file/iepe_visual_cursor_zoomIn.png")));
            zoomIn.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
//                    double tmp = cursor.getValue();
//                    index += (int) ((tmp - index) / 2);
//                    length /= 2;
//                    scrollLength();
                }
            });
            this.add(zoomIn);
            zoomOut = new JButton(new javax.swing.ImageIcon(getClass().getResource("/tw/edu/sju/ee/eea/module/iepe/file/iepe_visual_cursor_zoomOut.png")));
            zoomOut.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
//                    double tmp = cursor.getValue();
//                    index -= (int) (tmp - index);
//                    length *= 2;
//                    scrollLength();
                }
            });
            this.add(zoomOut);

        }

        @Override
        public void setEnabled(boolean b) {
            this.head.setEnabled(b);
            this.tail.setEnabled(b);
            this.zoomIn.setEnabled(b);
            this.zoomOut.setEnabled(b);
        }

    }

    private Lookup lkp;
    private IepeRealtimeObject rt;
    private JToolBar toolbar = new IepeVisualToolBar();
    private transient MultiViewElementCallback callback;

    public IepeRealtimeVisualElement(Lookup lkp) {
        this.lkp = lkp;
        this.rt = lkp.lookup(IepeRealtimeObject.class);
        assert rt != null;

        initComponents();
        toolbar.setEnabled(false);

        Thread t = new Thread(this);
        t.start();
    }

    
    private class VoltageChannel extends XYSeries implements IEPEInput.VoltageArrayOutout, VoltageOutput {

        private SampledOutputStream stream;
        
        public VoltageChannel(Comparable key) {
            super(key);
            stream = new SampledOutputStream(this, 3200);
        }

        @Override
        public void writeValue(double value) throws IOException {
            add(Calendar.getInstance().getTimeInMillis(), value);
        }

        @Override
        public void writeVoltageArray(double[] data) throws IOException {
            for (double d : data) {
                stream.writeSampled(d);
            }
        }

        @Override
        public void close() throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void flush() throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
    

//    private IEPEInput.IepeStream stream;
//    private XYSeries xySeries;
    private VoltageChannel channel0;
    
    @Override
    public void run() {
//        SampledStream sampled = new SampledStream(stream, 3200);
        long time = Calendar.getInstance().getTimeInMillis();
        while (true) {
            try {
                //            try {
//                xySeries.add(time, sampled.readSampled());
//            } catch (IOException ex) {
//                Exceptions.printStackTrace(ex);
//            }
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
            time += 100;
        }
    }

    private JFreeChart createChart() {

        SampledChart sampledChart = new SampledChart("Voltage Oscillogram");
//        sampledChart.addData(0, manager.getCollection(), manager.getRenderer());
        XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
        
        sampledChart.getXYPlot().setDataset(0, xySeriesCollection);
        sampledChart.getXYPlot().mapDatasetToRangeAxis(0, 0);
        sampledChart.getXYPlot().setRenderer(0, SampledChart.creatrRenderer());
        
//        xySeries = new XYSeries("ch0");
//        xySeriesCollection.addSeries(xySeries);
        channel0 = new VoltageChannel("ch0");
        xySeriesCollection.addSeries(channel0);

        ValueAxis axis = sampledChart.getXYPlot().getDomainAxis();
        axis.setAutoRange(true);
        axis.setFixedAutoRange(60000.0);  // 60 seconds
        
        IEPEInput iepe = lkp.lookup(IepeProject.class).getIepe();
//        try {
//            stream = new IEPEInput.IepeStream();
//        } catch (IOException ex) {
//            Exceptions.printStackTrace(ex);
//        }
        iepe.addStream(0, channel0);

        return sampledChart;
    }

    @Override
    public String getName() {
        return "IepeVisualElement";
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollBar = new javax.swing.JScrollBar();
        chartPanel = new ChartPanel(createChart());

        scrollBar.setOrientation(javax.swing.JScrollBar.HORIZONTAL);

        javax.swing.GroupLayout chartPanelLayout = new javax.swing.GroupLayout(chartPanel);
        chartPanel.setLayout(chartPanelLayout);
        chartPanelLayout.setHorizontalGroup(
            chartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 620, Short.MAX_VALUE)
        );
        chartPanelLayout.setVerticalGroup(
            chartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 437, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(chartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(scrollBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(chartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrollBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel chartPanel;
    private javax.swing.JScrollBar scrollBar;
    // End of variables declaration//GEN-END:variables
    @Override
    public JComponent getVisualRepresentation() {
        return this;
    }

    @Override
    public JComponent getToolbarRepresentation() {
        return toolbar;
    }

    @Override
    public Action[] getActions() {
        return new Action[0];
    }

    @Override
    public Lookup getLookup() {
        return lkp;
    }

    @Override
    public void componentOpened() {
    }

    @Override
    public void componentClosed() {
    }

    @Override
    public void componentShowing() {
    }

    @Override
    public void componentHidden() {
    }

    @Override
    public void componentActivated() {
    }

    @Override
    public void componentDeactivated() {
    }

    @Override
    public UndoRedo getUndoRedo() {
        return UndoRedo.NONE;
    }

    @Override
    public void setMultiViewCallback(MultiViewElementCallback callback) {
        this.callback = callback;
        callback.getTopComponent().setDisplayName(rt.getDisplayName());
    }

    @Override
    public CloseOperationState canCloseElement() {
        return CloseOperationState.STATE_OK;
    }

}
