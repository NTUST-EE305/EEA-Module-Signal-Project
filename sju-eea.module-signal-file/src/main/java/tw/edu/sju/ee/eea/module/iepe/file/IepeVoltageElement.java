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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.chart.plot.ValueMarker;
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
import tw.edu.sju.ee.eea.ui.chart.SampledChart;
import tw.edu.sju.ee.eea.utils.io.SampledInputStream;

@MultiViewElement.Registration(
        displayName = "#LBL_Iepe_Voltage",
        iconBase = "tw/edu/sju/ee/eea/module/iepe/file/iepe.png",
        mimeType = "application/iepe",
        persistenceType = TopComponent.PERSISTENCE_NEVER,
        preferredID = "IepeVisual",
        position = 2000
)
@Messages("LBL_Iepe_Voltage=Voltage Oscillogram")
public final class IepeVoltageElement extends JPanel implements MultiViewElement {

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
                    info.getCursor().setTime(0);
                }
            });
            this.add(head);
            tail = new JButton(new javax.swing.ImageIcon(getClass().getResource("/tw/edu/sju/ee/eea/module/iepe/file/iepe_visual_cursor_tail.png")));
            tail.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    info.getCursor().setTime(total);
                }
            });
            this.add(tail);
            this.addSeparator();
            zoomIn = new JButton(new javax.swing.ImageIcon(getClass().getResource("/tw/edu/sju/ee/eea/module/iepe/file/iepe_visual_cursor_zoomIn.png")));
            zoomIn.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    double tmp = cursor.getValue();
                    index += (int) ((tmp - index) / 2);
                    length /= 2;
                    scrollLength();
                }
            });
            this.add(zoomIn);
            zoomOut = new JButton(new javax.swing.ImageIcon(getClass().getResource("/tw/edu/sju/ee/eea/module/iepe/file/iepe_visual_cursor_zoomOut.png")));
            zoomOut.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    double tmp = cursor.getValue();
                    index -= (int) (tmp - index);
                    length *= 2;
                    scrollLength();
                }
            });
            this.add(zoomOut);

        }

    }

    private IepeDataInfo info;
    private JToolBar toolbar = new IepeVisualToolBar();
    private transient MultiViewElementCallback callback;
    private ValueMarker cursor;
    private boolean chartMouseClicked;
    private boolean chartScroll;
    private int index;
    private int length;
    private int total = 62500;

    public IepeVoltageElement(Lookup lkp) {
        info = lkp.lookup(IepeDataInfo.class);
        assert info != null;

        index = 0;
        length = 10000;

        cursor = new ValueMarker(0);
        cursor.setPaint(Color.black);
        initComponents();
        scrollBar.setMaximum(total);
        scrollLength();
        ((ChartPanel) chartPanel).addChartMouseListener(new ChartMouseListener() {

            @Override
            public void chartMouseClicked(ChartMouseEvent event) {
                chartMouseClicked = true;
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent event) {
            }
        });
        scrollBar.addAdjustmentListener(new AdjustmentListener() {

            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (chartScroll) {
                    index = e.getAdjustable().getValue();
                    length = e.getAdjustable().getVisibleAmount();
                    repaintChart();
                }
            }
        });
        info.getCursor().addIepeCursorListener(new IepeCursorListener() {

            @Override
            public void cursorMoved(IepeCursorEvent e) {
                cursor.setValue(e.getTime());
                double tmp = cursor.getValue() - index;
                if (tmp < 0 || tmp > length) {
                    index = (int) (cursor.getValue() - (length * 0.05));
                    scrollIndex();
                }
            }
        });
    }

    private JFreeChart createChart() {

        SampledChart sampledChart = new SampledChart("Voltage Oscillogram");
        sampledChart.addData(0, SampledChart.createSampledSeriesCollection("Ch_0", info.getInputStream(), index, info.getSamplerate(), length));
        sampledChart.addMarker(cursor);
        sampledChart.addProgressListener(new ChartProgressListener() {

            @Override
            public void chartProgress(ChartProgressEvent event) {
                if (event.getType() == ChartProgressEvent.DRAWING_FINISHED) {
                    if (chartMouseClicked) {
                        info.getCursor().setTime((int) event.getChart().getXYPlot().getDomainCrosshairValue());
                        chartMouseClicked = false;
                    }
                }
            }
        });
        return sampledChart;
    }

    private void scrollLength() {
        length = (length > total ? total : length);
        chartScroll = false;
        scrollBar.setVisibleAmount(length);
        scrollIndex();
    }

    private void scrollIndex() {
        index = (index > (total - length) ? total - length : index);
        index = (index < 0 ? 0 : index);
        chartScroll = false;
        scrollBar.setValue(index);
        repaintChart();
    }

    private void repaintChart() {
//        JFreeChart chart = ((ChartPanel) chartPanel).getChart();
//        chart = null;
//        ((ChartPanel) chartPanel).setChart(createChart());
        XYSeriesCollection sc = ((XYSeriesCollection) ((ChartPanel) chartPanel).getChart().getXYPlot().getDataset());
//        XYSeries series = sc.getSeries(0);
//        series.clear();
//        SampledStream ss = new
        sc.removeAllSeries();
        sc.addSeries(SampledChart.series("Ch_0", info.getInputStream(), index, info.getSamplerate(), length));
        chartScroll = true;
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
        return info.getLookup();
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
        callback.getTopComponent().setDisplayName(info.getDisplayName());
    }

    @Override
    public CloseOperationState canCloseElement() {
        return CloseOperationState.STATE_OK;
    }

}
