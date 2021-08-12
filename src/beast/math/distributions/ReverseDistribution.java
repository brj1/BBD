/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package beast.math.distributions;

import beast.core.Input;
import beast.core.State;
import beast.core.parameter.RealParameter;
import java.io.PrintStream;
import java.util.List;
import java.util.Random;
import beast.core.Distribution;

/**
 * Does not work in BEAUti because of recursion.
 * @author brad
 */
public class ReverseDistribution extends Distribution {
    final public Input<RealParameter> valueInput = new Input<>("value", "variable", Input.Validate.REQUIRED);
    final public Input<RealParameter> startInput = new Input<>("start", "value to subtract from");
    final public Input<ParametricDistribution> distInput = new Input<>("dist", "distribution of difference", Input.Validate.REQUIRED);

    double start;
    ParametricDistribution dist;
    
    double storedStart;
        
    @Override
    public void initAndValidate() {
        super.initAndValidate();
        
        if (startInput.get() == null) {
            start = 0;
        } else {
            start = startInput.get().getValue();
        }
        
        dist = distInput.get();
    }
    
    @Override
    public List<String> getArguments() {
        return null;
    }

    @Override
    public List<String> getConditions() {
        return null;
    }

    /* 
     To be implemented
    */
    @Override
    public void sample(State state, Random random) {
    }
    
    @Override
    public void store() {
        storedStart = start;
        
        super.store();
    }
    
    @Override
    public void restore() {
        start = storedStart;
        
        super.restore();
    }
    
    @Override
    public double calculateLogP() {
        RealParameter param = valueInput.get();        
        logP = 0;
        
        for (double x : param.getDoubleValues()) {
             logP += dist.logDensity(start - x);
             
             if (Double.isInfinite(logP)) {
                 return logP;
             }
        }
        
        return logP;
    }
    
    @Override
    public void log(final long sample, final PrintStream out) {
        out.print(getCurrentLogP() + "\t");
    }
}
