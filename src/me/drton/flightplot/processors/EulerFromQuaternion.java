package me.drton.flightplot.processors;

import me.drton.jmavlib.conversion.RotationConversion;

import javax.vecmath.Matrix3d;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ton on 05.01.15.
 */
public class EulerFromQuaternion extends PlotProcessor {
    private String[] param_Fields;
    private double param_Scale;
    private double param_pitch_rotation;
    private boolean[] show;
    private double[] q;
    private Matrix3d rot_q;
    private Matrix3d rot_target;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Fields", "ATT.qw ATT.qx ATT.qy ATT.qz");
        params.put("Show", "RPY");
        params.put("Scale", 1.0);
        params.put("Pitch Rot", 0.0);
        return params;
    }

    @Override
    public void init() {
        q = new double[4];
        param_Fields = ((String) parameters.get("Fields")).split(WHITESPACE_RE);
        param_Scale = (Double) parameters.get("Scale");
        param_pitch_rotation = (Double) parameters.get("Pitch Rot");
        String showStr = ((String) parameters.get("Show")).toUpperCase();
        show = new boolean[]{false, false, false};
        String[] axes = new String[]{"Roll", "Pitch", "Yaw"};
        for (int axis = 0; axis < 3; axis++) {
            String axisName = axes[axis];
            show[axis] = showStr.contains(axisName.substring(0, 1));
            if (show[axis]) {
                addSeries(axisName);
            }
        }
        rot_q = new Matrix3d();
        rot_target = new Matrix3d();
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        if (param_Fields.length < 4) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            Number v = (Number) update.get(param_Fields[i]);
            if (v == null) {
                return;
            }
            q[i] = v.doubleValue();
        }

        // Rotate with pitch if set
        rot_q.set((RotationConversion.rotationMatrixByQuaternion(q)));
        rot_target.set(RotationConversion.rotationMatrixByEulerAngles(0, Math.toRadians(param_pitch_rotation), 0));
        rot_q.mul(rot_target);

        double[] euler = RotationConversion.eulerAnglesByRotationMatrix(rot_q);
        int plot_idx = 0;
        for (int axis = 0; axis < 3; axis++) {
            if (show[axis]) {
                addPoint(plot_idx++, time, euler[axis] * param_Scale);
            }
        }
    }
}
