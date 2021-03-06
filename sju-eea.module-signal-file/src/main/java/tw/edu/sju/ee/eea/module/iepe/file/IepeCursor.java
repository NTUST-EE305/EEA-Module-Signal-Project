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

import javax.swing.event.EventListenerList;

/**
 *
 * @author Leo
 */
public class IepeCursor {

    private long index;
    private int samplerate;

    protected EventListenerList listenerList = new EventListenerList();

    public IepeCursor(int samplerate) {
        this.samplerate = samplerate;
    }

    public long getIndex() {
        return index;
    }

    public int getTime() {
        return (int) (index / 8 / (samplerate / 1000));
    }

    public void increase(long index) {
        this.index += index;
        CursorPerformed(new IepeCursorEvent(this, IepeCursorEvent.INCREASE));
    }

    public void setIndex(long index) {
        this.index = index;
        CursorPerformed(new IepeCursorEvent(this, IepeCursorEvent.SET));
    }

    public void setTime(int ms) {
        this.index = ms * (samplerate / 1000) * 8;
        CursorPerformed(new IepeCursorEvent(this, IepeCursorEvent.SET));
    }

    protected void CursorPerformed(IepeCursorEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == IepeCursorListener.class) {
                ((IepeCursorListener) listeners[i + 1]).cursorMoved(e);
            }
        }
    }

    public void addIepeCursorListener(IepeCursorListener l) {
        listenerList.add(IepeCursorListener.class, l);
    }

    public void removeIepeCursorListener(IepeCursorListener l) {
        listenerList.remove(IepeCursorListener.class, l);
    }

    public IepeCursorListener[] getIepeCursorListener() {
        return listenerList.getListeners(IepeCursorListener.class);
    }

}
