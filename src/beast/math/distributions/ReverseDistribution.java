/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package beast.math.distributions;

import beast.core.Input;
import beast.core.parameter.RealParameter;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.Distribution;
import org.apache.commons.math.distribution.IntegerDistribution;

/**
 * Does not work in BEAUti because of recursion.
 *
 * @author brad
 */
public class ReverseDistribution extends ParametricDistribution {

    final public Input<RealParameter> startInput = new Input<>("start", "value to subtract from");
    final public Input<ParametricDistribution> distInput = new Input<>("distr", "distribution of difference", Input.Validate.REQUIRED);

    double start;
    ParametricDistribution dist;

    ReverseDistributionImpl m_dist;

    @Override
    public void initAndValidate() {
        if (startInput.get() == null) {
            start = 0;
        } else {
            start = startInput.get().getValue();
        }

        dist = distInput.get();

        m_dist = ReverseDistributionImpl.createInstance(start, dist.getDistribution());
    }

    @Override
    public Distribution getDistribution() {
        return m_dist;
    }

    @Override
    protected double getMeanWithoutOffset() {
        initAndValidate();
        return start - dist.getMeanWithoutOffset();
    }

    @Override
    public double getMean() {
        initAndValidate();
        return start - dist.getMean();
    }

    public static class ReverseDistributionImpl implements Distribution {

        double start;
        Distribution dist;

        public ReverseDistributionImpl(double start, Distribution dist) {
            this.start = start;
            this.dist = dist;
        }

        @Override
        public double cumulativeProbability(double x) throws MathException {
            return dist.cumulativeProbability(start - x);
        }

        @Override
        public double cumulativeProbability(double x0, double x1) throws MathException {
            return dist.cumulativeProbability(start - x0, start - x1);
        }

        public static ReverseDistributionImpl createInstance(double start, Distribution dist) {
            if (dist instanceof ContinuousDistribution) {
                return new ReverseDistributionContinuousImpl(start, (ContinuousDistribution) dist);
            } else if (dist instanceof IntegerDistribution) {
                return new ReverseDistributionIntegerImpl(start, (IntegerDistribution) dist);
            } else {
                return new ReverseDistributionImpl(start, dist);
            }
        }
    }

    public static class ReverseDistributionContinuousImpl extends ReverseDistributionImpl implements ContinuousDistribution {

        ContinuousDistribution contDist;

        public ReverseDistributionContinuousImpl(double start, ContinuousDistribution dist) {
            super(start, dist);
            contDist = dist;
        }

        @Override
        public double inverseCumulativeProbability(double p) throws MathException {
            double x = contDist.inverseCumulativeProbability(1 - p);

            return start - x;
        }

        @Override
        public double density(double x) {
            return contDist.density(start - x);
        }

        @Override
        public double logDensity(double x) {
            return contDist.logDensity(start - x);
        }
    }

    public static class ReverseDistributionIntegerImpl extends ReverseDistributionImpl implements IntegerDistribution {

        IntegerDistribution intDist;
        int intStart;

        public ReverseDistributionIntegerImpl(double start, IntegerDistribution dist) {
            super(start, dist);
            intDist = dist;
            intStart = (int) start;
        }

        @Override
        public double probability(int x) {
            return intDist.probability(intStart - x);
        }

        @Override
        public double cumulativeProbability(int x) throws MathException {
            return intDist.cumulativeProbability(intStart - x);
        }

        @Override
        public double cumulativeProbability(int x0, int x1) throws MathException {
            return intDist.cumulativeProbability(intStart - x0, (int) start - x1);
        }

        @Override
        public int inverseCumulativeProbability(double p) throws MathException {
            int x = intDist.inverseCumulativeProbability(1 - p);

            return intStart - x;
        }

        @Override
        public double probability(double x) {
            return intDist.probability(start - x);
        }
    }
}
