package me.drton.flightplot.processors;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ada on 20.08.17.
 */
public class ParameterUpdate extends PlotProcessor {
    protected String paramField;
    protected String valueField;
    protected String typeField;

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("Field", "PARM.Name");
        params.put("Value Field", "PARM.Value");
        params.put("Type Field", "PARM.Type");
        return params;
    }

    @Override
    public void init() {
        paramField = (String) parameters.get("Field");
        valueField = (String) parameters.get("Value Field");
        typeField = (String) parameters.get("Type Field");
        addMarkersList();
    }

    @Override
    public void process(double time, Map<String, Object> update) {
        Object v = update.get(paramField);
        if (v != null && v instanceof String) {
            boolean isFloat = true;
            Object t = update.get(typeField);
            if (t != null && t instanceof Number) {
                isFloat = ((Number)t).equals(2);
            }

            Object n = update.get(valueField);
            if (n != null && n instanceof Number) {
                if (isFloat) {
                    addMarker(0, time, String.format("%s: %.3f", v, n));
                } else {
                    addMarker(0, time, String.format("%s: %d", v, ((Float)n).intValue()));
                }
            }
        }
    }
}
