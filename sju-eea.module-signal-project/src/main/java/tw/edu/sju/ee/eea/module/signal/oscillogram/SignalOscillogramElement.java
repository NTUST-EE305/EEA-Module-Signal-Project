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
package tw.edu.sju.ee.eea.module.signal.oscillogram;

import com.sun.javafx.tk.Toolkit;
import com.sun.scenario.animation.AbstractMasterTimer;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.Format;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.util.converter.NumberStringConverter;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import org.netbeans.core.spi.multiview.CloseOperationState;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.MultiViewElementCallback;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import tw.edu.sju.ee.eea.core.math.MetricPrefixFormat;
import tw.edu.sju.ee.eea.module.iepe.project.IepeProjectProperties;
import tw.edu.sju.ee.eea.module.signal.temp.OscillogramChannel;
import tw.edu.sju.ee.eea.utils.io.tools.EEAInput;
import tw.edu.sju.ee.eea.ui.swing.SpinnerMetricModel;

@MultiViewElement.Registration(
        displayName = "#LBL_Oscilloscope_Function",
        iconBase = "tw/edu/sju/ee/eea/module/iepe/file/iepe.png",
        mimeType = SignalOscillogramObject.MIMETYPE,
        persistenceType = TopComponent.PERSISTENCE_NEVER,
        preferredID = "FunctionVisual",
        position = 2000
)
@Messages("LBL_Oscilloscope_Function=Function Oscillogram")
public final class SignalOscillogramElement extends JPanel implements MultiViewElement, Runnable, ListDataListener {

    private class IepeVisualToolBar extends JToolBar {

        private JButton head;
        private JButton tail;
        private JButton zoomIn;
        private JButton zoomOut;

        private JLabel _label_horizontal;
        private Spinner _spinner_horizontal;
//        private JLabel _label_vertical;
//        private Spinner _spinner_vertical;

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

            //##################################################################
            this.addSeparator();
            _label_horizontal = new JLabel("horizontal");
            _label_horizontal.setBorder(new EmptyBorder(0, 10, 0, 10));
            _spinner_horizontal = new Spinner(new SpinnerMetricModel(1, 0.000000001, 1000000));
            _spinner_horizontal.setFormat(new MetricPrefixFormat("0.###"));
            _spinner_horizontal.setWidth(60);
            _spinner_horizontal.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    t = (Double) _spinner_horizontal.getValue();
//                    axis.setRange(0, t * 1000 * 1000);
//                    for (int i = 0; i < channels.length; i++) {
//                        channels[i].setLength(t);
//                    }
                }
            });
            this.add(_label_horizontal);
            this.add(_spinner_horizontal);

