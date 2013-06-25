package me.drton.flightplot.processors;

import me.drton.flightplot.processors.tools.RotationConversion;
import org.ejml.simple.SimpleMatrix;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * User: ton Date: 24.06.13 Time: 13:10
 */
public class AltitudeEstimator extends PlotProcessor {
    private String param_Field_Baro;
    private String[] param_Fields_Acc;
    private String[] param_Fields_Att;
    private double param_Weight_Acc;
    private double param_Weight_Baro;
    private double param_Offset;
    private int param_Baro_Latency;
    private double timePrev;
    private double[] x = new double[]{0.0, 0.0, 0.0};   // Pos, Vel, Acc
    private double corrBaro;
    private double corrAcc;
    private SimpleMatrix acc = new SimpleMatrix(3, 1);
    private SimpleMatrix r;
    private XYSeries seriesAlt;
    private XYSeries seriesAltV;
    private List<Double> baroFIFO = new LinkedList<Double>();

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Field Baro", "SENS.BaroAlt");
        params.put("Fields Acc", "IMU.AccX IMU.AccY IMU.AccZ");
        params.put("Fields Att", "ATT.Roll ATT.Pitch");
        params.put("Weight Baro", 1.0);
        params.put("Weight Acc", 50.0);
        params.put("Baro Latency", 0);
        params.put("Offset", 0.0);
        return params;
    }

    @Override
    public void init() {
        timePrev = Double.NaN;
        x[0] = 0.0;
        x[1] = 0.0;
        x[2] = 0.0;
        corrBaro = 0.0;
        corrAcc = 0.0;
        baroFIFO.clear();
        param_Field_Baro = (String) parameters.get("Field Baro");
        param_Fields_Acc = ((String) parameters.get("Fields Acc")).split(WHITESPACE_RE);
        param_Fields_Att = ((String) parameters.get("Fields Att")).split(WHITESPACE_RE);
        param_Weight_Baro = (Double) parameters.get("Weight Baro");
        param_Weight_Acc = (Double) parameters.get("Weight Acc");
        param_Baro_Latency = (Integer) parameters.get("Baro Latency");
        param_Offset = (Double) parameters.get("Offset");
        seriesAlt = createSeries("Alt");
        seriesAltV = createSeries("AltV");
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        boolean act = false;
        Number baroNum = (Number) update.get(param_Field_Baro);
        if (baroNum != null) {
            double baro = baroNum.doubleValue();
            baroFIFO.add(x[0]);
            if (baroFIFO.size() > param_Baro_Latency) {
                corrBaro = baro - baroFIFO.remove(0);
                act = true;
            }
        }
        Number accX = (Number) update.get(param_Fields_Acc[0]);
        Number accY = (Number) update.get(param_Fields_Acc[1]);
        Number accZ = (Number) update.get(param_Fields_Acc[2]);
        if (accX != null && accY != null && accZ != null) {
            acc.set(0, 0, accX.doubleValue());
            acc.set(1, 0, accY.doubleValue());
            acc.set(2, 0, accZ.doubleValue());
            act = true;
        }
        Number roll = (Number) update.get(param_Fields_Att[0]);
        Number pitch = (Number) update.get(param_Fields_Att[1]);
        if (roll != null && pitch != null) {
            r = RotationConversion.rotationMatrixByEulerAngles(roll.doubleValue(), pitch.doubleValue(), 0.0);
            act = true;
        }
        if (act) {
            SimpleMatrix accNED = r.mult(acc);
            if (!Double.isNaN(timePrev)) {
                double dt = time - timePrev;
                corrAcc = -accNED.get(2) - 9.81 - x[2];
                predict(dt);
                correct(dt, 0, corrBaro, param_Weight_Baro);
                correct(dt, 2, corrAcc, param_Weight_Acc);
                seriesAlt.add(time, x[0] + param_Offset);
                seriesAltV.add(time, x[1]);
            }
            timePrev = time;
        }
    }

    private void predict(double dt) {
        x[0] += x[1] * dt + x[2] * dt * dt;
        x[1] += x[2] * dt;
    }

    private void correct(double dt, int i, double e, double w) {
        double ewdt = w * e * dt;
        x[i] += ewdt;
        if (i == 0) {
            x[1] += w * ewdt;
            x[2] += w * w * ewdt / 3.0;
        } else if (i == 1) {
            x[2] += w * ewdt;
        }
    }

    @Override
    public XYSeriesCollection getSeriesCollection() {
        XYSeriesCollection seriesCollection = new XYSeriesCollection();
        seriesCollection.addSeries(seriesAlt);
        seriesCollection.addSeries(seriesAltV);
        return seriesCollection;
    }
}