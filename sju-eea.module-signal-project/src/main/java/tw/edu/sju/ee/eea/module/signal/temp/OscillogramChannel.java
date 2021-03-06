/*
 * Copyright (C) 2015 D10307009
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
package tw.edu.sju.ee.eea.module.signal.temp;

import java.awt.Color;
import java.util.concurrent.ConcurrentLinkedQueue;
import javafx.application.Platform;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import tw.edu.sju.ee.eea.module.signal.io.Channel;
import tw.edu.sju.ee.eea.module.signal.io.ChannelInfo;
import tw.edu.sju.ee.eea.module.signal.oscillogram.SignalRenderer;
import tw.edu.sju.ee.eea.utils.io.ValueInput;
import tw.edu.sju.ee.eea.utils.io.tools.DeviceInfo;

/**
 *
 * @author D10307009
 */
public class OscillogramChannel implements Channel {

    private XYChart.Series<Number, Number> series = new LineChart.Series<Number, Number>();
    private ConcurrentLinkedQueue<XYChart.Data> queue = new ConcurrentLinkedQueue<XYChart.Data>();
//    private DeviceInfo device;
    private ChannelInfo channel;
//    private String device;
//    private int channel;
    private Color color;
    private SignalRenderer renderer;
//    int samplerate = 10000;
    private ValueInput vi;

    public OscillogramChannel(ChannelInfo channel, SignalRenderer renderer, ValueInput vi) {
//        this.device = device;
        this.channel = channel;
        this.renderer = renderer;
        this.setName(channel.getName());
//        SineSimulator s = new SineSimulator(samplerate, f[getChannel()], 5);
//        vi = new SineInputStream(s);
        this.vi = vi;
    }

    @Override
    public DeviceInfo getDeviceInfo() {
        return channel.getDeviceInfo();
    }

    private static final int[] f = {100, 200, 300};

    public void update(double t) {
        renderer.renderer(queue, t, vi, channel.getDeviceInfo().getSamplerate());
//        try {
//            vi.skip(vi.available());
//        } catch (IOException ex) {
//            Exceptions.printStackTrace(ex);
//        }
    }

    public XYChart.Series<Number, Number> getSeries() {
        return series;
    }

    public ConcurrentLinkedQueue<XYChart.Data> getQueue() {
        return queue;
    }

    public String getDevice() {
        return channel.getDeviceInfo().getDeviceName();
    }

    public int getChannel() {
        return channel.getChannel();
    }

    public String getName() {
        return this.channel.getName();
    }

    public Color getColor() {
        return color;
    }

    public void setName(String name) {
        this.channel.setName(name);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                series.setName(name);

            }
        });
    }

    public void setColor(Color color) {
        this.color = color;
    }

}