//            _label_vertical = new JLabel("vertical");
//            _label_vertical.setBorder(new EmptyBorder(0, 10, 0, 10));
//            _spinner_vertical = new Spinner(new SpinnerPreferredNumberModel(0.000000001, 1000000));
//            _spinner_vertical.setFormat(new MetricPrefixFormat("0.###"));
//            _spinner_vertical.setWidth(60);
//            _spinner_vertical.addChangeListener(new ChangeListener() {
//
//                @Override
//                public void stateChanged(ChangeEvent e) {
//                    System.out.println(_spinner_vertical.getValue());
////                    axis.setFixedAutoRange((Integer)_spinner_horizontal.getValue());
//                }
//            });
//            this.add(_label_vertical);
//            this.add(_spinner_vertical);
            //##################################################################
            this.addSeparator();

        }

        @Override
        public void setEnabled(boolean b) {
            this.head.setEnabled(b);
            this.tail.setEnabled(b);
            this.zoomIn.setEnabled(b);
            this.zoomOut.setEnabled(b);
        }

    }

    private IepeProjectProperties properties;
    private Lookup lkp;
    private SignalOscillogramObject object;
    private JToolBar toolbar = new IepeVisualToolBar();
    private transient MultiViewElementCallback callback;

    public SignalOscillogramElement(Lookup lkp) {
        this.lkp = lkp;
        this.object = lkp.lookup(SignalOscillogramObject.class);
        assert object != null;
//        IepeProject project = lkp.lookup(IepeProject.class);
        properties = object.getProject().getProperties();

//        object.getChannels();
//        ChannelList list = rt.getChannelList();
//        EEAInput[] iepe = object.getProject().getInput();
//        list.addConfigure(this);
        object.getChannels().addChangeListener(this);
        initComponents();
        initfx();
        toolbar.setEnabled(false);
    }

    @Override
    public void intervalAdded(ListDataEvent e) {
        if (e.getSource() instanceof OscillogramChannel) {
            OscillogramChannel channel = (OscillogramChannel) e.getSource();
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    chart.getData().add(channel.getSeries());
                }
            });
        }
    }

    @Override
    public void intervalRemoved(ListDataEvent e) {
        if (e.getSource() instanceof OscillogramChannel) {
            OscillogramChannel channel = (OscillogramChannel) e.getSource();
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    chart.getData().remove(channel.getSeries());
                }
            });
        }
    }

    @Override
    public void contentsChanged(ListDataEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private static final int MAX_DATA_POINTS = 1000;
    private double t = 1;

    private LineChart<Number, Number> chart;
    private ExecutorService executor;
    private NumberAxis xAxis;

    private Scene createScene() {
        xAxis = new NumberAxis(0, MAX_DATA_POINTS, MAX_DATA_POINTS / 10);
        xAxis.setForceZeroInRange(true);
        xAxis.setAutoRanging(false);
        xAxis.setTickLabelFormatter(new NumberStringConverter(new MetricPrefixFormat("#.##")));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setAutoRanging(true);

        //-- Chart
        chart = new LineChart<Number, Number>(xAxis, yAxis) {
            // Override to remove symbols on each data point
            @Override
            protected void dataItemAdded(XYChart.Series<Number, Number> series, int itemIndex, XYChart.Data<Number, Number> item) {
            }
        };
        chart.setAnimated(false);
        chart.setId(object.getName());
        chart.setTitle(object.getName());
        chart.setCreateSymbols(false);
        return new Scene(chart);
    }

    private AnimationTimer timer;
    private boolean update = false;
    private Scene scene;

    private void initFX(JFXPanel fxPanel) {
        // This method is invoked on the JavaFX thread
        scene = createScene();
        fxPanel.setScene(scene);

        //-- Prepare Executor Services
        executor = Executors.newCachedThreadPool();
        executor.execute(this);
//        new Thread(this).start();

        //-- Prepare Timeline
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                SignalOscillogramElement.this.handle();
            }
        };
        timer.start();
    }

    public void handle() {
        if (update) {
            System.out.println("at");
            for (OscillogramChannel channel : object.getChannels()) {
                addDataToSeries(channel.getSeries(), channel.getQueue());
            }
            xAxis.setLowerBound(0);
            xAxis.setUpperBound(t);
            xAxis.setTickUnit(t / 10);
            update = false;
        }
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                System.out.println("run");
                // add a item of random data to queue
                for (OscillogramChannel channel : object.getChannels()) {
                    channel.update(t);
                }
                update = true;
//                Platform.setImplicitExit(false);
//                Platform.runLater(()
//                        -> handle()
//                );
                Thread.sleep(1000);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(SignalOscillogramElement.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("===== STOP =====");
    }

    private static void addDataToSeries(XYChart.Series<Number, Number> series, ConcurrentLinkedQueue<XYChart.Data> queue) {
        if (queue.size() > 0) {
            series.getData().remove(0, series.getData().size());
        }
        while (!queue.isEmpty()) {
            series.getData().add(queue.remove());
        }
        if (series.getData().size() == 0) {
            return;
        }
        // remove points to keep us at no more than MAX_DATA_POINTS
//        if (series.getData().size() > MAX_DATA_POINTS) {
//            series.getData().remove(0, series.getData().size() - MAX_DATA_POINTS);
//        }
    }

    private void initfx() {
        Platform.setImplicitExit(false);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                initFX(jFXPanel1);

            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFXPanel1 = new javafx.embed.swing.JFXPanel();

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jFXPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 620, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jFXPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javafx.embed.swing.JFXPanel jFXPanel1;
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
        Logger.getLogger(SignalOscillogramElement.class.getName()).log(Level.INFO, "open");
    }

    @Override
    public void componentClosed() {
        Logger.getLogger(SignalOscillogramElement.class.getName()).log(Level.INFO, "close");
    }

    @Override
    public void componentShowing() {
        Logger.getLogger(SignalOscillogramElement.class.getName()).log(Level.INFO, "show");
    }

    @Override
    public void componentHidden() {
        Logger.getLogger(SignalOscillogramElement.class.getName()).log(Level.INFO, "hide");
    }

    @Override
    public void componentActivated() {
        Logger.getLogger(SignalOscillogramElement.class.getName()).log(Level.INFO, "active");
    }

    @Override
    public void componentDeactivated() {
        Logger.getLogger(SignalOscillogramElement.class.getName()).log(Level.INFO, "deactive");
    }

    @Override
    public UndoRedo getUndoRedo() {
        return UndoRedo.NONE;
    }

    @Override
    public void setMultiViewCallback(MultiViewElementCallback callback) {
        this.callback = callback;
        callback.getTopComponent().setDisplayName(object.getDisplayName());
    }

    @Override
    public CloseOperationState canCloseElement() {
        return CloseOperationState.STATE_OK;
    }

    private static class Spinner extends JSpinner {

        public Spinner(SpinnerNumberModel model) {
            super(model);
        }

        public void setWidth(int width) {
            Dimension maximumSize = getMaximumSize();
            maximumSize.width = width;
            setMaximumSize(maximumSize);
        }

        public void setFormat(Format format) {
            NumberFormatter formatter = (NumberFormatter) ((DefaultFormatterFactory) (((JSpinner.DefaultEditor) getEditor()).getTextField()).getFormatterFactory()).getDefaultFormatter();
            formatter.setFormat(format);
        }

    }

}
