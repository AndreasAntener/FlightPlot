package me.drton.flightplot.processors;

import java.util.*;

/**
 * Created by ada on 07.05.14.
 */
public class EfficiencyCalculator extends PlotProcessor {
    protected String fieldCurrent;
    protected String fieldAltitude;
    protected String fieldSpeed;
    protected String fieldThrust;
    protected int overTime;
    protected double scale;
    protected int series;

    protected Deque<EfficiencyData> data;
    protected Number current;
    protected Number speed;
    protected Number thrust;
    protected Number altitude;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        // FIXME: Parameters should have programmatic keys, the GUI should care about display values.
        params.put("Field Current", "BATT.C");
        params.put("Field Altitude", "GPS.Alt");
        params.put("Field Speed", "AIRS.TrueSpeed");
        params.put("Field Thrust", "ATTC.Thrust");
        params.put("Over Time (sec)", Integer.valueOf(10));
        params.put("Scale", 2.0);
        return params;
    }

    @Override
    public void init() {
        super.init();
        this.data = new ArrayDeque<EfficiencyData>();
        this.fieldCurrent = (String) parameters.get("Field Current");
        this.fieldAltitude = (String) parameters.get("Field Altitude");
        this.fieldSpeed = (String) parameters.get("Field Speed");
        this.fieldThrust = (String) parameters.get("Field Thrust");
        this.overTime = (Integer) parameters.get("Over Time (sec)");
        this.scale = (Double) parameters.get("Scale");
        series = addSeries("Efficiency");
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        current = (Number) update.get(fieldCurrent);
        altitude = (Number) update.get(fieldAltitude);
        speed = (Number) update.get(fieldSpeed);
        thrust = (Number) update.get(fieldThrust);

        // remove old entries (head of deque)
        while (null != this.data.peek() && this.data.peek().time + this.overTime < time) {
            this.data.remove();
        }

        if (null != current
                && null != altitude
                && null != speed
                && null != thrust
                ) {

            EfficiencyData newEffData = new EfficiencyData();
            newEffData.current = current.doubleValue();
            newEffData.speed = speed.doubleValue();
            newEffData.altitude = altitude.doubleValue();
            newEffData.time = time;
            // add new data to end of deque
            this.data.add(newEffData);

            if (thrust.doubleValue() > 0.02) {
                double eff = 0;
                double lastAlt = -1;
                Iterator<EfficiencyData> effIt = this.data.iterator();
                while (effIt.hasNext()) {
                    EfficiencyData effData = effIt.next();
                    double altDiff = (effData.altitude - (lastAlt == -1 ? effData.altitude : lastAlt)) * 1;
                    eff += ((effData.speed + altDiff) / (effData.current)) * (1d / this.data.size());
                    lastAlt = effData.altitude;
                }
                addPoint(this.series, time, eff * this.scale);
            } else {
                addPoint(this.series, time, -1);
            }
        }


    }
}
